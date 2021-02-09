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
package org.idpass.smartscanner.lib.idpasslite

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import org.idpass.smartscanner.api.ScannerConstants
import org.idpass.smartscanner.lib.SmartScannerActivity
import org.idpass.smartscanner.lib.platform.BaseImageAnalyzer
import org.idpass.smartscanner.lib.platform.extension.toBitmap
import org.idpass.smartscanner.lib.scanner.config.Modes

class IDPassLiteAnalyzer(
        override var activity: Activity,
        override var intent: Intent,
        override var mode: String = Modes.IDPASS_LITE.value,
        private val onVerify: (ByteArray?) -> Unit
) : BaseImageAnalyzer(){

    @SuppressLint("UnsafeExperimentalUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            Log.d(SmartScannerActivity.TAG, "Bitmap: (${mediaImage.width}, ${mediaImage.height})")
            val rot = imageProxy.imageInfo.rotationDegrees
            val bf = mediaImage.toBitmap(rot, mode)
            val start = System.currentTimeMillis()
            val options = BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
            val image = InputImage.fromBitmap(bf, imageProxy.imageInfo.rotationDegrees)
            val scanner = BarcodeScanning.getClient(options)
            Log.d("${SmartScannerActivity.TAG}/SmartScanner", "ID PASS Lite: process")
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    Log.d("${SmartScannerActivity.TAG}/SmartScanner", "ID PASS Lite: barcodes $barcodes")
                    if (barcodes.isNotEmpty()) {
                        val timeRequired = System.currentTimeMillis() - start
                        Log.d("${SmartScannerActivity.TAG}/SmartScanner", "ID PASS Lite: success: $timeRequired ms")
                        val raw = barcodes[0].rawBytes
                        if (intent.action == ScannerConstants.IDPASS_SMARTSCANNER_IDPASS_LITE_INTENT ||
                            intent.action == ScannerConstants.IDPASS_SMARTSCANNER_ODK_IDPASS_LITE_INTENT) {
                            onVerify.invoke(raw)
                        } else {
                            IDPassManager.sendAnalyzerResult(activity = activity, result = raw)
                        }
                    } else {
                        Log.d("${SmartScannerActivity.TAG}/SmartScanner", "ID PASS Lite: nothing detected")
                    }
                    imageProxy.close()
                }
                .addOnFailureListener { e ->
                    imageProxy.close()
                    Log.d("${SmartScannerActivity.TAG}/SmartScanner", "ID PASS Lite: failure: ${e.message}")
                }
        }
    }
}