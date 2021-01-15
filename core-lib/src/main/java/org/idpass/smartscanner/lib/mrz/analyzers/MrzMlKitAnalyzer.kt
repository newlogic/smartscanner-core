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
import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import org.idpass.smartscanner.lib.R
import org.idpass.smartscanner.lib.SmartScannerActivity
import org.idpass.smartscanner.lib.mrz.MRZCleaner
import org.idpass.smartscanner.lib.platform.extension.getConnectionType
import org.idpass.smartscanner.lib.platform.extension.toBitmap
import org.idpass.smartscanner.lib.scanner.config.Modes
import java.net.URLEncoder

class MrzMlKitAnalyzer(
    private val activity: Activity,
    private val intent : Intent,
    private val imageResultType: String,
    private val format: String?,
    private val onConnectFail: (String) -> Unit
) : MRZAnalyzer(activity, intent, imageResultType, format) {

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
            // Pass image to an ML Kit Vision API
            Log.d("${SmartScannerActivity.TAG}/SmartScanner", "MRZ MLKit: start")
            val start = System.currentTimeMillis()
            val rotation = imageProxy.imageInfo.rotationDegrees
            val image = InputImage.fromBitmap(cropped, rotation)
            val recognizer = TextRecognition.getClient()
            Log.d("${SmartScannerActivity.TAG}/SmartScanner", "MRZ MLKit TextRecognition: process")
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
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
                        val mrz = MRZCleaner.clean(rawFullRead)
                        Log.d(
                            "${SmartScannerActivity.TAG}/SmartScanner",
                            "After cleaner = [${
                                URLEncoder.encode(mrz, "UTF-8")
                                    .replace("%3C", "<").replace("%0A", "↩")
                            }]"
                        )
                        processResult(mrz = mrz, bitmap = bf, rotation = rotation)
                    } catch (e: Exception) {
                        Log.d("${SmartScannerActivity.TAG}/SmartScanner", e.toString())
                    }
                    imageProxy.close()
                }
                .addOnFailureListener { e ->
                    Log.d(
                        "${SmartScannerActivity.TAG}/SmartScanner",
                        "MRZ MLKit TextRecognition: failure: ${e.message}"
                    )
                    val connectionId =
                        if (activity.getConnectionType() == 0) R.string.connection_text else R.string.model_text
                    onConnectFail.invoke(activity.getString(connectionId))
                    imageProxy.close()
                }
        }
    }
}