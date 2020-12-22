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
package org.idpass.smartscanner.lib.extension

import android.graphics.*
import android.media.Image
import android.util.Base64
import android.util.Log
import org.idpass.smartscanner.lib.SmartScannerActivity
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream


fun Image.toBitmap(rotation: Int = 0): Bitmap {
    val yBuffer = planes[0].buffer // Y
    val uBuffer = planes[1].buffer // U
    val vBuffer = planes[2].buffer // V

    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()

    val nv21 = ByteArray(ySize + uSize + vSize)

    // U and V are swapped
    yBuffer.get(nv21, 0, ySize)
    vBuffer.get(nv21, ySize, vSize)
    uBuffer.get(nv21, ySize + vSize, uSize)

    val yuvImage = YuvImage(nv21, ImageFormat.NV21, this.width, this.height, null)
    val out = ByteArrayOutputStream()

    val rect =  Rect()
    if (rotation == 90 || rotation == 270) {
        rect.left = this.width / 4
        rect.top = 0
        rect.right = this.width - rect.left
        rect.bottom = this.height
    } else {
        rect.left = 0
        rect.top = this.height / 4
        rect.right = this.width
        rect.bottom =  this.height - rect.top
    }

    Log.d(
        SmartScannerActivity.TAG,
        "Image ${this.width}x${this.height}, crop to: ${rect.left},${rect.top},${rect.right},${rect.bottom}"
    )

    yuvImage.compressToJpeg(rect, 100, out) // Ugly but it works
    //yuvImage.compressToJpeg(Rect(270, 20, 370, 460), 100, out)
    val imageBytes = out.toByteArray()
    return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
}

fun Bitmap.cacheImageToLocal(localPath: String, rotation: Int = 0, quality: Int = 100) {
    val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
    val b = Bitmap.createBitmap(this, 0, 0, this.width, this.height, matrix, true)
    val file = File(localPath)
    file.createNewFile()
    val ostream = FileOutputStream(file)
    b.compress(Bitmap.CompressFormat.JPEG, quality, ostream)
    ostream.flush()
    ostream.close()
}

fun Bitmap.resizeBitmap( newWidth: Int, newHeight: Int): Bitmap? {
    val width = this.width
    val height = this.height
    val scaleWidth = newWidth.toFloat() / width
    val scaleHeight = newHeight.toFloat() / height
    // CREATE A MATRIX FOR THE MANIPULATION
    val matrix = Matrix()
    // RESIZE THE BIT MAP
    matrix.postScale(scaleWidth, scaleHeight)

    // "RECREATE" THE NEW BITMAP
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, false)
}

fun String.decodeBase64(): Bitmap? {
    val decodedBytes = Base64.decode(this, 0)
    return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
}

fun Bitmap.encodeBase64(rotation: Int = 0): String? {
    val outputStream = ByteArrayOutputStream()
    val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
    val b = Bitmap.createBitmap(this, 0, 0, this.width, this.height, matrix, true)
    b.compress(Bitmap.CompressFormat.JPEG, 50, outputStream)
    return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
}

fun Bitmap.rotate(rotation: Int = 0): Bitmap {
    val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
    return Bitmap.createBitmap(this, 0, 0, this.width, this.height, matrix, true)
}

fun String.toBitmap() : Bitmap =  BitmapFactory.decodeFile(this)