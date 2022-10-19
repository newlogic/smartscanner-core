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
package org.idpass.smartscanner.lib.utils.extension

import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.util.Base64
import android.util.Base64OutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*


val Float.px: Float get() = (this * Resources.getSystem().displayMetrics.density)
val Int.px: Int get() = ((this * Resources.getSystem().displayMetrics.density).toInt())

// extension function to change bitmap contrast
fun Bitmap.setContrast(
    contrast: Float = 1.0F
): Bitmap? {
    val bitmap = copy(Bitmap.Config.ARGB_8888, true)
    val paint = Paint()

    // contrast 0..2, 1 is default
    // you may tweak the range
    val matrix = ColorMatrix(
        floatArrayOf(
            contrast, 0f, 0f, 0f, 0f,
            0f, contrast, 0f, 0f, 0f,
            0f, 0f, contrast, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    )

    val filter = ColorMatrixColorFilter(matrix)
    paint.colorFilter = filter

    Canvas(bitmap).drawBitmap(this, 0f, 0f, paint)
    return bitmap
}

// extension function to change bitmap brightness
fun Bitmap.setBrightness(
    brightness: Float = 0.0F
): Bitmap? {
    val bitmap = copy(Bitmap.Config.ARGB_8888, true)
    val paint = Paint()

    // brightness -200..200, 0 is default
    // you may tweak the range
    val matrix = ColorMatrix(
        floatArrayOf(
            1.0F, 0f, 0f, 0f, brightness,
            0f, 1.0F, 0f, 0f, brightness,
            0f, 0f, 1.0F, 0f, brightness,
            0f, 0f, 0f, 1f, 0f
        )
    )

    val filter = ColorMatrixColorFilter(matrix)
    paint.colorFilter = filter

    Canvas(bitmap).drawBitmap(this, 0f, 0f, paint)
    return bitmap
}

fun Bitmap.cacheImageToLocal(localPath: String, rotation: Int = 0, quality: Int = 80) {
    val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
    val b = Bitmap.createBitmap(this, 0, 0, this.width, this.height, matrix, true)
    val file = File(localPath)
    file.createNewFile()
    val ostream = FileOutputStream(file)
    try {
        b.compress(Bitmap.CompressFormat.JPEG, quality, ostream)
        ostream.close()
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        ostream.flush()
        ostream.close()
    }
}

fun Bitmap.resize(newWidth: Int, newHeight: Int): Bitmap? {
    val scaledBitmap = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888)
    val ratioX = newWidth / this.width.toFloat()
    val ratioY = newHeight / this.height.toFloat()
    val scaleMatrix = Matrix()
    scaleMatrix.setScale(ratioX, ratioY, 0f, 0f)
    val canvas = Canvas(scaledBitmap)
    canvas.setMatrix(scaleMatrix)
    val paint = Paint(Paint.FILTER_BITMAP_FLAG)
    canvas.drawBitmap(this, 0f, 0f, paint)
    return scaledBitmap
}

fun String.decodeBase64(): Bitmap? {
    val decodedBytes = Base64.decode(this, 0)
    return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
}

fun Bitmap.encodeBase64(rotation: Int = 0): String? {
    val outputStream = ByteArrayOutputStream()
    val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
    val b = Bitmap.createBitmap(this, 0, 0, this.width, this.height, matrix, true)
    val canvas = Canvas(b)
    canvas.drawBitmap(b, 0f, 0f, null)
    b.compress(Bitmap.CompressFormat.JPEG, 50, outputStream)
    return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
}

fun File.encodeBase64(): String {
    return FileInputStream(this).use { inputStream ->
        ByteArrayOutputStream().use { outputStream ->
            Base64OutputStream(outputStream, Base64.DEFAULT).use { base64FilterStream ->
                inputStream.copyTo(base64FilterStream)
                base64FilterStream.close()
                outputStream.toString()
            }
        }
    }
}

fun Bitmap.rotate(rotation: Int = 0): Bitmap {
    val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
    return Bitmap.createBitmap(this, 0, 0, this.width, this.height, matrix, true)
}

fun String.toBitmap(): Bitmap = BitmapFactory.decodeFile(this)

fun Context.cacheImagePath(identifier: String = "Scanner"): String {
    val date = Calendar.getInstance().time
    val formatter = SimpleDateFormat("yyyyMMddHHmmss", Locale.ROOT)
    val currentDateTime = formatter.format(date)
    return "${this.cacheDir}/$identifier-$currentDateTime.jpg"
}

fun Bitmap.cropCenter() : Bitmap {
    return if (this.width >= this.height){
       Bitmap.createBitmap(
            this,
           this.width /2 - this.height /2,
            0,
           this.height,
           this.height
        )

    }else{

        Bitmap.createBitmap(
            this,
            0,
            this.height /2 - this.width /2,
            this.width,
            this.width
        )
    }
}
