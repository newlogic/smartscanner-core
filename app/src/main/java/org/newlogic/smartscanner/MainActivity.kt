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
package org.newlogic.smartscanner

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.nfc.NfcAdapter
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import org.idpass.smartscanner.api.ScannerConstants
import org.idpass.smartscanner.api.ScannerIntent
import org.idpass.smartscanner.lib.SmartScannerActivity
import org.idpass.smartscanner.lib.SmartScannerActivity.Companion.SCANNER_RESULT
import org.idpass.smartscanner.lib.SmartScannerActivity.Companion.SCANNER_RESULT_BYTES
import org.idpass.smartscanner.lib.nfc.NFCActivity
import org.idpass.smartscanner.lib.scanner.config.*
import org.newlogic.smartscanner.databinding.ActivityMainBinding
import org.newlogic.smartscanner.result.IDPassResultActivity
import org.newlogic.smartscanner.result.ResultActivity
import org.newlogic.smartscanner.settings.SettingsActivity
import org.newlogic.smartscanner.settings.SettingsActivity.Companion.ORIENTATION


class MainActivity : AppCompatActivity() {

    companion object {
        const val OP_SCANNER = 1001
        var imageType = ImageResultType.PATH.value

        private fun sampleConfig(isManualCapture: Boolean, label: String = "", orientation : String? = Orientation.PORTRAIT.value) = Config(
            branding = true,
            imageResultType = imageType,
            label = label,
            isManualCapture = isManualCapture,
            orientation = orientation
        )
    }

    private var preference : SharedPreferences? = null
    private lateinit var binding : ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        preference = getSharedPreferences(SmartScannerApplication.SHARED, Context.MODE_PRIVATE)
    }

    override fun onStart() {
        super.onStart()
        // Choose scan type
        binding.itemBarcode.item.setOnClickListener { scanBarcode(BarcodeOptions.default) }
        binding.itemIdpassLite.item.setOnClickListener { scanIDPassLite() }
        binding.itemMrz.item.setOnClickListener { scanMRZ() }
        binding.itemQR.item.setOnClickListener { scanQRCode() }
        binding.itemNfc.item.setOnClickListener { scanNFC() }
        // Change language
        binding.languageSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun isNFCSupported() : Boolean {
        val adapter = NfcAdapter.getDefaultAdapter(this)
        return adapter != null
    }

    private fun scanBarcode(barcodeOptions: BarcodeOptions? = null) {
        val intent = Intent(this, SmartScannerActivity::class.java)
        intent.putExtra(
            SmartScannerActivity.SCANNER_OPTIONS,
            ScannerOptions(
                mode = Modes.BARCODE.value,
                language = getLanguage(preference),
                scannerSize = ScannerSize.SMALL.value,
                config = sampleConfig(false),
                barcodeOptions = barcodeOptions
            )
        )
        startActivityForResult(intent, OP_SCANNER)
    }

    private fun scanIDPassLite() {
        val intent = Intent(this, SmartScannerActivity::class.java)
        intent.putExtra(
            SmartScannerActivity.SCANNER_OPTIONS,
            ScannerOptions(
                mode = Modes.IDPASS_LITE.value,
                language = getLanguage(preference),
                scannerSize = ScannerSize.LARGE.value,
                config = sampleConfig(false)
            )
        )
        startActivityForResult(intent, OP_SCANNER)
    }

    private fun scanMRZ() {
        val intent = Intent(this, SmartScannerActivity::class.java)
        intent.putExtra(
            SmartScannerActivity.SCANNER_OPTIONS,
            ScannerOptions(
                mode = Modes.MRZ.value,
                language = getLanguage(preference),
                config = sampleConfig(isManualCapture = true, orientation = getOrientation(preference)),
            )
        )
        startActivityForResult(intent, OP_SCANNER)
    }

    private fun scanNFC() {
        if (isNFCSupported()) {
            val intent = Intent(this, SmartScannerActivity::class.java)
            intent.putExtra(NFCActivity.FOR_SMARTSCANNER_APP, true)
            intent.putExtra(
                SmartScannerActivity.SCANNER_OPTIONS,
                ScannerOptions(
                    mode = Modes.NFC_SCAN.value,
                    language = getLanguage(preference),
                    nfcOptions = NFCOptions.default,
                    sentryLogger = SentryLogger.default,
                    config = Config(
                        header = getString(R.string.label_scan_nfc_capture),
                        subHeader = getString(R.string.label_scan_nfc_via_mrz),
                        isManualCapture = false,
                        branding = true
                    )
                )
            )
            startActivityForResult(intent, OP_SCANNER)
        } else Snackbar.make(binding.main, R.string.required_nfc_not_supported, Snackbar.LENGTH_LONG).show()

    }

    private fun scanQRCode()  {
        val intent = ScannerIntent.intentQRCode(
            isGzipped = true,
            isJson = true,
            jsonPath = "" // ex: "$.members[1].lastName"
        )
        startActivityForResult(intent, OP_SCANNER)
    }

    private fun getOrientation(pref : SharedPreferences?) = pref?.getString(ORIENTATION, Orientation.PORTRAIT.value)
    private fun getLanguage(pref : SharedPreferences?) = pref?.getString(Language.NAME, Language.EN)

    @SuppressLint("LogNotTimber")
    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        Log.d(SmartScannerActivity.TAG, "Scanner requestCode $requestCode")
        if (requestCode == OP_SCANNER) {
            Log.d(SmartScannerActivity.TAG, "Scanner resultCode $resultCode")
            if (resultCode == RESULT_OK) {
                // Get Result from Bundle Intent Call Out
                intent?.getBundleExtra(ScannerConstants.RESULT)?.let { bundleResult ->
                    Log.d(SmartScannerActivity.TAG, "Scanner result bundle: $bundleResult")
                    if (bundleResult.getString(ScannerConstants.MODE) == Modes.IDPASS_LITE.value) {
                        // Go to ID PASS Lite Results Screen via bundle
                        val myIntent = Intent(this, IDPassResultActivity::class.java)
                        myIntent.putExtra(IDPassResultActivity.BUNDLE_RESULT, bundleResult)
                        startActivity(myIntent)
                    } else {
                        // Go to Barcode/MRZ Results Screen via bundle
                        val resultIntent = Intent(this, ResultActivity::class.java)
                        resultIntent.putExtra(ResultActivity.BUNDLE_RESULT, bundleResult)
                        startActivity(resultIntent)
                    }
                } ?: run {
                    // Get Result from JSON String
                    val result = intent?.getStringExtra(SCANNER_RESULT)
                    Log.d(SmartScannerActivity.TAG, "Scanner result string: $result")
                    if (!result.isNullOrEmpty()) {
                        // Go to Barcode/MRZ Results Screen
                        val resultIntent = Intent(this, ResultActivity::class.java)
                        resultIntent.putExtra(ResultActivity.RESULT, result)
                        startActivity(resultIntent)
                    } else {
                        // Go to ID PASS Lite Results Screen
                        val resultBytes = intent?.getByteArrayExtra(SCANNER_RESULT_BYTES)
                        val myIntent = Intent(this, IDPassResultActivity::class.java)
                        myIntent.putExtra(IDPassResultActivity.RESULT, resultBytes)
                        startActivity(myIntent)
                    }
                }
            }
        }
    }
}