/*
 * Copyright (C) 2020 Newlogic Pte. Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 *
 */
package org.idpass.smartscanner.result

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import com.bumptech.glide.Glide
import com.google.gson.JsonParser
import org.idpass.smartscanner.MainActivity.Companion.imageType
import org.idpass.smartscanner.R
import org.idpass.smartscanner.api.ScannerConstants
import org.idpass.smartscanner.databinding.ActivityResultBinding
import org.idpass.smartscanner.lib.platform.extension.decodeBase64
import org.idpass.smartscanner.lib.platform.extension.isValidUrl
import org.idpass.smartscanner.lib.scanner.config.ImageResultType
import org.idpass.smartscanner.lib.scanner.config.Modes

class ResultActivity : AppCompatActivity() {

    companion object {
        const val RESULT = "SCAN_RESULT"
        const val BUNDLE_RESULT = "SCAN_BUNDLE_RESULT"
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
        intent.getStringExtra(RESULT)?.let {
            val scanResult = intent.getStringExtra(RESULT)
            setupResult(result = scanResult, imageType = imageType)
            resultString = getShareResult(result = scanResult)
        } ?: run {
            intent.getBundleExtra(BUNDLE_RESULT)?.let {
                val result = when (it.getString(ScannerConstants.MODE)) {
                    Modes.BARCODE.value -> it.getString(ScannerConstants.BARCODE_VALUE)
                    Modes.QRCODE.value -> it.getString(ScannerConstants.QRCODE_TEXT)
                    Modes.MRZ.value -> it.getString(ScannerConstants.MRZ_RAW)
                    else -> null
                }
                resultString = result
                displayRaw(result)
            } ?: run {
                binding.textResult.text = getString(R.string.label_result_none)
            }
        }
    }

    private fun setupResult(result: String? = null, imageType: String) {
        val dump: StringBuilder = getResult(result)
        // Text Data Result
        if (dump.isNotEmpty()) {
            binding.textResult.visibility = VISIBLE
            binding.textResult.text = dump.toString()
        }
        // Image & Raw Data Result
        result?.let {
            // image object from MRZ or Barcode
            var image = JsonParser.parseString(it).asJsonObject["image"]
            if (image != null) {
                displayImage(image.asString, imageType)
            }
        } ?: run {
            // TODO implement proper image passing
            //  if (bundle != null) {
            //  showResultImage(bundle.getString(ScannerConstants.MRZ_IMAGE) ?: "", imageType)
            //  }
        }
        displayRaw(result)
    }

    private fun displayRaw(result: String?) {
        // Raw Data Result
        if (result?.isNotEmpty() != null) {
            binding.editTextRaw.setText(result)
            binding.textRawLabel.paintFlags = binding.textRawLabel.paintFlags or Paint.UNDERLINE_TEXT_FLAG
            binding.textRawLabel.visibility = VISIBLE
            binding.editTextRaw.visibility = VISIBLE
        } else {
            binding.textRawLabel.visibility = GONE
            binding.editTextRaw.visibility = GONE
        }

        // When raw result is valid url route to browser
        if (result?.isValidUrl() == true) {
            val color = resources.getColor(R.color.blue)
            val defaultFont = ResourcesCompat.getFont(this, R.font.sourcesanspro_regular)
            binding.editTextRaw.setTextColor(color)
            binding.editTextRaw.setTypeface(defaultFont, Typeface.BOLD)
            binding.editTextRaw.maxLines = 1 // One line for url only
            binding.editTextRaw.background.setColorFilter(color, PorterDuff.Mode.SRC_IN)
            binding.editTextRaw.setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(result)))
            }
        }
    }

    private fun displayImage(image: String, imageType: String) {
        if (image.isNotEmpty()) {
            val imageBitmap = if (imageType == ImageResultType.PATH.value) BitmapFactory.decodeFile(image) else image.decodeBase64()
            Glide.with(this)
                .load(imageBitmap)
                .into(binding.imageResult)
            binding.imageLabel.paintFlags = binding.imageLabel.paintFlags or Paint.UNDERLINE_TEXT_FLAG
            binding.imageLabel.visibility = VISIBLE
            binding.imageResult.visibility = VISIBLE
        } else {
            binding.imageLabel.visibility = GONE
            binding.imageResult.visibility = GONE
        }
    }


    private fun getShareResult(result: String? = null) : String {
        val dump: StringBuilder = getResult(result = result)
        if (dump.isEmpty()) {
            dump.append(result)
        }
        dump.insert(0, "Scan Result via ID PASS SmartScanner:\n\n-------------------------\n")
        return dump.toString()
    }

    private fun getResult(result: String? = null): StringBuilder {
        val dump = StringBuilder()
        val givenNames: String?
        val surname: String?
        val dateOfBirth: String?
        val nationality: String?
        val documentNumber: String?
        val resultObject = JsonParser.parseString(result).asJsonObject
        givenNames = if (resultObject["givenNames"] != null) resultObject["givenNames"].asString else ""
        surname = if (resultObject["surname"]!= null) resultObject["surname"].asString else ""
        dateOfBirth = if (resultObject["dateOfBirth"]!= null) resultObject["dateOfBirth"].asString else ""
        nationality =  if (resultObject["nationality"]!= null) resultObject["nationality"].asString else ""
        documentNumber = if (resultObject["documentNumber"]!= null) resultObject["documentNumber"].asString else ""
        if (givenNames != null) {
            if (givenNames.isNotEmpty()) dump.append("Given Name: ${givenNames}\n")
        }
        if (surname != null) {
            if (surname.isNotEmpty()) dump.append("Surname: ${surname}\n")
        }
        if (dateOfBirth != null) {
            if (dateOfBirth.isNotEmpty()) dump.append("Birthday: ${dateOfBirth}\n")
        }
        if (nationality != null) {
            if (nationality.isNotEmpty()) dump.append("Nationality: ${nationality}\n")
        }
        if (documentNumber != null) {
            if (documentNumber.isNotEmpty()) dump.append("Document Number: ${documentNumber}\n")
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