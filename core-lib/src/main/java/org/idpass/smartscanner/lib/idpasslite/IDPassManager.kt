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

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import org.api.proto.Certificates
import org.api.proto.KeySet
import org.idpass.lite.Card
import org.idpass.lite.IDPassReader
import org.idpass.lite.android.IDPassLiteHelper
import org.idpass.lite.exceptions.CardVerificationException
import org.idpass.lite.exceptions.InvalidCardException
import org.idpass.lite.exceptions.InvalidKeyException
import org.idpass.smartscanner.api.ScannerConstants
import org.idpass.smartscanner.lib.SmartScannerActivity
import org.idpass.smartscanner.lib.scanner.config.Modes
import org.idpass.smartscanner.lib.utils.DateUtils


object IDPassManager {

    fun getIDPassReader(): IDPassReader {
        // Initialize needed ks and rootcert from demo key values
        val keysetbuf = IDPassLiteHelper.generateAndroidKeyset()
        val rootcertbuf = IDPassLiteHelper.generateAndroidRootcert()
        val ks = KeySet.parseFrom(keysetbuf)
        val rootcert = Certificates.parseFrom(rootcertbuf)
        // Initialize reader with ks and rootcert
        return IDPassReader(ks, rootcert)
    }

    fun verifyCard(
        activity: Activity,
        idPassReader: IDPassReader,
        intent: Intent, raw: ByteArray,
        pinCode: String = "",
        onResult: () -> Unit
    ) {
        val card: Card? = readCard(idPassReader, raw)
        try {
            if (card != null) {
                val idPassLiteResult = IDPassLiteResult(card, raw.toList())
                if (pinCode.isNotEmpty()) {
                    // verify id pass lite when pin code is inputted
                    try {
                        card.authenticateWithPIN(pinCode)
                        Toast.makeText(activity, "Authentication Success", Toast.LENGTH_SHORT).show()
                        sendBundleResult(activity, intent, idPassLiteResult)
                        onResult.invoke()
                    } catch (ve: CardVerificationException) {
                        Toast.makeText(activity, "Authentication Fail", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // send result bundle if verification is skipped
                    sendBundleResult(activity, intent, idPassLiteResult)
                    onResult.invoke()
                }
            } else {
                Log.d(SmartScannerActivity.TAG, "ID PASS exception: Invalid IDPASS CARD")
            }
        } catch (ike: InvalidKeyException) {
            Log.d(SmartScannerActivity.TAG, "ID PASS exception: ${ike.localizedMessage}")
        } catch (e: Exception) {
            Log.d(SmartScannerActivity.TAG, "ID PASS exception: ${e.localizedMessage}")
        }
    }

    private fun readCard(idPassReader: IDPassReader, raw: ByteArray): Card? {
        var card: Card? = null
        try {
            card = try {
                idPassReader.open(raw)
            } catch (ice: InvalidCardException) {
                idPassReader.open(raw, true)
            }
        } catch (ike: InvalidKeyException) {
            Log.d(SmartScannerActivity.TAG, "ID PASS exception: ${ike.localizedMessage}")
        } catch (e: Exception) {
            Log.d(SmartScannerActivity.TAG, "ID PASS exception: ${e.localizedMessage}")
        }
        Log.d(SmartScannerActivity.TAG, "card $card")
        return card
    }

    fun sendAnalyzerResult(activity: Activity, result: ByteArray? = null) {
        val data = Intent()
        Log.d(SmartScannerActivity.TAG, "Success from IDPASS LITE")
        Log.d(SmartScannerActivity.TAG, "value: $result")
        data.putExtra(ScannerConstants.MODE, Modes.IDPASS_LITE.value)
        data.putExtra(SmartScannerActivity.SCANNER_RESULT_BYTES, result)
        activity.setResult(Activity.RESULT_OK, data)
        activity.finish()
    }

    private fun sendBundleResult(
        activity: Activity,
        intent: Intent,
        idPassLiteResult: IDPassLiteResult
    ) {
        val bundle = Bundle()
        val card = idPassLiteResult.card
        if (card != null) {
            Log.d(SmartScannerActivity.TAG, "Success from IDPASS LITE")
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
                val birthday = if (DateUtils.isValidDate(DateUtils.formatDate(dateOfBirth))) DateUtils.formatDate(dateOfBirth) else ""
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
        }
        bundle.putString(ScannerConstants.MODE, Modes.IDPASS_LITE.value)
        bundle.putByteArray(ScannerConstants.IDPASS_LITE_RAW, idPassLiteResult.raw?.toByteArray())

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