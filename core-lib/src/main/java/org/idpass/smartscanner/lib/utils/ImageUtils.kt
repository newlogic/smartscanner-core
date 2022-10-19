/*
 * Copyright (C) 2020 Newlogic Pte. Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 *
 */
package org.idpass.smartscanner.lib.utils

import android.graphics.*
import android.media.Image
import org.jnbis.internal.WsqDecoder
import java.io.*


object ImageUtils {

    private val TAG = ImageUtils::class.java.simpleName

    var JPEG_MIME_TYPE = "image/jpeg"
    var JPEG2000_MIME_TYPE = "image/jp2"
    var JPEG2000_ALT_MIME_TYPE = "image/jpeg2000"
    var WSQ_MIME_TYPE = "image/x-wsq"

    fun imageToByteArray(image: Image): ByteArray? {
        var data: ByteArray? = null
        if (image.format == ImageFormat.JPEG) {
            val planes = image.planes
            val buffer = planes[0].buffer
            data = ByteArray(buffer.capacity())
            buffer.get(data)
            return data
        } else if (image.format == ImageFormat.YUV_420_888) {
            data = NV21toJPEG(
                    YUV_420_888toNV21(image),
                    image.width, image.height)
        }
        return data
    }

    fun YUV_420_888toNV21(image: Image): ByteArray {
        val nv21: ByteArray
        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        nv21 = ByteArray(ySize + uSize + vSize)

        //U and V are swapped
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        return nv21
    }

    private fun NV21toJPEG(nv21: ByteArray, width: Int, height: Int): ByteArray {
        val out = ByteArrayOutputStream()
        val yuv = YuvImage(nv21, ImageFormat.NV21, width, height, null)
        yuv.compressToJpeg(Rect(0, 0, width, height), 100, out)
        return out.toByteArray()
    }

    @Throws(IOException::class)
    fun decodeImage(inputStream: InputStream, imageLength: Int, mimeType: String): Bitmap {
        var inputStream = inputStream
        synchronized(inputStream) {
            val dataIn = DataInputStream(inputStream)
            val bytes = ByteArray(imageLength)
            dataIn.readFully(bytes)
            inputStream = ByteArrayInputStream(bytes)
        }
        if (JPEG2000_MIME_TYPE.equals(mimeType, ignoreCase = true) || JPEG2000_ALT_MIME_TYPE.equals(mimeType, ignoreCase = true)) {
            val bitmap = org.jmrtd.jj2000.JJ2000Decoder.decode(inputStream)
            return toAndroidBitmap(bitmap)
        } else if (WSQ_MIME_TYPE.equals(mimeType, ignoreCase = true)) {
            val wsqDecoder = WsqDecoder()
            val bitmap = wsqDecoder.decode(inputStream.readBytes())
            val byteData = bitmap.pixels
            val intData = IntArray(byteData.size)
            for (j in byteData.indices) {
                intData[j] = -0x1000000 or ((byteData[j].toInt() and 0xFF) shl 16) or ((byteData[j].toInt() and 0xFF) shl 8) or (byteData[j].toInt() and 0xFF)
            }
            return Bitmap.createBitmap(intData, 0, bitmap.width, bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        } else {
            return BitmapFactory.decodeStream(inputStream)
        }
    }

    fun rotateBitmap(source: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    private fun toAndroidBitmap(bitmap: org.jmrtd.jj2000.Bitmap): Bitmap {
        val intData = bitmap.pixels
        return Bitmap.createBitmap(intData, 0, bitmap.width, bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
    }
}
