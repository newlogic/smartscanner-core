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
package org.idpass.smartscanner.lib.nfc

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.net.Uri
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.AsyncTask
import android.os.Bundle
import android.provider.Settings
import android.view.MenuItem
import android.view.View
import android.view.View.VISIBLE
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.common.util.IOUtils
import com.google.android.material.snackbar.Snackbar
import com.google.gson.JsonParser
import net.sf.scuba.smartcards.CardService
import net.sf.scuba.smartcards.CardServiceException
import org.bouncycastle.asn1.ASN1InputStream
import org.bouncycastle.asn1.ASN1Primitive
import org.bouncycastle.asn1.ASN1Sequence
import org.bouncycastle.asn1.ASN1Set
import org.bouncycastle.asn1.x509.Certificate
import org.idpass.smartscanner.lib.R
import org.idpass.smartscanner.lib.databinding.ActivityNfcScannerBinding
import org.idpass.smartscanner.lib.nfc.details.AdditionalPersonDetails
import org.idpass.smartscanner.lib.nfc.details.DocType
import org.idpass.smartscanner.lib.nfc.details.EDocument
import org.idpass.smartscanner.lib.nfc.details.PersonDetails
import org.idpass.smartscanner.lib.platform.utils.DateUtils
import org.idpass.smartscanner.lib.platform.utils.Image
import org.idpass.smartscanner.lib.platform.utils.ImageUtils
import org.idpass.smartscanner.lib.platform.utils.LoggerUtils
import org.jmrtd.BACKey
import org.jmrtd.BACKeySpec
import org.jmrtd.PassportService
import org.jmrtd.lds.*
import org.jmrtd.lds.icao.*
import org.jmrtd.lds.iso19794.FaceImageInfo
import org.jmrtd.lds.iso19794.FingerImageInfo
import java.io.ByteArrayInputStream
import java.io.IOException
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.Signature
import java.security.cert.CertPathValidator
import java.security.cert.CertificateFactory
import java.security.cert.PKIXParameters
import java.security.spec.MGF1ParameterSpec
import java.security.spec.PSSParameterSpec
import java.util.*


class NFCScannerActivity : AppCompatActivity() {

    companion object {
        const val RESULT = "SCAN_RESULT"
    }
    private val REQUEST_CODE_PERMISSIONS = 11
    private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)

    private lateinit var binding: ActivityNfcScannerBinding
    private var adapter: NfcAdapter? = null
    private var birthDate: String? = null
    private var expirationDate: String? = null
    private var mrzString: String? = null
    private var passportNumber: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNfcScannerBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_close)
        intent.getStringExtra(RESULT)?.let {
            mrzString = JsonParser.parseString(it).asJsonObject["mrz"].asString
        }
        // Request storage permissions
        if (allPermissionsGranted()) {
            setupConfiguration()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }

    private fun setupConfiguration() {
        mrzString?.let {
            readCard(it)
            LoggerUtils.writeLogToFile(this, identifier = "NFC")
        }
    }

    private fun readCard(mrz: String) {
        try {
            val mrzInfo = MRZInfo(mrz)
            setMrzData(mrzInfo)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setMrzData(mrzInfo: MRZInfo) {
        adapter = NfcAdapter.getDefaultAdapter(this)
        binding.mainLayout.visibility = View.GONE
        binding.imageLayout.visibility = VISIBLE
        passportNumber = mrzInfo.documentNumber
        expirationDate = mrzInfo.dateOfExpiry
        birthDate = mrzInfo.dateOfBirth
    }

    override fun onResume() {
        super.onResume()
        if (adapter != null) {
            if (adapter?.isEnabled == true) {
                val intent = Intent(applicationContext, this.javaClass)
                intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                val pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
                val filter = arrayOf(arrayOf("android.nfc.tech.IsoDep"))
                adapter!!.enableForegroundDispatch(this, pendingIntent, null, filter)
            } else checkNFC()
        }
    }

    override fun onPause() {
        super.onPause()
        if (adapter != null) {
            adapter!!.disableForegroundDispatch(this)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (NfcAdapter.ACTION_TECH_DISCOVERED == intent.action ||
                NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action ||
                NfcAdapter.ACTION_TAG_DISCOVERED == intent.action
        ) {
            val tag = intent.extras!!.getParcelable<Tag>(NfcAdapter.EXTRA_TAG)
            if (listOf(*tag!!.techList).contains("android.nfc.tech.IsoDep")) {
                clearViews()
                if (passportNumber != null && !passportNumber!!.isEmpty()
                    && expirationDate != null && !expirationDate!!.isEmpty()
                    && birthDate != null && !birthDate!!.isEmpty()
                ) {
                    val bacKey: BACKeySpec = BACKey(passportNumber, birthDate, expirationDate)
                    ReadTask(assets, this@NFCScannerActivity, IsoDep.get(tag), bacKey).execute()
                    binding.mainLayout.visibility = View.GONE
                    binding.imageLayout.visibility = View.GONE
                    binding.loadingLayout.visibility = VISIBLE
                } else {
                    Snackbar.make(
                        binding.loadingLayout,
                        R.string.error_input,
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun checkNFC() {
        val dialog: AlertDialog.Builder = AlertDialog.Builder(this)
        dialog.setMessage(getString(R.string.label_nfc_on))
        dialog.setPositiveButton(R.string.label_turn_on) { alert, which ->
            val intent = Intent(Settings.ACTION_NFC_SETTINGS)
            startActivity(intent)
        }
        dialog.setNegativeButton(R.string.label_close) { alert, which -> }
        dialog.show()
    }

    @SuppressLint("StaticFieldLeak")
    inner class ReadTask internal constructor(
        private val assets: AssetManager,
        private val context: Context,
        private val isoDep: IsoDep,
        private val bacKey: BACKeySpec
    ) : AsyncTask<Void?, Void?, Exception?>() {
        var eDocument: EDocument = EDocument()
        var docType: DocType = DocType.OTHER
        var personDetails: PersonDetails = PersonDetails()
        var additionalPersonDetails: AdditionalPersonDetails = AdditionalPersonDetails()
        private var dg1File: DG1File? = null
        private var dg2File: DG2File? = null
        private var dg14File: DG14File? = null
        private var dg15File: DG15File? = null
        private var sodFile: SODFile? = null
        private var chipAuthSucceeded = false
        private var passiveAuthSucceeded = false
        private var activeAuthSucceeded = false
        private var dg14Encoded = ByteArray(0)
        private fun doActiveAuth(service: PassportService) {
            // Active Authentication using Data Group 15
            var dg15Encoded: ByteArray? = ByteArray(0)
            try {
                val dg15In = service.getInputStream(PassportService.EF_DG15)
                dg15Encoded = IOUtils.toByteArray(dg15In)
                val dg15InByte = ByteArrayInputStream(dg15Encoded)
                dg15File = DG15File(dg15InByte)
                val publicKey = dg15File!!.publicKey
                val random = SecureRandom()
                val challenge = ByteArray(8)
                random.nextBytes(challenge)
                service.doAA(publicKey, null, "SHA256withRSA", challenge)
                activeAuthSucceeded = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        private fun doChipAuth(service: PassportService) {
            try {
                val dg14In = service.getInputStream(PassportService.EF_DG14)
                dg14Encoded = IOUtils.toByteArray(dg14In)
                val dg14InByte = ByteArrayInputStream(dg14Encoded)
                dg14File = DG14File(dg14InByte)
                val dg14FileSecurityInfos = dg14File!!.securityInfos
                for (securityInfo in dg14FileSecurityInfos) {
                    if (securityInfo is ChipAuthenticationPublicKeyInfo) {
                        val publicKeyInfo = securityInfo
                        val keyId = publicKeyInfo.keyId
                        val publicKey = publicKeyInfo.subjectPublicKey
                        val oid = publicKeyInfo.objectIdentifier
                        service.doEACCA(
                            keyId,
                            ChipAuthenticationPublicKeyInfo.ID_CA_ECDH_AES_CBC_CMAC_256,
                            oid,
                            publicKey
                        )
                        chipAuthSucceeded = true
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        @Throws(CardServiceException::class, IOException::class)
        private fun doPassiveAuth(service: PassportService) {
            val sodIn = service.getInputStream(PassportService.EF_SOD)
            sodFile = SODFile(sodIn)
            try {
                val digest = MessageDigest.getInstance(
                    sodFile!!.digestAlgorithm
                )
                val dataHashes = sodFile!!.dataGroupHashes
                var dg14Hash: ByteArray? = ByteArray(0)
                if (chipAuthSucceeded) {
                    dg14Hash = digest.digest(dg14Encoded)
                }
                val dg1Hash = digest.digest(dg1File!!.encoded)
                val dg2Hash = digest.digest(dg2File!!.encoded)
                if (Arrays.equals(dg1Hash, dataHashes[1]) && Arrays.equals(
                        dg2Hash,
                        dataHashes[2]
                    ) && (!chipAuthSucceeded || Arrays.equals(dg14Hash, dataHashes[14]))
                ) {
                    // We retrieve the CSCA from the german master list
                    val asn1InputStream = ASN1InputStream(assets.open("masterList"))
                    var p: ASN1Primitive?
                    val keystore = KeyStore.getInstance(KeyStore.getDefaultType())
                    keystore.load(null, null)
                    val cf = CertificateFactory.getInstance("X.509")
                    while (asn1InputStream.readObject().also { p = it } != null) {
                        val asn1 = ASN1Sequence.getInstance(p)
                        require(!(asn1 == null || asn1.size() == 0)) { "null or empty sequence passed." }
                        require(asn1.size() == 2) { "Incorrect sequence size: " + asn1.size() }
                        val certSet = ASN1Set.getInstance(asn1.getObjectAt(1))
                        for (i in 0 until certSet.size()) {
                            val certificate = Certificate.getInstance(certSet.getObjectAt(i))
                            val pemCertificate = certificate.encoded
                            val javaCertificate =
                                cf.generateCertificate(ByteArrayInputStream(pemCertificate))
                            keystore.setCertificateEntry(i.toString(), javaCertificate)
                        }
                    }
                    val docSigningCertificates = sodFile!!.docSigningCertificates
                    for (docSigningCertificate in docSigningCertificates) {
                        docSigningCertificate.checkValidity()
                    }

                    // We check if the certificate is signed by a trusted CSCA
                    // TODO: verify if certificate is revoked
                    val cp = cf.generateCertPath(docSigningCertificates)
                    val pkixParameters = PKIXParameters(keystore)
                    pkixParameters.isRevocationEnabled = false
                    val cpv = CertPathValidator.getInstance(CertPathValidator.getDefaultType())
                    cpv.validate(cp, pkixParameters)
                    var sodDigestEncryptionAlgorithm = sodFile!!.digestEncryptionAlgorithm
                    var isSSA = false
                    if (sodDigestEncryptionAlgorithm == "SSAwithRSA/PSS") {
                        sodDigestEncryptionAlgorithm = "SHA256withRSA/PSS"
                        isSSA = true
                    }
                    val sign = Signature.getInstance(sodDigestEncryptionAlgorithm)
                    if (isSSA) {
                        sign.setParameter(
                            PSSParameterSpec(
                                "SHA-256",
                                "MGF1",
                                MGF1ParameterSpec.SHA256,
                                32,
                                1
                            )
                        )
                    }
                    sign.initVerify(sodFile!!.docSigningCertificate)
                    sign.update(sodFile!!.eContent)
                    passiveAuthSucceeded = sign.verify(sodFile!!.encryptedDigest)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun doInBackground(vararg params: Void?): Exception? {
            try {
                val cardService = CardService.getInstance(isoDep)
                cardService.open()
                val service = PassportService(
                    cardService,
                    PassportService.NORMAL_MAX_TRANCEIVE_LENGTH,
                    PassportService.DEFAULT_MAX_BLOCKSIZE,
                    false,
                    false
                )
                service.open()
                var paceSucceeded = false
                try {
                    val cardAccessFile = CardAccessFile(service.getInputStream(PassportService.EF_CARD_ACCESS))
                    val securityInfoCollection = cardAccessFile.securityInfos
                    for (securityInfo in securityInfoCollection) {
                        if (securityInfo is PACEInfo) {
                            val paceInfo = securityInfo
                            service.doPACE(
                                bacKey,
                                paceInfo.objectIdentifier,
                                PACEInfo.toParameterSpec(paceInfo.parameterId),
                                null
                            )
                            paceSucceeded = true
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                service.sendSelectApplet(paceSucceeded)
                if (!paceSucceeded) {
                    try {
                        service.getInputStream(PassportService.EF_COM).read()
                    } catch (e: Exception) {
                        service.doBAC(bacKey)
                    }
                }

                // -- Personal Details -- //
                val dg1In = service.getInputStream(PassportService.EF_DG1)
                dg1File = DG1File(dg1In)
                val mrzInfo = dg1File!!.mrzInfo
                personDetails.name = mrzInfo.secondaryIdentifier.replace("<", " ").trim { it <= ' ' }
                personDetails.surname = mrzInfo.primaryIdentifier.replace("<", " ").trim { it <= ' ' }
                personDetails.personalNumber = mrzInfo.personalNumber
                personDetails.gender = mrzInfo.gender.toString()
                personDetails.birthDate = DateUtils.convertFromMrzDate(mrzInfo.dateOfBirth)
                personDetails.expiryDate = DateUtils.convertFromMrzDate(mrzInfo.dateOfExpiry)
                personDetails.serialNumber = mrzInfo.documentNumber
                personDetails.nationality = mrzInfo.nationality
                personDetails.issuerAuthority = mrzInfo.issuingState
                if ("I" == mrzInfo.documentCode) {
                    docType = DocType.ID_CARD
                } else if ("P" == mrzInfo.documentCode) {
                    docType = DocType.PASSPORT
                }

                // -- Face Image -- //
                val dg2In = service.getInputStream(PassportService.EF_DG2)
                dg2File = DG2File(dg2In)
                val faceInfos = dg2File!!.faceInfos
                val allFaceImageInfos: MutableList<FaceImageInfo> = ArrayList()
                for (faceInfo in faceInfos) {
                    allFaceImageInfos.addAll(faceInfo.faceImageInfos)
                }
                if (!allFaceImageInfos.isEmpty()) {
                    val faceImageInfo = allFaceImageInfos.iterator().next()
                    val image: Image = ImageUtils.getImage(context, faceImageInfo)
                    personDetails.faceImage = image.bitmapImage
                    personDetails.faceImageBase64 = image.base64Image
                }

                // -- Fingerprint (if exist)-- //
                try {
                    val dg3In = service.getInputStream(PassportService.EF_DG3)
                    val dg3File = DG3File(dg3In)
                    val fingerInfos = dg3File.fingerInfos
                    val allFingerImageInfos: MutableList<FingerImageInfo> = ArrayList()
                    for (fingerInfo in fingerInfos) {
                        allFingerImageInfos.addAll(fingerInfo.fingerImageInfos)
                    }
                    val fingerprintsImage: MutableList<Bitmap> = ArrayList()
                    if (!allFingerImageInfos.isEmpty()) {
                        for (fingerImageInfo in allFingerImageInfos) {
                            val image: Image = ImageUtils.getImage(context, fingerImageInfo)
                            fingerprintsImage.add(image.bitmapImage)
                        }
                        personDetails.fingerprints = fingerprintsImage
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // -- Portrait Picture -- //
                try {
                    val dg5In = service.getInputStream(PassportService.EF_DG5)
                    val dg5File = DG5File(dg5In)
                    val displayedImageInfos = dg5File.images
                    if (!displayedImageInfos.isEmpty()) {
                        val displayedImageInfo = displayedImageInfos.iterator().next()
                        val image: Image = ImageUtils.getImage(context, displayedImageInfo)
                        personDetails.portraitImage = image.bitmapImage
                        personDetails.portraitImageBase64 = image.base64Image
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // -- Signature (if exist) -- //
                try {
                    val dg7In = service.getInputStream(PassportService.EF_DG7)
                    val dg7File = DG7File(dg7In)
                    val signatureImageInfos = dg7File.images
                    if (!signatureImageInfos.isEmpty()) {
                        val displayedImageInfo = signatureImageInfos.iterator().next()
                        val image: Image = ImageUtils.getImage(context, displayedImageInfo)
                        personDetails.portraitImage = image.bitmapImage
                        personDetails.portraitImageBase64 = image.base64Image
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // -- Additional Details (if exist) -- //
                try {
                    val dg11In = service.getInputStream(PassportService.EF_DG11)
                    val dg11File = DG11File(dg11In)
                    if (dg11File.length > 0) {
                        additionalPersonDetails.fullName = dg11File.nameOfHolder?.replace("<", " ")?.trim()?.replace("[ ]{2,}", " ")
                        additionalPersonDetails.custodyInformation = dg11File.custodyInformation
                        additionalPersonDetails.nameOfHolder = dg11File.nameOfHolder
                        additionalPersonDetails.fullDateOfBirth = dg11File.fullDateOfBirth
                        additionalPersonDetails.otherNames = dg11File.otherNames
                        additionalPersonDetails.otherValidTDNumbers = dg11File.otherValidTDNumbers
                        additionalPersonDetails.permanentAddress = dg11File.permanentAddress
                        additionalPersonDetails.personalNumber = dg11File.personalNumber
                        additionalPersonDetails.personalSummary = dg11File.personalSummary
                        additionalPersonDetails.placeOfBirth = dg11File.placeOfBirth
                        additionalPersonDetails.profession = dg11File.profession
                        additionalPersonDetails.proofOfCitizenship = dg11File.proofOfCitizenship
                        additionalPersonDetails.tag = dg11File.tag
                        additionalPersonDetails.tagPresenceList = dg11File.tagPresenceList
                        additionalPersonDetails.telephone = dg11File.telephone
                        additionalPersonDetails.title = dg11File.title
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // -- Document Public Key -- //
                try {
                    val dg15In = service.getInputStream(PassportService.EF_DG15)
                    val dg15File = DG15File(dg15In)
                    val publicKey = dg15File.publicKey
                    eDocument.docPublicKey = publicKey
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                eDocument.docType = docType
                eDocument.personDetails = personDetails
                eDocument.additionalPersonDetails = additionalPersonDetails

                // Now, execute three types of authencitation checks
                doChipAuth(service)
                doPassiveAuth(service)
                doActiveAuth(service)
            } catch (e: Exception) {
                return e
            }
            return null
        }

        override fun onPostExecute(exception: Exception?) {
            binding.mainLayout.visibility = VISIBLE
            binding.loadingLayout.visibility = View.GONE
            if (exception == null) {
                setResultToView(
                    eDocument,
                    chipAuthSucceeded,
                    passiveAuthSucceeded,
                    activeAuthSucceeded
                )
            } else {
                binding.imageLayout.visibility = VISIBLE
                binding.loadingLayout.visibility = View.GONE
                Snackbar.make(binding.mainLayout, exception.localizedMessage ?: "", Snackbar.LENGTH_LONG).show()
            }
        }
    }


    private fun setResultToView(
            eDocument: EDocument,
            chipAuth: Boolean,
            passiveAuth: Boolean,
            activeAuth: Boolean
    ) {
        val image = ImageUtils.scaleImage(eDocument.personDetails.faceImage)
        binding.viewPhoto.setImageBitmap(image)
        var result = """NAME: ${eDocument.personDetails.name}""".trimIndent()
        result += "\n"+ """SURNAME: ${eDocument.personDetails.surname}""".trimIndent()
        result += "\n"+ """FULLNAME: ${eDocument.additionalPersonDetails.fullName}""".trimIndent()
        result += "\n"+ """PERSONAL NUMBER: ${eDocument.personDetails.personalNumber}""".trimIndent()
        result += "\n"+ """GENDER: ${eDocument.personDetails.gender}""".trimIndent()
        result += "\n"+ """BIRTH DATE: ${eDocument.personDetails.birthDate}""".trimIndent()
        result += "\n"+ """EXPIRY DATE: ${eDocument.personDetails.expiryDate}""".trimIndent()
        result += "\n"+ """SERIAL NUMBER: ${eDocument.personDetails.serialNumber}""".trimIndent()
        result += "\n"+ """NATIONALITY: ${eDocument.personDetails.nationality}""".trimIndent()
        result += "\n"+ """DOC TYPE: ${eDocument.docType.name}""".trimIndent()
        result += "\n"+ """ISSUER AUTHORITY: ${eDocument.personDetails.issuerAuthority}""".trimIndent()
        result += "\nChipAuth: $chipAuth"
        result += "\nPassiveAuth: $passiveAuth"
        result += "\nActiveAuth: $activeAuth"
        binding.textResult.text = result
    }

    private fun clearViews() {
        binding.viewPhoto.setImageBitmap(null)
        binding.textResult.text = ""
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                setupConfiguration()
            } else {
                val snackBar: Snackbar = Snackbar.make(binding.nfcScanner, R.string.required_perms_not_given, Snackbar.LENGTH_INDEFINITE)
                snackBar.setAction(R.string.settings) {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                }
                snackBar.show()
            }
        }
    }
}