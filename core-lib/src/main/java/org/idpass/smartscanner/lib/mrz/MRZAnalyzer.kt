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
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.camera.core.ImageProxy
import com.google.gson.Gson
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import org.idpass.smartscanner.api.ScannerConstants
import org.idpass.smartscanner.lib.R
import org.idpass.smartscanner.lib.SmartScannerActivity
import org.idpass.smartscanner.lib.scanner.BaseImageAnalyzer
import org.idpass.smartscanner.lib.scanner.config.ImageResultType
import org.idpass.smartscanner.lib.scanner.config.Modes
import org.idpass.smartscanner.lib.scanner.config.MrzFormat
import org.idpass.smartscanner.lib.utils.BitmapUtils
import org.idpass.smartscanner.lib.utils.extension.*
import java.io.File
import java.net.URLEncoder

open class MRZAnalyzer(
    override val activity: Activity,
    override val intent: Intent,
    override val mode: String = Modes.MRZ.value,
    private val language: String? = null,
    private val label: String? = null,
    private val locale: String? = null,
    private val withPhoto: Boolean? = null,
    private val withMrzPhoto: Boolean? = null,
    private val captureLog: Boolean? = null,
    private val enableLogging: Boolean? = null,
    private val isMLKit: Boolean,
    private val imageResultType: String,
    private val format: String?,
    private val analyzeStart: Long,
    private val isShowGuide: Boolean? = false
) : BaseImageAnalyzer() {

    @SuppressLint("UnsafeExperimentalUsageError", "UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val bitmap = BitmapUtils.getBitmap(imageProxy)
        bitmap?.let { bf ->
            val rotation = imageProxy.imageInfo.rotationDegrees
            bf.apply {
                // Increase brightness and contrast for clearer image to be processed
                setContrast(1.1F)
                setBrightness(3F)
            }

            val rectGuide = activity.findViewById<ImageView>(R.id.rect_guide)
            val viewFinder = activity.findViewById<View>(R.id.view_finder)
            var inputBitmap = bf
            var inputRot = rotation
            var rotatedBF = BitmapUtils.rotateImage(bf, rotation)

            if (isShowGuide != null && isShowGuide) {
                // try to cropped forcefully

                // Crop preview area
                val cropHeight = if (rotatedBF.width < viewFinder.width) {
                    // if preview area larger than analysing image
                    val koeff = rotatedBF.width.toFloat() / viewFinder!!.width.toFloat()
                    viewFinder.height.toFloat() * koeff
                } else {
                    // if preview area smaller than analysing image
                    val prc = 100 - (viewFinder.width.toFloat() / (rotatedBF.width.toFloat() / 100f))
                    viewFinder.height + ((viewFinder.height.toFloat() / 100f) * prc)
                }
                val cropTop = (rotatedBF.height / 2) - (cropHeight / 2)
                rotatedBF = Bitmap.createBitmap(rotatedBF, 0, cropTop.toInt(), rotatedBF.width, cropHeight.toInt())

                // Crop MRZ area
                val ratio = rotatedBF.width.toFloat() / viewFinder.width.toFloat()
                val x = (25 - 16).toPx * ratio
                val y = (viewFinder.height - 30.toPx - rectGuide.height) * ratio
                val width = rectGuide.width * ratio
                val height = rectGuide.height * ratio
                inputBitmap = Bitmap.createBitmap(rotatedBF, x.toInt(), y.toInt(), width.toInt(), height.toInt())
                inputRot = 0
            }


            // Pass image to an ML Kit Vision API
            Log.d("${SmartScannerActivity.TAG}/SmartScanner", "MRZ MLKit: start")
            val start = System.currentTimeMillis()
            val image = InputImage.fromBitmap(inputBitmap, inputRot)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            Log.d("${SmartScannerActivity.TAG}/SmartScanner", "MRZ MLKit TextRecognition: process")

            recognizer.process(image)

                .addOnSuccessListener { visionText ->

//                    rectGuide.setImageBitmap(cropped);

//                    Log.d(
//                        "${SmartScannerActivity.TAG}/SmartScanner",
//                        "rect imagveview box ${rectGuide.width} , ${rectGuide.height}",
//                    )

                    val timeRequired = System.currentTimeMillis() - start
                    Log.d(
                        "${SmartScannerActivity.TAG}/SmartScanner",
                        "MRZ MLKit TextRecognition: success: $timeRequired ms"
                    )
                    var rawFullRead = ""
                    val blocks = visionText.textBlocks

//                    val boxes = ArrayList<MRZBox>()
//                    val bdParent = activity.findViewById<RelativeLayout>(R.id.rect_bounding_layout)
//
//                    if (bdParent.childCount > 1) {
//                        bdParent.removeAllViews()
//                    }

                    for (i in blocks.indices) {
                        val lines = blocks[i].lines
                        for (j in lines.indices) {
                            if (lines[j].text.contains('<')) {
                                rawFullRead += lines[j].text + "\n"

                                // get boundingBox here
//                                blocks[i].boundingBox?.let { MRZBox(it) }?.let { boxes.add(it) }

//                                val element = blocks[i].boundingBox?.let { BoundingBoxDraw(activity, it) }

//                                bdParent.addView(element)

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


//                        val tsLong = System.currentTimeMillis() / 1000
//                        val ts = tsLong.toString()

//                        BitmapUtils.saveImage(rotatedBF, "mrz-${ts}-original.png")
//                        BitmapUtils.saveImage(cropped, "mrz-${ts}-check.png")
                        processResult(result = cleanMRZ, bitmap = bf, rotation = rotation)
//                        BitmapUtils.saveImage(cropped, "mrz-${ts}-success.png")

                    } catch (e: Exception) {
                        Log.d("${SmartScannerActivity.TAG}/SmartScanner", e.toString())
                    }
                    imageProxy.close()
                }
                .addOnFailureListener { e ->
                    e.printStackTrace()
                    imageProxy.close()
                }


        }
    }

    internal open fun processResult(result: String, bitmap: Bitmap, rotation: Int) {
        val imagePath = activity.cacheImagePath()
        bitmap.cropCenter().cacheImageToLocal(
            imagePath,
            rotation,
            if (imageResultType == ImageResultType.BASE_64.value) 40 else 80
        )
        val imageFile = File(imagePath)
        val imageString = if (imageResultType == ImageResultType.BASE_64.value) imageFile.encodeBase64() else imagePath
        val mrz = when (format) {
            MrzFormat.MRTD_TD1.value -> MRZResult.formatMrtdTd1Result(MRZCleaner.parseAndCleanMrtdTd1(result), imageString)
            else -> MRZResult.formatMrzResult(MRZCleaner.parseAndClean(result), imageString)
        }
        if (intent.action == ScannerConstants.IDPASS_SMARTSCANNER_MRZ_INTENT ||
            intent.action == ScannerConstants.IDPASS_SMARTSCANNER_ODK_MRZ_INTENT
        ) {
            sendBundleResult(mrzResult = mrz)
        } else {
            val jsonString = Gson().toJson(mrz)
            sendAnalyzerResult(result = jsonString)
        }
    }

    private fun sendAnalyzerResult(result: String) {
        val data = Intent()
        Log.d(SmartScannerActivity.TAG, "Success from MRZ")
        Log.d(SmartScannerActivity.TAG, "value: $result")
        data.putExtra(SmartScannerActivity.SCANNER_IMAGE_TYPE, imageResultType)
        data.putExtra(SmartScannerActivity.SCANNER_RESULT, result)
        data.putExtra(ScannerConstants.MODE, mode)
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
