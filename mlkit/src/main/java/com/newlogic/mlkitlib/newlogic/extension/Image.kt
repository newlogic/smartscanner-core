package com.newlogic.mlkitlib.newlogic.extension

import android.graphics.*
import android.media.Image
import android.util.Base64
import android.util.Log
import com.newlogic.mlkitlib.newlogic.MLKitActivity
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
        MLKitActivity.TAG,
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
fun Bitmap.decodeBase64(input: String): Bitmap? {
    val decodedBytes = Base64.decode(input, 0)
    return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
}

fun Bitmap.convertBase64String(): String? {
    val outputStream = ByteArrayOutputStream()
    this.compress(Bitmap.CompressFormat.JPEG, 50, outputStream)
    return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
}