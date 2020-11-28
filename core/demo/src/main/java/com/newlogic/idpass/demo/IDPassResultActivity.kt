package com.newlogic.idpass.demo

import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.newlogic.lib.idpass.extension.hideKeyboard
import com.newlogic.mlkit.R
import kotlinx.android.synthetic.main.activity_idpass_result.*
import org.idpass.lite.Card
import org.idpass.lite.IDPassReader
import org.idpass.lite.exceptions.CardVerificationException
import org.idpass.lite.exceptions.InvalidCardException
import org.idpass.lite.exceptions.InvalidKeyException
import java.text.SimpleDateFormat

class IDPassResultActivity : AppCompatActivity() {

    companion object {
        private var m_reader = IDPassReader(null)
        const val RESULT = "idpass_result"
    }

    var m_pincode: String = ""
    var m_qrbytes:ByteArray? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_idpass_result)
        overridePendingTransition(R.anim.slide_in_up, android.R.anim.fade_out)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_close)

        val pincodeauth: Button = findViewById(R.id.pincodeauth)
        pincodeauth.setOnClickListener {
            val cardpincode = findViewById<EditText>(R.id.cardpincode)
            m_pincode = cardpincode.text.toString()
            m_qrbytes?.let {
                val qrstr = readCard(it)
                val tv =  (findViewById<TextView>(R.id.hex))
                tv.text = "\n\n" + qrstr + "\n"
            }
            hideKeyboard(pincodeauth)
        }

        val intent = intent
        val qrbytes = intent.getByteArrayExtra(RESULT)
        val qrstr = qrbytes?.let { readCard(it) }

        val tv =  (findViewById<TextView>(R.id.hex))
        tv.text = "\n\n" + qrstr + "\n"

    }

    fun readCard(qrbytes: ByteArray, charsPerLine: Int = 33): String {
        if (charsPerLine < 4 || qrbytes.isEmpty()) {
            return ""
        }

        val dump = StringBuilder()
        var authStatus = "NO"
        var certStatus = ""
        var card: Card?

        try {
            try {
                card = m_reader.open(qrbytes)
                certStatus = if (card.hasCertificate()) "Verified" else "No certificate"
            } catch (ice: InvalidCardException) {
                card = m_reader.open(qrbytes, true)
                certStatus = if (card.hasCertificate()) "Not Verified" else "No certificate"
            }

            if (card != null) {
                if (m_pincode.isNotEmpty()) {
                    try {
                        card.authenticateWithPIN(m_pincode)
                        authStatus = "YES"
                        Toast.makeText(applicationContext, "Authentication Success", Toast.LENGTH_SHORT).show()
                    } catch (ve: CardVerificationException) {
                        Toast.makeText(applicationContext, "Authentication Fail", Toast.LENGTH_SHORT).show()
                    }
                }

                val sdf = SimpleDateFormat("yyyy/MM/dd")
                val surname = card.surname
                val givenname = card.givenName
                val dob = card.dateOfBirth
                val pob = card.placeOfBirth

                dump.append("Surname: $surname\n")
                dump.append("Given Name: $givenname\n")

                if (dob != null) {
                    dump.append("Date of Birth: ${sdf.format(dob)}\n")
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

                m_qrbytes = qrbytes.clone()

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
