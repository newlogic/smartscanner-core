package com.newlogic.mlkit.demo

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.newlogic.mlkit.databinding.ActivityMainBinding
import com.newlogic.mlkit.demo.ResultActivity.Companion.SCAN_RESULT
import com.newlogic.mlkitlib.idpass.SmartScannerActivity
import com.newlogic.mlkitlib.idpass.SmartScannerActivity.Companion.MLKIT_RESULT
import com.newlogic.mlkitlib.idpass.SmartScannerActivity.Companion.MLKIT_RESULT_BYTES
import com.newlogic.mlkitlib.idpass.config.BarcodeFormat
import com.newlogic.mlkitlib.idpass.config.BarcodeOptions
import com.newlogic.mlkitlib.idpass.config.Config
import com.newlogic.mlkitlib.idpass.config.ImageResultType.PATH
import com.newlogic.mlkitlib.idpass.config.ScannerOptions
import com.newlogic.mlkitlib.idpass.extension.empty


class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "Newlogic-MainActivity"
        private const val OP_MLKIT = 1001
        val imageType = PATH.value

        private fun sampleConfig(isManualCapture : Boolean) = Config(
                branding = false,
                background = String.empty(),
                font = String.empty(),
                imageResultType = imageType,
                label = String.empty(),
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
        binding.itemMrz.item.setOnClickListener { startMrzScan() }
        binding.itemQr.item.setOnClickListener { startBarcode(BarcodeOptions(arrayListOf("QR_CODE"))) }
        binding.itemBarcode.item.setOnClickListener { startBarcode(BarcodeOptions(BarcodeFormat.default))}
        binding.itemIdpassLite.item.setOnClickListener { startBarcode(BarcodeOptions(arrayListOf("QR_CODE"), idPassLiteSupport = true)) }
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (requestCode == OP_MLKIT) {
            Log.d(TAG, "Plugin post ML Activity resultCode $resultCode")
            if (resultCode == RESULT_OK) {
                val result = intent?.getStringExtra(MLKIT_RESULT)
                if (result != null) {
                    val resultIntent = Intent(this, ResultActivity::class.java)
                    resultIntent.putExtra(SCAN_RESULT, result)
                    startActivity(resultIntent)
                } else {
                    val resultBytes = intent?.getByteArrayExtra(MLKIT_RESULT_BYTES)
                    val myIntent = Intent(this, IDPassResultActivity::class.java)
                    myIntent.putExtra(IDPassResultActivity.RESULT, resultBytes)
                    startActivity(myIntent)
                }

            }
        }
    }

    private fun startIntentCallOut() {
        try {
            val intent = Intent("com.newlogic.mlkitlib.SCAN")
            intent.putExtra(SmartScannerActivity.SCANNER_OPTIONS, ScannerOptions.defaultForMRZ)
            startActivityForResult(intent, OP_MLKIT)
        } catch (ex: ActivityNotFoundException) {
            ex.printStackTrace()
            Log.e(TAG, "smart scanner is not installed!")
        }
    }

    private fun startMrzScan() {
        val intent = Intent(this, SmartScannerActivity::class.java)
        intent.putExtra(SmartScannerActivity.SCANNER_OPTIONS, ScannerOptions.sampleMrz(sampleConfig(true)))
        startActivityForResult(intent, OP_MLKIT)
    }

    private fun startBarcode(barcodeOptions: BarcodeOptions? = null) {
        val intent = Intent(this, SmartScannerActivity::class.java)
        intent.putExtra(SmartScannerActivity.SCANNER_OPTIONS, ScannerOptions.sampleBarcode(sampleConfig(false), barcodeOptions))
        startActivityForResult(intent, OP_MLKIT)
    }
}