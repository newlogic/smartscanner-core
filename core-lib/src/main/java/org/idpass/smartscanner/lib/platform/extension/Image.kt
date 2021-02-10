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
package org.idpass.smartscanner.lib.platform.extension

import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.media.Image
import android.util.Base64
import android.util.Log
import org.idpass.smartscanner.lib.SmartScannerActivity
import org.idpass.smartscanner.lib.scanner.config.Modes
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*


val Float.px: Float get() = (this * Resources.getSystem().displayMetrics.density)
val Int.px: Int get() = ((this * Resources.getSystem().displayMetrics.density).toInt())

fun Image.toBitmap(rotation: Int = 0, mode: String?): Bitmap {
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
    // Use higher value of 6 for barcode/qrcode/idpass-lite which fixes bounding box issues upon scanning,
    // and mrz uses previous default value of 4
    val scaleIdentifier = if (mode == Modes.BARCODE.value ||
                              mode == Modes.IDPASS_LITE.value ||
                              mode == Modes.QRCODE.value ) 6 else 4
    if (rotation == 90 || rotation == 270) {
        rect.left = this.width / scaleIdentifier
        rect.top = 0
        rect.right = this.width - rect.left
        rect.bottom = this.height
    } else {
        rect.left = 0
        rect.top = this.height / scaleIdentifier
        rect.right = this.width
        rect.bottom =  this.height - rect.top
    }

    Log.d(
        SmartScannerActivity.TAG,
        "Image ${this.width}x${this.height}, crop to: ${rect.left},${rect.top},${rect.right},${rect.bottom}"
    )

    yuvImage.compressToJpeg(rect, 100, out) // Ugly but it works
    val imageBytes = out.toByteArray()
    return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
}

fun Bitmap.cacheImageToLocal(localPath: String, rotation: Int = 0, quality: Int = 100) {
    val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
    val b = Bitmap.createBitmap(this, 0, 0, this.width, this.height, matrix, true)
    val file = File(localPath)
    file.createNewFile()
    if (file.exists()) {
        val ostream = FileOutputStream(file)
        b.compress(Bitmap.CompressFormat.JPEG, quality, ostream)
        ostream.flush()
        ostream.close()
    }
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

fun Context.cacheImagePath(identifier : String = "Scanner") : String {
    val date = Calendar.getInstance().time
    val formatter = SimpleDateFormat("yyyyMMddHHmmss", Locale.ROOT)
    val currentDateTime = formatter.format(date)
    return "${this.cacheDir}/$identifier-$currentDateTime.jpg"
}