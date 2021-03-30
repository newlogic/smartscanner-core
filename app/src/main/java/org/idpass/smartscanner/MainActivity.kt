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
package org.idpass.smartscanner

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.nfc.NfcAdapter
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import org.idpass.smartscanner.api.ScannerConstants
import org.idpass.smartscanner.api.ScannerIntent
import org.idpass.smartscanner.databinding.ActivityMainBinding
import org.idpass.smartscanner.lib.BuildConfig
import org.idpass.smartscanner.lib.R.string.required_nfc_not_supported
import org.idpass.smartscanner.lib.R.string.required_perms_not_given
import org.idpass.smartscanner.lib.SmartScannerActivity
import org.idpass.smartscanner.lib.SmartScannerActivity.Companion.SCANNER_RESULT
import org.idpass.smartscanner.lib.SmartScannerActivity.Companion.SCANNER_RESULT_BYTES
import org.idpass.smartscanner.lib.nfc.NFCActivity
import org.idpass.smartscanner.lib.platform.utils.FileUtils
import org.idpass.smartscanner.lib.scanner.config.*
import org.idpass.smartscanner.result.IDPassResultActivity
import org.idpass.smartscanner.result.ResultActivity
import org.idpass.smartscanner.settings.SettingsActivity


class MainActivity : AppCompatActivity() {

    companion object {
        const val OP_SCANNER = 1001
        var imageType = ImageResultType.PATH.value

        private fun sampleConfig(isManualCapture: Boolean, label: String = "") = Config(
            branding = true,
            imageResultType = imageType,
            label = label,
            isManualCapture = isManualCapture
        )
    }

    private val REQUEST_CODE_PERMISSIONS = 11
    private val REQUEST_CODE_PERMISSIONS_VERSION_R = 2296
    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    private lateinit var binding : ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        if (BuildConfig.DEBUG) {
            setupAppDirectory()
        }
    }

    private fun setupAppDirectory() {
        if (allPermissionsGranted()) {
            FileUtils.createSmartScannerDirs()
        } else {
            requestStoragePermissions()
        }
    }

    override fun onStart() {
        super.onStart()
        // Choose scan type
        binding.itemBarcode.item.setOnClickListener { scanBarcode(BarcodeOptions.default) }
        binding.itemIdpassLite.item.setOnClickListener { scanIDPassLite() }
        binding.itemMrz.item.setOnClickListener { scanMRZ() }
        binding.itemQR.item.setOnClickListener { scanQRCode() }
        binding.itemNfc.item.setOnClickListener {
            if (isNFCSupported()) {
                val mrzFromTxt = FileUtils.getMRZFromTxtFile()
                if (mrzFromTxt.isNullOrEmpty()) {
                    scanNFC()
                } else {
                    val resultIntent = Intent(this, NFCActivity::class.java)
                    resultIntent.putExtra(NFCActivity.FOR_MRZ_LOG, mrzFromTxt)
                    startActivity(resultIntent)
                }
            } else Snackbar.make(binding.main, required_nfc_not_supported, Snackbar.LENGTH_LONG).show()
        }
        // Change language
        binding.languageSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun isNFCSupported() : Boolean {
        val adapter = NfcAdapter.getDefaultAdapter(this)
        return adapter != null
    }

    @SuppressLint("LogNotTimber")
    private fun startIntentCallOut() {
        try {
            // Note: Scanner via intent can either be for barcode, idpass-lite, mrz, nfc, qrcode
            // Please see ScannerIntent class for more details
            // barcode -> val intent = ScannerIntent.intentBarcode()
            // idpass-lite -> val intent = ScannerIntent.intentIDPassLite()
            // mrz -> val intent = ScannerIntent.intentMrz()
            // nfc -> val intent = ScannerIntent.intentNFCScan()
            // qrcode -> val intent = ScannerIntent.intentQRCode()
            val intent = ScannerIntent.intentNFCScan()
            startActivityForResult(intent, OP_SCANNER)
        } catch (ex: ActivityNotFoundException) {
            ex.printStackTrace()
            Log.e(SmartScannerActivity.TAG, "ID PASS SmartScanner is not installed!")
        }
    }

    private fun scanBarcode(barcodeOptions: BarcodeOptions? = null) {
        val intent = Intent(this, SmartScannerActivity::class.java)
        intent.putExtra(
            SmartScannerActivity.SCANNER_OPTIONS,
            ScannerOptions(
                mode = Modes.BARCODE.value,
                scannerSize = ScannerSize.LARGE.value,
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
            ScannerOptions(mode = Modes.MRZ.value, config = sampleConfig(true))
        )
        startActivityForResult(intent, OP_SCANNER)
    }

    private fun scanNFC() {
        val intent = Intent(this, SmartScannerActivity::class.java)
        intent.putExtra(NFCActivity.FOR_SMARTSCANNER_APP, true)
        intent.putExtra(
            SmartScannerActivity.SCANNER_OPTIONS,
            ScannerOptions.defaultForNFCScan
        )
        startActivityForResult(intent, OP_SCANNER)
    }

    @SuppressLint("InflateParams")
    private fun scanQRCode()  {
        val bottomSheetDialog = BottomSheetDialog(this)
        val sheetViewBarcode = layoutInflater.inflate(R.layout.sheet_qrcode, null)
        bottomSheetDialog.setContentView(sheetViewBarcode)
        // bottom sheet ids
        val btnGzipped = sheetViewBarcode.findViewById<LinearLayout>(R.id.btnGzipped)
        val btnRegular = sheetViewBarcode.findViewById<LinearLayout>(R.id.btnRegular)
        val btnCancel = sheetViewBarcode.findViewById<LinearLayout>(R.id.btnCancel)
        // bottom sheet listeners
        btnGzipped.setOnClickListener {
            val intent = ScannerIntent.intentQRCode(
                isGzipped = true,
                isJson = true,
                jsonPath = "" // ex: "$.members[1].lastName"
            )
            startActivityForResult(intent, OP_SCANNER)
            bottomSheetDialog.dismiss()
        }
        btnRegular.setOnClickListener {
            val intent = ScannerIntent.intentQRCode()
            startActivityForResult(intent, OP_SCANNER)
            bottomSheetDialog.dismiss()
        }
        btnCancel.setOnClickListener { bottomSheetDialog.dismiss() }
        bottomSheetDialog.show()
    }

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
                    if (result != null) {
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_CODE_PERMISSIONS_VERSION_R,
            REQUEST_CODE_PERMISSIONS -> {
                if (allPermissionsGranted()) {
                    FileUtils.createSmartScannerDirs()
                } else {
                    val snackBar: Snackbar = Snackbar.make(binding.main, required_perms_not_given, Snackbar.LENGTH_INDEFINITE)
                    snackBar.setAction(org.idpass.smartscanner.lib.R.string.settings) {
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
        if (SDK_INT >= Build.VERSION_CODES.R) {
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
        return if (SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }
}