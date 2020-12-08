package com.newlogic.idpass.demo

import android.graphics.BitmapFactory
import android.graphics.Paint
import android.os.Bundle
import android.view.MenuItem
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.JsonParser
import com.newlogic.idpass.demo.MainActivity.Companion.imageType
import com.newlogic.lib.idpass.config.ImageResultType.PATH
import com.newlogic.lib.idpass.extension.decodeBase64
import com.newlogic.mlkit.R
import com.newlogic.mlkit.databinding.ActivityResultBinding


class ResultActivity : AppCompatActivity() {

    companion object {
        const val SCAN_RESULT = "SCAN_RESULT"
    }

    private lateinit var binding : ActivityResultBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        overridePendingTransition(R.anim.slide_in_up, android.R.anim.fade_out)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_close)
        intent.getStringExtra(SCAN_RESULT)?.let {
            setupResult(it, imageType)
        } ?: run {
            binding.textResult.text = getString(R.string.label_result_none)
        }
    }

    private fun setupResult(result: String, imageType: String) {
        val resultObject = JsonParser.parseString(result).asJsonObject
        // Text Data Result
        val dump = StringBuilder()
        val givenNames = resultObject["givenNames"]
        val surname = resultObject["surname"]
        val dateOfBirth = resultObject["dateOfBirth"]
        val nationality = resultObject["nationality"]
        if (givenNames != null) {
            if (givenNames.asString.isNotEmpty()) dump.append("Given Name: ${givenNames.asString}\n")
        }
        if (surname != null) {
            if (surname.asString.isNotEmpty()) dump.append("Surname: ${surname.asString}\n")
        }
        if (dateOfBirth != null) {
            if (dateOfBirth.asString.isNotEmpty()) dump.append("Birthday: ${dateOfBirth.asString}\n")
        }
        if (nationality != null) {
            if (nationality.asString.isNotEmpty()) dump.append("Nationality: ${nationality.asString}\n")
        }
        if (dump.isNotEmpty()) {
            dump.append("-------------------------")
            binding.textResult.text = dump.toString()
        } else {
            binding.textResult.visibility = GONE
        }
        // Image Data Result
        val image = resultObject["image"]
        if (image != null) {
            val imageString = image.asString
            binding.imageResult.setImageBitmap(if (imageType == PATH.value) BitmapFactory.decodeFile(imageString) else imageString.decodeBase64())
            binding.imageLabel.paintFlags = binding.imageLabel.paintFlags or Paint.UNDERLINE_TEXT_FLAG
            binding.imageLabel.visibility = VISIBLE
            binding.imageResult.visibility = VISIBLE
        } else {
            binding.imageLabel.visibility = GONE
            binding.imageResult.visibility = GONE
        }
        // Raw Data Result
        if (result.isNotEmpty()) {
            binding.editTextRaw.setText(result)
            binding.textRawLabel.paintFlags = binding.textRawLabel.paintFlags or Paint.UNDERLINE_TEXT_FLAG
            binding.textRawLabel.visibility = VISIBLE
            binding.editTextRaw.visibility = VISIBLE
        } else {
            binding.textRawLabel.visibility = GONE
            binding.editTextRaw.visibility = GONE
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }
}