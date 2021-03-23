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

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.nfc.NfcAdapter
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import org.idpass.smartscanner.api.ScannerConstants
import org.idpass.smartscanner.lib.BuildConfig
import org.idpass.smartscanner.lib.R
import org.idpass.smartscanner.lib.SmartScannerActivity
import org.idpass.smartscanner.lib.nfc.passport.Passport
import org.idpass.smartscanner.lib.nfc.passport.PassportDetailsFragment
import org.idpass.smartscanner.lib.nfc.passport.PassportPhotoFragment
import org.idpass.smartscanner.lib.platform.extension.arrayToString
import org.idpass.smartscanner.lib.platform.utils.DateUtils
import org.idpass.smartscanner.lib.platform.utils.DateUtils.formatStandardDate
import org.idpass.smartscanner.lib.platform.utils.LoggerUtils
import org.idpass.smartscanner.lib.scanner.config.Modes
import org.jmrtd.lds.icao.MRZInfo
import java.util.*


class NFCActivity : FragmentActivity(), NFCFragment.NfcFragmentListener, PassportDetailsFragment.PassportDetailsFragmentListener, PassportPhotoFragment.PassportPhotoFragmentListener {

    companion object {
        private val TAG = NFCActivity::class.java.simpleName

        private val TAG_NFC = "TAG_NFC"
        private val TAG_PASSPORT_DETAILS = "TAG_PASSPORT_DETAILS"
        private val TAG_PASSPORT_PICTURE = "TAG_PASSPORT_PICTURE"

        const val FOR_MRZ_LOG = "FOR_MRZ_LOG"
        const val FOR_SMARTSCANNER_APP = "FOR_SMARTSCANNER_APP"
    }
    private val REQUEST_CODE_PERMISSIONS = 11
    private val REQUEST_CODE_PERMISSIONS_VERSION_R = 2296
    private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)

    private var mrzInfo: MRZInfo? = null
    private var nfcAdapter: NfcAdapter? = null
    private var pendingIntent: PendingIntent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nfc)
        if (BuildConfig.DEBUG) {
            setupLogs()
        }
        val mrz = intent.getStringExtra(ScannerConstants.NFC_MRZ_STRING) ?: run {
            intent.getStringExtra(FOR_MRZ_LOG)
        }
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        try {
            mrzInfo = MRZInfo(mrz)
            mrzInfo?.let {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.container, NFCFragment.newInstance(it), TAG_NFC)
                    .commit()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    public override fun onResume() {
        super.onResume()
        if (nfcAdapter != null && nfcAdapter?.isEnabled == true) {
            pendingIntent = PendingIntent.getActivity(this, 0,
                Intent(this, this.javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0)
        } else checkNFC()
    }

    public override fun onPause() {
        super.onPause()
    }

    public override fun onNewIntent(intent: Intent) {
        if (NfcAdapter.ACTION_TAG_DISCOVERED == intent.action || NfcAdapter.ACTION_TECH_DISCOVERED == intent.action) {
            // drop NFC events
            handleIntent(intent)
        }else{
            super.onNewIntent(intent)
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

    /////////////////////////////////////////////////////
    //  NFC Fragment events
    /////////////////////////////////////////////////////
    override fun onEnableNfc() {
        nfcAdapter?.let {
            if (!it.isEnabled) showWirelessSettings()
            it.enableForegroundDispatch(this, pendingIntent, null, null)
        }
    }

    override fun onDisableNfc() {
        val nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        nfcAdapter.disableForegroundDispatch(this)
    }

    override fun onPassportRead(passport: Passport?) {
        passport?.let {
            val action = intent.getStringExtra(ScannerConstants.NFC_ACTION)
            val personDetails = it.personDetails
            val additionalPersonDetails = it.additionalPersonDetails
            val additionalDocumentDetails = it.additionalDocumentDetails
            val currentLanguage = Locale.getDefault().displayLanguage
            // Get proper names
            var givenNames : String? = ""
            var surname : String? = ""
            if (currentLanguage.toLowerCase(Locale.ROOT).contains("en")) {
                givenNames = personDetails?.secondaryIdentifier?.replace("<<", " ")?.replace("<", "")
                surname = personDetails?.primaryIdentifier?.replace("<<", " ")?.replace("<", "")
            } else {
                val full = additionalPersonDetails?.nameOfHolder?.replace("<<", " ")?.replace("<", " ")
                val parts  = full?.split(" ")?.toMutableList()
                val firstName = parts!!.firstOrNull()
                parts.removeAt(0)
                givenNames = firstName+" "+parts[0]+" "+parts[1]
                surname = parts[3]
            }
            // Get proper date of birth
            val dateOfBirth = if (additionalPersonDetails?.fullDateOfBirth.isNullOrEmpty()) {
                DateUtils.toAdjustedDate (
                    formatStandardDate(personDetails?.dateOfBirth)
                )
            } else formatStandardDate(additionalPersonDetails?.fullDateOfBirth,"yyyyMMdd")

            // Send NFC Results
            if (action == ScannerConstants.IDPASS_SMARTSCANNER_NFC_INTENT ||
                action == ScannerConstants.IDPASS_SMARTSCANNER_ODK_NFC_INTENT ) {
                val bundle = Bundle()
                Log.d(TAG, "Success from NFC -- BUNDLE")
                Log.d(TAG, "value: $passport")
                if (intent.action == ScannerConstants.IDPASS_SMARTSCANNER_ODK_NFC_INTENT) {
                    bundle.putString(ScannerConstants.IDPASS_ODK_INTENT_DATA, personDetails?.documentNumber)
                }
                bundle.putString(ScannerConstants.NFC_GIVEN_NAMES, givenNames)
                bundle.putString(ScannerConstants.NFC_SURNAME, surname)
                bundle.putString(ScannerConstants.NFC_NAME_OF_HOLDER, additionalPersonDetails?.nameOfHolder)
                bundle.putString(ScannerConstants.NFC_GENDER, personDetails?.gender?.name)
                bundle.putString(ScannerConstants.NFC_DOCUMENT_NUMBER, personDetails?.documentNumber)
                bundle.putString(ScannerConstants.NFC_EXPIRY_DATE, DateUtils.toReadableDate(formatStandardDate(personDetails?.dateOfExpiry)))
                bundle.putString(ScannerConstants.NFC_ISSUING_STATE, personDetails?.issuingState)
                bundle.putString(ScannerConstants.NFC_NATIONALITY, personDetails?.nationality)
                bundle.putString(ScannerConstants.NFC_OTHER_NAMES, additionalPersonDetails?.otherNames?.arrayToString())
                bundle.putString(ScannerConstants.NFC_DATE_OF_BIRTH, dateOfBirth)
                bundle.putString(ScannerConstants.NFC_CUSTODY_INFO, additionalPersonDetails?.custodyInformation)
                bundle.putString(ScannerConstants.NFC_PROFESSION, additionalPersonDetails?.profession)
                bundle.putString(ScannerConstants.NFC_TELEPHONE, additionalPersonDetails?.telephone)
                bundle.putString(ScannerConstants.NFC_TITLE, additionalPersonDetails?.title)
                bundle.putString(ScannerConstants.NFC_DATE_TIME_PERSONALIZATION, additionalDocumentDetails?.dateAndTimeOfPersonalization)
                bundle.putString(ScannerConstants.NFC_DATE_OF_ISSUE, formatStandardDate(additionalDocumentDetails?.dateOfIssue, "yyyyMMdd"))
                bundle.putString(ScannerConstants.NFC_ENDORSEMENTS_AND_OBSERVATIONS, additionalDocumentDetails?.endorsementsAndObservations)
                bundle.putString(ScannerConstants.NFC_ISSUING_AUTHORITY, additionalDocumentDetails?.issuingAuthority)
                bundle.putString(ScannerConstants.NFC_PERSONAL_SYSTEM_SERIAL_NUMBER, additionalDocumentDetails?.personalizationSystemSerialNumber)
                bundle.putString(ScannerConstants.NFC_TAX_EXIT_REQUIREMENTS, additionalDocumentDetails?.taxOrExitRequirements)
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
                if (intent.hasExtra(FOR_SMARTSCANNER_APP)) showFragmentDetails(passport)
                else {
                    val result = NFCResult(
                        givenNames = givenNames,
                        surname = surname,
                        nameOfHolder = additionalPersonDetails?.nameOfHolder,
                        gender = personDetails?.gender?.name,
                        documentNumber = personDetails?.documentNumber,
                        dateOfExpiry = DateUtils.toReadableDate(formatStandardDate(personDetails?.dateOfExpiry)),
                        issuingState = personDetails?.issuingState,
                        nationality = personDetails?.nationality,
                        otherNames = additionalPersonDetails?.otherNames?.arrayToString(),
                        dateOfBirth = dateOfBirth,
                        custodyInformation = additionalPersonDetails?.custodyInformation,
                        profession = additionalPersonDetails?.profession,
                        telephone = additionalPersonDetails?.telephone,
                        title = additionalPersonDetails?.title,
                        dateAndTimeOfPersonalization = additionalDocumentDetails?.dateAndTimeOfPersonalization,
                        dateOfIssue = formatStandardDate(additionalDocumentDetails?.dateOfIssue, "yyyyMMdd"),
                        endorsementsAndObservations = additionalDocumentDetails?.endorsementsAndObservations,
                        issuingAuthority = additionalDocumentDetails?.issuingAuthority,
                        personalizationSystemSerialNumber = additionalDocumentDetails?.personalizationSystemSerialNumber,
                        taxOrExitRequirements = additionalDocumentDetails?.taxOrExitRequirements,
                        mrzOptional = mrzInfo?.optionalData1,
                        mrzOptional2 = mrzInfo?.optionalData2
                    )
                    val data = Intent()
                    Log.d(TAG, "Success from NFC -- RESULT")
                    Log.d(TAG, "value: $result")
                    data.putExtra(SmartScannerActivity.SCANNER_RESULT, Gson().toJson(result))
                    setResult(Activity.RESULT_OK, data)
                    finish()
                }
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


    private fun showFragmentDetails(passport: Passport?) {
        supportFragmentManager.beginTransaction()
                .replace(R.id.container, PassportDetailsFragment.newInstance(passport))
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

    private fun setupLogs() {
        if (allPermissionsGranted()) {
            LoggerUtils.writeLogToFile(identifier = "NFC")
        } else {
            requestStoragePermissions()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_CODE_PERMISSIONS_VERSION_R,
            REQUEST_CODE_PERMISSIONS -> {
                if (allPermissionsGranted()) {
                    LoggerUtils.writeLogToFile(identifier = "NFC")
                } else {
                    val container = findViewById<FrameLayout>(R.id.container)
                    val snackBar: Snackbar = Snackbar.make(container, R.string.required_perms_not_given, Snackbar.LENGTH_INDEFINITE)
                    snackBar.setAction(R.string.settings) {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        val uri = Uri.fromParts("package", packageName, null)
                        intent.data = uri
                        startActivity(intent)
                    }
                    snackBar.show()
                }
            }
        }
    }

    private fun requestStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.addCategory("android.intent.category.DEFAULT")
                intent.data = Uri.parse(String.format("package:%s", applicationContext.packageName))
                startActivityForResult(intent, REQUEST_CODE_PERMISSIONS_VERSION_R)
            } catch (e: Exception) {
                val intent = Intent()
                intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                startActivityForResult(intent, REQUEST_CODE_PERMISSIONS_VERSION_R)
            }
        } else {
            //below android 11
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }

    private fun allPermissionsGranted() : Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            REQUIRED_PERMISSIONS.all {
                ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
            }
        }
    }
}