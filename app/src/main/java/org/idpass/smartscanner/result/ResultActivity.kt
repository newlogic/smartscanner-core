package org.idpass.smartscanner.result

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.gson.JsonParser
import org.idpass.smartscanner.MainActivity.Companion.imageType
import org.idpass.smartscanner.R
import org.idpass.smartscanner.databinding.ActivityResultBinding
import org.idpass.smartscanner.lib.config.ImageResultType
import org.idpass.smartscanner.lib.extension.decodeBase64
import java.util.*


class ResultActivity : AppCompatActivity() {

    companion object {
        const val SCAN_RESULT = "SCAN_RESULT"
        const val SCAN_MODE = "SCAN_MODE"
    }

    private lateinit var binding : ActivityResultBinding
    private var resultString : String? = null

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
            resultString = getShareResult(it, intent.getStringExtra(SCAN_MODE))
        } ?: run {
            binding.textResult.text = getString(R.string.label_result_none)
        }
    }

    private fun setupResult(result: String, imageType: String) {
        // Text Data Result
        val dump = getResult(result)
        if (dump.isNotEmpty()) {
            binding.textResult.text = dump.toString()
        } else {
            binding.textResult.visibility = GONE
        }
        // Image Data Result
        val image = JsonParser.parseString(result).asJsonObject["image"]
        if (image != null) {
            val imageBitmap = if (imageType == ImageResultType.PATH.value) BitmapFactory.decodeFile(image.asString) else image.asString.decodeBase64()
            Glide
                .with(this)
                .load(imageBitmap)
                .into(binding.imageResult)
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

    private fun getShareResult(result: String, mode: String?) : String {
        val dump = getResult(result)
        if (dump.isEmpty()) {
            dump.append(result)
        }
        dump.insert(0, "Scan Result via ID PASS SmartScanner:\n\n${mode?.toUpperCase(Locale.getDefault())}\n-------------------------\n")
        return dump.toString()
    }

    private fun getResult(result: String): StringBuilder {
        val dump = StringBuilder()
        val resultObject = JsonParser.parseString(result).asJsonObject
        val givenNames = resultObject["givenNames"]
        val surname = resultObject["surname"]
        val dateOfBirth = resultObject["dateOfBirth"]
        val nationality = resultObject["nationality"]
        val documentNumber = resultObject["documentNumber"]
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
        if (documentNumber != null) {
            if (documentNumber.asString.isNotEmpty()) dump.append("Document Number: ${documentNumber.asString}\n")
        }
        if (dump.isNotEmpty()) dump.append("-------------------------")
        return dump
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.share_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.share -> {
                resultString?.let {
                    val sendIntent: Intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, it)
                        type = "text/plain"
                    }
                    val shareIntent = Intent.createChooser(sendIntent, null)
                    startActivity(shareIntent)
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }
}