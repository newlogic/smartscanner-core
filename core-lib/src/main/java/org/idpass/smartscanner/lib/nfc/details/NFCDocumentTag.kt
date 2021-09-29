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
package org.idpass.smartscanner.lib.nfc.details

import android.content.Context
import android.graphics.BitmapFactory
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.util.Log
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.sentry.Sentry
import net.sf.scuba.smartcards.CardService
import net.sf.scuba.smartcards.CardServiceException
import org.idpass.smartscanner.lib.nfc.passport.Passport
import org.idpass.smartscanner.lib.nfc.passport.PassportNFC
import org.idpass.smartscanner.lib.nfc.passport.PassportNfcUtils
import org.jmrtd.*
import org.jmrtd.lds.icao.DG1File
import org.jmrtd.lds.icao.MRZInfo
import java.security.Security


class NFCDocumentTag(val readDG2: Boolean = true, val captureLog: Boolean = false) {

    fun handleTag(context: Context, tag: Tag, mrzInfo: MRZInfo, mrtdTrustStore: MRTDTrustStore, passportCallback: PassportCallback):Disposable{
        return  Single.fromCallable {
            var passport: Passport? = null
            var cardServiceException: Exception? = null

            var ps: PassportService? = null
            try {
                val nfc = IsoDep.get(tag)
                nfc.timeout = 5 * 1000 //5 seconds timeout
                val cs = CardService.getInstance(nfc)
                ps = PassportService(cs, 256, 224, false, true)
                /* Note commented out logs of full APDU command/response tracing for future use
                ps.addAPDUListener(APDUListener { e ->
                    val cmdBuf = e.commandAPDU.bytes
                    val respBuf = e.responseAPDU.bytes
                    val cmdBufStr: String = printLine(cmdBuf, "PACKET ==> ")
                    val respBufStr: String = printLine(respBuf, "PACKET <== ")
                    Log.w(TAG, cmdBufStr)
                    Log.w(TAG, respBufStr)
                })*/
                ps.open()

                val passportNFC = PassportNFC(ps, mrtdTrustStore, mrzInfo, readDG2)
                val verifySecurity = passportNFC.verifySecurity()
                val features = passportNFC.features
                val verificationStatus = passportNFC.verificationStatus

                passport = Passport()
                passport.featureStatus = features
                passport.verificationStatus = verificationStatus
                passport.sodFile = passportNFC.sodFile

                //Passport features and verification
                if (captureLog) {
                    Sentry.captureMessage(features.summary(mrzInfo.documentNumber))
                    Sentry.captureMessage(verificationStatus.summary(mrzInfo.documentNumber))
                } else {
                    Log.i(TAG, features.summary(mrzInfo.documentNumber))
                    Log.i(TAG, verificationStatus.summary(mrzInfo.documentNumber))
                }

                //Basic Information
                if (passportNFC.dg1File != null) {
                    val info = (passportNFC.dg1File as DG1File).mrzInfo
                    val personDetails = PersonDetails()
                    personDetails.dateOfBirth = info.dateOfBirth
                    personDetails.dateOfExpiry = info.dateOfExpiry
                    personDetails.documentCode = info.documentCode
                    personDetails.documentNumber = info.documentNumber
                    personDetails.optionalData1 = info.optionalData1
                    personDetails.optionalData2 = info.optionalData2
                    personDetails.issuingState = info.issuingState
                    personDetails.primaryIdentifier = info.primaryIdentifier
                    personDetails.secondaryIdentifier = info.secondaryIdentifier
                    personDetails.nationality = info.nationality
                    personDetails.gender = info.gender
                    passport.personDetails = personDetails
                }

                //Picture
                if (passportNFC.dg2File != null && passportNFC.readDG2) {
                    //Get the picture
                    try {
                        val faceImage = PassportNfcUtils.retrieveFaceImage(context, passportNFC.dg2File!!)
                        passport.face = faceImage
                    } catch (e: Exception) {
                        //Don't do anything
                        e.printStackTrace()
                    }

                }

                //Portrait
                //Get the picture
                if (passportNFC.dg5File != null) {
                    //Get the picture
                    try {
                        val faceImage = PassportNfcUtils.retrievePortraitImage(context, passportNFC.dg5File!!)
                        passport.portrait = faceImage
                    } catch (e: Exception) {
                        //Don't do anything
                        e.printStackTrace()
                    }

                }

                val dg11 = passportNFC.dg11File
                if (dg11 != null) {
                    val additionalPersonDetails = AdditionalPersonDetails()
                    additionalPersonDetails.custodyInformation = dg11.custodyInformation
                    additionalPersonDetails.fullDateOfBirth = dg11.fullDateOfBirth
                    additionalPersonDetails.nameOfHolder = dg11.nameOfHolder
                    additionalPersonDetails.otherNames = dg11.otherNames
                    additionalPersonDetails.otherNames = dg11.otherNames
                    additionalPersonDetails.otherValidTDNumbers = dg11.otherValidTDNumbers
                    additionalPersonDetails.permanentAddress = dg11.permanentAddress
                    additionalPersonDetails.personalNumber = dg11.personalNumber
                    additionalPersonDetails.personalSummary = dg11.personalSummary
                    additionalPersonDetails.placeOfBirth = dg11.placeOfBirth
                    additionalPersonDetails.profession = dg11.profession
                    additionalPersonDetails.proofOfCitizenship = dg11.proofOfCitizenship
                    additionalPersonDetails.tag = dg11.tag
                    additionalPersonDetails.tagPresenceList = dg11.tagPresenceList
                    additionalPersonDetails.telephone = dg11.telephone
                    additionalPersonDetails.title = dg11.title

                    passport.additionalPersonDetails = additionalPersonDetails

                    // Hash Checking
                    val hashCheckNotSucceeded = "hash-check not SUCCEEDED"
                    if (verifySecurity.ht != VerificationStatus.Verdict.SUCCEEDED) {
                        if (captureLog) {
                            Sentry.captureMessage(hashCheckNotSucceeded)
                        } else {
                            Log.e(TAG, hashCheckNotSucceeded)
                        }
                    }

                    // DG11 NameOfHolder
                    if (captureLog) {
                        val dg11NameOfHolder : String? = dg11.nameOfHolder ?: null
                        if (dg11NameOfHolder == null) {
                            // Send to sentry null nameOfHolder
                            Sentry.captureMessage( "nameOfHolder = $dg11NameOfHolder")
                        } else {
                            // Send to sentry nameOfHolder length
                            Sentry.captureMessage( "nameOfHolder.length = ${dg11NameOfHolder.length}")
                            // Log nameOfHolder in app
                            Log.i(TAG, dg11NameOfHolder)
                        }
                    }

                } else {
                    val dg11Null = "DG11 is null"
                    if (captureLog) {
                        Sentry.captureMessage(dg11Null)
                    } else {
                        Log.e(TAG, dg11Null)
                    }
                }

                //Finger prints
                //Get the pictures
                if (passportNFC.dg3File != null) {
                    //Get the picture
                    try {
                        val bitmaps = PassportNfcUtils.retrieveFingerPrintImage(
                            context,
                            passportNFC.dg3File!!
                        )
                        passport.fingerprints = bitmaps
                    } catch (e: Exception) {
                        //Don't do anything
                        e.printStackTrace()
                    }

                }

                //Signature
                //Get the pictures
                if (passportNFC.dg7File != null) {
                    //Get the picture
                    try {
                        val bitmap = PassportNfcUtils.retrieveSignatureImage(context, passportNFC.dg7File!!)
                        passport.signature = bitmap
                    } catch (e: Exception) {
                        //Don't do anything
                        e.printStackTrace()
                    }
                }

                //Additional Document Details
                val dg12 = passportNFC.dg12File
                if (dg12 != null) {
                    val additionalDocumentDetails = AdditionalDocumentDetails()
                    additionalDocumentDetails.dateAndTimeOfPersonalization = dg12.dateAndTimeOfPersonalization
                    additionalDocumentDetails.dateOfIssue = dg12.dateOfIssue
                    additionalDocumentDetails.endorsementsAndObservations = dg12.endorsementsAndObservations
                    try {
                        val imageOfFront = dg12.imageOfFront
                        val bitmapImageOfFront =
                            BitmapFactory.decodeByteArray(imageOfFront, 0, imageOfFront.size)
                        additionalDocumentDetails.imageOfFront = bitmapImageOfFront
                    } catch (e: Exception) {
                        Log.e(TAG, "Additional document image front: $e")
                    }
                    try {
                        val imageOfRear = dg12.imageOfRear
                        val bitmapImageOfRear =
                            BitmapFactory.decodeByteArray(imageOfRear, 0, imageOfRear.size)
                        additionalDocumentDetails.imageOfRear = bitmapImageOfRear
                    } catch (e: Exception) {
                        Log.e(TAG, "Additional document image rear: $e")
                    }
                    additionalDocumentDetails.issuingAuthority = dg12.issuingAuthority
                    additionalDocumentDetails.namesOfOtherPersons = dg12.namesOfOtherPersons
                    additionalDocumentDetails.personalizationSystemSerialNumber = dg12.personalizationSystemSerialNumber
                    additionalDocumentDetails.taxOrExitRequirements = dg12.taxOrExitRequirements

                    passport.additionalDocumentDetails = additionalDocumentDetails
                }

            } catch (e: Exception) {
                //TODO EAC
                cardServiceException = e
            } finally {
                try {
                    ps?.close()
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }

            PassportDTO(passport, cardServiceException)

        }.doOnSubscribe{
            passportCallback.onPassportReadStart()
        }
        .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { passportDTO ->
                if (passportDTO.cardServiceException != null) {
                    val cardServiceException = passportDTO.cardServiceException
                    if (cardServiceException is AccessDeniedException) {
                        passportCallback.onAccessDeniedException(cardServiceException)
                    } else if (cardServiceException is BACDeniedException) {
                        passportCallback.onBACDeniedException(cardServiceException)
                    } else if (cardServiceException is PACEException) {
                        passportCallback.onPACEException(cardServiceException)
                    } else if (cardServiceException is CardServiceException) {
                        passportCallback.onCardException(cardServiceException)
                    } else {
                        passportCallback.onGeneralException(cardServiceException)
                    }
                    // Capture card exceptions and Send to Sentry
                    if (captureLog) {
                        Sentry.captureException(cardServiceException)
                    }
                } else {
                    passportCallback.onPassportRead(passportDTO.passport)
                }
                passportCallback.onPassportReadFinish()
            }
    }

    private fun printLine(bytes: ByteArray, title: String?): String {
        val sb = StringBuilder()
        if (title != null) {
            sb.append(title)
        }
        for (b in bytes) {
            sb.append(String.format("%02X", b))
        }
        return sb.toString()
    }

    data class PassportDTO(val passport: Passport? = null, val cardServiceException: Exception? = null)

    interface PassportCallback {
        fun onPassportReadStart()
        fun onPassportReadFinish()
        fun onPassportRead(passport: Passport?)
        fun onAccessDeniedException(exception: AccessDeniedException)
        fun onBACDeniedException(exception: BACDeniedException)
        fun onPACEException(exception: PACEException)
        fun onCardException(exception: CardServiceException)
        fun onGeneralException(exception: Exception?)
    }

    companion object {

        private val TAG = NFCDocumentTag::class.java.simpleName

        init {
            Security.insertProviderAt(org.spongycastle.jce.provider.BouncyCastleProvider(), 1)
        }

        private val EMPTY_TRIED_BAC_ENTRY_LIST = emptyList<Any>()
        private val EMPTY_CERTIFICATE_CHAIN = emptyList<Any>()
    }
}