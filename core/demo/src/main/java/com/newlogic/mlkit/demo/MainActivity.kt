package com.newlogic.mlkit.demo

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.gson.JsonParser
import com.newlogic.mlkit.R
import com.newlogic.mlkit.databinding.ActivityMainBinding
import com.newlogic.mlkit.demo.utils.AnimationUtils
import com.newlogic.mlkitlib.newlogic.MLKitActivity
import com.newlogic.mlkitlib.newlogic.config.Config
import com.newlogic.mlkitlib.newlogic.config.ImageResultType.PATH
import com.newlogic.mlkitlib.newlogic.config.Modes
import com.newlogic.mlkitlib.newlogic.config.MrzFormat
import com.newlogic.mlkitlib.newlogic.extension.decodeBase64
import com.newlogic.mlkitlib.newlogic.extension.empty
import java.util.*


class MainActivity : AppCompatActivity() {
    private lateinit var binding : ActivityMainBinding

    override fun onCreate (savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
    }

    public override fun onActivityResult (requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (requestCode == OP_MLKIT) {
            Log.d(TAG, "Plugin post ML Activity resultCode $resultCode")
            if (resultCode == RESULT_OK) {
                val returnedResult = intent?.getStringExtra(MLKitActivity.MLKIT_RESULT)
                binding.textLabelEmpty.visibility = GONE
                returnedResult?.let { setupResultView(it) }
            }
        }
    }

    private fun setupResultView (result: String) {
        val resultObject = JsonParser.parseString(result).asJsonObject
        val originalHeight = 750 // Approx. 250dp for image and textView
        // Result Details
        binding.textResultDetails.visibility = VISIBLE
        // Image
        if (resultObject["image"] != null) {
            val image = resultObject["image"].asString
            binding.imageView.setImageBitmap(if (imageType == PATH.value) BitmapFactory.decodeFile(image) else image.decodeBase64())
            binding.imageView.visibility = VISIBLE
            binding.txtImgAction.visibility = VISIBLE
            binding.txtImgAction.paintFlags = binding.txtImgAction.paintFlags or Paint.UNDERLINE_TEXT_FLAG
            binding.txtImgAction.setOnClickListener {
                AnimationUtils.expandCollapse(binding.imageView, originalHeight)
                binding.txtImgAction.text = if (binding.imageView.visibility == GONE) getString(R.string.action_show_hide) else getString(R.string.action_show)
            }
        } else {
            binding.imageView.visibility = GONE
            binding.txtImgAction.visibility = GONE
        }
        // Simple Data
        if (resultObject["givenNames"] != null) {
            binding.textName.visibility = VISIBLE
            binding.textName.text = getString(R.string.label_name, resultObject["givenNames"].asString.toLowerCase(Locale.ROOT).capitalize())
        } else binding.textName.visibility = GONE
        if (resultObject["surname"] != null) {
            binding.textSurName.visibility = VISIBLE
            binding.textSurName.text = getString(R.string.label_surname, resultObject["surname"].asString.toLowerCase(Locale.ROOT).capitalize())
        } else binding.textSurName.visibility = GONE
        if (resultObject["dateOfBirth"] != null) {
            binding.textBirthday.visibility = VISIBLE
            binding.textBirthday.text = getString(R.string.label_birthday, resultObject["dateOfBirth"].asString)
        } else binding.textBirthday.visibility = GONE
        if (resultObject["nationality"] != null) {
            binding.textNationality.visibility = VISIBLE
            binding.textNationality.text = getString(R.string.label_nationality, resultObject["nationality"].asString)
        } else binding.textNationality.visibility = GONE
        //Raw Data
        binding.editTextTextMultiLine.setText(result)
        binding.editTextTextMultiLine.visibility = VISIBLE
        binding.txtRawDataAction.visibility = VISIBLE
        binding.txtRawDataAction.paintFlags = binding.txtRawDataAction.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        binding.txtRawDataAction.setOnClickListener {
            AnimationUtils.expandCollapse(binding.editTextTextMultiLine, originalHeight)
            binding.txtRawDataAction.text = if (binding.editTextTextMultiLine.visibility == GONE) getString(R.string.action_show_hide) else getString(R.string.action_show)
        }
    }

    fun startScanningActivity (view: View) {
        val intent = Intent(this, MLKitActivity::class.java)
        intent.putExtra(MLKitActivity.MODE, Modes.MRZ.value)
        intent.putExtra(MLKitActivity.MRZ_FORMAT, MrzFormat.MRTD_TD1.value)
        intent.putExtra(MLKitActivity.CONFIG, sampleConfig())
        startActivityForResult(intent, OP_MLKIT)
    }

    fun startQRCodeScanningActivity (view: View) {
        val qrFormatOnly = arrayListOf("QR_CODE")
        startBarcode(qrFormatOnly)
    }

    @SuppressLint("InflateParams")
    fun startBarcodeScanningActivity (view: View)  {
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

    private fun startBarcode (barcodeOptions : ArrayList<String>? = null) {
        val intent = Intent(this, MLKitActivity::class.java)
        intent.putExtra(MLKitActivity.MODE, Modes.BARCODE.value)
        intent.putStringArrayListExtra(MLKitActivity.BARCODE_OPTIONS, barcodeOptions)
        intent.putExtra(MLKitActivity.CONFIG, sampleConfig())
        startActivityForResult(intent, OP_MLKIT)
    }

    private fun sampleConfig() = Config(
        branding = true,
        background = String.empty(),
        font = String.empty(),
        imageResultType = imageType,
        label = String.empty(),
        isManualCapture = true
    )

    companion object {
        private const val TAG = "Newlogic-MLkit"
        private const val OP_MLKIT = 1001
        private val imageType = PATH.value
    }
}