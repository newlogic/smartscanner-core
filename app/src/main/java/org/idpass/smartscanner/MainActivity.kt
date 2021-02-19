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
import android.os.Bundle
import android.provider.Settings
import android.view.View.GONE
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import org.idpass.smartscanner.api.ScannerConstants
import org.idpass.smartscanner.api.ScannerIntent
import org.idpass.smartscanner.databinding.ActivityMainBinding
import org.idpass.smartscanner.lib.SmartScannerActivity
import org.idpass.smartscanner.lib.SmartScannerActivity.Companion.SCANNER_RESULT
import org.idpass.smartscanner.lib.SmartScannerActivity.Companion.SCANNER_RESULT_BYTES
import org.idpass.smartscanner.lib.nfc.NFCScannerActivity
import org.idpass.smartscanner.lib.platform.utils.FileUtils
import org.idpass.smartscanner.lib.scanner.config.*
import org.idpass.smartscanner.result.IDPassResultActivity
import org.idpass.smartscanner.result.ResultActivity
import timber.log.Timber
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException


class MainActivity : AppCompatActivity() {

    companion object {
        private val TAG = MainActivity::class.java.simpleName
        private const val OP_SCANNER = 1001
        var imageType = ImageResultType.PATH.value

        private fun sampleConfig(isManualCapture: Boolean, label : String = "") = Config(
            branding = true,
            imageResultType = imageType,
            label = label,
            isManualCapture = isManualCapture
        )
    }

    private val REQUEST_CODE_PERMISSIONS = 11
    private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)

    private lateinit var binding : ActivityMainBinding
    private var isNFC = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        checkNFCSupport()
    }

    override fun onStart() {
        super.onStart()
        // Request storage permissions
        if (allPermissionsGranted()) {
            FileUtils.createSmartScannerDirs()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
        binding.itemBarcode.item.setOnClickListener { scanBarcode(BarcodeOptions.default) }
        binding.itemIdpassLite.item.setOnClickListener { scanIDPassLite() }
        binding.itemMrz.item.setOnClickListener { scanMRZ() }
        binding.itemQR.item.setOnClickListener { scanQRCode() }
        binding.itemNfc.item.setOnClickListener {
            val mrzFromTxt = getMRZFromTxtFile()
            if (mrzFromTxt.isNullOrEmpty()) {
                scanNFC()
            } else {
                val resultIntent = Intent(this, NFCScannerActivity::class.java)
                resultIntent.putExtra(NFCScannerActivity.RESULT_FROM_TXT, mrzFromTxt)
                startActivity(resultIntent)
                isNFC = false
            }
        }
    }

    private fun checkNFCSupport() {
        val adapter = NfcAdapter.getDefaultAdapter(this)
        if (adapter == null) {
            binding.itemNfc.item.visibility = GONE
        }
    }

    private fun startIntentCallOut() {
        try {
            // Note: Scanner via intent can either be for barcode, idpass-lite, mrz, qrcode
            // Please see ScannerIntent class for more details
            // barcode -> val intent = ScannerIntent.intentBarcode()
            // idpass-lite -> val intent = ScannerIntent.intentIDPassLite()
            // mrz -> val intent = ScannerIntent.intentMrz()
            // gzipped -> val intent = ScannerIntent.intentQRCode()
            val intent = ScannerIntent.intentMRZ(
                isManualCapture = true,
                mrzFormat = ScannerConstants.MRZ_FORMAT_MRTD_TD1
            )
            startActivityForResult(intent, OP_SCANNER)
        } catch (ex: ActivityNotFoundException) {
            ex.printStackTrace()
            Timber.e( "ID PASS SmartScanner is not installed!")
        }
    }

    private fun scanBarcode(barcodeOptions: BarcodeOptions? = null) {
        val intent = Intent(this, SmartScannerActivity::class.java)
        intent.putExtra(
            SmartScannerActivity.SCANNER_OPTIONS, ScannerOptions.configBarcode(
                config = sampleConfig(false),
                scannerSize = ScannerSize.LARGE.value,
                barcodeOptions = barcodeOptions
            )
        )
        startActivityForResult(intent, OP_SCANNER)
    }

    private fun scanIDPassLite() {
        val intent = Intent(this, SmartScannerActivity::class.java)
        intent.putExtra(
            SmartScannerActivity.SCANNER_OPTIONS,
            ScannerOptions.configIdPassLite(config = sampleConfig(false))
        )
        startActivityForResult(intent, OP_SCANNER)
    }

    private fun scanMRZ() {
        val intent = Intent(this, SmartScannerActivity::class.java)
        intent.putExtra(
            SmartScannerActivity.SCANNER_OPTIONS,
            ScannerOptions.configMrz(config = sampleConfig(true))
        )
        startActivityForResult(intent, OP_SCANNER)
    }

    private fun scanNFC() {
        val intent = Intent(this, SmartScannerActivity::class.java)
        intent.putExtra(
            SmartScannerActivity.SCANNER_OPTIONS,
            ScannerOptions.configMrz(config = sampleConfig(false, label = "Please scan MRZ to verify ID"))
        )
        startActivityForResult(intent, OP_SCANNER)
        isNFC = true
    }

    private fun gotoNFCReader(mrz: String?, mrzFromTxt: String? = null) {
        val resultIntent = Intent(this, NFCScannerActivity::class.java)
        resultIntent.putExtra(NFCScannerActivity.RESULT, mrz)
        resultIntent.putExtra(NFCScannerActivity.RESULT_FROM_TXT, mrzFromTxt)
        startActivity(resultIntent)
        isNFC = false
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
                isJson  = true,
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

    public override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (requestCode == OP_SCANNER) {
            Timber.d("Scanner resultCode $resultCode")
            if (resultCode == RESULT_OK) {
                // Get Result from Bundle Intent Call Out
                intent?.getBundleExtra(ScannerConstants.RESULT)?.let {
                    Timber.d("Scanner result bundle: $it")
                    Timber.d("Scanner result bundle qr_code_json_value (from path): ${it.getString(ScannerConstants.QRCODE_JSON_VALUE)}")
                    Timber.d("Scanner result bundle qr_code_text: ${it.getString(ScannerConstants.QRCODE_TEXT)}")
                    if (it.getString(ScannerConstants.MODE) == Modes.IDPASS_LITE.value) {
                        // Go to ID PASS Lite Results Screen via bundle
                        val myIntent = Intent(this, IDPassResultActivity::class.java)
                        myIntent.putExtra(IDPassResultActivity.BUNDLE_RESULT, it)
                        startActivity(myIntent)
                    } else {
                        // Go to Barcode/MRZ Results Screen via bundle
                        val resultIntent = Intent(this, ResultActivity::class.java)
                        resultIntent.putExtra(ResultActivity.BUNDLE_RESULT, it)
                        startActivity(resultIntent)
                    }
                } ?: run {
                    // Get Result from JSON String
                    val result = intent?.getStringExtra(SCANNER_RESULT)
                    Timber.d("Scanner result string: $result")
                    if (result != null) {
                        if (isNFC) {
                            // Go to NFC Scanner Screen
                            val resultIntent = Intent(this, NFCScannerActivity::class.java)
                            resultIntent.putExtra(NFCScannerActivity.RESULT, result)
                            startActivity(resultIntent)
                            isNFC = false
                        } else {
                            // Go to Barcode/MRZ Results Screen
                            val resultIntent = Intent(this, ResultActivity::class.java)
                            resultIntent.putExtra(ResultActivity.RESULT, result)
                            startActivity(resultIntent)
                        }
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
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                FileUtils.createSmartScannerDirs()
            } else {
                val snackBar: Snackbar = Snackbar.make(binding.main, org.idpass.smartscanner.lib.R.string.required_perms_not_given, Snackbar.LENGTH_INDEFINITE)
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

    private fun getMRZFromTxtFile() : String?{
        val file = File("${FileUtils.directory}/ad769ced-7727-443e-b613-59d52299f979.txt")
        var mrz : String? = null
        if(file.exists()){
            //Read text from file
            val text = StringBuilder()
            try {
                val br = BufferedReader(FileReader(file))
                var line: String?
                while (br.readLine().also { line = it } != null) {
                    text.append(line)
                }
                br.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            mrz = text.toString()
        }
        return mrz
    }
}