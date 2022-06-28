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
package org.idpass.smartscanner.lib.nfc

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.nfc.NfcAdapter
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentActivity
import com.google.gson.Gson
import io.sentry.Sentry
import org.idpass.smartscanner.api.ScannerConstants
import org.idpass.smartscanner.lib.R
import org.idpass.smartscanner.lib.SmartScannerActivity
import org.idpass.smartscanner.lib.nfc.details.IntentData
import org.idpass.smartscanner.lib.nfc.passport.Passport
import org.idpass.smartscanner.lib.nfc.passport.PassportDetailsFragment
import org.idpass.smartscanner.lib.nfc.passport.PassportPhotoFragment
import org.idpass.smartscanner.lib.scanner.config.Modes
import org.jmrtd.lds.icao.MRZInfo


class NFCActivity : FragmentActivity(), NFCFragment.NfcFragmentListener, PassportDetailsFragment.PassportDetailsFragmentListener, PassportPhotoFragment.PassportPhotoFragmentListener {

    companion object {
        private val TAG = NFCActivity::class.java.simpleName

        private val TAG_NFC = "TAG_NFC"
        private val TAG_PASSPORT_DETAILS = "TAG_PASSPORT_DETAILS"
        private val TAG_PASSPORT_PICTURE = "TAG_PASSPORT_PICTURE"

        const val FOR_SMARTSCANNER_APP = "FOR_SMARTSCANNER_APP"
    }

    private var mrzInfo: MRZInfo? = null
    private var nfcAdapter: NfcAdapter? = null
    private var pendingIntent: PendingIntent? = null
    private var label: String? = null
    private var language: String? = null
    private var mrzImage: String? = null
    private var locale: String? = null
    private var withPhoto: Boolean = true
    private var captureLog: Boolean = false
    private var enableLogging: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nfc)
        // Fetch MRZ from log intent
        val mrz = intent.getStringExtra(ScannerConstants.NFC_MRZ_STRING) as String
        // fetch data from intent
        language = intent.getStringExtra(ScannerConstants.LANGUAGE)
        locale = intent.getStringExtra(ScannerConstants.NFC_LOCALE)
        label = intent.getStringExtra(IntentData.KEY_LABEL)
        mrzImage = intent.getStringExtra(IntentData.KEY_MRZ_PHOTO)
        withPhoto = intent.getBooleanExtra(IntentData.KEY_WITH_PHOTO, true)
        captureLog = intent.getBooleanExtra(IntentData.KEY_CAPTURE_LOG, false)
        enableLogging = intent.getBooleanExtra(IntentData.KEY_ENABLE_LOGGGING, false)
        // setup nfc adapter
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        try {
            mrzInfo = MRZInfo(mrz)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        finally {
            // when an exception occurs and mrzInfo is still null execute initialization of MrzInfo
            try {
                if (mrzInfo == null) {
                    mrzInfo = MRZInfo(mrz)
                }
            } catch (ioe: IllegalArgumentException) {
                ioe.printStackTrace()
                this.finish()
                Toast.makeText(applicationContext, "Invalid MRZ scanned", Toast.LENGTH_SHORT).show()
            }

        }
        showNFCFragment()
        if (captureLog) {
            val documentNumber = "MRZ documentNumber: ${mrzInfo?.documentNumber}"
            Sentry.captureMessage(documentNumber)
        }
    }

    public override fun onResume() {
        super.onResume()
        val flags = if (VERSION.SDK_INT >= VERSION_CODES.S) {
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else PendingIntent.FLAG_UPDATE_CURRENT or 0
        if (nfcAdapter != null && nfcAdapter?.isEnabled == true) {
            pendingIntent = PendingIntent.getActivity(this, 0,
                Intent(this, this.javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), flags)
        } else checkNFC()
    }

    public override fun onPause() {
        super.onPause()
    }

    public override fun onNewIntent(intent: Intent) {
        if (NfcAdapter.ACTION_TAG_DISCOVERED == intent.action || NfcAdapter.ACTION_TECH_DISCOVERED == intent.action) {
            // drop NFC events
            handleIntent(intent)
        } else {
            super.onNewIntent(intent)
        }
    }

    /////////////////////////////////////////////////////
    //  NFC Fragment events
    /////////////////////////////////////////////////////
    private fun showNFCFragment() {
        if (mrzInfo != null) {
            supportFragmentManager.beginTransaction()
                    .replace(R.id.container,
                            NFCFragment.newInstance(mrzInfo = mrzInfo, label = label, language = language, locale = locale, withPhoto = withPhoto, captureLog = captureLog), TAG_NFC)
                    .commit()
        }
    }

    private fun checkNFC() {
        val dialog: AlertDialog.Builder = AlertDialog.Builder(this)
        dialog.setMessage(getString(R.string.warning_enable_nfc))
        dialog.setPositiveButton(R.string.label_turn_on) { alert, which ->
            val intent = Intent(Settings.ACTION_NFC_SETTINGS)
            startActivity(intent)
        }
        dialog.setNegativeButton(R.string.label_close) { alert, which -> }
        dialog.show()
    }

    private fun handleIntent(intent: Intent) {
        val fragmentByTag = supportFragmentManager.findFragmentByTag(TAG_NFC)
        if (fragmentByTag is NFCFragment) {
            fragmentByTag.handleNfcTag(intent)
        }
    }

    override fun onEnableNfc() {
        if (nfcAdapter != null) {
            if (nfcAdapter?.isEnabled == false) {
                showWirelessSettings()
            }
            nfcAdapter?.enableForegroundDispatch(this@NFCActivity, pendingIntent, null, null)
        } else {
            Toast.makeText(this, R.string.required_nfc_not_supported, Toast.LENGTH_LONG).show()
        }
    }

    override fun onDisableNfc() {
        val nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        nfcAdapter.disableForegroundDispatch(this)
    }

    override fun onPassportRead(passport: Passport?) {
        val action = intent.getStringExtra(ScannerConstants.NFC_ACTION)
        val nfcResult = NFCResult.formatResult(passport = passport, locale = locale, mrzInfo = mrzInfo, mrzImage = mrzImage)

        if (action == ScannerConstants.IDPASS_SMARTSCANNER_NFC_INTENT ||
                action == ScannerConstants.IDPASS_SMARTSCANNER_ODK_NFC_INTENT) {
            // Send NFC Results via Bundle
            val bundle = Bundle()
            Log.d(TAG, "Success from NFC -- BUNDLE")
            Log.d(TAG, "value: $passport")
            if (intent.action == ScannerConstants.IDPASS_SMARTSCANNER_ODK_NFC_INTENT) {
                bundle.putString(ScannerConstants.IDPASS_ODK_INTENT_DATA, nfcResult.documentNumber)
            }
            bundle.putString(ScannerConstants.NFC_GIVEN_NAMES, nfcResult.givenNames)
            bundle.putString(ScannerConstants.NFC_SURNAME, nfcResult.surname)
            bundle.putString(ScannerConstants.NFC_NAME_OF_HOLDER, nfcResult.nameOfHolder)
            bundle.putString(ScannerConstants.NFC_GENDER, nfcResult.gender)
            bundle.putString(ScannerConstants.NFC_DOCUMENT_NUMBER, nfcResult.documentNumber)
            bundle.putString(ScannerConstants.NFC_EXPIRY_DATE, nfcResult.dateOfExpiry)
            bundle.putString(ScannerConstants.NFC_ISSUING_STATE, nfcResult.issuingState)
            bundle.putString(ScannerConstants.NFC_NATIONALITY, nfcResult.nationality)
            bundle.putString(ScannerConstants.NFC_OTHER_NAMES, nfcResult.otherNames)
            bundle.putString(ScannerConstants.NFC_DATE_OF_BIRTH, nfcResult.dateOfBirth)
            bundle.putString(ScannerConstants.NFC_CUSTODY_INFO, nfcResult.custodyInformation)
            bundle.putString(ScannerConstants.NFC_PROFESSION, nfcResult.profession)
            bundle.putString(ScannerConstants.NFC_TELEPHONE, nfcResult.telephone)
            bundle.putString(ScannerConstants.NFC_TITLE, nfcResult.title)
            bundle.putString(ScannerConstants.NFC_DATE_TIME_PERSONALIZATION, nfcResult.dateAndTimeOfPersonalization)
            bundle.putString(ScannerConstants.NFC_DATE_OF_ISSUE, nfcResult.dateOfIssue)
            bundle.putString(ScannerConstants.NFC_ENDORSEMENTS_AND_OBSERVATIONS, nfcResult.endorsementsAndObservations)
            bundle.putString(ScannerConstants.NFC_ISSUING_AUTHORITY, nfcResult.issuingAuthority)
            bundle.putString(ScannerConstants.NFC_PERSONAL_SYSTEM_SERIAL_NUMBER, nfcResult.personalizationSystemSerialNumber)
            bundle.putString(ScannerConstants.NFC_TAX_EXIT_REQUIREMENTS, nfcResult.taxOrExitRequirements)
            bundle.putString(ScannerConstants.MODE, Modes.NFC_SCAN.value)
            val result = Intent()
            val prefix = if (intent.hasExtra(ScannerConstants.IDPASS_ODK_PREFIX_EXTRA)) {
                intent.getStringExtra(ScannerConstants.IDPASS_ODK_PREFIX_EXTRA)
            } else { "" }
            result.putExtra(ScannerConstants.RESULT, bundle)
            // Copy all the values in the intent result to be compatible with other implementations than commcare
            for (key in bundle.keySet()) {
                result.putExtra(prefix + key, bundle.getString(key))
            }
            setResult(Activity.RESULT_OK, result)
            finish()
        } else {
            // Send NFC Results via App
            if (intent.hasExtra(FOR_SMARTSCANNER_APP)) showFragmentDetails(passport, language, locale)
            else {
                // Send NFC Results via Plugin
                val data = Intent()
                Log.d(TAG, "Success from NFC -- RESULT")
                Log.d(TAG, "value: $nfcResult")
                data.putExtra(SmartScannerActivity.SCANNER_RESULT, Gson().toJson(nfcResult))
                setResult(Activity.RESULT_OK, data)
                finish()
            }
        }
    }

    override fun onCardException(cardException: Exception?) {
        cardException?.printStackTrace()
    }

    private fun showWirelessSettings() {
        Toast.makeText(this, getString(R.string.warning_enable_nfc), Toast.LENGTH_SHORT).show()
        val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS)
        startActivity(intent)
    }


    private fun showFragmentDetails(passport: Passport?, language: String?, locale: String?) {
        supportFragmentManager.beginTransaction()
                .replace(R.id.container, PassportDetailsFragment.newInstance(passport, language, locale))
                .addToBackStack(TAG_PASSPORT_DETAILS)
                .commit()
    }

    private fun showFragmentPhoto(bitmap: Bitmap) {
        supportFragmentManager.beginTransaction()
                .replace(R.id.container, PassportPhotoFragment.newInstance(bitmap))
                .addToBackStack(TAG_PASSPORT_PICTURE)
                .commit()
    }


    override fun onImageSelected(bitmap: Bitmap?) {
        if (bitmap != null) {
            showFragmentPhoto(bitmap)
        }
    }
}