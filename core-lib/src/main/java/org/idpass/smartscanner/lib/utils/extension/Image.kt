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
import kotlin.math.exp


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

// extension function to convert bitmap to grayscale
fun Bitmap.toGrayscale(): Bitmap? {
    val bitmap = copy(Bitmap.Config.ARGB_8888, true) ?: return null
    val paint = Paint()
    val matrix = ColorMatrix().apply { setSaturation(0f) }
    paint.colorFilter = ColorMatrixColorFilter(matrix)
    Canvas(bitmap).drawBitmap(this, 0f, 0f, paint)
    return bitmap
}

/**
 * Adaptive thresholding: converts to grayscale and binarizes using a local mean.
 * Each pixel is compared to the average intensity in its [blockSize] neighborhood.
 * If it is darker by more than [C], it becomes black; otherwise white.
 * This handles uneven lighting, shadows, and faded ink far better than global contrast.
 *
 * @param blockSize The size of the local neighborhood (must be odd, default 15)
 * @param C Constant subtracted from the local mean (higher = more black pixels, default 10)
 */
fun Bitmap.adaptiveThreshold(blockSize: Int = 15, C: Int = 10): Bitmap {
    val w = width
    val h = height
    val pixels = IntArray(w * h)
    getPixels(pixels, 0, w, 0, 0, w, h)

    // Step 1: Convert to grayscale values
    val gray = IntArray(w * h)
    for (i in pixels.indices) {
        val p = pixels[i]
        val r = (p shr 16) and 0xFF
        val g = (p shr 8) and 0xFF
        val b = p and 0xFF
        gray[i] = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
    }

    // Step 2: Compute integral image for fast local mean calculation
    val integral = LongArray(w * h)
    for (y in 0 until h) {
        var rowSum = 0L
        for (x in 0 until w) {
            rowSum += gray[y * w + x]
            integral[y * w + x] = rowSum + if (y > 0) integral[(y - 1) * w + x] else 0L
        }
    }

    // Step 3: For each pixel, compute local mean and threshold
    val half = blockSize / 2
    val result = IntArray(w * h)
    for (y in 0 until h) {
        for (x in 0 until w) {
            val x1 = (x - half - 1).coerceAtLeast(-1)
            val y1 = (y - half - 1).coerceAtLeast(-1)
            val x2 = (x + half).coerceAtMost(w - 1)
            val y2 = (y + half).coerceAtMost(h - 1)

            val count = (x2 - x1) * (y2 - y1)
            var sum = integral[y2 * w + x2]
            if (x1 >= 0) sum -= integral[y2 * w + x1]
            if (y1 >= 0) sum -= integral[y1 * w + x2]
            if (x1 >= 0 && y1 >= 0) sum += integral[y1 * w + x1]

            val mean = (sum / count).toInt()
            val value = if (gray[y * w + x] < mean - C) 0 else 255
            result[y * w + x] = (0xFF shl 24) or (value shl 16) or (value shl 8) or value
        }
    }

    val output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    output.setPixels(result, 0, w, 0, 0, w, h)
    return output
}

/**
 * Morphological dilation for black-on-white text: thickens dark strokes.
 * Takes the MIN brightness in the [radius] neighborhood (expands dark regions).
 */
fun Bitmap.dilate(radius: Int = 1): Bitmap {
    val w = width; val h = height
    val pixels = IntArray(w * h)
    getPixels(pixels, 0, w, 0, 0, w, h)
    val gray = IntArray(w * h)
    for (i in pixels.indices) {
        val p = pixels[i]
        gray[i] = ((p shr 16 and 0xFF) * 299 + (p shr 8 and 0xFF) * 587 + (p and 0xFF) * 114) / 1000
    }
    val result = IntArray(w * h)
    for (y in 0 until h) {
        for (x in 0 until w) {
            var minVal = 255
            for (dy in -radius..radius) {
                for (dx in -radius..radius) {
                    val nx = (x + dx).coerceIn(0, w - 1)
                    val ny = (y + dy).coerceIn(0, h - 1)
                    minVal = minOf(minVal, gray[ny * w + nx])
                }
            }
            result[y * w + x] = (0xFF shl 24) or (minVal shl 16) or (minVal shl 8) or minVal
        }
    }
    val output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    output.setPixels(result, 0, w, 0, 0, w, h)
    return output
}

/**
 * Morphological erosion for black-on-white text: thins/sharpens dark strokes.
 * Takes the MAX brightness in the [radius] neighborhood (shrinks dark regions).
 */
fun Bitmap.erode(radius: Int = 1): Bitmap {
    val w = width; val h = height
    val pixels = IntArray(w * h)
    getPixels(pixels, 0, w, 0, 0, w, h)
    val gray = IntArray(w * h)
    for (i in pixels.indices) {
        val p = pixels[i]
        gray[i] = ((p shr 16 and 0xFF) * 299 + (p shr 8 and 0xFF) * 587 + (p and 0xFF) * 114) / 1000
    }
    val result = IntArray(w * h)
    for (y in 0 until h) {
        for (x in 0 until w) {
            var maxVal = 0
            for (dy in -radius..radius) {
                for (dx in -radius..radius) {
                    val nx = (x + dx).coerceIn(0, w - 1)
                    val ny = (y + dy).coerceIn(0, h - 1)
                    maxVal = maxOf(maxVal, gray[ny * w + nx])
                }
            }
            result[y * w + x] = (0xFF shl 24) or (maxVal shl 16) or (maxVal shl 8) or maxVal
        }
    }
    val output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    output.setPixels(result, 0, w, 0, 0, w, h)
    return output
}

/**
 * Gaussian blur using separable 1D passes. Reduces noise before thresholding.
 * @param radius Blur radius (default 2 = 5x5 kernel)
 */
fun Bitmap.gaussianBlur(radius: Int = 2): Bitmap {
    val w = width; val h = height
    val pixels = IntArray(w * h)
    getPixels(pixels, 0, w, 0, 0, w, h)
    val gray = IntArray(w * h)
    for (i in pixels.indices) {
        val p = pixels[i]
        gray[i] = ((p shr 16 and 0xFF) * 299 + (p shr 8 and 0xFF) * 587 + (p and 0xFF) * 114) / 1000
    }
    // Build 1D Gaussian kernel
    val size = 2 * radius + 1
    val kernel = FloatArray(size)
    val sigma = radius / 2.0f
    var sum = 0f
    for (i in 0 until size) {
        val x = (i - radius).toFloat()
        kernel[i] = exp(-(x * x) / (2 * sigma * sigma))
        sum += kernel[i]
    }
    for (i in 0 until size) kernel[i] /= sum
    // Horizontal pass
    val temp = IntArray(w * h)
    for (y in 0 until h) {
        for (x in 0 until w) {
            var acc = 0f
            for (k in 0 until size) {
                val nx = (x + k - radius).coerceIn(0, w - 1)
                acc += gray[y * w + nx] * kernel[k]
            }
            temp[y * w + x] = acc.toInt().coerceIn(0, 255)
        }
    }
    // Vertical pass
    val blurred = IntArray(w * h)
    for (y in 0 until h) {
        for (x in 0 until w) {
            var acc = 0f
            for (k in 0 until size) {
                val ny = (y + k - radius).coerceIn(0, h - 1)
                acc += temp[ny * w + x] * kernel[k]
            }
            val v = acc.toInt().coerceIn(0, 255)
            blurred[y * w + x] = (0xFF shl 24) or (v shl 16) or (v shl 8) or v
        }
    }
    val output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    output.setPixels(blurred, 0, w, 0, 0, w, h)
    return output
}

/**
 * Otsu's global thresholding: auto-finds optimal binarization threshold by
 * minimizing intra-class variance. Best for bimodal histograms (clear text vs background).
 */
fun Bitmap.globalThreshold(): Bitmap {
    val w = width; val h = height
    val pixels = IntArray(w * h)
    getPixels(pixels, 0, w, 0, 0, w, h)
    val gray = IntArray(w * h)
    for (i in pixels.indices) {
        val p = pixels[i]
        gray[i] = ((p shr 16 and 0xFF) * 299 + (p shr 8 and 0xFF) * 587 + (p and 0xFF) * 114) / 1000
    }
    // Build histogram
    val histogram = IntArray(256)
    for (v in gray) histogram[v]++
    // Otsu's method
    val total = w * h
    var sumAll = 0.0
    for (i in 0..255) sumAll += i * histogram[i]
    var sumB = 0.0; var wB = 0; var maxVariance = 0.0; var bestThreshold = 0
    for (t in 0..255) {
        wB += histogram[t]
        if (wB == 0) continue
        val wF = total - wB
        if (wF == 0) break
        sumB += t * histogram[t]
        val meanB = sumB / wB
        val meanF = (sumAll - sumB) / wF
        val variance = wB.toDouble() * wF.toDouble() * (meanB - meanF) * (meanB - meanF)
        if (variance > maxVariance) { maxVariance = variance; bestThreshold = t }
    }
    val result = IntArray(w * h)
    for (i in gray.indices) {
        val v = if (gray[i] < bestThreshold) 0 else 255
        result[i] = (0xFF shl 24) or (v shl 16) or (v shl 8) or v
    }
    val output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    output.setPixels(result, 0, w, 0, 0, w, h)
    return output
}

/**
 * Sharpening via unsharp mask: enhances edges by amplifying difference from neighbors.
 * Makes text edges crisper for OCR.
 * @param amount Sharpening strength (default 1.5)
 */
fun Bitmap.sharpen(amount: Float = 1.5f): Bitmap {
    val w = width; val h = height
    val pixels = IntArray(w * h)
    getPixels(pixels, 0, w, 0, 0, w, h)
    val result = IntArray(w * h)
    for (y in 0 until h) {
        for (x in 0 until w) {
            val center = pixels[y * w + x]
            val cr = ((center shr 16) and 0xFF).toFloat()
            val cg = ((center shr 8) and 0xFF).toFloat()
            val cb = (center and 0xFF).toFloat()
            var nrSum = 0f; var ngSum = 0f; var nbSum = 0f
            val offsets = arrayOf(intArrayOf(0, -1), intArrayOf(0, 1), intArrayOf(-1, 0), intArrayOf(1, 0))
            for (off in offsets) {
                val nx = (x + off[0]).coerceIn(0, w - 1)
                val ny = (y + off[1]).coerceIn(0, h - 1)
                val np = pixels[ny * w + nx]
                nrSum += ((np shr 16) and 0xFF).toFloat()
                ngSum += ((np shr 8) and 0xFF).toFloat()
                nbSum += (np and 0xFF).toFloat()
            }
            val rr = (cr + amount * (cr - nrSum / 4f)).coerceIn(0f, 255f).toInt()
            val rg = (cg + amount * (cg - ngSum / 4f)).coerceIn(0f, 255f).toInt()
            val rb = (cb + amount * (cb - nbSum / 4f)).coerceIn(0f, 255f).toInt()
            result[y * w + x] = (0xFF shl 24) or (rr shl 16) or (rg shl 8) or rb
        }
    }
    val output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    output.setPixels(result, 0, w, 0, 0, w, h)
    return output
}

/**
 * CLAHE (Contrast Limited Adaptive Histogram Equalization).
 * Enhances local contrast by equalizing histograms within tiles, with a clip limit
 * to prevent over-amplification of noise. Uses bilinear interpolation between tiles
 * for smooth transitions.
 * @param tileGridSize Number of tiles in each dimension (default 8 → 8×8 grid)
 * @param clipLimit Contrast limit factor (default 2.0)
 */
fun Bitmap.clahe(tileGridSize: Int = 8, clipLimit: Float = 2.0f): Bitmap {
    val w = width; val h = height
    val pixels = IntArray(w * h)
    getPixels(pixels, 0, w, 0, 0, w, h)

    // Convert to grayscale
    val gray = IntArray(w * h)
    for (i in pixels.indices) {
        val p = pixels[i]
        val r = (p shr 16) and 0xFF
        val g = (p shr 8) and 0xFF
        val b = p and 0xFF
        gray[i] = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
    }

    val tilesX = tileGridSize
    val tilesY = tileGridSize
    val tileW = w.toFloat() / tilesX
    val tileH = h.toFloat() / tilesY

    // Compute clipped histogram + CDF lookup for each tile
    val tileLuts = Array(tilesY) { ty -> Array(tilesX) { tx ->
        val startX = (tx * tileW).toInt()
        val startY = (ty * tileH).toInt()
        val endX = kotlin.math.min(((tx + 1) * tileW).toInt(), w)
        val endY = kotlin.math.min(((ty + 1) * tileH).toInt(), h)
        val count = (endX - startX) * (endY - startY)

        // Build histogram
        val hist = IntArray(256)
        for (y in startY until endY) {
            for (x in startX until endX) {
                hist[gray[y * w + x]]++
            }
        }

        // Clip histogram and redistribute excess
        val limit = (clipLimit * count / 256).toInt().coerceAtLeast(1)
        var excess = 0
        for (i in 0 until 256) {
            if (hist[i] > limit) { excess += hist[i] - limit; hist[i] = limit }
        }
        val inc = excess / 256
        val rem = excess % 256
        for (i in 0 until 256) { hist[i] += inc; if (i < rem) hist[i]++ }

        // Build CDF lookup table
        val lut = IntArray(256)
        var cdf = 0
        for (i in 0 until 256) {
            cdf += hist[i]
            lut[i] = (cdf * 255.0 / count).toInt().coerceIn(0, 255)
        }
        lut
    }}

    // Apply with bilinear interpolation between tile centers
    val result = IntArray(w * h)
    for (y in 0 until h) {
        for (x in 0 until w) {
            val gv = gray[y * w + x]
            val txf = (x / tileW) - 0.5f
            val tyf = (y / tileH) - 0.5f
            val tx0 = txf.toInt().coerceIn(0, tilesX - 1)
            val ty0 = tyf.toInt().coerceIn(0, tilesY - 1)
            val tx1 = (tx0 + 1).coerceAtMost(tilesX - 1)
            val ty1 = (ty0 + 1).coerceAtMost(tilesY - 1)
            val fx = (txf - tx0).coerceIn(0f, 1f)
            val fy = (tyf - ty0).coerceIn(0f, 1f)

            val v00 = tileLuts[ty0][tx0][gv].toFloat()
            val v10 = tileLuts[ty0][tx1][gv].toFloat()
            val v01 = tileLuts[ty1][tx0][gv].toFloat()
            val v11 = tileLuts[ty1][tx1][gv].toFloat()
            val top = v00 + (v10 - v00) * fx
            val bot = v01 + (v11 - v01) * fx
            val value = (top + (bot - top) * fy).toInt().coerceIn(0, 255)
            result[y * w + x] = (0xFF shl 24) or (value shl 16) or (value shl 8) or value
        }
    }

    val output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    output.setPixels(result, 0, w, 0, 0, w, h)
    return output
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

/**
*Checks if the image quality is within the blur threshold.
*This will help with blurry images that will yield inaccurate results.
*/
fun Bitmap.isImageBlur(threshold: Double) : Boolean {
    val width = this.width
    val height = this.height
    val pixels = IntArray(width * height)
    this.getPixels(pixels, 0, width, 0, 0, width, height)

    var sumVariance = 0.0
    for (x in 0 until width) {
        for (y in 0 until height) {
            val pixel = pixels[y * width + x]
            val red = (pixel shr 16) and 0xFF
            val green = (pixel shr 8) and 0xFF
            val blue = pixel and 0xFF

            val gray = (0.2989 * red + 0.5870 * green + 0.1140 * blue).toInt()
            sumVariance += gray * gray.toDouble()
        }
    }

    val averageVariance = sumVariance / (width * height)
    val blurThreshold = threshold * threshold  // Adjust the threshold as needed

    return averageVariance < blurThreshold
}
