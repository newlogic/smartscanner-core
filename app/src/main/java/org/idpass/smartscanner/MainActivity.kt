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

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
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

        private fun sampleConfig(isManualCapture : Boolean) = Config (
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
        binding.itemBarcode.item.setOnClickListener { startBarcode(BarcodeOptions.default)}
        binding.itemIdpassLite.item.setOnClickListener { startIDPassLite() }
        binding.itemMrz.item.setOnClickListener { startMrzScan() }
    }

    private fun startIntentCallOut() {
        try {
            // Note: Scanner via intent can either be for barcode, idpass-lite, mrz
            // barcode -> val intent = ScannerIntent.intentBarcode()
            // idpass-lite -> val intent = ScannerIntent.intentIDPassLite()
            // mrz -> val intent = ScannerIntent.intentMrz()
            val intent = ScannerIntent.intentMrz(isManualCapture = true, mrzFormat = ScannerConstants.MRZ_FORMAT_MRTD_TD1)
            startActivityForResult(intent, OP_SCANNER)
        } catch (ex: ActivityNotFoundException) {
            ex.printStackTrace()
            Log.e(TAG, "smart scanner is not installed!")
        }
    }

    private fun startMrzScan() {
        imageType = ImageResultType.PATH.value
        val intent = Intent(this, SmartScannerActivity::class.java)
        intent.putExtra(SmartScannerActivity.SCANNER_OPTIONS, ScannerOptions.sampleMrz(config = sampleConfig(true)))
        startActivityForResult(intent, OP_SCANNER)
    }

    private fun startBarcode(barcodeOptions: BarcodeOptions? = null) {
        val intent = Intent(this,SmartScannerActivity::class.java)
        intent.putExtra(SmartScannerActivity.SCANNER_OPTIONS, ScannerOptions.sampleBarcode(config = sampleConfig(false), barcodeOptions = barcodeOptions))
        startActivityForResult(intent, OP_SCANNER)
    }

    private fun startIDPassLite() {
        val intent = Intent(this,SmartScannerActivity::class.java)
        intent.putExtra(SmartScannerActivity.SCANNER_OPTIONS, ScannerOptions.sampleIdPassLite(config = sampleConfig(false)))
        startActivityForResult(intent, OP_SCANNER)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (requestCode == OP_SCANNER) {
            Log.d(TAG, "Plugin post SmartScanner Activity resultCode $resultCode")
            if (resultCode == RESULT_OK) {
                // Get Result from Bundle Intent Call Out
                intent?.getBundleExtra(ScannerConstants.RESULT)?.let {
                    if (it.getString(ScannerConstants.MODE) == Modes.MRZ.value || it.getString(ScannerConstants.MODE) == Modes.BARCODE.value) {
                        val resultIntent = Intent(this, ResultActivity::class.java)
                        resultIntent.putExtra(ResultActivity.RESULT, it)
                        startActivity(resultIntent)
                    } else {
                        val myIntent = Intent(this, IDPassResultActivity::class.java)
                        myIntent.putExtra(IDPassResultActivity.BUNDLE_RESULT, it)
                        startActivity(myIntent)
                    }
                } ?: run {
                    // Get Result from JSON String
                    val result = intent?.getStringExtra(SCANNER_RESULT)
                    if (result != null) {
                        val resultIntent = Intent(this, ResultActivity::class.java)
                        resultIntent.putExtra(ResultActivity.RESULT, result)
                        startActivity(resultIntent)
                    } else {
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