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
import org.idpass.smartscanner.lib.SmartScannerActivity
import org.idpass.smartscanner.lib.SmartScannerActivity.Companion.SCANNER_FAIL_RESULT
import org.idpass.smartscanner.lib.SmartScannerActivity.Companion.SCANNER_HEADER_RESULT
import org.idpass.smartscanner.lib.SmartScannerActivity.Companion.SCANNER_IMAGE_TYPE
import org.idpass.smartscanner.lib.SmartScannerActivity.Companion.SCANNER_INTENT_EXTRAS
import org.idpass.smartscanner.lib.SmartScannerActivity.Companion.SCANNER_JWT_CONFIG_UPDATE
import org.idpass.smartscanner.lib.SmartScannerActivity.Companion.SCANNER_RAW_RESULT
import org.idpass.smartscanner.lib.SmartScannerActivity.Companion.SCANNER_RESULT
import org.idpass.smartscanner.lib.SmartScannerActivity.Companion.SCANNER_RESULT_BYTES
import org.idpass.smartscanner.lib.SmartScannerActivity.Companion.SCANNER_SETTINGS_CALL
import org.idpass.smartscanner.lib.SmartScannerActivity.Companion.SCANNER_SIGNATURE_VERIFICATION
import org.idpass.smartscanner.lib.nfc.NFCActivity
import org.idpass.smartscanner.lib.scanner.config.*
import org.idpass.smartscanner.lib.scanner.config.Config.Companion.HEIGHT_GUIDE
import org.idpass.smartscanner.lib.scanner.config.Config.Companion.ORIENTATION
import org.idpass.smartscanner.lib.scanner.config.Config.Companion.REGEX
import org.idpass.smartscanner.lib.scanner.config.Config.Companion.WIDTH_GUIDE
import org.idpass.smartscanner.lib.scanner.config.Config.Companion.X_GUIDE
import org.idpass.smartscanner.lib.scanner.config.Config.Companion.Y_GUIDE
import org.idpass.smartscanner.lib.utils.LanguageUtils
import org.newlogic.smartscanner.databinding.ActivityMainBinding
import org.newlogic.smartscanner.result.IDPassResultActivity
import org.newlogic.smartscanner.result.ResultActivity
import org.newlogic.smartscanner.settings.SettingsActivity
import java.util.Locale


class MainActivity : AppCompatActivity() {

    companion object {

        const val OP_SCANNER = 1001
        const val OP_SETTINGS = 1002
        private val imageType = ImageResultType.PATH.value

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
    private var currentLanguage: String = Language.EN

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preference = getSharedPreferences(Config.SHARED, Context.MODE_PRIVATE)
        currentLanguage = preference?.getString(Language.NAME, Language.EN) ?: Language.EN
        LanguageUtils.changeLanguageIfNotMatch(this, currentLanguage)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
    }

    override fun onResume() {
        super.onResume()
        checkLanguage()
    }

    private fun checkLanguage() {
        //if saved language does not match locale language.
        if (LanguageUtils.getLocaleLanguage(this) != Locale(currentLanguage).language) {
            //Recreate so that the correct language will reflect on app.
            recreate()
        }
    }

    override fun onStart() {
        super.onStart()
        // Choose scan modes
        binding.itemBarcode.item.setOnClickListener { scanBarcode(BarcodeOptions.default) }
        binding.itemIdpassLite.item.setOnClickListener { scanIDPassLite() }
        binding.itemMrz.item.setOnClickListener { scanMRZ() }
        binding.itemOcr.item.setOnClickListener { scanOCR() }
        binding.itemQr.item.setOnClickListener { scanQRCode() }
//        binding.itemQrGzip.item.setOnClickListener { scanQRCodeGzip() }
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
                language = getLanguage(),
                scannerSize = ScannerSize.SMALL.value,
                config = sampleConfig(false).copy(showSettings = true),
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
                language = getLanguage(),
                scannerSize = ScannerSize.LARGE.value,
                config = sampleConfig(false).copy(showSettings = true)
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
                language = getLanguage(),
                config = Config(
                    branding = true,
                    imageResultType = imageType,
                    label = "",
                    isManualCapture = true,
                    orientation = getOrientation(),
                    showGuide = true,
                    showSettings = true
                )
            )
        )
        startActivityForResult(intent, OP_SCANNER)
    }

    private fun scanOCR() {
        val intent = Intent(this, SmartScannerActivity::class.java)
        val enableOCRGuide = getWidthGuide() != null && getHeightGuide() != null && getWidthGuide() != 0 && getHeightGuide() != 0
        intent.putExtra(
            SmartScannerActivity.SCANNER_OPTIONS,
            ScannerOptions(
                mode = Modes.OCR.value,
                language = getLanguage(),
                ocrOptions = OCROptions(
                    analyzeStart = 1000,
                    regex = getRegex()
                ),
                config = Config(
                    branding = true,
                    imageResultType = imageType,
                    label = "",
                    isManualCapture = false,
                    showOcrGuide = enableOCRGuide,
                    orientation = getOrientation(),
                    widthGuide = getWidthGuide() ?: 0,
                    heightGuide = getHeightGuide() ?: 0,
                    xGuide = getXGuide(),
                    yGuide = getYGuide(),
                    showSettings = true
                )
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
                    language = getLanguage(),
                    nfcOptions = NFCOptions.default,
                    sentryLogger = SentryLogger.default,
                    config = Config(
                        header = getString(R.string.label_scan_nfc_capture),
                        subHeader = getString(R.string.label_scan_nfc_via_mrz),
                        isManualCapture = false,
                        branding = true,
                        showGuide = true,
                        showSettings = true
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
                language = getLanguage(),
                scannerSize = ScannerSize.SMALL.value,
                config = sampleConfig(false).copy(showSettings = true)
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
                qrCodeOptions = QRcodeOptions(isJson = true),
                config = sampleConfig(false).copy(showSettings = true)
            )
        )
        startActivityForResult(intent, OP_SCANNER)
    }

    private fun getOrientation() = preference?.getString(ORIENTATION, Orientation.PORTRAIT.value)
    private fun getLanguage() = preference?.getString(Language.NAME, Language.EN)
    private fun getRegex() = preference?.getString(REGEX, null)
    private fun getWidthGuide() = preference?.getInt(WIDTH_GUIDE, 0)
    private fun getHeightGuide() = preference?.getInt(HEIGHT_GUIDE, 0)
    private fun getXGuide() = preference?.getFloat(X_GUIDE, 0f)
    private fun getYGuide() = preference?.getFloat(Y_GUIDE, 0f)

    @SuppressLint("LogNotTimber")
    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        Log.d(SmartScannerActivity.TAG, "Scanner requestCode $requestCode")
        if (requestCode == OP_SETTINGS) {
            if (resultCode == RESULT_OK) {
                val extras: ScannerOptions? = intent?.getParcelableExtra(SettingsActivity.SCANNER_INTENT_EXTRAS)

                val scannerIntent = Intent(this, SmartScannerActivity::class.java)
                scannerIntent.putExtra(SmartScannerActivity.SCANNER_OPTIONS, extras)

                startActivityForResult(scannerIntent, OP_SCANNER)
            }
        } else if (requestCode == OP_SCANNER) {
            Log.d(SmartScannerActivity.TAG, "Scanner resultCode $resultCode")
            if (resultCode == RESULT_OK) {
                val bundle = intent?.getBundleExtra(ScannerConstants.RESULT)
                val mIntent: Intent;

                if (bundle != null) {
                    // Get Result from Bundle Intent Call Out
                    if (bundle.getString(ScannerConstants.MODE) == Modes.IDPASS_LITE.value) {
                        // Go to ID PASS Lite Results Screen via bundle
                        mIntent = Intent(this, IDPassResultActivity::class.java)
                        mIntent.putExtra(IDPassResultActivity.BUNDLE_RESULT, bundle)
                    } else {
                        // Go to Results Screen via bundle
                        mIntent = Intent(this, ResultActivity::class.java)
                        mIntent.putExtra(ResultActivity.BUNDLE_RESULT, bundle)
                    }
                } else {
                    // Get Result from Intent extras
                    if (intent?.getStringExtra(ScannerConstants.MODE) == Modes.IDPASS_LITE.value) {
                        // Go to ID PASS Lite Results Screen
                        val resultBytes = intent.getByteArrayExtra(SCANNER_RESULT_BYTES)
                        mIntent = Intent(this, IDPassResultActivity::class.java)
                        mIntent.putExtra(IDPassResultActivity.RESULT, resultBytes)
                    } else {

                        // Check if it should go to the settings instead
                        val isConfigUpdated = intent?.getBooleanExtra(SCANNER_JWT_CONFIG_UPDATE, false) ?: false

                        // should go to settings
                        if (isConfigUpdated) {
                            val sIntent = Intent(this, SettingsActivity::class.java)
                            sIntent.putExtra(SettingsActivity.CONFIG_UPDATED, true)
                            startActivity(sIntent)
                            return
                        }

                        val isSettingsCall = intent?.getBooleanExtra(SCANNER_SETTINGS_CALL, false) ?: false

                        // should go to settings
                        if (isSettingsCall) {
                            val extras = intent?.getParcelableExtra<ScannerOptions>(SCANNER_INTENT_EXTRAS)

                            val sIntent = Intent(this, SettingsActivity::class.java)
                            sIntent.putExtra(SettingsActivity.SCANNER_INTENT_EXTRAS, extras)
                            if (intent?.getStringExtra(ScannerConstants.MODE) == Modes.OCR.value) {
                                sIntent.putExtra(SettingsActivity.OCR_SETTINGS_CALL, true)
                            }
                            startActivityForResult(sIntent, OP_SETTINGS)
                            return
                        }

                        // Go to Results Screen
                        val result = intent?.getStringExtra(SCANNER_RESULT)
                        val verified = intent?.getBooleanExtra(SCANNER_SIGNATURE_VERIFICATION, false)
                        val rawResult = intent?.getStringExtra(SCANNER_RAW_RESULT)
                        val failResult = intent?.getStringExtra(SCANNER_FAIL_RESULT)
                        val headerResult = intent?.getStringExtra(SCANNER_HEADER_RESULT)

                        mIntent = Intent(this, ResultActivity::class.java)
                        mIntent.putExtra(ResultActivity.SIGNATURE_VERIFIED, verified)
                        mIntent.putExtra(ResultActivity.IMAGE_TYPE, intent?.getStringExtra(SCANNER_IMAGE_TYPE))
                        mIntent.putExtra(ResultActivity.RESULT, result)
                        mIntent.putExtra(ResultActivity.FAIL_RESULT, failResult)
                        mIntent.putExtra(ResultActivity.RAW_RESULT, rawResult)
                        mIntent.putExtra(ResultActivity.HEADER_RESULT, headerResult)

                    }
                }


                mIntent.putExtra(ScannerConstants.MODE, intent?.getStringExtra(ScannerConstants.MODE))
                startActivity(mIntent)
            }
        }
    }
}