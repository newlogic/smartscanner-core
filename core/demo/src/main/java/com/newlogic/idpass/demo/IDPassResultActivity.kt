package com.newlogic.idpass.demo

import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.newlogic.idpass.demo.utils.DateUtils.formatDate
import com.newlogic.idpass.demo.utils.DateUtils.isValidDate
import com.newlogic.lib.idpass.extension.empty
import com.newlogic.lib.idpass.extension.hideKeyboard
import com.newlogic.lib.idpass.utils.FileUtils
import com.newlogic.mlkit.R
import kotlinx.android.synthetic.main.activity_idpass_result.*
import org.idpass.lite.Card
import org.idpass.lite.IDPassReader
import org.idpass.lite.exceptions.CardVerificationException
import org.idpass.lite.exceptions.InvalidCardException
import org.idpass.lite.exceptions.InvalidKeyException

class IDPassResultActivity : AppCompatActivity() {

    companion object {
        const val RESULT = "idpass_result"
    }

    private var pinCode: String = ""
    private var qrBytes:ByteArray? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_idpass_result)
        overridePendingTransition(R.anim.slide_in_up, android.R.anim.fade_out)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_close)
        val extDirPath: String = getExternalFilesDir(null)!!.absolutePath
        FileUtils.copyAssets(this, "idpass", extDirPath)
        val idPassReader = IDPassReader("$extDirPath/idpass/demokeys.p12")
        val pinCodeBtn: Button = findViewById(R.id.pinCodeAuth)
        pinCodeBtn.setOnClickListener {
            val cardpincode = findViewById<EditText>(R.id.cardPinCode)
            pinCode = cardpincode.text.toString()
            qrBytes?.let {
                val qrstr = readCard(idPassReader, it)
                val tv =  (findViewById<TextView>(R.id.hex))
                tv.text = "\n" + qrstr + "\n"
            }
            hideKeyboard(pinCodeBtn)
        }
        val intent = intent
        val qrbytes = intent.getByteArrayExtra(RESULT)
        val qrstr = qrbytes?.let { readCard(idPassReader, it) }
        val tv =  (findViewById<TextView>(R.id.hex))
        tv.text = "\n" + qrstr + "\n"
    }

    private fun readCard(idPassReader: IDPassReader, qrbytes: ByteArray, charsPerLine: Int = 33): String {
        if (charsPerLine < 4 || qrbytes.isEmpty()) {
            return ""
        }
        val dump = StringBuilder()
        var authStatus = "NO"
        var certStatus = ""
        var card: Card?
        try {
            try {
                card = idPassReader.open(qrbytes)
                certStatus = if (card.verifyCertificate()) "Verified" else "No certificate"
            } catch (ice: InvalidCardException) {
                card = idPassReader.open(qrbytes, true)
                certStatus = if (card.verifyCertificate()) "Not Verified" else "No certificate"
            }
            if (card != null) {
                if (pinCode.isNotEmpty()) {
                    try {
                        card.authenticateWithPIN(pinCode)
                        authStatus = "YES"
                        Toast.makeText(applicationContext, "Authentication Success", Toast.LENGTH_SHORT).show()
                    } catch (ve: CardVerificationException) {
                        Toast.makeText(applicationContext, "Authentication Fail", Toast.LENGTH_SHORT).show()
                    }
                }
                val fullName = card.getfullName()
                val givenName = card.givenName
                val surname = card.surname
                val dob = card.dateOfBirth
                val pob = card.placeOfBirth
                if (fullName != null) {
                    dump.append("Full Name: $fullName\n")
                }
                if (givenName != null) {
                    dump.append("Given Name: $givenName\n")
                }
                if (surname != null) {
                    dump.append("Surname: $surname\n")
                }
                if (dob != null) {
                    val birthday = if (isValidDate(formatDate(dob))) formatDate(dob) else String.empty()
                    dump.append("Date of Birth: ${birthday}\n")
                }
                if (pob.isNotEmpty()) {
                    dump.append("Place of Birth: $pob\n")
                }
                dump.append("\n-------------------------\n\n")
                for ((key, value) in card.cardExtras) {
                    dump.append("$key: $value\n")
                }
                dump.append("\n-------------------------\n\n")
                dump.append("Authenticated: $authStatus\n")
                dump.append("Certificate  : $certStatus\n")
                qrBytes = qrbytes.clone()
                return dump.toString()
            } else {
                return "Error: Invalid IDPASS CARD"
            }
        } catch (ike: InvalidKeyException) {
            return "Error: Reader keyset is not authorized"
        } catch (e: Exception) {
            return "ERROR: NOT AN IDPASS CARD"
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }
}