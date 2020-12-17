package org.idpass.smartscanner

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import org.idpass.smartscanner.databinding.ActivityMainBinding
import org.idpass.smartscanner.lib.SmartScannerActivity
import org.idpass.smartscanner.lib.SmartScannerActivity.Companion.SCANNER_RESULT
import org.idpass.smartscanner.lib.SmartScannerActivity.Companion.SCANNER_RESULT_BYTES
import org.idpass.smartscanner.lib.config.*
import org.idpass.smartscanner.result.IDPassResultActivity
import org.idpass.smartscanner.result.ResultActivity
import org.idpass.smartscanner.result.ResultActivity.Companion.SCAN_RESULT


class MainActivity : AppCompatActivity() {

    companion object {
        private val TAG = MainActivity::class.java.simpleName
        private const val OP_SCANNER = 1001
        val imageType = ImageResultType.PATH.value

        private fun sampleConfig(isManualCapture : Boolean) = Config (
                branding = true,
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
        binding.itemBarcode.item.setOnClickListener { startBarcode(BarcodeOptions(BarcodeFormat.default))}
        binding.itemIdpassLite.item.setOnClickListener { startBarcode(BarcodeOptions(arrayListOf("QR_CODE"), idPassLiteSupport = true)) }
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (requestCode == OP_SCANNER) {
            Log.d(TAG, "Plugin post SmartScanner Activity resultCode $resultCode")
            if (resultCode == RESULT_OK) {
                val result = intent?.getStringExtra(SCANNER_RESULT)
                if (result != null) {
                    val resultIntent = Intent(this, ResultActivity::class.java)
                    resultIntent.putExtra(SCAN_RESULT, result)
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

    private fun startIntentCallOut(scannerType : String) {
        try {
            val intent = Intent("org.idpass.smartscanner.SCAN")
            // scannerType: can either be "barcode", "idpass-lite", "mrz"
            intent.putExtra(SmartScannerActivity.SCANNER, scannerType)
            startActivityForResult(intent, OP_SCANNER)
        } catch (ex: ActivityNotFoundException) {
            ex.printStackTrace()
            Log.e(TAG, "smart scanner is not installed!")
        }
    }

    private fun startMrzScan() {
        val intent = Intent(this, SmartScannerActivity::class.java)
        intent.putExtra(SmartScannerActivity.SCANNER_OPTIONS, ScannerOptions.sampleMrz(config = sampleConfig(true)))
        startActivityForResult(intent, OP_SCANNER)
    }

    private fun startBarcode(barcodeOptions: BarcodeOptions? = null) {
        val intent = Intent(this,SmartScannerActivity::class.java)
        intent.putExtra(SmartScannerActivity.SCANNER_OPTIONS, ScannerOptions.sampleBarcode(config = sampleConfig(false), barcodeOptions = barcodeOptions))
        startActivityForResult(intent, OP_SCANNER)
    }
}