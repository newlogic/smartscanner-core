package com.newlogic.mlkit.demo

import android.graphics.BitmapFactory
import android.graphics.Paint
import android.os.Bundle
import android.view.MenuItem
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.JsonParser
import com.newlogic.mlkit.R
import com.newlogic.mlkit.databinding.ActivityResultBinding
import com.newlogic.mlkit.demo.MainActivity.Companion.imageType
import com.newlogic.mlkit.demo.utils.AnimationUtils
import com.newlogic.mlkitlib.newlogic.config.ImageResultType.PATH
import com.newlogic.mlkitlib.newlogic.extension.decodeBase64
import java.util.*


class ResultActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "Newlogic-ResultActivity"
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

        intent.getStringExtra(SCAN_RESULT)?.let { setupResultView(it, imageType) }
    }

    private fun setupResultView(result: String, imageType: String) {
        val resultObject = JsonParser.parseString(result).asJsonObject
        val originalHeight = 750 // Approx. 250dp for image and textView
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }
}
