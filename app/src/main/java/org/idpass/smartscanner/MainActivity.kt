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

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.idpass.smartscanner.api.ScannerConstants
import org.idpass.smartscanner.api.ScannerIntent
import org.idpass.smartscanner.databinding.ActivityMainBinding
import org.idpass.smartscanner.lib.SmartScannerActivity
import org.idpass.smartscanner.lib.SmartScannerActivity.Companion.SCANNER_RESULT
import org.idpass.smartscanner.lib.SmartScannerActivity.Companion.SCANNER_RESULT_BYTES
import org.idpass.smartscanner.lib.scanner.config.*
import org.idpass.smartscanner.result.IDPassResultActivity
import org.idpass.smartscanner.result.ResultActivity


class MainActivity : AppCompatActivity() {

    companion object {
        private val TAG = MainActivity::class.java.simpleName
        private const val OP_SCANNER = 1001
        var imageType = ImageResultType.BASE_64.value

        private fun sampleConfig(isManualCapture: Boolean) = Config(
            branding = true,
            imageResultType = imageType,
            isManualCapture = isManualCapture
        )
    }

    private lateinit var binding : ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
    }

    override fun onStart() {
        super.onStart()
        binding.itemBarcode.item.setOnClickListener { scanBarcode(BarcodeOptions.default) }
        binding.itemIdpassLite.item.setOnClickListener { scanIDPassLite() }
        binding.itemMrz.item.setOnClickListener { scanMRZ() }
        binding.itemQR.item.setOnClickListener { scanQRCode() }
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
            Log.e(TAG, "ID PASS SmartScanner is not installed!")
        }
    }

    private fun scanBarcode(barcodeOptions: BarcodeOptions? = null) {
        val intent = Intent(this, SmartScannerActivity::class.java)
        intent.putExtra(
            SmartScannerActivity.SCANNER_OPTIONS, ScannerOptions.sampleBarcode(
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
            ScannerOptions.sampleIdPassLite(config = sampleConfig(false))
        )
        startActivityForResult(intent, OP_SCANNER)
    }

    private fun scanMRZ() {
        imageType = ImageResultType.PATH.value
        val intent = Intent(this, SmartScannerActivity::class.java)
        intent.putExtra(
            SmartScannerActivity.SCANNER_OPTIONS,
            ScannerOptions.sampleMrz(config = sampleConfig(true))
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
            Log.d("${SmartScannerActivity.TAG}/SmartScanner", "Scanner resultCode $resultCode")
            if (resultCode == RESULT_OK) {
                // Get Result from Bundle Intent Call Out
                intent?.getBundleExtra(ScannerConstants.RESULT)?.let {
                    Log.d("${SmartScannerActivity.TAG}/SmartScanner", "Scanner result bundle: $it")
                    Log.d("${SmartScannerActivity.TAG}/SmartScanner", "Scanner result bundle qr_code_json_value (from path): ${it.getString(ScannerConstants.QRCODE_JSON_VALUE)}")
                    Log.d("${SmartScannerActivity.TAG}/SmartScanner", "Scanner result bundle qr_code_text: ${it.getString(ScannerConstants.QRCODE_TEXT)}")
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
                    Log.d("${SmartScannerActivity.TAG}/SmartScanner", "Scanner result string: $result")
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
}