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
package org.idpass.smartscanner.lib.mrz

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import androidx.camera.core.ImageProxy
import com.google.gson.Gson
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.googlecode.tesseract.android.TessBaseAPI
import org.idpass.smartscanner.api.ScannerConstants
import org.idpass.smartscanner.lib.R
import org.idpass.smartscanner.lib.SmartScannerActivity
import org.idpass.smartscanner.lib.platform.BaseImageAnalyzer
import org.idpass.smartscanner.lib.platform.extension.*
import org.idpass.smartscanner.lib.platform.utils.FileUtils
import org.idpass.smartscanner.lib.scanner.SmartScannerException
import org.idpass.smartscanner.lib.scanner.config.ImageResultType
import org.idpass.smartscanner.lib.scanner.config.Modes
import org.idpass.smartscanner.lib.scanner.config.MrzFormat
import java.net.URLEncoder
import kotlin.concurrent.thread

open class MRZAnalyzer(
        override val activity: Activity,
        override val intent: Intent,
        override val mode: String = Modes.MRZ.value,
        private val isMLKit: Boolean,
        private val imageResultType: String,
        private val format: String?,
        private val analyzeStart: Long,
        private val onConnectSuccess: (String) -> Unit,
        private val onConnectFail: (String) -> Unit
) : BaseImageAnalyzer() {

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
            val bf = mediaImage.toBitmap(rot, mode)
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

            if (isMLKit) {
                // Pass image to an ML Kit Vision API
                Log.d("${SmartScannerActivity.TAG}/SmartScanner", "MRZ MLKit: start")
                val start = System.currentTimeMillis()
                val rotation = imageProxy.imageInfo.rotationDegrees
                val image = InputImage.fromBitmap(cropped, rotation)
                val recognizer = TextRecognition.getClient()
                Log.d("${SmartScannerActivity.TAG}/SmartScanner", "MRZ MLKit TextRecognition: process")
                recognizer.process(image)
                        .addOnSuccessListener { visionText ->
                            onConnectSuccess.invoke(activity.getString(R.string.model_text_loaded))
                            val timeRequired = System.currentTimeMillis() - start
                            Log.d(
                                    "${SmartScannerActivity.TAG}/SmartScanner",
                                    "MRZ MLKit TextRecognition: success: $timeRequired ms"
                            )
                            var rawFullRead = ""
                            val blocks = visionText.textBlocks
                            for (i in blocks.indices) {
                                val lines = blocks[i].lines
                                for (j in lines.indices) {
                                    if (lines[j].text.contains('<')) {
                                        rawFullRead += lines[j].text + "\n"
                                    }
                                }
                            }
                            try {
                                Log.d(
                                        "${SmartScannerActivity.TAG}/SmartScanner",
                                        "Before cleaner: [${
                                            URLEncoder.encode(rawFullRead, "UTF-8")
                                                    .replace("%3C", "<").replace("%0A", "↩")
                                        }]"
                                )
                                val cleanMRZ = MRZCleaner.clean(rawFullRead)
                                Log.d(
                                        "${SmartScannerActivity.TAG}/SmartScanner",
                                        "After cleaner = [${
                                            URLEncoder.encode(cleanMRZ, "UTF-8")
                                                    .replace("%3C", "<").replace("%0A", "↩")
                                        }]"
                                )
                                processResult(result = cleanMRZ, bitmap = bf, rotation = rotation)
                            } catch (e: Exception) {
                                Log.d("${SmartScannerActivity.TAG}/SmartScanner", e.toString())
                            }
                            imageProxy.close()
                        }
                        .addOnFailureListener { e ->
                            e.printStackTrace()
                            val timeElapsed = (System.currentTimeMillis() - analyzeStart).toDouble() / 1000
                            Log.d(
                                    "${SmartScannerActivity.TAG}/SmartScanner",
                                    "MRZ MLKit TextRecognition: failure: ${e.message}"
                            )
                            val connectionId = if (activity.getConnectionType() == 0) {
                                R.string.connection_text
                            }
                            else {
                                when (timeElapsed) {
                                    in 0.0..10.0 -> R.string.model_text_waiting // 0-10 secs msg
                                    in 10.0..60.0 -> R.string.model_text // 10-60 secs msg
                                    in 60.0..180.0-> R.string.model_text_download // 60-180 secs msg
                                    in 180.0..300.0-> R.string.model_text_process // 180-300 secs msg
                                    else -> R.string.model_text_process_wait
                                }
                            }
                            onConnectFail.invoke(activity.getString(connectionId))
                            imageProxy.close()
                        }
            } else {
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
                        val timeRequired = System.currentTimeMillis() - start
                        Log.d(
                            "${SmartScannerActivity.TAG}/SmartScanner",
                            "MRZ Tesseract result: success: $timeRequired ms"
                        )
                        try {
                            Log.d(
                                    "${SmartScannerActivity.TAG}/Tesseract",
                                    "Before cleaner: [${
                                        URLEncoder.encode(tessResult, "UTF-8")
                                                .replace("%3C", "<").replace("%0A", "↩")
                                    }]"
                            )
                            val cleanMRZ = MRZCleaner.clean(tessResult)
                            Log.d(
                                    "${SmartScannerActivity.TAG}/Tesseract",
                                    "After cleaner = [${
                                        URLEncoder.encode(cleanMRZ, "UTF-8")
                                                .replace("%3C", "<")
                                                .replace("%0A", "↩")
                                    }]"
                            )
                            processResult(result = cleanMRZ, bitmap = bf, rotation = imageProxy.imageInfo.rotationDegrees)
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

    internal open fun processResult(result: String, bitmap: Bitmap, rotation: Int) {
        val imagePathFile = activity.cacheImagePath()
        bitmap.cacheImageToLocal(imagePathFile, rotation)
        val imageString = if (imageResultType == ImageResultType.BASE_64.value) bitmap.encodeBase64(rotation) else imagePathFile
        val mrz = when (format) {
            MrzFormat.MRTD_TD1.value -> MRZResult.formatMrtdTd1Result(MRZCleaner.parseAndCleanMrtdTd1(result), imageString)
            else -> MRZResult.formatMrzResult(MRZCleaner.parseAndClean(result), imageString)
        }
        if (intent.action == ScannerConstants.IDPASS_SMARTSCANNER_MRZ_INTENT ||
            intent.action == ScannerConstants.IDPASS_SMARTSCANNER_ODK_MRZ_INTENT) {
            sendBundleResult(mrzResult = mrz)
        } else {
            val jsonString = Gson().toJson(mrz)
            sendAnalyzerResult(result = jsonString)
        }
    }

    private fun sendAnalyzerResult(result: String? = null) {
        val data = Intent()
        Log.d(SmartScannerActivity.TAG, "Success from MRZ")
        Log.d(SmartScannerActivity.TAG, "value: $result")
        data.putExtra(SmartScannerActivity.SCANNER_RESULT, result)
        activity.setResult(Activity.RESULT_OK, data)
        activity.finish()
    }

    private fun sendBundleResult(mrzResult: MRZResult? = null) {
        val bundle = Bundle()
        Log.d(SmartScannerActivity.TAG, "Success from MRZ")
        mrzResult?.let { result ->
            if (intent.action == ScannerConstants.IDPASS_SMARTSCANNER_ODK_MRZ_INTENT) {
                bundle.putString(ScannerConstants.IDPASS_ODK_INTENT_DATA, result.documentNumber)
            }
            // TODO implement proper image passing
            //  bundle.putString(ScannerConstants.MRZ_IMAGE, result.image)
            bundle.putString(ScannerConstants.MRZ_CODE, result.code)
            bundle.putShort(ScannerConstants.MRZ_CODE_1, result.code1 ?: -1)
            bundle.putShort(ScannerConstants.MRZ_CODE_2, result.code2 ?: -1)
            bundle.putString(ScannerConstants.MRZ_DATE_OF_BIRTH, result.dateOfBirth)
            bundle.putString(ScannerConstants.MRZ_DOCUMENT_NUMBER, result.documentNumber)
            bundle.putString(ScannerConstants.MRZ_EXPIRY_DATE, result.expirationDate)
            bundle.putString(ScannerConstants.MRZ_FORMAT, result.format)
            bundle.putString(ScannerConstants.MRZ_GIVEN_NAMES, result.givenNames)
            bundle.putString(ScannerConstants.MRZ_SURNAME, result.surname)
            bundle.putString(ScannerConstants.MRZ_ISSUING_COUNTRY, result.issuingCountry)
            bundle.putString(ScannerConstants.MRZ_NATIONALITY, result.nationality)
            bundle.putString(ScannerConstants.MRZ_SEX, result.sex)
            bundle.putString(ScannerConstants.MRZ_RAW, result.mrz)
        }
        bundle.putString(ScannerConstants.MODE, mode)

        val result = Intent()
        val prefix = if (intent.hasExtra(ScannerConstants.IDPASS_ODK_PREFIX_EXTRA)) {
            intent.getStringExtra(ScannerConstants.IDPASS_ODK_PREFIX_EXTRA)
        } else { "" }
        result.putExtra(ScannerConstants.RESULT, bundle)
        // Copy all the values in the intent result to be compatible with other implementations than commcare
        for (key in bundle.keySet()) {
            result.putExtra(prefix + key, bundle.getString(key))
        }
        activity.setResult(Activity.RESULT_OK, result)
        activity.finish()
    }
}