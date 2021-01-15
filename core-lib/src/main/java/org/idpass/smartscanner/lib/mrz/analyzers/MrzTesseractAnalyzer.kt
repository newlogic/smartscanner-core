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
package org.idpass.smartscanner.lib.mrz.analyzers

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.ImageProxy
import com.googlecode.tesseract.android.TessBaseAPI
import org.idpass.smartscanner.lib.SmartScannerActivity
import org.idpass.smartscanner.lib.mrz.MRZCleaner
import org.idpass.smartscanner.lib.platform.extension.toBitmap
import org.idpass.smartscanner.lib.platform.utils.FileUtils
import org.idpass.smartscanner.lib.scanner.SmartScannerException
import org.idpass.smartscanner.lib.scanner.config.Modes
import java.net.URLEncoder
import kotlin.concurrent.thread

class MrzTesseractAnalyzer(
    private val activity: Activity,
    private val intent : Intent,
    private val imageResultType: String,
    private val format: String?
) : MRZAnalyzer(activity, intent, imageResultType, format) {

    private lateinit var tessBaseAPI: TessBaseAPI

    fun initializeTesseract(context : Context) {
        val extDirPath: String = context.getExternalFilesDir(null)!!.absolutePath
        Log.d(SmartScannerActivity.TAG, "path: $extDirPath")
        FileUtils.copyAssets(context, "tessdata", extDirPath)
        tessBaseAPI = TessBaseAPI()
        tessBaseAPI.init(extDirPath, "ocrb_int", TessBaseAPI.OEM_DEFAULT)
        tessBaseAPI.pageSegMode = TessBaseAPI.PageSegMode.PSM_SINGLE_BLOCK
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val rot = imageProxy.imageInfo.rotationDegrees
            val bf = mediaImage.toBitmap(rot, Modes.BARCODE.value)
            val cropped = if (rot == 90 || rot == 270) Bitmap.createBitmap(
                bf,
                bf.width / 2,
                0,
                bf.width / 2,
                bf.height
            )
            else Bitmap.createBitmap(bf, 0, bf.height / 2, bf.width, bf.height / 2)
            Log.d(
                SmartScannerActivity.TAG,
                "Bitmap: (${mediaImage.width}, ${mediaImage.height} Cropped: (${cropped.width}, ${cropped.height}), Rotation: $rot"
            )
            if (::tessBaseAPI.isInitialized) {
                Log.d("${SmartScannerActivity.TAG}/SmartScanner", "MRZ Tesseract: start")
                val start = System.currentTimeMillis()
                thread(start = true) {
                    Log.d("${SmartScannerActivity.TAG}/SmartScanner", "MRZ Tesseract: process")
                    val rotation = imageProxy.imageInfo.rotationDegrees
                    val matrix = Matrix()
                    matrix.postRotate(rotation.toFloat())
                    val rotatedBitmap = Bitmap.createBitmap(
                        cropped,
                        0,
                        0,
                        cropped.width,
                        cropped.height,
                        matrix,
                        true
                    )
                    tessBaseAPI.setImage(rotatedBitmap)
                    val tessResult = tessBaseAPI.utF8Text
                    try {
                        Log.d(
                            "${SmartScannerActivity.TAG}/Tesseract",
                            "Before cleaner: [${
                                URLEncoder.encode(tessResult, "UTF-8")
                                    .replace("%3C", "<").replace("%0A", "↩")
                            }]"
                        )
                        val mrz = MRZCleaner.clean(tessResult)
                        Log.d(
                            "${SmartScannerActivity.TAG}/Tesseract",
                            "After cleaner = [${
                                URLEncoder.encode(mrz, "UTF-8")
                                    .replace("%3C", "<")
                                    .replace("%0A", "↩")
                            }]"
                        )
                        processResult(mrz = mrz, bitmap = bf, rotation = imageProxy.imageInfo.rotationDegrees)
                    } catch (e: Exception) { // MrzParseException, IllegalArgumentException
                        Log.d("${SmartScannerActivity.TAG}/Tesseract", e.toString())
                    }
                    imageProxy.close()
                }
            } else {
                imageProxy.close()
                throw SmartScannerException("Please initialize Tesseract properly using initializeTesseract() method.")
            }
        }
    }
}