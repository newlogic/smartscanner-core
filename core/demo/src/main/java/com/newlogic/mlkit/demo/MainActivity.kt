package com.newlogic.mlkit.demo

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.newlogic.mlkit.R
import com.newlogic.mlkit.databinding.ActivityMainBinding
import com.newlogic.mlkit.demo.ResultActivity.Companion.SCAN_RESULT
import com.newlogic.mlkitlib.idpass.SmartScannerActivity
import com.newlogic.mlkitlib.idpass.config.*
import com.newlogic.mlkitlib.idpass.config.ImageResultType.PATH
import com.newlogic.mlkitlib.idpass.extension.empty


class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "Newlogic-MainActivity"
        private const val OP_MLKIT = 1001
        val imageType = PATH.value

        private fun sampleConfig() = Config(
                branding = true,
                background = String.empty(),
                font = String.empty(),
                imageResultType = imageType,
                label = String.empty(),
                isManualCapture = true
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
        binding.itemQr.item.setOnClickListener { startQrScan() }
        binding.itemBarcode.item.setOnClickListener { startBarcodeScan() }
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (requestCode == OP_MLKIT) {
            Log.d(TAG, "Plugin post ML Activity resultCode $resultCode")
            if (resultCode == RESULT_OK) {
                val result = intent?.getStringExtra(SmartScannerActivity.MLKIT_RESULT)
                val resultIntent = Intent(this, ResultActivity::class.java)
                resultIntent.putExtra(SCAN_RESULT, result)
                startActivity(resultIntent)
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
        intent.putExtra(SmartScannerActivity.SCANNER_OPTIONS, ScannerOptions.sampleMrz(sampleConfig()))
        startActivityForResult(intent, OP_MLKIT)
    }

    @SuppressLint("InflateParams")
    private fun startBarcodeScan()  {
        val bottomSheetDialog = BottomSheetDialog(this)
        val sheetViewBarcode = layoutInflater.inflate(R.layout.bottom_sheet_barcode, null)
        bottomSheetDialog.setContentView(sheetViewBarcode)
        // bottom sheet ids
        val btnPdf417 = sheetViewBarcode.findViewById<LinearLayout>(R.id.btnPdf417)
        val btnBarcode = sheetViewBarcode.findViewById<LinearLayout>(R.id.btnBarcode)
        val btnCancel = sheetViewBarcode.findViewById<LinearLayout>(R.id.btnCancel)
        // bottom sheet listeners
        btnPdf417.setOnClickListener {
            startBarcode(BarcodeOptions(arrayListOf("PDF_417"), ScannerSize.LARGE.value))
            bottomSheetDialog.dismiss()
        }
        btnBarcode.setOnClickListener {
            startBarcode(BarcodeOptions(BarcodeFormat.default, ScannerSize.SMALL.value))
            bottomSheetDialog.dismiss()
        }
        btnCancel.setOnClickListener { bottomSheetDialog.dismiss() }
        bottomSheetDialog.show()
    }

    private fun startQrScan() = startBarcode(BarcodeOptions(arrayListOf("QR_CODE"), ScannerSize.CUSTOM_QR.value))
    private fun startBarcode(barcodeOptions: BarcodeOptions? = null) {
        val intent = Intent(this, SmartScannerActivity::class.java)
        intent.putExtra(SmartScannerActivity.SCANNER_OPTIONS, ScannerOptions.sampleBarcode(sampleConfig(), barcodeOptions))
        startActivityForResult(intent, OP_MLKIT)
    }
}