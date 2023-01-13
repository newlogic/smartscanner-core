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
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
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
import org.newlogic.smartscanner.R
import org.newlogic.smartscanner.adapters.RecyclerResultAdapter
import org.newlogic.smartscanner.databinding.ActivityResultBinding
import org.newlogic.smartscanner.result.RawResultActivity.Companion.PAYLOAD


class ResultActivity : AppCompatActivity() {

    companion object {
        const val RAW_RESULT = "SCAN_RAW_RESULT"
        const val HEADER_RESULT = "SCAN_HEADER_RESULT"
        const val RESULT = "SCAN_RESULT"
        const val FAIL_RESULT = "SCAN_FAIL_RESULT"
        const val BUNDLE_RESULT = "SCAN_BUNDLE_RESULT"
        const val IMAGE_TYPE = "SCAN_IMAGE_TYPE"
        const val SIGNATURE_VERIFIED = "SCAN_SIGNATURE_VERIFIED"
    }

    private lateinit var binding : ActivityResultBinding
    private var result : String? = null
    private var rawResult : String? = null
    private var headerResult : String? = null
    private var failResult : String? = null
    private var imageType : String? = null
    private var resultList = mutableMapOf<String, String>();
    private var isVerifiedSignature: Boolean = false

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

        rawResult = intent.getStringExtra(RAW_RESULT)
        headerResult = intent.getStringExtra(HEADER_RESULT)
        failResult = intent.getStringExtra(FAIL_RESULT)
        result = intent.getStringExtra(RESULT)
        imageType = intent.getStringExtra(IMAGE_TYPE)
        isVerifiedSignature = intent.getBooleanExtra(SIGNATURE_VERIFIED, false)

        binding.rvResultList.layoutManager = LinearLayoutManager(this)
        binding.rvResultList.adapter = RecyclerResultAdapter(resultList as HashMap<String, String>)

        val dividerItemDecoration = DividerItemDecoration(
            this,
            LinearLayoutManager(this).orientation
        )
        binding.rvResultList.addItemDecoration(dividerItemDecoration)
        binding.btnViewRawResult.setOnClickListener { showRawResult() }

    }

    override fun onStart() {
        super.onStart()

        if (failResult?.isNotEmpty() == true) {
            showFailResult()
            return
        }

        if (result != null) {
            when (intent.getStringExtra(ScannerConstants.MODE)) {

                // Display for MRZ here
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

                // Display for QR Code here
                Modes.QRCODE.value -> showListResult(result)

                // Otherwise
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

                // this should not show raw result, for this one it only needs the result directly
                // we have to end this
                finish()
                showRawResult()
            } else {
                binding.textResult.text = getString(R.string.label_result_none)
            }
        }

    }

    private fun displayResult(result: String? = null, imageType: String?) {
        if (result?.isJSONValid() == true) {
            val predefinedResult: StringBuilder = getResult(result)
            // Text Data Result

            binding.textResult.text = if (predefinedResult.isNotEmpty()) predefinedResult.toString() else result
            binding.textResult.visibility = VISIBLE

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
    }


    private fun showRawResult(isRawOnly: Boolean = false) {
        val intent = Intent(this, RawResultActivity::class.java)

        if (!isRawOnly) {
            intent.putExtra(HEADER_RESULT, headerResult)
            intent.putExtra(PAYLOAD, result)
        }

        intent.putExtra(RESULT, rawResult)

        startActivity(intent)
    }

    private fun showFailResult() {

        // Lets try to map fail result here
        var failMessage: String? = ""
        failMessage = if (failResult?.contains("JWT signature does not match") == true) {
            "Error: Signature verification failed. Please ensure a configuration profile was loaded"
        } else {
            failResult
        }



        binding.tvInformation.text = failMessage
        binding.tvInformation.visibility = VISIBLE
        binding.btnViewRawResult.visibility = GONE
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


    private fun showListResult(result: String?) {

        if (result == null) {
            // show error here
            failResult = "Err: Unable to get any result."
            showFailResult()
            return
        } else if (!result.isJSONValid()) {
            // show the raw result
            showRawResult(true)
            finish()
            return
        }


        val jsonResult = JSONObject(result.toString())
        val iResult: Iterator<String> = jsonResult.keys()

        while (iResult.hasNext()) {
            val mKey: String = iResult.next()
            try {
                val value: String = jsonResult.get(mKey).toString()

                resultList[mKey] = value
            } catch (e: JSONException) {
                // TODO Something went wrong!
            }
        }

        binding.rvResultList.visibility = VISIBLE
        binding.rvResultList.adapter?.notifyItemInserted(jsonResult.length())

        binding.tvInformation.visibility = GONE
        if (isVerifiedSignature) {
            binding.tvSignatureVerified.visibility = VISIBLE
        }
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