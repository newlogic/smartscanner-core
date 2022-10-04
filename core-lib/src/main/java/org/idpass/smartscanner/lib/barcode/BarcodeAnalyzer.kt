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
package org.idpass.smartscanner.lib.barcode

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.camera.core.ImageProxy
import com.google.gson.Gson
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import org.idpass.smartscanner.api.ScannerConstants
import org.idpass.smartscanner.lib.SmartScannerActivity
import org.idpass.smartscanner.lib.scanner.BaseImageAnalyzer
import org.idpass.smartscanner.lib.scanner.config.ImageResultType
import org.idpass.smartscanner.lib.scanner.config.Modes
import org.idpass.smartscanner.lib.utils.BitmapUtils
import org.idpass.smartscanner.lib.utils.extension.*
import java.io.File


class BarcodeAnalyzer(
    override val activity: Activity,
    override val intent: Intent,
    override val mode: String = Modes.BARCODE.value,
    private val hasPDF417: Boolean,
    private val imageResultType: String,
    private val barcodeFormats: List<Int>
) : BaseImageAnalyzer() {

    @SuppressLint("UnsafeExperimentalUsageError", "UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val bitmap = BitmapUtils.getBitmap(imageProxy)
        bitmap?.let { bf ->
            Log.d(SmartScannerActivity.TAG, "Bitmap: (${bf.width}, ${bf.height})")
            val start = System.currentTimeMillis()
            val rot = imageProxy.imageInfo.rotationDegrees
            bf.apply {
                // Increase contrast and brightness for better image processing and reduce Moiré effect
                setContrast(1.5F)
                setBrightness(5F)
            }
            var barcodeFormat = Barcode.FORMAT_QR_CODE
            barcodeFormats.forEach {
                barcodeFormat = it or barcodeFormat // bitwise different barcode format options
            }
            val options = BarcodeScannerOptions.Builder().setBarcodeFormats(barcodeFormat).build()
            val image = InputImage.fromBitmap(bf, imageProxy.imageInfo.rotationDegrees)
            val scanner = BarcodeScanning.getClient(options)
            Log.d("${SmartScannerActivity.TAG}/SmartScanner", "barcode: process")
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    val timeRequired = System.currentTimeMillis() - start
                    val cornersString: String
                    val rawValue: String?
                    Log.d(
                        "${SmartScannerActivity.TAG}/SmartScanner",
                        "barcode: success: $timeRequired ms"
                    )
                    val filePath = activity.cacheImagePath()
                    if (barcodes != null && barcodes.isNotEmpty()) {
                        val barcode = barcodes[0]
                        val corners = barcode.cornerPoints
                        val builder = StringBuilder()
                        if (corners != null) {
                            for (corner in corners) {
                                builder.append("${corner.x},${corner.y} ")
                            }
                        }
                        val bitmapResult = if (hasPDF417) bf.resize(640, 480) else bf
                        bitmapResult?.cropCenter()?.cacheImageToLocal(
                            filePath,
                            imageProxy.imageInfo.rotationDegrees,
                            if (imageResultType == ImageResultType.BASE_64.value) 30 else 80
                        )
                        cornersString = builder.toString()
                        rawValue =  barcode.rawValue ?: barcode.displayValue
                        val imageFile = File(filePath)
                        val imageResult = if (imageResultType == ImageResultType.BASE_64.value) imageFile.encodeBase64() else filePath
                        val result = BarcodeResult(imagePath = filePath, image = imageResult, corners = cornersString, value = rawValue)
                        when (intent.action) {
                            ScannerConstants.IDPASS_SMARTSCANNER_BARCODE_INTENT,
                            ScannerConstants.IDPASS_SMARTSCANNER_ODK_BARCODE_INTENT -> {
                                sendBundleResult(barcodeResult = result)
                            }
                            else -> {
                                val jsonString = Gson().toJson(result)
                                sendAnalyzerResult(result = jsonString)
                            }
                        }
                    } else {
                        Log.d(
                            "${SmartScannerActivity.TAG}/SmartScanner",
                            "barcode: nothing detected"
                        )
                    }
                    imageProxy.close()
                }
                .addOnFailureListener { e ->
                    imageProxy.close()
                    Log.d(
                        "${SmartScannerActivity.TAG}/SmartScanner",
                        "barcode: failure: ${e.message}"
                    )
                }
        }
    }

    private fun sendAnalyzerResult(result: String) {
        val data = Intent()
        Log.d(SmartScannerActivity.TAG, "Success from BARCODE")
        Log.d(SmartScannerActivity.TAG, "value: $result")
        data.putExtra(SmartScannerActivity.SCANNER_IMAGE_TYPE, imageResultType)
        data.putExtra(SmartScannerActivity.SCANNER_RESULT, result)
        data.putExtra(ScannerConstants.MODE, mode)
        activity.setResult(Activity.RESULT_OK, data)
        activity.finish()
    }

    private fun sendBundleResult(barcodeResult: BarcodeResult? = null) {
        val bundle = Bundle()
        Log.d(SmartScannerActivity.TAG, "Success from BARCODE")
        if (intent.action == ScannerConstants.IDPASS_SMARTSCANNER_ODK_BARCODE_INTENT) {
            bundle.putString(ScannerConstants.IDPASS_ODK_INTENT_DATA, barcodeResult?.value)
        }
        bundle.putString(ScannerConstants.MODE, mode)
        bundle.putString(ScannerConstants.BARCODE_IMAGE, barcodeResult?.imagePath)
        bundle.putString(ScannerConstants.BARCODE_CORNERS, barcodeResult?.corners)
        bundle.putString(ScannerConstants.BARCODE_VALUE, barcodeResult?.value)
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