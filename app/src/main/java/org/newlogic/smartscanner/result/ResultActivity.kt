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
package org.newlogic.smartscanner.result

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.google.gson.JsonParser
import org.idpass.smartscanner.api.ScannerConstants
import org.idpass.smartscanner.lib.scanner.config.ImageResultType
import org.idpass.smartscanner.lib.scanner.config.Modes
import org.idpass.smartscanner.lib.utils.extension.decodeBase64
import org.idpass.smartscanner.lib.utils.extension.isJSONValid
import org.json.JSONException
import org.json.JSONObject
import org.idpass.smartscanner.lib.SmartScannerActivity.Companion.SCANNER_IMAGE_TYPE
import org.newlogic.smartscanner.R
import org.newlogic.smartscanner.databinding.ActivityResultBinding


class ResultActivity : AppCompatActivity() {

    companion object {
        const val RESULT = "SCAN_RESULT"
        const val BUNDLE_RESULT = "SCAN_BUNDLE_RESULT"
        const val IMAGE_TYPE = "SCAN_IMAGE_TYPE"
    }

    private lateinit var binding : ActivityResultBinding
    private var result : String? = null
    private var imageType : String? = null

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
        result = intent.getStringExtra(RESULT)
        imageType = intent.getStringExtra(IMAGE_TYPE)
    }

    override fun onStart() {
        super.onStart()
        if (result != null) {
            when (intent.getStringExtra(ScannerConstants.MODE)) {
                Modes.MRZ.value -> {
                    displayResult(result = result, imageType = imageType)
                    // Check composite validity
                    val resultObj = JsonParser.parseString(result).asJsonObject
                    val validComposite = if (resultObj["validComposite"]!= null) resultObj["validComposite"].asBoolean else true
                    if (!validComposite) {
                        val snackBar = Snackbar.make(binding.root, getString(R.string.label_warning_invalid_composite_digit), Snackbar.LENGTH_INDEFINITE)
                        snackBar.setAction("Dismiss") { _ -> snackBar.dismiss() }
                        snackBar.setActionTextColor(ContextCompat.getColor(this, R.color.idpass_orange))
                        snackBar.show()
                    }
                }
                Modes.QRCODE.value -> {
                    // TODO update Display for QR Code here
                    displayResult(result = result, imageType = imageType)
                }
                else -> displayResult(result = result, imageType = imageType)
            }
        } else {
            // Result from intent extras is null, check bundle result instead
            val bundleResult = intent.getBundleExtra(BUNDLE_RESULT)
            if (bundleResult != null) {
                result = when (bundleResult.getString(ScannerConstants.MODE)) {
                    Modes.BARCODE.value -> bundleResult.getString(ScannerConstants.BARCODE_VALUE)
                    Modes.QRCODE.value -> bundleResult.getString(ScannerConstants.QRCODE_TEXT)
                    Modes.MRZ.value -> bundleResult.getString(ScannerConstants.MRZ_RAW)
                    else -> null
                }
                // Raw Data Result
                displayRaw(result)
            } else {
                binding.textResult.text = getString(R.string.label_result_none)
            }
        }

    }

    private fun displayResult(result: String? = null, imageType: String?) {
        if (result?.isJSONValid() == true) {
            val dump: StringBuilder = getResult(result)
            // Text Data Result
            if (dump.isNotEmpty()) {
                binding.textResult.visibility = VISIBLE
                binding.textResult.text = dump.toString()
            }
            // image object from result
            val imageJson = JsonParser.parseString(result).asJsonObject["image"]
            if (imageJson != null) {
                val image = imageJson.asString
                if (image.isNotEmpty()) {
                    val imageBitmap = if (imageType == ImageResultType.PATH.value) BitmapFactory.decodeFile(image) else image.decodeBase64()
                    Glide.with(this)
                            .load(imageBitmap)
                            .optionalCenterCrop()
                            .into(binding.imageResult)
                    binding.imageLabel.paintFlags = binding.imageLabel.paintFlags or Paint.UNDERLINE_TEXT_FLAG
                    binding.imageLabel.visibility = VISIBLE
                    binding.imageResult.visibility = VISIBLE
                } else {
                    binding.imageLabel.visibility = GONE
                    binding.imageResult.visibility = GONE
                }
            }
        }
        // Raw Data Result
        displayRaw(result)
    }

    private fun displayRaw(result : String?) {
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

    private fun getShareResult(result: String? = null) : String {
        val dump: StringBuilder = if (result?.isJSONValid() == true)  getResult(result = result) else StringBuilder()
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
                val sendIntent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, getShareResult(result = result))
                    type = "text/plain"
                }
                val shareIntent = Intent.createChooser(sendIntent, null)
                startActivity(shareIntent)

            }
        }
        return super.onOptionsItemSelected(item)
    }
}