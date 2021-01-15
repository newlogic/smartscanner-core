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
import org.idpass.smartscanner.api.ScannerConstants
import org.idpass.smartscanner.databinding.ActivityResultBinding
import org.idpass.smartscanner.lib.platform.extension.decodeBase64
import org.idpass.smartscanner.lib.scanner.config.ImageResultType

class ResultActivity : AppCompatActivity() {

    companion object {
        const val RESULT = "SCAN_RESULT"
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
        if (intent.getStringExtra(RESULT) != null) {
            val scanResult = intent.getStringExtra(RESULT)
            setupResult(result = scanResult, imageType = imageType)
            resultString = getShareResult(result = scanResult)
        } else {
            intent.getBundleExtra(RESULT)?.let {
                setupResult(bundle = it, imageType = imageType)
                resultString = getShareResult(bundle = it)
            } ?: run {
                binding.textResult.text = getString(R.string.label_result_none)
            }
        }
    }

    private fun setupResult(result: String? = null, bundle: Bundle? = null, imageType: String) {
        val dump: StringBuilder
        if (bundle != null) {
            dump = getResult(bundle = bundle)
        } else {
            dump = getResult(result = result)
        }
        // Text Data Result
        if (dump.isNotEmpty()) {
            binding.textResult.text = dump.toString()
        } else {
            binding.textResult.visibility = GONE
        }
        // Image & Raw Data Result
        result?.let {
            val image = JsonParser.parseString(it).asJsonObject["image"]
            if (image != null) {
                showResultImage(image.asString, imageType)
            }
        } ?: run {
            // TODO implement proper image passing
            //  if (bundle != null) {
            //  showResultImage(bundle.getString(ScannerConstants.MRZ_IMAGE) ?: "", imageType)
            //  }
        }

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
    }

    private fun showResultImage(image: String, imageType: String) {
        if (image.isNotEmpty()) {
            val imageBitmap = if (imageType == ImageResultType.PATH.value) BitmapFactory.decodeFile(image) else image.decodeBase64()
            Glide
                .with(this)
                .load(imageBitmap)
                .into(binding.imageResult)
            binding.imageLabel.paintFlags =
                binding.imageLabel.paintFlags or Paint.UNDERLINE_TEXT_FLAG
            binding.imageLabel.visibility = VISIBLE
            binding.imageResult.visibility = VISIBLE
        } else {
            binding.imageLabel.visibility = GONE
            binding.imageResult.visibility = GONE
        }
    }


    private fun getShareResult(result: String? = null, bundle: Bundle? = null) : String {
        val dump: StringBuilder
        if (bundle != null) {
            dump = getResult(bundle = bundle)
        } else {
            dump = getResult(result = result)
        }
        if (dump.isEmpty()) {
            dump.append(result)
        }
        dump.insert(0, "Scan Result via ID PASS SmartScanner:\n\n-------------------------\n")
        return dump.toString()
    }

    private fun getResult(result: String? = null, bundle: Bundle? = null): StringBuilder {
        val dump = StringBuilder()
        val givenNames: String?
        val surname: String?
        val dateOfBirth: String?
        val nationality: String?
        val documentNumber: String?
        if (bundle != null) {
            givenNames = bundle.getString(ScannerConstants.MRZ_GIVEN_NAMES)
            surname = bundle.getString(ScannerConstants.MRZ_SURNAME)
            dateOfBirth = bundle.getString(ScannerConstants.MRZ_DATE_OF_BIRTH)
            nationality = bundle.getString(ScannerConstants.MRZ_NATIONALITY)
            documentNumber = bundle.getString(ScannerConstants.MRZ_NATIONALITY)
        } else {
            val resultObject = JsonParser.parseString(result).asJsonObject
            givenNames = if (resultObject["givenNames"] != null) resultObject["givenNames"].asString else ""
            surname = if (resultObject["surname"]!= null) resultObject["surname"].asString else ""
            dateOfBirth = if (resultObject["dateOfBirth"]!= null) resultObject["dateOfBirth"].asString else ""
            nationality =  if (resultObject["nationality"]!= null) resultObject["nationality"].asString else ""
            documentNumber = if (resultObject["documentNumber"]!= null) resultObject["documentNumber"].asString else ""
        }
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