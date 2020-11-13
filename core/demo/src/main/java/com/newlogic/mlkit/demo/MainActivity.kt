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
import com.newlogic.mlkitlib.newlogic.MLKitActivity
import com.newlogic.mlkitlib.newlogic.config.Config
import com.newlogic.mlkitlib.newlogic.config.ImageResultType.PATH
import com.newlogic.mlkitlib.newlogic.config.Modes
import com.newlogic.mlkitlib.newlogic.config.MrzFormat
import com.newlogic.mlkitlib.newlogic.extension.empty
import java.util.*


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
                val result = intent?.getStringExtra(MLKitActivity.MLKIT_RESULT)
                val resultIntent = Intent(this, ResultActivity::class.java)
                resultIntent.putExtra(SCAN_RESULT, result)
                startActivity(resultIntent)
            }
        }
    }

    private fun startIntentCallOut() {
        try {
            val intent = Intent("com.newlogic.mlkitlib.SCAN")
            intent.putExtra(MLKitActivity.MODE, Modes.MRZ.value)
            intent.putExtra(MLKitActivity.MRZ_FORMAT, MrzFormat.MRP.value)
            intent.putExtra(MLKitActivity.CONFIG, sampleConfig())
            startActivityForResult(intent, OP_MLKIT)
        } catch (ex: ActivityNotFoundException) {
            ex.printStackTrace()
            Log.e(TAG, "smart scanner is not installed!")
        }
    }

    private fun startMrzScan() {
        val intent = Intent(this, MLKitActivity::class.java)
        intent.putExtra(MLKitActivity.MODE, Modes.MRZ.value)
        intent.putExtra(MLKitActivity.CONFIG, sampleConfig())
        startActivityForResult(intent, OP_MLKIT)
    }

    private fun startQrScan() {
        val qrFormatOnly = arrayListOf("QR_CODE")
        startBarcode(qrFormatOnly)
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
            val pdf417FormatOnly = arrayListOf("PDF_417")
            startBarcode(pdf417FormatOnly)
            bottomSheetDialog.dismiss()
        }
        btnBarcode.setOnClickListener {
            startBarcode(MLKitActivity.defaultBarcodeOptions())
            bottomSheetDialog.dismiss()
        }
        btnCancel.setOnClickListener { bottomSheetDialog.dismiss() }
        bottomSheetDialog.show()
    }

    private fun startBarcode(barcodeOptions: ArrayList<String>? = null) {
        val intent = Intent(this, MLKitActivity::class.java)
        intent.putExtra(MLKitActivity.MODE, Modes.BARCODE.value)
        intent.putStringArrayListExtra(MLKitActivity.BARCODE_OPTIONS, barcodeOptions)
        intent.putExtra(MLKitActivity.CONFIG, sampleConfig())
        startActivityForResult(intent, OP_MLKIT)
    }
}