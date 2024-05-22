package org.idpass.smartscanner.lib.barcode.qr

import android.util.Base64
import android.util.Log
import com.google.gson.JsonObject
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.idpass.smartscanner.lib.SmartScannerActivity.Companion.TAG
import org.jose4j.jws.JsonWebSignature
import org.jose4j.lang.JoseException
import org.json.JSONObject
import java.io.IOException
import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature
import java.security.cert.CertificateExpiredException
import java.security.cert.CertificateNotYetValidException
import java.security.spec.X509EncodedKeySpec

const val TAG = "VcModule"

fun verifySignature(jwtStr: String): Boolean {
    var isValid = false
    val jws = JsonWebSignature()
    try {
        jws.compactSerialization = jwtStr
        val certificateChainHeaderValue = jws.certificateChainHeaderValue
        val certificate = certificateChainHeaderValue[0]
        certificate.checkValidity()
        val publicKey: PublicKey = certificate.publicKey
        jws.key = publicKey
        isValid = jws.verifySignature()
    } catch (e: JoseException) {
        Log.d(TAG, e.message ?: "JoseException")
    } catch (e: CertificateNotYetValidException) {
        Log.d(TAG, e.message ?: "CertificateNotYetValidException")
    } catch (e: CertificateExpiredException) {
        Log.d(TAG, e.message ?: "CertificateExpiredException")
    }
    return isValid
}

fun verifySignature(data: ByteArray, publicKey: PublicKey, signatureStr: String): Boolean {
    val signatureBytes = Base64.decode(signatureStr, Base64.URL_SAFE)
    val signature = Signature.getInstance("SHA256withRSA")
    signature.initVerify(publicKey)
    signature.update(data)
    return signature.verify(signatureBytes)
}


fun getJwtStr(credentialJson: String) {
    try {
        val jsonObject = JSONObject(credentialJson)
        val proof = jsonObject.optJSONObject("proof") ?: jsonObject.optJSONObject("credential")?.optJSONObject("proof")
        val verificationMethod = proof?.getString("verificationMethod")
        val proofValue = proof?.optString("proofValue")
        val jws = proof?.optString("jws")

        if (verificationMethod != null) {
            fetchPublicKeyFromUrl(verificationMethod) { publicKey ->
                if (publicKey != null) {
                    if (proofValue != null) {
                        // Signature verification logic for proofValue (Ed25519Signature2020)
                        val data = getVerificationData(jsonObject)
                        val isVerified = verifySignature(data, publicKey, proofValue)
                        println("Signature verified: $isVerified")
                    } else if (jws != null) {
                        // Signature verification logic for jws (RsaSignature2018)
                        val parts = jws.split(".")
                        val signatureStr = parts[2]
                        val signedData = "${parts[0]}.${parts[1]}".toByteArray()
                        val isVerified = verifySignature(signedData, publicKey, signatureStr)
                        println("Signature verified: $isVerified")
                    }
                } else {
                    println("Failed to fetch public key")
                }
            }
        } else {
            println("No verification method found")
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun getVerificationData(jsonObject: JSONObject): ByteArray {
    // Extract the necessary data for signature verification
    // Customize this function according to the proof structure
    val credential = jsonObject.optJSONObject("credential")
    return credential?.toString()?.toByteArray() ?: jsonObject.toString().toByteArray()
}

//private fun extractPublicKey(json: String, callback: (PublicKey?) -> Unit) {
//    val jsonObject = JSONObject(json)
//    val proof = jsonObject.getJSONObject("credential").getJSONObject("proof")
//    val jwkUrl = proof.getString("verificationMethod")
//    fetchPublicKeyFromUrl(jwkUrl) {
//        val publicKeyPem = it.toString().trimIndent()
//
//        val publicKeyBytes = Base64.decode(publicKeyPem
//            .replace("-----BEGIN PUBLIC KEY-----", "")
//            .replace("-----END PUBLIC KEY-----", "")
//            .replace("\\s".toRegex(), ""), Base64.DEFAULT)
//
//        val keySpec = X509EncodedKeySpec(publicKeyBytes)
//        val keyFactory = KeyFactory.getInstance("RSA")
//        callback(keyFactory.generatePublic(keySpec))
//    }
//}

private fun fetchPublicKeyFromUrl(url: String, callback: (PublicKey?) -> Unit) {
    val client = OkHttpClient()
    val request = Request.Builder()
        .url(url)
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            e.printStackTrace()
            callback(null)
        }

        override fun onResponse(call: Call, response: Response) {
            if (!response.isSuccessful) {
                callback(null)
                return
            }

            val responseBody = response.body?.string() ?: run {
                callback(null)
                return
            }

            try {
                val jsonObject = JSONObject(responseBody)
                val keys = jsonObject.getJSONArray("keys")
                val key = keys.getJSONObject(0)  // Assuming the key we need is the first one

                val modulus = key.getString("n")
                val exponent = key.getString("e")

                val decodedModulus = Base64.decode(modulus, Base64.URL_SAFE)
                val decodedExponent = Base64.decode(exponent, Base64.URL_SAFE)

                val keySpec = X509EncodedKeySpec(decodedModulus)
                val keyFactory = KeyFactory.getInstance("RSA")
                val publicKey = keyFactory.generatePublic(keySpec)
                callback(publicKey)
            } catch (e: Exception) {
                e.printStackTrace()
                callback(null)
            }
        }
    })
}