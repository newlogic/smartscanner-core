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

import COSE.AlgorithmID
import COSE.OneKey
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Message
import android.util.Base64
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.ImageProxy
import com.github.wnameless.json.flattener.JsonFlattener
import com.google.crypto.tink.subtle.Ed25519Verify
import com.google.gson.JsonElement
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.jayway.jsonpath.JsonPath
import com.upokecenter.cbor.CBORObject
import net.i2p.crypto.eddsa.EdDSASecurityProvider
import nl.minvws.encoding.Base45
import org.apache.commons.codec.binary.Base64 as B64
import org.idpass.smartscanner.api.ScannerConstants
import org.idpass.smartscanner.lib.BuildConfig
import org.idpass.smartscanner.lib.R
import org.idpass.smartscanner.lib.SmartScannerActivity
import org.idpass.smartscanner.lib.platform.BaseImageAnalyzer
import org.idpass.smartscanner.lib.platform.extension.setContrast
import org.idpass.smartscanner.lib.platform.utils.BitmapUtils
import org.idpass.smartscanner.lib.platform.utils.GzipUtils
import org.idpass.smartscanner.lib.scanner.config.Modes
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.json.simple.JSONObject as JsonObject
import org.json.simple.parser.JSONParser
import se.sics.ace.cwt.CWT
import se.sics.ace.cwt.CwtCryptoCtx
import java.security.GeneralSecurityException
import java.security.KeyFactory
import java.security.PublicKey
import java.security.Security
import java.security.spec.X509EncodedKeySpec
import java.util.zip.ZipException
import java.io.IOException


class QRCodeAnalyzer(
    override val activity: Activity,
    override val intent: Intent,
    override val mode: String = Modes.QRCODE.value
) : BaseImageAnalyzer() {
    var context:Context = activity
    var dialogShown = false

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("UnsafeExperimentalUsageError", "UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val bitmap = BitmapUtils.getBitmap(imageProxy)
        bitmap?.let { bf ->
            Log.d(SmartScannerActivity.TAG, "Bitmap: (${bf.width}, ${bf.height})")
            val start = System.currentTimeMillis()
            bf.apply {
                // Increase contrast and brightness for better image processing and reduce Moiré effect
                setContrast(1.5F)
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
                        when (intent.action) {
                            ScannerConstants.IDPASS_SMARTSCANNER_QRCODE_INTENT,
                            ScannerConstants.IDPASS_SMARTSCANNER_ODK_QRCODE_INTENT, -> {
                                sendResult(
                                    rawValue = rawValue,
                                    rawBytes = barcodes[0].rawBytes
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

    @RequiresApi(Build.VERSION_CODES.O)
    private fun sendResult(rawValue: String?, rawBytes: ByteArray?) {
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

        if (data != null && intent.action ==  ScannerConstants.IDPASS_SMARTSCANNER_ODK_QRCODE_INTENT)
        {
            if(intent.getStringExtra("public_key") != null){
                var status = false
                var statusMessage = ""
                try {
                    Log.d("PH1 Version:", data.toString())
                    val publicKey = intent.getStringExtra("public_key")

                    // PH1 decode logic
                    var base45Decoder = Base45.getDecoder()
                    var qrCodeData = data.substring(4)
                    var base45DecodedData = base45Decoder.decode(qrCodeData)

                    Security.addProvider(EdDSASecurityProvider())
                    val publicBytes: ByteArray = B64.decodeBase64(publicKey)
                    val keySpec = X509EncodedKeySpec(publicBytes)

                    val keyFactory: KeyFactory = KeyFactory.getInstance("EdDSA")
                    val pubKey: PublicKey = keyFactory.generatePublic(keySpec)
                    var resultJson = decode(base45DecodedData, OneKey(pubKey, null))
                    var returnJson : JSONObject

                    if (resultJson != null) {
                        returnJson = getflattenedJSON(JSONObject(resultJson))
                        for (key in returnJson.keys()) {
                            bundle.putString(key, returnJson.get(key).toString())
                        }
                    }
                    else {
                        status = false
                        statusMessage = "QR Code Not Recognized"
                        invalidQRCodesteps(intent, bundle, data, status, statusMessage, 105)
                    }


                    // PH1 decode logic ends here
                    val result = Intent()
                    val prefix = if (intent.hasExtra(ScannerConstants.IDPASS_ODK_PREFIX_EXTRA)) {
                        intent.getStringExtra(ScannerConstants.IDPASS_ODK_PREFIX_EXTRA)
                    } else {
                        ""
                    }
                    result.putExtra(ScannerConstants.RESULT, bundle)
                    // Copy all the values in the intent result to be compatible with other implementations than commcare
                    for (key in bundle.keySet()) {
                        Log.d("Final Bundle Item", "$key : ${bundle.getString(key)}")
                        result.putExtra(prefix + key, bundle.getString(key))
                    }

                    result.putExtra("QRCODE_SCAN_status", "true")
                    result.putExtra("QRCODE_SCAN_status_message", "Success")
                    result.putExtra("QRCODE_SCAN_status_code", "")
                    Log.d("Result Items", result.extras.toString())
                    activity.setResult(Activity.RESULT_OK, result)
                    activity.finish()
                } catch (ex: Exception){
                    status = false
                    statusMessage = "QR Code Not Recognized"
                    invalidQRCodesteps(intent, bundle,data, status, statusMessage,102)
                }
            }
            else {
                identifier(data, bundle, intent)
            }
        } else {
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
            // Status Message Here
            activity.setResult(Activity.RESULT_OK, result)
            activity.finish()
        }
    }
    private fun identifier(data: String, bundle: Bundle, intent: Intent) {
//        val qr_code_types = JSONArray(intent.getStringExtra("qr_code_type"))
        val qr_code_types:JSONArray
        try {
            qr_code_types = JSONArray(intent.getStringExtra("qr_code_type"))
        } catch (ex: Exception) {
            invalidQRCodesteps(intent, bundle,data,false,"QR Code Types Not Specified Correctly",101)
            return
        }
        Log.d("qr_code_types",qr_code_types.toString())
        var status : Array<Boolean> = emptyArray()
        var statusMessage : Array<String> = emptyArray()
        var statusCode : Array<Int> = emptyArray()
        for (type_idx in 0 until qr_code_types.length()) {
            val qr_code_type = JSONObject(qr_code_types.get(type_idx).toString())
            if (qr_code_type.get("type") == "cwt") {
                try {
                    Log.d("PH1 Version:", data.toString())
                    val qrcode_index = qr_code_type.get("qrcode_index").toString()
                    val publicKey:String
                    try {
                        publicKey = getPublicKey(intent, qrcode_index)
                    } catch (ex: Exception) {
                        status = status.plus(false)
                        statusMessage = statusMessage.plus("Public Key Not Specified Correctly")
                        statusCode = statusCode.plus(102)
                        continue
                    }
                    Log.d("Public Key for PH1", publicKey.toString())

                    // PH1 decode logic
                    var base45Decoder = Base45.getDecoder()
                    var qrCodeData = data.substring(4)
                    var base45DecodedData = base45Decoder.decode(qrCodeData)

                    Security.addProvider(EdDSASecurityProvider())
                    val publicBytes: ByteArray = B64.decodeBase64(publicKey)
                    val keySpec = X509EncodedKeySpec(publicBytes)

                    val keyFactory: KeyFactory = KeyFactory.getInstance("EdDSA")
                    val pubKey: PublicKey = keyFactory.generatePublic(keySpec)
                    var resultJson = decode(base45DecodedData, OneKey(pubKey, null))
                    var field_mapper:JSONObject
                    try {
                        field_mapper = JSONObject(getFieldMapper(intent, qrcode_index))
                    } catch(ex:Exception) {
                        status = status.plus(false)
                        statusMessage = statusMessage.plus("Fields Not Specified Correctly in Scanner Input")
                        statusCode = statusCode.plus(104)
                        continue
                    }

                    if (resultJson != null) {
                        val ret = getFields(JSONObject(resultJson), field_mapper, bundle)
                        if (ret == false) {
                            status = status.plus(false)
                            statusMessage = statusMessage.plus("Fields Not Specified Correctly in Scanner Input")
                            statusCode = statusCode.plus(104)
                            continue
                        }
                    }
                    else {
                        status = status.plus(false)
                        statusMessage = statusMessage.plus("QR Code Not Recognized")
                        statusCode = statusCode.plus(105)
                        continue
                    }


                    // PH1 decode logic ends here
                    val result = Intent()
                    val prefix = if (intent.hasExtra(ScannerConstants.IDPASS_ODK_PREFIX_EXTRA)) {
                        intent.getStringExtra(ScannerConstants.IDPASS_ODK_PREFIX_EXTRA)
                    } else {
                        ""
                    }
                    result.putExtra(ScannerConstants.RESULT, bundle)
                    // Copy all the values in the intent result to be compatible with other implementations than commcare
                    for (key in bundle.keySet()) {
                        Log.d("Final Bundle Item", "$key : ${bundle.getString(key)}")
                        result.putExtra(prefix + key, bundle.getString(key))
                    }

                    result.putExtra("QRCODE_SCAN_status", "true")
                    result.putExtra("QRCODE_SCAN_status_message", "Success")
                    result.putExtra("QRCODE_SCAN_status_code", "")
                    Log.d("Result Items", result.extras.toString())
                    activity.setResult(Activity.RESULT_OK, result)
                    activity.finish()
                } catch (ex: Exception){
                    status = status.plus(false)
                    statusMessage = statusMessage.plus("QR Code Not Recognized")
                    statusCode = statusCode.plus(105)
                    continue
                }
            } else if(qr_code_type.get("type") == "json_plain") {
                try {
                    Log.d("First Version:", data.toString())
                    val qrcode_index = qr_code_type.get("qrcode_index").toString()

                    // Try block Here to check if data is valid.
                    var jsonData = JSONObject(data)

                    //var subject: String = jsonData.get("subject").toString()
                    var field_mapper:JSONObject
                    try {
                        field_mapper = JSONObject(getFieldMapper(intent, qrcode_index))
                    } catch(ex:Exception) {
                        status = status.plus(false)
                        statusMessage = statusMessage.plus("Fields Not Specified Correctly in Scanner Input")
                        statusCode = statusCode.plus(104)
                        continue
                    }
                    try {

                        if (jsonData != null) {
                            var ret = getFields(jsonData, field_mapper, bundle)
                            if (ret == false) {
                                status = status.plus(false)
                                statusMessage = statusMessage.plus("Fields Not Specified Correctly in Scanner Input")
                                statusCode = statusCode.plus(104)
                                continue

                            }
                        }
                        else {
                            status = status.plus(false)
                            statusMessage = statusMessage.plus("QR Code Not Recognized")
                            statusCode = statusCode.plus(105)
                            continue
                        }

                        bundle.putString(ScannerConstants.MODE, mode)

                        val result = Intent()
                        val prefix =
                            if (intent.hasExtra(ScannerConstants.IDPASS_ODK_PREFIX_EXTRA)) {
                                intent.getStringExtra(ScannerConstants.IDPASS_ODK_PREFIX_EXTRA)
                            } else {
                                ""
                            }
                        result.putExtra(ScannerConstants.RESULT, bundle)
                        // Copy all the values in the intent result to be compatible with other implementations than commcare
                        for (key in bundle.keySet()) {
                            Log.d("Final Bundle Item", "$key : ${bundle.getString(key)}")
                            result.putExtra(prefix + key, bundle.getString(key))
                        }

                        result.putExtra("QRCODE_SCAN_status", "true")
                        result.putExtra("QRCODE_SCAN_status_message", "Success")
                        result.putExtra("QRCODE_SCAN_status_code", "")
                        activity.setResult(Activity.RESULT_OK, result)
                        activity.finish()

                    } catch (ex: GeneralSecurityException) {
                        Log.d(
                            "${SmartScannerActivity.TAG}/SmartScanner",
                            "Signature Verify Result : ${ex.message}"
                        )

//                        if (dialogShown == false)
//                            showErrorMessage(ex.message.toString())
                        // Status Message Here
                        status = status.plus(false)
                        statusMessage = statusMessage.plus("QR Code Not Recognized")
                        statusCode = statusCode.plus(105)
                        Log.d("Exception", ex.message.toString())
                        continue
                    } catch (ex: Exception) {
                        Log.d(
                            "${SmartScannerActivity.TAG}/SmartScanner",
                            "Signature Verify Result : ${ex.message}"
                        )
//                        if (dialogShown == false)
//                            showErrorMessage(ex.message.toString())
                        // Status Message Here
                        status = status.plus(false)
                        statusMessage = statusMessage.plus("Fields Not Specified Correctly in Scanner Input")
                        statusCode = statusCode.plus(104)
                        Log.d("Exception", ex.message.toString())
                        continue
                    }
                } catch (ex: JSONException) {
                    status = status.plus(false)
                    statusMessage = statusMessage.plus("QR Code Not Recognized")
                    statusCode = statusCode.plus(105)
                    Log.d("Exception", ex.message.toString())
                    continue
                } catch (ex:Exception) {
                    status = status.plus(false)
                    statusMessage = statusMessage.plus("Fields Not Specified Correctly in Scanner Input")
                    statusCode = statusCode.plus(104)
                    Log.d("Exception", ex.message.toString())
                    continue
                }
            }
            else {
                try {
                    Log.d("First Version:", data.toString())
                    val qrcode_index = qr_code_type.get("qrcode_index").toString()
                    val publicKey:String
                    try {
                        publicKey = getPublicKey(intent, qrcode_index)
                    } catch (ex: Exception) {
                        status = status.plus(false)
                        statusMessage = statusMessage.plus("Public Key Not Specified Correctly")
                        statusCode = statusCode.plus(102)
                        continue
                    }

                    // Try block Here to check if data is valid.
                    var jsonData = JSONObject(data)

                    var subject: String = jsonData.get("subject").toString()
                    var signature: String = jsonData.get("signature").toString()

                    // Getting keys for new JSON
                    var signaturePayload = JSONObject()
                    val ret:Pair<String, Int>
                    try {
                        ret = getSignatureMapper(intent, qrcode_index)
                    } catch (ex:Exception) {
                        status = status.plus(false)
                        statusMessage = statusMessage.plus("Signature Mapper Not Specified Correctly")
                        statusCode = statusCode.plus(103)
                        continue
                    }
                    val signatureFields = JSONArray(ret.first)
                    val pretty_spaces = ret.second
                    for (key_idx in 0 until signatureFields.length()) {
                        var key = signatureFields[key_idx]
                        Log.d("Signature Fields", key.toString())
                        signaturePayload.put(key.toString(), getJSONElement(jsonData, key.toString()))
                    }
                    var signaturePayloadPretty =
                        if(pretty_spaces>=0) {
                            signaturePayload.toString(pretty_spaces)
                        } else {
                            signaturePayload.toString()
                        }
                    Log.d("Signature Payload", signaturePayloadPretty)

                    val publicKeyDecoded = Base64.decode(publicKey.toByteArray(), 0)
                    var signatureDecoded = Base64.decode(signature.toByteArray(), 0);
                    val ed = Ed25519Verify(publicKeyDecoded)
                    var field_mapper:JSONObject
                    try {
                        field_mapper = JSONObject(getFieldMapper(intent, qrcode_index))
                    } catch(ex:Exception) {
                        status = status.plus(false)
                        statusMessage = statusMessage.plus("Fields Not Specified Correctly in Scanner Input")
                        statusCode = statusCode.plus(104)
                        continue
                    }
                    try {
                        ed.verify(signatureDecoded, signaturePayloadPretty.toByteArray())
                        Log.d(
                            "${SmartScannerActivity.TAG}/SmartScanner",
                            "Signature Verify Result : Success"
                        )

                        if (jsonData != null) {
                            var ret = getFields(jsonData, field_mapper, bundle)
                            if (ret == false) {
                                status = status.plus(false)
                                statusMessage = statusMessage.plus("Fields Not Specified Correctly in Scanner Input")
                                statusCode = statusCode.plus(104)
                                continue

                            }
                        }
                        else {
                            status = status.plus(false)
                            statusMessage = statusMessage.plus("QR Code Not Recognized")
                            statusCode = statusCode.plus(105)
                            continue
                        }

                        bundle.putString(ScannerConstants.MODE, mode)

                        val result = Intent()
                        val prefix =
                            if (intent.hasExtra(ScannerConstants.IDPASS_ODK_PREFIX_EXTRA)) {
                                intent.getStringExtra(ScannerConstants.IDPASS_ODK_PREFIX_EXTRA)
                            } else {
                                ""
                            }
                        result.putExtra(ScannerConstants.RESULT, bundle)
                        // Copy all the values in the intent result to be compatible with other implementations than commcare
                        for (key in bundle.keySet()) {
                            Log.d("Final Bundle Item", "$key : ${bundle.getString(key)}")
                            result.putExtra(prefix + key, bundle.getString(key))
                        }

                        result.putExtra("QRCODE_SCAN_status", "true")
                        result.putExtra("QRCODE_SCAN_status_message", "Success")
                        result.putExtra("QRCODE_SCAN_status_code", "")
                        activity.setResult(Activity.RESULT_OK, result)
                        activity.finish()

                    } catch (ex: GeneralSecurityException) {
                        Log.d(
                            "${SmartScannerActivity.TAG}/SmartScanner",
                            "Signature Verify Result : ${ex.message}"
                        )

//                        if (dialogShown == false)
//                            showErrorMessage(ex.message.toString())
                        // Status Message Here
                        status = status.plus(false)
                        statusMessage = statusMessage.plus("QR Code Not Recognized")
                        statusCode = statusCode.plus(105)
                        Log.d("Exception", ex.message.toString())
                        continue
                    } catch (ex: Exception) {
                        Log.d(
                            "${SmartScannerActivity.TAG}/SmartScanner",
                            "Signature Verify Result : ${ex.message}"
                        )
//                        if (dialogShown == false)
//                            showErrorMessage(ex.message.toString())
                        // Status Message Here
                        status = status.plus(false)
                        statusMessage = statusMessage.plus("Fields Not Specified Correctly in Scanner Input")
                        statusCode = statusCode.plus(104)
                        Log.d("Exception", ex.message.toString())
                        continue
                    }
                } catch (ex: JSONException) {
                    status = status.plus(false)
                    statusMessage = statusMessage.plus("QR Code Not Recognized")
                    statusCode = statusCode.plus(105)
                    Log.d("Exception", ex.message.toString())
                    continue
                } catch (ex:Exception) {
                    status = status.plus(false)
                    statusMessage = statusMessage.plus("Fields Not Specified Correctly in Scanner Input")
                    statusCode = statusCode.plus(104)
                    Log.d("Exception", ex.message.toString())
                    continue
                }
            }
        }
        Log.d("Size of status code", status.size.toString())
        var final_status = false
        var final_status_message = "Failure But Reason Unknown"
        var final_status_code = 106
        for (idx in statusCode.indices) {
            Log.d("Status Codes", statusCode.get(idx).toString())
            if (statusCode.get(idx) < final_status_code) {
                final_status_code = statusCode.get(idx)
                final_status_message = statusMessage.get(idx)
                final_status = status.get(idx)
            }
        }
        try {
            val fieldMappers = JSONArray(intent.getStringExtra("field_mapper"))
            val first_mapper = JSONObject(JSONObject(fieldMappers.get(0).toString()).get("mapper").toString())
            for (key in first_mapper.keys()){
                bundle.putString(key.toString(), "")
            }
        } catch (ex: Exception) {

        }

        invalidQRCodesteps(intent, bundle, data, final_status, final_status_message, final_status_code)
    }

    private fun getflattenedJSON(data: JSONObject) : JSONObject {
        var result = JSONObject()
        for (key in data.keys()) {
            try {
                var temp_json = getflattenedJSON(data.getJSONObject(key))
                for (key in temp_json.keys()) {
                    result.put(key, temp_json.get(key))
                }
            }
            catch (ex: JSONException) {
                result.put(key, data.get(key))
            }
        }
        return result
    }

    private fun invalidQRCodesteps(intent: Intent, bundle: Bundle, data: String, status: Boolean, statusMessage: String, statusCode: Int) {
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
        // Status Message Here
        result.putExtra("QRCODE_SCAN_status", status.toString())
        result.putExtra("QRCODE_SCAN_status_message", statusMessage.toString())
        result.putExtra("QRCODE_SCAN_status_code", statusCode.toString())
        activity.setResult(Activity.RESULT_OK, result)
        activity.finish()
    }

    private fun getPublicKey(intent: Intent, qrcode_index: String) : String{
        val publicKeys = JSONArray(intent.getStringExtra("public_keys"))
        for (public_key_idx in 0 until publicKeys.length()){
            val public_key_info = JSONObject(publicKeys.get(public_key_idx).toString())
            if(public_key_info.get("qrcode_index").toString() == qrcode_index){
                return public_key_info.get("public_key").toString()
            }
        }
        return ""
    }

    private fun getFieldMapper(intent: Intent, qrcode_index: String) : String{
        val fieldMappers = JSONArray(intent.getStringExtra("field_mapper"))
        for (field_mapper_idx in 0 until fieldMappers.length()){
            val field_mapper_info = JSONObject(fieldMappers.get(field_mapper_idx).toString())
            if(field_mapper_info.get("qrcode_index").toString() == qrcode_index){
                return field_mapper_info.get("mapper").toString()
            }
        }
        return ""
    }

    private fun getSignatureMapper(intent: Intent, qrcode_index: String) : Pair<String, Int>{
        val signatureMappers = JSONArray(intent.getStringExtra("signature_mapper"))
        for (signature_mapper_idx in 0 until signatureMappers.length()){
            val signature_mapper_info = JSONObject(signatureMappers.get(signature_mapper_idx).toString())
            if(signature_mapper_info.get("qrcode_index").toString() == qrcode_index){
                return Pair(signature_mapper_info.get("mapper").toString(),signature_mapper_info.get("pretty_spaces") as Int)
            }
        }
        return Pair("",-1)
    }


    private fun getFields(data: JSONObject, field_mapper: JSONObject, bundle: Bundle): Boolean {
        for (field in field_mapper.keys()) {

            try {
                Log.d("Field:", field )

                val mapped_field : String= field_mapper.get(field).toString()
                Log.d("Mapped Field:", mapped_field )
                if(mapped_field.contains('[')) {
                    val root_field_part = mapped_field.split('.')[0].toString()
                    val root_field = root_field_part.substring(0,root_field_part.indexOf('['))
                    Log.d("Root Field:", root_field )
                    val array_index_part = root_field_part.substring(root_field_part.indexOf('['), root_field_part.indexOf(']') + 1)
                    Log.d("Array Index Part:", array_index_part )
                    val array_index = array_index_part.substring(1,array_index_part.length-1).toInt()
                    Log.d("Array Index:", array_index.toString() )
                    val leaf_field = mapped_field.split('.')[1]
                    Log.d("leaf_field", leaf_field )
                    val root_field_value = getJSONElement(data, root_field).toString()
                    Log.d("root_field_value:", root_field_value )
//                    Log.d("root field type:", root_field_value::class.java.typeName )
                    val root_field_json = JSONObject(JSONArray(root_field_value)[array_index].toString())
                    val fieldValue : String = getJSONElement(root_field_json, leaf_field).toString()
                    bundle.putString(field, fieldValue)
                } else {
                    val fieldValue: String = getJSONElement(data, field_mapper.get(field).toString()).toString()
                    Log.d("Field Value:", fieldValue )
                    bundle.putString(field, fieldValue)
                }

            } catch (ex:Exception){

                Log.d("Parsing Exception",ex.toString())
                return false
            }
        }
        return true
    }

    private fun getJSONElement(data: JSONObject, element: String): Any {
        val keys = element.split(".")
        var data_json = data
        var result:Any = ""
        for (key in keys) {
            try {
                data_json = data_json.getJSONObject(key)
                result = data_json
            } catch (ex: JSONException){
                result = data_json.get(key)
            }
        }
        return result
    }

    private fun getGzippedData(rawBytes: ByteArray?) : String? {
        var data: String? = null
        try {
            data = if (rawBytes != null)  GzipUtils.decompress(rawBytes) else null
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
    private fun showErrorMessage(message:String) {
        dialogShown = true
        val dialog: AlertDialog.Builder = AlertDialog.Builder(context)
        dialog.setMessage(message)
        dialog.setNegativeButton(R.string.label_close) { alert, which ->
            run {
                dialogShown = false
            }
        }
        dialog.show()
    }
    @Throws(IOException::class)
    fun decode(rawCbor: ByteArray?, oneKey: OneKey): JsonObject? {
        val alg = AlgorithmID.ECDSA_256.AsCBOR()
        val ctx = CwtCryptoCtx.sign1Verify(oneKey.PublicKey(), alg)
        var cwt: CWT? = null
        try {
            cwt = CWT.processCOSE(rawCbor, ctx)
        } catch (e: Exception) {
//            LOGGER.error("error while converting to CWT object")
        }
        val cborObject: CBORObject = (cwt?.getClaim(169.toShort()) ?: null)!!
        val parser = JSONParser()
        val json = parser.parse(cborObject.ToJSONString()) as JsonObject
        val bytes = cborObject["img"].GetByteString()
        json.put("img", B64.encodeBase64String(bytes))
        return json
    }

    fun verifySignature(cborData: ByteArray?, oneKey: OneKey) {
        val alg: CBORObject = AlgorithmID.EDDSA.AsCBOR()
        val ctx = CwtCryptoCtx.sign1Verify(oneKey.PublicKey(), alg)
        try {
            val cwt2: CWT = CWT.processCOSE(cborData, ctx)
        } catch (e: Exception) {
//            LOGGER.error("signature is invalid", e)
        }
    }


}