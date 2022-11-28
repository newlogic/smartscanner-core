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
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.camera.core.ImageProxy
import com.github.wnameless.json.flattener.JsonFlattener
import com.google.gson.JsonParseException
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.jayway.jsonpath.JsonPath
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jws
import org.idpass.smartscanner.api.ScannerConstants
import org.idpass.smartscanner.lib.SmartScannerActivity
import org.idpass.smartscanner.lib.scanner.BaseImageAnalyzer
import org.idpass.smartscanner.lib.scanner.config.Config
import org.idpass.smartscanner.lib.scanner.config.Modes
import org.idpass.smartscanner.lib.utils.BitmapUtils
import org.idpass.smartscanner.lib.utils.GzipUtils
import org.idpass.smartscanner.lib.utils.JWTUtils
import org.idpass.smartscanner.lib.utils.JWTUtils.getJsonBody
import org.idpass.smartscanner.lib.utils.JWTUtils.getJsonHeader
import org.idpass.smartscanner.lib.utils.JWTUtils.isDefaultConfigPublicKey
import org.idpass.smartscanner.lib.utils.JWTUtils.isJWT
import org.idpass.smartscanner.lib.utils.extension.setBrightness
import org.idpass.smartscanner.lib.utils.extension.setContrast
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.lang.RuntimeException
import java.util.zip.ZipException


class QRCodeAnalyzer(
    override val activity: Activity,
    override val intent: Intent,
    override val mode: String = Modes.QRCODE.value,
    private val imageResultType: String,
    private var isGzipped: Boolean? = null,
    private var isJson: Boolean? = false,
    private var jsonPath: String? = null
) : BaseImageAnalyzer() {

    @SuppressLint("UnsafeExperimentalUsageError", "UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val bitmap = BitmapUtils.getBitmap(imageProxy)
        bitmap?.let { bf ->
            Log.d(SmartScannerActivity.TAG, "Bitmap: (${bf.width}, ${bf.height})")
            val start = System.currentTimeMillis()
            bf.apply {
                // Increase contrast and brightness for better image processing and reduce Moiré effect
                setContrast(1.5F)
                setBrightness(5F)
            }
            val barcodeFormat = Barcode.FORMAT_QR_CODE
            val options = BarcodeScannerOptions.Builder().setBarcodeFormats(barcodeFormat).build()
            val image = InputImage.fromBitmap(bf, imageProxy.imageInfo.rotationDegrees)
            val scanner = BarcodeScanning.getClient(options)
            Log.d("${SmartScannerActivity.TAG}/SmartScanner", "qrcode: process")
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    val timeRequired = System.currentTimeMillis() - start
                    val rawValue: String?
                    Log.d(
                        "${SmartScannerActivity.TAG}/SmartScanner",
                        "qrcode: success: $timeRequired ms"
                    )
                    if (barcodes.isNotEmpty()) {
                        rawValue = barcodes[0].rawValue
                        if (intent.action == ScannerConstants.IDPASS_SMARTSCANNER_QRCODE_INTENT ||
                            intent.action == ScannerConstants.IDPASS_SMARTSCANNER_ODK_QRCODE_INTENT){
                            sendBundleResult(
                                rawValue = rawValue,
                                rawBytes = barcodes[0].rawBytes
                            )
                        } else {
                            sendResult( rawValue = rawValue, rawBytes = barcodes[0].rawBytes)
                            scanner.close()
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

    private fun sendResult(rawValue: String?, rawBytes: ByteArray?) {

        try {
            val intent = Intent()

            val result: String? = when (rawValue?.isJWT()) {
                true -> {
                    val value = getValueJWT(rawValue)
                    intent.putExtra(SmartScannerActivity.SCANNER_HEADER_RESULT, value.getJsonHeader().toString())
                    intent.putExtra(SmartScannerActivity.SCANNER_SIGNATURE_VERIFICATION, true)


                    // checks for config result jwt
                    val jsonResult = JSONObject(value.getJsonBody().toString())

                    if (jsonResult.has("conf") and jsonResult.has("pub")) {

                        val preference = activity.getSharedPreferences(Config.SHARED, Context.MODE_PRIVATE)
                        val editor = preference?.edit()

                        editor?.remove(Config.CONFIG_PROFILE_NAME)?.apply()
                        editor?.putString(Config.CONFIG_PROFILE_NAME, jsonResult.getString("conf"))

                        editor?.remove(Config.CONFIG_PUB_KEY)?.apply()
                        editor?.putString(Config.CONFIG_PUB_KEY, jsonResult.getString("pub"))

                        editor?.apply()

                        intent.putExtra(SmartScannerActivity.SCANNER_JWT_CONFIG_UPDATE, true)
                    }

                    value.getJsonBody().toString()
                }
                else -> getGzippedData(rawBytes) ?: rawValue

            }

            if (isJson == true && result != null && result != rawValue) {
                jsonPath?.let { path ->
                    val ctx = JsonPath.parse(result)
                    intent.putExtra(
                        ScannerConstants.QRCODE_JSON_VALUE,
                        ctx.read<Any>(path).toString()
                    )
                }
                val flattenMap = flattenJson(result)
                for ((k, v) in flattenMap) {
                    intent.putExtra(k, v)
                }
            }



            Log.d(SmartScannerActivity.TAG, "raw " + rawValue.toString())
            Log.d(SmartScannerActivity.TAG, "Success from QRCODE")
            Log.d(SmartScannerActivity.TAG, "value: $result")
            intent.putExtra(ScannerConstants.MODE, mode)
            intent.putExtra(SmartScannerActivity.SCANNER_IMAGE_TYPE, imageResultType)
            intent.putExtra(SmartScannerActivity.SCANNER_RESULT, result)
            intent.putExtra(SmartScannerActivity.SCANNER_RAW_RESULT, rawValue)


            activity.setResult(Activity.RESULT_OK, intent)
            activity.finish()
        } catch (ex : Exception) {
            Log.d(SmartScannerActivity.TAG, "Exception: ${ex.localizedMessage}")

            intent.putExtra(SmartScannerActivity.SCANNER_FAIL_RESULT, ex.localizedMessage)
            activity.setResult(Activity.RESULT_OK, intent)
            activity.finish()
        }
    }

    private fun sendBundleResult(rawValue: String?, rawBytes: ByteArray?) {
        // parse and read qr data and add to bundle intent
        val bundle = Bundle()
        Log.d(SmartScannerActivity.TAG, "Success from QRCODE")
        val isOdk = intent.action == ScannerConstants.IDPASS_SMARTSCANNER_ODK_QRCODE_INTENT
        val isGzipped = if (isOdk) intent.getStringExtra(ScannerConstants.GZIPPED_ENABLED) == "1" else intent.getBooleanExtra(ScannerConstants.GZIPPED_ENABLED, false)
        val isJson = if (isOdk) intent.getStringExtra(ScannerConstants.JSON_ENABLED) == "1" else intent.getBooleanExtra(ScannerConstants.JSON_ENABLED, false)
        val jsonPath = intent.getStringExtra(ScannerConstants.JSON_PATH)
        // check gzipped parameters for bundle return result
        var data : String? = when (isGzipped) {
            true -> getGzippedData(rawBytes)
            else -> {
                if (rawValue?.isJWT() == true) {
                    getValueJWT(rawValue).getJsonBody().toString()
                } else {
                    rawValue
                }
            }
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

    private fun getGzippedData(rawBytes: ByteArray?) : String? {
        var data: String? = null
        try {
            val inputStream = ByteArrayInputStream(rawBytes)
            data = if (rawBytes != null && GzipUtils.isGZipped(inputStream)) GzipUtils.decompress(rawBytes) else null
        } catch (ez : ZipException) {
            ez.printStackTrace()
        }
        return data
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

    private fun getValueJWT(rawValue: String): Jws<Claims> {

        // identify which public key to use first
        val preference = activity.getSharedPreferences(Config.SHARED, Context.MODE_PRIVATE)
        val publicKey = preference.getString(Config.CONFIG_PUB_KEY, null)

        return try {
            JWTUtils.getWithSignedKey(rawValue, publicKey)

        } catch (ex : Exception) {
            // Fallback goal is to allow multiple save of the config

            // We only fallback if the publicKey saved is not the default public key
            if (publicKey != null && !publicKey.isDefaultConfigPublicKey()) {
                // Lets add fallback here for JWT rawValue
                val valueJws = JWTUtils.getWithSignedKey(rawValue, null)
                // and we only allow fallback if payload is conf and public
                if (valueJws.body.containsKey("conf")) {
                    return valueJws
                }
            }

            // otherwise lets throw the exception
            throw ex
        }
    }

}