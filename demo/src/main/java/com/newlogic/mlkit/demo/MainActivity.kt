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
import com.newlogic.mlkit.demo.utils.AnimationUtils
import com.newlogic.mlkitlib.newlogic.MLKitActivity
import com.newlogic.mlkitlib.newlogic.config.BarcodeFormat
import com.newlogic.mlkitlib.newlogic.config.Config
import com.newlogic.mlkitlib.newlogic.config.ImageResultType.PATH
import com.newlogic.mlkitlib.newlogic.config.Modes
import com.newlogic.mlkitlib.newlogic.config.MrzFormat
import com.newlogic.mlkitlib.newlogic.extension.decodeBase64
import com.newlogic.mlkitlib.newlogic.extension.empty
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*


class MainActivity : AppCompatActivity() {

    override fun onCreate (savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    public override fun onActivityResult (requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (requestCode == OP_MLKIT) {
            Log.d(TAG, "Plugin post ML Activity resultCode $resultCode")
            if (resultCode == RESULT_OK) {
                val returnedResult = intent?.getStringExtra(MLKitActivity.MLKIT_RESULT)
                textLabelEmpty.visibility = GONE
                returnedResult?.let { setupResultView(it) }
            }
        }
    }

    private fun setupResultView (result: String) {
        val resultObject = JsonParser.parseString(result).asJsonObject
        val originalHeight = 750 // Approx. 250dp for image and textView
        // Result Details
        textResultDetails.visibility = VISIBLE
        // Image
        if (resultObject["image"] != null) {
            val image = resultObject["image"].asString
            imageView.setImageBitmap(if (imageType == PATH.value) BitmapFactory.decodeFile(image) else image.decodeBase64())
            imageView.visibility = VISIBLE
            txtImgAction.visibility = VISIBLE
            txtImgAction.paintFlags = txtImgAction.paintFlags or Paint.UNDERLINE_TEXT_FLAG
            txtImgAction.setOnClickListener {
                AnimationUtils.expandCollapse(imageView, originalHeight)
                txtImgAction.text = if (imageView.visibility == GONE) getString(R.string.action_show_hide) else getString(R.string.action_show)
            }
        } else {
            imageView.visibility = GONE
            txtImgAction.visibility = GONE
        }
        // Simple Data
        if (resultObject["givenNames"] != null) {
            textName.visibility = VISIBLE
            textName.text = getString(R.string.label_name, resultObject["givenNames"].asString.toLowerCase(Locale.ROOT).capitalize())
        } else textName.visibility = GONE
        if (resultObject["surname"] != null) {
            textSurName.visibility = VISIBLE
            textSurName.text = getString(R.string.label_surname, resultObject["surname"].asString.toLowerCase(Locale.ROOT).capitalize())
        } else textSurName.visibility = GONE
        if (resultObject["dateOfBirth"] != null) {
            textBirthday.visibility = VISIBLE
            textBirthday.text = getString(R.string.label_birthday, resultObject["dateOfBirth"].asString)
        } else textBirthday.visibility = GONE
        if (resultObject["nationality"] != null) {
            textNationality.visibility = VISIBLE
            textNationality.text = getString(R.string.label_nationality, resultObject["nationality"].asString)
        } else textNationality.visibility = GONE
        //Raw Data
        editTextTextMultiLine.setText(result)
        editTextTextMultiLine.visibility = VISIBLE
        txtRawDataAction.visibility = VISIBLE
        txtRawDataAction.paintFlags = txtRawDataAction.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        txtRawDataAction.setOnClickListener {
            AnimationUtils.expandCollapse(editTextTextMultiLine, originalHeight)
            txtRawDataAction.text = if (editTextTextMultiLine.visibility == GONE) getString(R.string.action_show_hide) else getString(R.string.action_show)
        }
    }

    fun startScanningActivity (view: View) {
        val intent = Intent(this, MLKitActivity::class.java)
        intent.putExtra(MLKitActivity.MODE, Modes.MRZ.value)
        intent.putExtra(MLKitActivity.MRZ_FORMAT, MrzFormat.MRP.value)
        intent.putExtra(MLKitActivity.CONFIG, sampleConfig())
        startActivityForResult(intent, OP_MLKIT)
    }

    fun startQRCodeScanningActivity (view: View) {
        startBarcode(BarcodeFormat.QR_CODE.value)
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
            startBarcode(BarcodeFormat.PDF_417.value)
            bottomSheetDialog.dismiss()
        }
        btnBarcode.setOnClickListener {
            startBarcode(BarcodeFormat.COMMON.value)
            bottomSheetDialog.dismiss()
        }
        btnCancel.setOnClickListener { bottomSheetDialog.dismiss() }
        // show bottom sheet
        bottomSheetDialog.show()
    }

    private fun startBarcode (barcodeOption: String) {
        val intent = Intent(this, MLKitActivity::class.java)
        intent.putExtra(MLKitActivity.MODE, Modes.BARCODE.value)
        intent.putExtra(MLKitActivity.BARCODE_OPTION, barcodeOption)
        intent.putExtra(MLKitActivity.CONFIG, sampleConfig())
        startActivityForResult(intent, OP_MLKIT)
    }

    private fun sampleConfig() = Config(
        branding = true,
        background = String.empty(),
        font = String.empty(),
        label = String.empty(),
        imageResultType = imageType
    )

    companion object {
        private const val TAG = "Newlogic-MLkit"
        private const val OP_MLKIT = 1001
        private val imageType = PATH.value
    }
}