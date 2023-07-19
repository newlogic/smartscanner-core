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


//Authenticate

//Asynch function
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.URLSpan
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.auth0.jwk.Jwk
import com.auth0.jwk.NetworkException
import com.auth0.jwk.UrlJwkProvider
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.bumptech.glide.Glide
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectReader
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import org.idpass.smartscanner.api.ScannerConstants
import org.idpass.smartscanner.lib.platform.extension.decodeBase64
import org.idpass.smartscanner.lib.scanner.config.ImageResultType
import org.idpass.smartscanner.lib.scanner.config.Modes
import org.newlogic.smartscanner.MainActivity.Companion.imageType
import org.newlogic.smartscanner.R
import org.newlogic.smartscanner.databinding.ActivityResultBinding
import java.io.IOException
import java.net.Proxy
import java.net.URL
import java.net.URLConnection
import java.security.interfaces.RSAPublicKey
import java.text.SimpleDateFormat
import java.util.*
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.BufferedReader


class ResultActivity : AppCompatActivity() {

    companion object {
        const val RESULT = "SCAN_RESULT"
        const val BUNDLE_RESULT = "SCAN_BUNDLE_RESULT"
    }


    private lateinit var binding : ActivityResultBinding
    private var resultString : String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        // caching
        val sharedPrefFile = "ScannerJwksFile"
        val sharedPreferences: SharedPreferences = this.getSharedPreferences(sharedPrefFile, Context.MODE_PRIVATE)


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
                val parts = result?.split(".")
                if (parts != null) {
                    if (parts.size != 3){
                        displayFailed()
                    }
                    else{
                        val decodedPayload = parseJwt(result)
                        val iss = decodedPayload?.get("iss") as String?
                        println(iss)
                        val jwksUrl = iss + "/.well-known/jwks.json"
                        val myCoroutineScope = CoroutineScope(Dispatchers.Main)
                        myCoroutineScope.launch {
                            val (key, output) = verification(result, jwksUrl)
                            println("verification 1 output")
                            println(key)
                            println(output)
                            if (key != null){
                                if (output){
                                    val editor = sharedPreferences.edit()
                                    editor.putString(iss, key).apply()
                                    decodePayLoad(decodedPayload)
                                }
                                else{
                                    displayFailed()
                                }
                            }
                            else{
                                val cache_key = sharedPreferences.getString(iss, null)
                                if (cache_key != null){
                                    val out = verification_offline(result, cache_key)
                                    if (out){
                                        decodePayLoad(decodedPayload)
                                    }
                                    else{
                                        println("offline verification failed")
                                        displayFailed()
                                    }
                                }
                                else{
                                    println("key not found")
                                    displayFailed()
                                }
                            }
                    }
                }
                }
            } ?: run {
                binding.textResult.text = getString(R.string.label_result_none)
            }
        }
    }

    fun convertSecondsToDate(seconds: Long): String {
        val formatter = SimpleDateFormat("dd-MM-yyyy")
        formatter.timeZone = TimeZone.getDefault()
        val date = Date(seconds * 1000)
        return formatter.format(date)
    }

    private fun decodePayLoad(decodedPayload: Map<String, Any>?){
        val name = decodedPayload?.get("bName") as String?
        val iss = decodedPayload?.get("iss") as String?
        val address = decodedPayload?.get("bAddress") as String?
        val amount = decodedPayload?.get("amount") as String?
        val code = decodedPayload?.get("code") as String?
        val dateIssue = decodedPayload?.get("iat") as Double?
        val dateExpire = decodedPayload?.get("exp") as Double?
        val serviceProvider = decodedPayload?.get("spName") as String?
        val docs = decodedPayload?.get("docs") as? List<*>
        if (docs != null) {
            for (doc in docs) {
                println(doc)
            }
        }
        if (dateIssue != null) {
            if (dateExpire != null) {
                displayVerified(name, address, amount, code,
                    convertSecondsToDate(dateIssue.toLong()),
                    convertSecondsToDate(dateExpire.toLong()),
                    serviceProvider, docs, iss)
            }
        }
    }
    private fun parseJwt(token: String?): Map<String, Any>? {
        val parts = token?.split(".")
        val base64Url = parts?.get(1)
        val base64 = base64Url?.replace('-', '+')?.replace('_', '/')
        val jsonBytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
        val jsonString = String(jsonBytes, Charsets.UTF_8)
        val gson = Gson()
        return gson.fromJson(jsonString, object : TypeToken<Map<String, Any>>() {}.type)
    }

    // async function
    suspend fun verification(jwt: String?, jwksUrl: String): Pair<String?, Boolean> = withContext(Dispatchers.IO) {
        try {
//            val jwkProvider = UrlJwkProvider(URL(jwksUrl),10, 10 )
            val jwkstring = getJwks(URL(jwksUrl), 30000, 30000, null)
            val jwks = jacksonObjectMapper().readValue<Map<String, List<Map<String, Any>>>>(jwkstring)["keys"]
                ?: throw RuntimeException("jwks is null")
//            val jwksFinal : MutableList<Jwk> = mutableListOf()
//            for (jwk in jwks){
//                jwksFinal.add(Jwk.fromValues(jwk))
//            }
            println(jwkstring)
            val jwtDecoded = JWT.decode(jwt)
            println("jwt decoded")
            val keyId = jwtDecoded.keyId
            val jwk = Jwk.fromValues(jwks.filter {Jwk.fromValues(it).id ==  keyId}[0])
            println(jwk)
            val publicKey: RSAPublicKey = jwk.publicKey as RSAPublicKey
            println(publicKey)
            val algorithm = when (jwk.algorithm) {
                "RS256" -> Algorithm.RSA256(publicKey, null)
                else -> throw Exception("Unsupported Algorithm")
            }
            val verifier = JWT.require(algorithm).build()
            val verified = verifier.verify(jwt) != null

            Pair(jwkstring, verified)
        } catch (ex: Exception) {
            println(ex)
            Pair(null, false)
        }
    }

    private fun getJwks(url : URL, connectTimeout :Int?, readTimeout : Int?, proxy : Proxy?): String {
        try {
            val c: URLConnection =
                if (proxy == null) url.openConnection() else url.openConnection(proxy)
            if (connectTimeout != null) {
                c.connectTimeout = connectTimeout
            }
            if (readTimeout != null) {
                c.readTimeout = readTimeout
            }
//            for ((key, value): Map.Entry<String, String> in headers.entries) {
//                c.setRequestProperty(key, value)
//            }
            return c.getInputStream().bufferedReader().use(BufferedReader::readText)

//            c.getInputStream().use { inputStream ->
//                return jacksonObjectMapper().readValue(
//                    inputStream
//                )
//            }
        } catch (e: IOException) {
            println(e)
            throw NetworkException("Cannot obtain jwks from url " + url.toString(), e)
        }
    }

        suspend fun verification_offline(jwt: String?, jwkstring : String): Boolean = withContext(Dispatchers.IO) {
        try {
            val jwks = jacksonObjectMapper().readValue<Map<String, List<Map<String, Any>>>>(jwkstring)["keys"]
                ?: throw RuntimeException("jwks is null")

            val jwtDecoded = JWT.decode(jwt)
            println("jwt decoded from offline verification")
            val keyId = jwtDecoded.keyId
            val jwk = Jwk.fromValues(jwks.filter {Jwk.fromValues(it).id ==  keyId}[0])
            println(jwk)
            val publicKey: RSAPublicKey = jwk.publicKey as RSAPublicKey
            println(publicKey)
            val algorithm = when (jwk.algorithm) {
                "RS256" -> Algorithm.RSA256(publicKey, null)
                else -> throw Exception("Unsupported Algorithm")
            }
            val verifier = JWT.require(algorithm).build()
            verifier.verify(jwt) != null
        } catch (ex: Exception) {
            false
        }
    }


        private fun setupResult(result: String? = null,  imageType: String) {
        val dump: StringBuilder = getResult(result)
        // Text Data Result
        if (dump.isNotEmpty()) {
            binding.textResult.visibility = VISIBLE
            binding.textResult.text = dump.toString()
        }
        // Image & Raw Data Result
        result?.let {
            // image object from MRZ or Barcode
            val image = JsonParser.parseString(it).asJsonObject["image"]
            if (image != null) {
                displayImage(image.asString, imageType)
            }
        } ?: run {
            // TODO implement proper image passing
            //  if (bundle != null) {
            //  showResultImage(bundle.getString(ScannerConstants.MRZ_IMAGE) ?: "", imageType)
            //  }
        }
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

    private fun displayVerified(name: String?, address: String?, amount: String?, voucherCode: String?,
                                issueDate: String?, expiryDate: String?, serviceProvider: String?, docs: List<*>?, iss: String?) {

        val link = iss?.split("/")
        val iss_url = link?.subList(0, 3)?.joinToString("/")
        binding.editTextName.setText(name)
        binding.editTextAddress.setText(address)
        binding.editTextAmount.setText("₱" +amount)
        binding.editTextCode.setText(voucherCode)
        binding.editTextDateIssue.setText(issueDate)
        binding.editTextDateExpiry.setText(expiryDate)
        binding.editTextServiceProvider.setText(serviceProvider)

        val docSize = if(docs != null) docs.size else 0
        println(docSize)

        if (docSize != 0){
            if (docSize > 0){
                val docOne: TextView = findViewById(R.id.docOne)
                val linkText = docs?.get(0)?.toString()
                val spannableString = SpannableString(linkText)
                createDocHyperlink(docOne, iss_url, linkText, spannableString)

                binding.docOneBox.visibility = VISIBLE
                binding.iconOne.visibility = VISIBLE}
            if (docSize > 1){
                val docTwo: TextView = findViewById(R.id.docTwo)
                val linkText = (docs?.get(1)?.toString())
                val spannableString = SpannableString(linkText)
                createDocHyperlink(docTwo, iss_url, linkText, spannableString)
                binding.docTwoBox.visibility = VISIBLE
                binding.iconTwo.visibility = VISIBLE}
            if (docSize > 2){
                val docThree: TextView = findViewById(R.id.docThree)
                val linkText = (docs?.get(2)?.toString())
                val spannableString = SpannableString(linkText)
                createDocHyperlink(docThree, iss_url, linkText, spannableString)
                binding.docThreeBox.visibility = VISIBLE
                binding.iconThree.visibility = VISIBLE}

            binding.Box2.visibility= VISIBLE
        }

        binding.greenTick.visibility = VISIBLE
        binding.Box1.visibility = VISIBLE
        binding.logos.visibility = VISIBLE


    }

    private fun createDocHyperlink(doc : TextView, iss_url: String?, linkText: String?, spannableString: SpannableString){
        val urlSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(iss_url + "/storage.file/" + linkText))
                startActivity(intent)
            }
        }

        if (linkText != null) {
            spannableString.setSpan(urlSpan, 0, linkText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        doc.text = spannableString
        doc.movementMethod = LinkMovementMethod.getInstance()
        doc.isFocusable = false
        doc.isClickable = false
        doc.isLongClickable = false
    }

    private fun displayFailed(){
        binding.failed.visibility = VISIBLE
    }

    private fun displayImage(image: String, imageType: String) {
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