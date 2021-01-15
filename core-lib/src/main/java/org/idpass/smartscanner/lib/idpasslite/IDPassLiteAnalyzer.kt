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
import android.os.Bundle
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.gson.Gson
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import org.idpass.lite.Card
import org.idpass.lite.IDPassReader
import org.idpass.lite.exceptions.InvalidCardException
import org.idpass.smartscanner.api.ScannerConstants
import org.idpass.smartscanner.lib.SmartScannerActivity
import org.idpass.smartscanner.lib.platform.extension.toBitmap
import org.idpass.smartscanner.lib.platform.utils.DateUtils
import org.idpass.smartscanner.lib.scanner.config.Modes

class IDPassLiteAnalyzer(
    private val activity: Activity,
    private val intent : Intent
) : ImageAnalysis.Analyzer {

    @SuppressLint("UnsafeExperimentalUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            Log.d(SmartScannerActivity.TAG, "Bitmap: (${mediaImage.width}, ${mediaImage.height})")
            val rot = imageProxy.imageInfo.rotationDegrees
            val bf = mediaImage.toBitmap(rot, Modes.BARCODE.value)
            val start = System.currentTimeMillis()
            val options = BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE ).build()
            val image = InputImage.fromBitmap(bf, imageProxy.imageInfo.rotationDegrees)
            val scanner = BarcodeScanning.getClient(options)
            Log.d("${SmartScannerActivity.TAG}/SmartScanner", "ID PASS Lite: process")
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    val timeRequired = System.currentTimeMillis() - start
                    Log.d("${SmartScannerActivity.TAG}/SmartScanner", "ID PASS Lite: success: $timeRequired ms")
                    if (barcodes.isNotEmpty()) {
                        val raw = barcodes[0].rawBytes
                        val idPassReader = IDPassReader()
                        var card: Card?
                        try {
                            card = idPassReader.open(raw)
                        } catch (ice: InvalidCardException) {
                            card = idPassReader.open(raw, true)
                            ice.printStackTrace()
                        }
                        val result = IDPassLiteResult(card, raw?.toList())
                        if (intent.action == ScannerConstants.IDPASS_SMARTSCANNER_IDPASS_LITE_INTENT ||
                            intent.action == ScannerConstants.IDPASS_SMARTSCANNER_ODK_IDPASS_LITE_INTENT) {
                            sendBundleResult(activity, intent, result)
                        } else {
                            val jsonString = Gson().toJson(result)
                            sendAnalyzerResult(activity, raw)
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

    private fun sendAnalyzerResult(activity: Activity, result: ByteArray? = null) {
        val data = Intent()
        Log.d(SmartScannerActivity.TAG, "Success from IDPASS LITE")
        Log.d(SmartScannerActivity.TAG, "value: $result")
        data.putExtra(SmartScannerActivity.SCANNER_RESULT_BYTES, result)
        activity.setResult(Activity.RESULT_OK, data)
        activity.finish()
    }

    private fun sendBundleResult(activity: Activity, intent : Intent, idPassResult : IDPassLiteResult? = null) {
        val bundle = Bundle()
        Log.d(SmartScannerActivity.TAG, "Success from IDPASS LITE")
        val card = idPassResult?.card
        if (card != null) {
            val fullName = card.getfullName()
            val givenName = card.givenName
            val surname = card.surname
            val dateOfBirth = card.dateOfBirth
            val placeOfBirth = card.placeOfBirth
            val uin = card.uin
            val address = card.postalAddress

            if (intent.action == ScannerConstants.IDPASS_SMARTSCANNER_ODK_IDPASS_LITE_INTENT) {
                if (uin != null) {
                    bundle.putString(ScannerConstants.IDPASS_ODK_INTENT_DATA, uin)
                }
            }
            if (fullName != null) {
                bundle.putString(ScannerConstants.IDPASS_LITE_FULL_NAME, fullName)
            }

            if (givenName != null) {
                bundle.putString(ScannerConstants.IDPASS_LITE_GIVEN_NAMES, givenName)
            }
            if (surname != null) {
                bundle.putString(ScannerConstants.IDPASS_LITE_SURNAME, surname)
            }
            if (dateOfBirth != null) {
                val birthday = if (DateUtils.isValidDate(DateUtils.formatDate(dateOfBirth))) DateUtils.formatDate(
                    dateOfBirth
                ) else ""
                bundle.putString(ScannerConstants.IDPASS_LITE_DATE_OF_BIRTH, birthday)
            }
            if (placeOfBirth.isNotEmpty()) {
                bundle.putString(ScannerConstants.IDPASS_LITE_PLACE_OF_BIRTH, placeOfBirth)
            }
            if (uin != null) {
                bundle.putString(ScannerConstants.IDPASS_LITE_UIN, uin)
            }

            if (address != null) {
                val addressLines = address.addressLinesList.joinToString("\n")
                bundle.putString(ScannerConstants.IDPASS_LITE_ADDRESS_POSTAL_CODE, address.postalCode)
                bundle.putString(ScannerConstants.IDPASS_LITE_ADDRESS_ADMINISTRATIVE_AREA, address.administrativeArea)
                bundle.putString(ScannerConstants.IDPASS_LITE_ADDRESS_ADDRESS_LINES, address.languageCode)
                bundle.putString(ScannerConstants.IDPASS_LITE_ADDRESS_LANGUAGE_CODE, addressLines)
                bundle.putString(ScannerConstants.IDPASS_LITE_ADDRESS_SORTING_CODE, address.sortingCode)
                bundle.putString(ScannerConstants.IDPASS_LITE_ADDRESS_LOCALITY, address.locality)
                bundle.putString(ScannerConstants.IDPASS_LITE_ADDRESS_SUBLOCALITY, address.sublocality)
                bundle.putString(ScannerConstants.IDPASS_LITE_ADDRESS_ORGANIZATION, address.organization)
            }
            bundle.putByteArray(ScannerConstants.IDPASS_LITE_RAW, idPassResult.raw?.toByteArray())
        }

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