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
import org.idpass.smartscanner.lib.SmartScannerActivity.Companion.SCANNER_IMAGE_TYPE
import org.idpass.smartscanner.lib.SmartScannerActivity.Companion.SCANNER_RESULT
import org.idpass.smartscanner.lib.SmartScannerActivity.Companion.SCANNER_RESULT_BYTES
import org.idpass.smartscanner.lib.nfc.NFCActivity
import org.idpass.smartscanner.lib.scanner.config.*
import org.idpass.smartscanner.lib.scanner.config.Config.Companion.OP_SCANNER
import org.idpass.smartscanner.lib.scanner.config.Config.Companion.ORIENTATION
import org.newlogic.smartscanner.databinding.ActivityMainBinding
import org.newlogic.smartscanner.result.IDPassResultActivity
import org.newlogic.smartscanner.result.ResultActivity
import org.newlogic.smartscanner.settings.SettingsActivity


class MainActivity : AppCompatActivity() {


    companion object {
        const val OP_SCANNER = 1001
    }

    private var preference : SharedPreferences? = null
    private lateinit var binding : ActivityMainBinding
    private var imageType = ImageResultType.PATH.value

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        preference = getSharedPreferences(Config.SHARED, Context.MODE_PRIVATE)
    }

    override fun onStart() {
        super.onStart()
        // Choose scan modes
        binding.itemBarcode.item.setOnClickListener { scanBarcode(BarcodeOptions.default) }
        binding.itemIdpassLite.item.setOnClickListener { scanIDPassLite() }
        binding.itemMrz.item.setOnClickListener { scanMRZ() }
        binding.itemNfc.item.setOnClickListener { scanNFC() }
        binding.itemPdf417.item.setOnClickListener { scanPDF417() }
        binding.itemQr.item.setOnClickListener { scanQRCode() }
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
                language = preference?.getString(Language.NAME, Language.EN),
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
                language = preference?.getString(Language.NAME, Language.EN),
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
                language = preference?.getString(Language.NAME, Language.EN),
                config = sampleConfig(isManualCapture = true, orientation = preference?.getString(ORIENTATION, Orientation.PORTRAIT.value)),
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
                    language = preference?.getString(Language.NAME, Language.EN),
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

    private fun scanPDF417() {
        val intent = Intent(this, SmartScannerActivity::class.java)
        intent.putExtra(
                SmartScannerActivity.SCANNER_OPTIONS,
                ScannerOptions(
                    mode = Modes.PDF_417.value,
                    language = getLanguage(preference),
                    scannerSize = ScannerSize.SMALL.value,
                    config = sampleConfig(false)
                )
        )
        startActivityForResult(intent, OP_SCANNER)
    }

    private fun scanQRCode()  {
        val intent = Intent(this, SmartScannerActivity::class.java)
        intent.putExtra(
                SmartScannerActivity.SCANNER_OPTIONS,
                ScannerOptions(
                        mode = Modes.QRCODE.value,
                        language = preference?.getString(Language.NAME, Language.EN),
                        scannerSize = ScannerSize.LARGE.value,
                        config = sampleConfig(false)
                )
        )
        startActivityForResult(intent, OP_SCANNER)
    }

    private fun scanQRCodeGzip()  {
        val intent = ScannerIntent.intentQRCode(
            isGzipped = true,
            isJson = true,
            jsonPath = "" // ex: "$.members[1].lastName"
        )
        startActivityForResult(intent, OP_SCANNER)
    }

    private fun sampleConfig(isManualCapture: Boolean, label: String = "", orientation : String? = Orientation.PORTRAIT.value) = Config(
        branding = true,
        imageResultType = imageType,
        label = label,
        isManualCapture = isManualCapture,
        orientation = orientation
    )

    @SuppressLint("LogNotTimber")
    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        Log.d(SmartScannerActivity.TAG, "Scanner requestCode $requestCode")
        if (requestCode == OP_SCANNER) {
            Log.d(SmartScannerActivity.TAG, "Scanner resultCode $resultCode")
            if (resultCode == RESULT_OK) {
                val bundle = intent?.getBundleExtra(ScannerConstants.RESULT)
                if (bundle != null) {
                    // Get Result from Bundle Intent Call Out
                    if (bundle.getString(ScannerConstants.MODE) == Modes.IDPASS_LITE.value) {
                        // Go to ID PASS Lite Results Screen via bundle
                        val myIntent = Intent(this, IDPassResultActivity::class.java)
                        myIntent.putExtra(IDPassResultActivity.BUNDLE_RESULT, bundle)
                        startActivity(myIntent)
                    } else {
                        // Go to Results Screen via bundle
                        val resultIntent = Intent(this, ResultActivity::class.java)
                        resultIntent.putExtra(ResultActivity.BUNDLE_RESULT, bundle)
                        startActivity(resultIntent)
                    }
                } else {
                    // Get Result from Intent extras
                    val mode = intent?.getStringExtra(ScannerConstants.MODE)
                    if (mode == Modes.IDPASS_LITE.value) {
                        // Go to ID PASS Lite Results Screen
                        val resultBytes = intent.getByteArrayExtra(SCANNER_RESULT_BYTES)
                        val myIntent = Intent(this, IDPassResultActivity::class.java)
                        myIntent.putExtra(IDPassResultActivity.RESULT, resultBytes)
                        startActivity(myIntent)
                    } else {
                        // Go to Results Screen
                        val result = intent?.getStringExtra(SCANNER_RESULT)
                        val resultIntent = Intent(this, ResultActivity::class.java)
                        resultIntent.putExtra(ResultActivity.IMAGE_TYPE, intent?.getStringExtra(SCANNER_IMAGE_TYPE))
                        resultIntent.putExtra(ScannerConstants.MODE, mode)
                        resultIntent.putExtra(ResultActivity.RESULT, result)
                        startActivity(resultIntent)
                    }
                }
            }
        }
    }
}
