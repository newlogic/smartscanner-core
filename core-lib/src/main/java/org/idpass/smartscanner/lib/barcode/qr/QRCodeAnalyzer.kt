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
package org.idpass.smartscanner.lib.barcode.qr

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.camera.core.ImageProxy
import com.github.wnameless.json.flattener.JsonFlattener
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.jayway.jsonpath.JsonPath
import org.idpass.smartscanner.api.ScannerConstants
import org.idpass.smartscanner.lib.SmartScannerActivity
import org.idpass.smartscanner.lib.platform.BaseImageAnalyzer
import org.idpass.smartscanner.lib.platform.extension.toBitmap
import org.idpass.smartscanner.lib.platform.utils.GzipUtils
import org.idpass.smartscanner.lib.scanner.config.Modes
import org.json.JSONObject
import java.util.zip.ZipException


class QRCodeAnalyzer(
    override val activity: Activity,
    override val intent: Intent,
    override val mode: String = Modes.QRCODE.value
) : BaseImageAnalyzer() {

    @SuppressLint("UnsafeExperimentalUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            Log.d(SmartScannerActivity.TAG, "Bitmap: (${mediaImage.width}, ${mediaImage.height})")
            val rot = imageProxy.imageInfo.rotationDegrees
            val bf = mediaImage.toBitmap(rot, mode)
            val start = System.currentTimeMillis()
            val barcodeFormat = Barcode.FORMAT_QR_CODE
            val options = BarcodeScannerOptions.Builder().setBarcodeFormats(barcodeFormat).build()
            val image = InputImage.fromBitmap(bf, imageProxy.imageInfo.rotationDegrees)
            val scanner = BarcodeScanning.getClient(options)
            Log.d("${SmartScannerActivity.TAG}/SmartScanner", "qrcode: process")
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    val timeRequired = System.currentTimeMillis() - start
                    val rawValue: String
                    Log.d(
                        "${SmartScannerActivity.TAG}/SmartScanner",
                        "qrcode: success: $timeRequired ms"
                    )
                    if (barcodes.isNotEmpty()) {
                        rawValue = barcodes[0].rawValue!!
                        when (intent.action) {
                            ScannerConstants.IDPASS_SMARTSCANNER_QRCODE_INTENT,
                            ScannerConstants.IDPASS_SMARTSCANNER_ODK_QRCODE_INTENT, -> {
                                sendResult(
                                        rawValue = rawValue,
                                        rawBytes = barcodes[0].rawBytes!!
                                )
                            }
                        }
                    } else {
                        Log.d(
                            "${SmartScannerActivity.TAG}/SmartScanner",
                            "qrcode: nothing detected"
                        )
                    }
                    imageProxy.close()
                }
                .addOnFailureListener { e ->
                    imageProxy.close()
                    Log.d(
                        "${SmartScannerActivity.TAG}/SmartScanner",
                        "qrcode: failure: ${e.message}"
                    )
                }
        }
    }

    private fun sendResult(rawValue: String, rawBytes: ByteArray) {
        // parse and read qr data and add to bundle intent
        val bundle = Bundle()
        Log.d(SmartScannerActivity.TAG, "Success from QRCODE")
        val isOdk = intent.action == ScannerConstants.IDPASS_SMARTSCANNER_ODK_QRCODE_INTENT
        val isGzipped = if (isOdk) intent.getStringExtra(ScannerConstants.GZIPPED_ENABLED) == "1" else intent.getBooleanExtra(ScannerConstants.GZIPPED_ENABLED, false)
        val isJson = if (isOdk) intent.getStringExtra(ScannerConstants.JSON_ENABLED) == "1" else intent.getBooleanExtra(ScannerConstants.JSON_ENABLED, false)
        val jsonPath = intent.getStringExtra(ScannerConstants.JSON_PATH)
        // check gzipped parameters for bundle return result
        var data : String? = if (isGzipped) {
            getGzippedData(rawBytes)
        } else {
            rawValue
        }
        // check json parameters for bundle return result
        if (isJson) {
            if (data != null) {
                jsonPath?.let { path ->
                    val ctx = JsonPath.parse(data)
                    bundle.putString(ScannerConstants.QRCODE_JSON_VALUE, ctx.read<Any>(path).toString())
                }
                val flattenMap = flattenJson(data)
                for ((k, v) in flattenMap) {
                    bundle.putString(k, v)
                }
            } else {
                data = rawValue
            }
        }
        Log.d(
            "${SmartScannerActivity.TAG}/SmartScanner",
            "bundle: $bundle"
        )
        bundle.putString(ScannerConstants.MODE, mode)
        bundle.putString(ScannerConstants.QRCODE_TEXT, data)

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

    private fun getGzippedData(rawBytes: ByteArray) : String?{
        return try {
            GzipUtils.decompress(rawBytes)
        } catch (ez : ZipException) {
            ez.printStackTrace()
            null
        }
    }

    private fun flattenJson(json: String): HashMap<String, String> {
        val flattenedMap = JsonFlattener.flattenAsMap(json)
        val map: HashMap<String, String> = HashMap()
        for ((k, v) in flattenedMap) {
            val key = k.replace(".", "_").replace("[", "_").replace("]", "_").replace("__", "_")
            if (v != null) {
                map[key] = v.toString()
                print("$key, ")
            }
        }
        Log.d(
            "${SmartScannerActivity.TAG}/SmartScanner",
            "flattenedMap: ${JSONObject(map as Map<*, *>)}"
        )
        return map
    }
}