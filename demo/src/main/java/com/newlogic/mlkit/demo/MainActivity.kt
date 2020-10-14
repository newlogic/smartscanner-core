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
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.gson.JsonParser
import com.newlogic.mlkit.R
import com.newlogic.mlkit.demo.utils.AnimationUtils
import com.newlogic.mlkitlib.newlogic.MLKitActivity
import com.newlogic.mlkitlib.newlogic.config.Config
import com.newlogic.mlkitlib.newlogic.config.Modes
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
        if (resultObject["imagePath"] != null) {
            val path = resultObject["imagePath"].asString
            val myBitmap = BitmapFactory.decodeFile(path)
            imageView.setImageBitmap(myBitmap)
            imageView.visibility = VISIBLE
            txtImgAction.visibility = VISIBLE
            txtImgAction.paintFlags = txtImgAction.paintFlags or Paint.UNDERLINE_TEXT_FLAG
            txtImgAction.setOnClickListener {
                AnimationUtils.expandCollapse(imageView, originalHeight)
                txtImgAction.text = if (imageView.visibility == GONE) getString(R.string.action_show_hide) else getString(R.string.action_show)
            }
        }
        // Simple Data
        if (resultObject["givenNames"] != null) {
            textName.visibility = VISIBLE
            textName.text = getString(R.string.label_name, resultObject["givenNames"].asString.toLowerCase(Locale.ROOT).capitalize(Locale.ROOT))
        }
        if (resultObject["surname"] != null) {
            textSurName.visibility = VISIBLE
            textSurName.text = getString(R.string.label_surname, resultObject["surname"].asString.toLowerCase(Locale.ROOT).capitalize(Locale.ROOT))
        }
        if (resultObject["dateOfBirth"] != null) {
            textBirthday.visibility = VISIBLE
            textBirthday.text = getString(R.string.label_birthday, resultObject["dateOfBirth"].asString)
        }
        if (resultObject["nationality"] != null) {
            textNationality.visibility = VISIBLE
            textNationality.text = getString(R.string.label_nationality, resultObject["nationality"].asString)
        }
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
        intent.putExtra(MLKitActivity.MLKIT_CONFIG, sampleConfig(Modes.MRZ.value))
        startActivityForResult(intent, OP_MLKIT)
    }

    fun startQRCodeScanningActivity (view: View) {
        // TODO add QR implementation Modes.QR_CODE
        Toast.makeText(this, "Not yet available!", Toast.LENGTH_LONG).show()
    }

    @SuppressLint("InflateParams")
    fun startBarcodeScanningActivity (view: View)  {
        val bottomSheetDialog = BottomSheetDialog(this)
        val sheetViewBarcode = layoutInflater.inflate(R.layout.bottom_sheet_barcode, null)
        bottomSheetDialog.setContentView(sheetViewBarcode)

        val btnPdf417 = sheetViewBarcode.findViewById<LinearLayout>(R.id.btnPdf417)
        val btnBarcode = sheetViewBarcode.findViewById<LinearLayout>(R.id.btnBarcode)
        val btnCancel = sheetViewBarcode.findViewById<LinearLayout>(R.id.btnCancel)

        btnPdf417.setOnClickListener {
            startBarcode(Modes.PDF_417.value)
            bottomSheetDialog.dismiss()
        }
        btnBarcode.setOnClickListener {
            startBarcode(Modes.BARCODE.value)
            bottomSheetDialog.dismiss()
        }
        btnCancel.setOnClickListener { bottomSheetDialog.dismiss() }

        bottomSheetDialog.show()
    }

    private fun startBarcode (mode: String) {
        val intent = Intent(this, MLKitActivity::class.java)
        intent.putExtra(MLKitActivity.MLKIT_CONFIG, sampleConfig(mode))
        startActivityForResult(intent, OP_MLKIT)
    }

    private fun sampleConfig (mode: String) = Config(
        font = String.empty(),
        language = String.empty(),
        label = String.empty(),
        mode = mode,
        true
    )

    companion object {
        private const val TAG = "Newlogic-MLkit"
        private const val OP_MLKIT = 1001
    }
}