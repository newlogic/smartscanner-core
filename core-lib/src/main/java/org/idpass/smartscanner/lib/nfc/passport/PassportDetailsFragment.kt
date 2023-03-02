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
package org.idpass.smartscanner.lib.nfc.passport

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import org.idpass.smartscanner.lib.R
import org.idpass.smartscanner.lib.databinding.FragmentPassportDetailsBinding
import org.idpass.smartscanner.lib.nfc.NFCResult
import org.idpass.smartscanner.lib.nfc.details.IntentData
import org.idpass.smartscanner.lib.utils.DateUtils
import org.idpass.smartscanner.lib.utils.DateUtils.formatStandardDate
import org.idpass.smartscanner.lib.utils.extension.arrayToString
import org.idpass.smartscanner.lib.utils.extension.bytesToHex
import org.jmrtd.FeatureStatus
import org.jmrtd.VerificationStatus
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*
import javax.security.auth.x500.X500Principal


class PassportDetailsFragment : androidx.fragment.app.Fragment() {

    private var passportDetailsFragmentListener: PassportDetailsFragmentListener? = null

    internal var simpleDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)

    private var passport: Passport? = null
    private var language: String? = null
    private var locale: String? = null
    private lateinit var binding : FragmentPassportDetailsBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = FragmentPassportDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val arguments = arguments
        if (arguments?.containsKey(IntentData.KEY_PASSPORT) == true) {
            passport = arguments.getParcelable<Passport>(IntentData.KEY_PASSPORT)
        }
        if (arguments?.containsKey(IntentData.KEY_LANGUAGE) == true) {
            language = arguments.getString(IntentData.KEY_LANGUAGE)
        }
        if (arguments?.containsKey(IntentData.KEY_LOCALE) == true) {
            locale = arguments.getString(IntentData.KEY_LOCALE)
        }

        binding.iconPhoto.setOnClickListener {
            var bitmap = passport?.face
            if (bitmap == null) {
                bitmap = passport?.portrait
            }
            if (passportDetailsFragmentListener != null) {
                passportDetailsFragmentListener?.onImageSelected(bitmap)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshData(passport)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("SetTextI18n")
    private fun refreshData(passport: Passport?) {
        if (passport == null) return

        if (passport.face != null) {
            //Add the face
            binding.iconPhoto.setImageBitmap(passport.face)
        } else if (passport.portrait != null) {
            //If we don't have the face, we try with the portrait
            binding.iconPhoto.setImageBitmap(passport.portrait)
        }

        val resultDetails = NFCResult.formatResult(passport, locale)
        binding.valueName.text = resultDetails.givenNames
        binding.lname.text = resultDetails.surname
        binding.valueDOB.text = resultDetails.dateOfBirth
        binding.valueGender.text = resultDetails.gender
        binding.valuePassportNumber.text = resultDetails.documentNumber
        binding.valueExpirationDate.text = resultDetails.dateOfExpiry
        binding.valueIssuingState.text = resultDetails.issuingState
        binding.valueNationality.text = resultDetails.nationality

        val additionalPersonDetails = passport.additionalPersonDetails
        if (additionalPersonDetails != null) {
            //This object it's not available in the majority of passports
            binding.cardViewAdditionalPersonInformation.visibility = View.VISIBLE

            if (additionalPersonDetails.custodyInformation != null) {
                binding.valueCustody.text = additionalPersonDetails.custodyInformation
            }
            if (additionalPersonDetails.fullDateOfBirth != null) {
                binding.valueDateOfBirth.text = formatStandardDate(additionalPersonDetails.fullDateOfBirth, "yyyyMMdd")
            }
            if (additionalPersonDetails.otherNames != null && additionalPersonDetails.otherNames?.isNotEmpty() == true) {
                binding.valueOtherNames.text = additionalPersonDetails.otherNames?.arrayToString()
            }
            if (additionalPersonDetails.otherValidTDNumbers != null && additionalPersonDetails.otherValidTDNumbers?.isNotEmpty() == true) {
                binding.valueOtherTdNumbers.text = additionalPersonDetails.otherValidTDNumbers?.arrayToString()
            }
            if (additionalPersonDetails.permanentAddress != null && additionalPersonDetails.permanentAddress?.isNotEmpty() == true) {
                binding.valuePermanentAddress.text = additionalPersonDetails.permanentAddress?.arrayToString()
            }

            if (additionalPersonDetails.personalNumber != null) {
                binding.valuePersonalNumber.text = additionalPersonDetails.personalNumber
            }

            if (additionalPersonDetails.personalSummary != null) {
                binding.valuePersonalSummary.text = additionalPersonDetails.personalSummary
            }

            if (additionalPersonDetails.placeOfBirth != null && additionalPersonDetails.placeOfBirth?.isNotEmpty() == true) {
                binding.valuePlaceOfBirth.text = additionalPersonDetails.placeOfBirth?.arrayToString()
            }

            if (additionalPersonDetails.profession != null) {
                binding.valueProfession.text = additionalPersonDetails.profession
            }

            if (additionalPersonDetails.telephone != null) {
                binding.valueTelephone.text = additionalPersonDetails.telephone
            }

            if (additionalPersonDetails.title != null) {
                binding.valueTitle.text = additionalPersonDetails.title
            }
        } else {
            binding.cardViewAdditionalPersonInformation.visibility = View.GONE
        }

        val additionalDocumentDetails = passport.additionalDocumentDetails
        if (additionalDocumentDetails != null) {
            binding.cardViewAdditionalDocumentInformation.visibility = View.VISIBLE

            if (additionalDocumentDetails.dateAndTimeOfPersonalization != null) {
                binding.valueDatePersonalization.text = additionalDocumentDetails.dateAndTimeOfPersonalization
            }
            if (additionalDocumentDetails.dateOfIssue != null) {
                binding.valueDateIssue.text = DateUtils.toReadableDate(formatStandardDate(additionalDocumentDetails.dateOfIssue, "yyyyMMdd"))
            }

            if (additionalDocumentDetails.endorsementsAndObservations != null) {
                binding.valueEndorsements.text = additionalDocumentDetails.endorsementsAndObservations
            }

            if (additionalDocumentDetails.issuingAuthority != null) {
                binding.valueIssuingAuthority.text = additionalDocumentDetails.issuingAuthority
            }

            if (additionalDocumentDetails.namesOfOtherPersons != null) {
                binding.valueNamesOtherPersons.text = additionalPersonDetails?.otherNames?.arrayToString()
            }

            if (additionalDocumentDetails.personalizationSystemSerialNumber != null) {
                binding.valueSystemSerialNumber.text = additionalDocumentDetails.personalizationSystemSerialNumber
            }

            if (additionalDocumentDetails.taxOrExitRequirements != null) {
                binding.valueTaxExit.text = additionalDocumentDetails.taxOrExitRequirements
            }
        } else {
            binding.cardViewAdditionalDocumentInformation.visibility = View.GONE
        }

        displayAuthenticationStatus(passport.verificationStatus, passport.featureStatus)
        displayWarningTitle(passport.verificationStatus, passport.featureStatus)


        val sodFile = passport.sodFile
        if (sodFile != null) {
            val countrySigningCertificate = sodFile.issuerX500Principal
            val dnRFC2253 = countrySigningCertificate.getName(X500Principal.RFC2253)
            val dnCANONICAL = countrySigningCertificate.getName(X500Principal.CANONICAL)
            val dnRFC1779 = countrySigningCertificate.getName(X500Principal.RFC1779)
            val name = countrySigningCertificate.name //new X509Certificate(countrySigningCertificate);

            val docSigningCertificate = sodFile.docSigningCertificate

            if (docSigningCertificate != null) {
                binding.valueDocumentSigningCertificateSerialNumber.text = docSigningCertificate.serialNumber.toString()
                binding.valueDocumentSigningCertificatePublicKeyAlgorithm.text = docSigningCertificate.publicKey.algorithm
                binding.valueDocumentSigningCertificateSignatureAlgorithm.text = docSigningCertificate.sigAlgName

                try {
                    binding.valueDocumentSigningCertificateThumbprint.text = (MessageDigest.getInstance("SHA-1").digest(docSigningCertificate.encoded)).bytesToHex().toUpperCase(Locale.ROOT)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                binding.valueDocumentSigningCertificateIssuer.text = docSigningCertificate.issuerDN.name
                binding.valueDocumentSigningCertificateSubject.text = docSigningCertificate.subjectDN.name
                binding.valueDocumentSigningCertificateValidFrom.text = simpleDateFormat.format(docSigningCertificate.notBefore)
                binding.valueDocumentSigningCertificateValidTo.text = simpleDateFormat.format(docSigningCertificate.notAfter)

            } else {
                binding.cardViewDocumentSigningCertificate.visibility = View.GONE
            }

        } else {
            binding.cardViewDocumentSigningCertificate.visibility = View.GONE
        }
    }

    private fun displayWarningTitle(verificationStatus: VerificationStatus?, featureStatus: FeatureStatus?) {
        var colorCard = android.R.color.holo_green_light
        var message = ""
        var title = ""
        if (featureStatus?.hasCA() == FeatureStatus.Verdict.PRESENT) {
            if (verificationStatus?.ca == VerificationStatus.Verdict.SUCCEEDED && verificationStatus.ht == VerificationStatus.Verdict.SUCCEEDED && verificationStatus.cs == VerificationStatus.Verdict.SUCCEEDED) {
                //Everything is fine
                colorCard = android.R.color.holo_green_light
                title = getString(R.string.document_valid_passport)
                message = getString(R.string.document_chip_content_success)
            } else if (verificationStatus?.ca == VerificationStatus.Verdict.FAILED) {
                //Chip authentication failed
                colorCard = android.R.color.holo_red_light
                title = getString(R.string.document_invalid_passport)
                message = getString(R.string.document_chip_failure)
            } else if (verificationStatus?.ht == VerificationStatus.Verdict.FAILED) {
                //Document information
                colorCard = android.R.color.holo_red_light
                title = getString(R.string.document_invalid_passport)
                message = getString(R.string.document_document_failure)
            } else if (verificationStatus?.cs == VerificationStatus.Verdict.FAILED) {
                //CSCA information
                colorCard = android.R.color.holo_red_light
                title = getString(R.string.document_invalid_passport)
                message = getString(R.string.document_csca_failure)
            } else {
                //Unknown
                colorCard = android.R.color.darker_gray
                title = getString(R.string.document_unknown_passport_title)
                message = getString(R.string.document_unknown_passport_message)
            }
        } else if (featureStatus?.hasCA() == FeatureStatus.Verdict.NOT_PRESENT) {
            if (verificationStatus?.ht == VerificationStatus.Verdict.SUCCEEDED) {
                //Document information is fine
                colorCard = android.R.color.holo_green_light
                title = getString(R.string.document_valid_passport)
                message = getString(R.string.document_content_success)
            } else if (verificationStatus?.ht == VerificationStatus.Verdict.FAILED) {
                //Document information
                colorCard = android.R.color.holo_red_light
                title = getString(R.string.document_invalid_passport)
                message = getString(R.string.document_document_failure)
            } else if (verificationStatus?.cs == VerificationStatus.Verdict.FAILED) {
                //CSCA information
                colorCard = android.R.color.holo_red_light
                title = getString(R.string.document_invalid_passport)
                message = getString(R.string.document_csca_failure)
            } else {
                //Unknown
                colorCard = android.R.color.darker_gray
                title = getString(R.string.document_unknown_passport_title)
                message = getString(R.string.document_unknown_passport_message)
            }
        } else {
            //Unknown
            colorCard = android.R.color.darker_gray
            title = getString(R.string.document_unknown_passport_title)
            message = getString(R.string.document_unknown_passport_message)
        }
        binding.cardViewWarning.setCardBackgroundColor(ContextCompat.getColor(requireContext(), colorCard))
        binding.textWarningTitle.text = title
        binding.textWarningMessage.text = message
    }


    private fun displayAuthenticationStatus(verificationStatus: VerificationStatus?, featureStatus: FeatureStatus?) {

        if (featureStatus?.hasBAC() == FeatureStatus.Verdict.PRESENT) {
            binding.rowBac.visibility = View.VISIBLE
        } else {
            binding.rowBac.visibility = View.GONE
        }

        if (featureStatus?.hasAA() == FeatureStatus.Verdict.PRESENT) {
            binding.rowActive.visibility = View.VISIBLE
        } else {
            binding.rowActive.visibility = View.GONE
        }

        if (featureStatus?.hasSAC() == FeatureStatus.Verdict.PRESENT) {
            binding.rowPace.visibility = View.VISIBLE
        } else {
            binding.rowPace.visibility = View.GONE
        }

        if (featureStatus?.hasCA() == FeatureStatus.Verdict.PRESENT) {
            binding.rowChip.visibility = View.VISIBLE
        } else {
            binding.rowChip.visibility = View.GONE
        }

        if (featureStatus?.hasEAC() == FeatureStatus.Verdict.PRESENT) {
            binding.rowEac.visibility = View.VISIBLE
        } else {
            binding.rowEac.visibility = View.GONE
        }

        displayVerificationStatusIcon(binding.valueBac, verificationStatus?.bac)
        displayVerificationStatusIcon(binding.valuePace, verificationStatus?.sac)
        displayVerificationStatusIcon(binding.valuePassive, verificationStatus?.ht)
        displayVerificationStatusIcon(binding.valueActive, verificationStatus?.aa)
        displayVerificationStatusIcon(binding.valueDocumentSigning, verificationStatus?.ds)
        displayVerificationStatusIcon(binding.valueCountrySigning, verificationStatus?.cs)
        displayVerificationStatusIcon(binding.valueChip, verificationStatus?.ca)
        displayVerificationStatusIcon(binding.valueEac, verificationStatus?.eac)
    }

    private fun displayVerificationStatusIcon(imageView: ImageView, verdictStatus: VerificationStatus.Verdict?) {
        var verdict = verdictStatus
        if (verdict == null) {
            verdict = VerificationStatus.Verdict.UNKNOWN
        }
        val resourceIconId: Int
        val resourceColorId: Int
        when (verdict) {
            VerificationStatus.Verdict.SUCCEEDED -> {
                resourceIconId = R.drawable.ic_check_circle_outline
                resourceColorId = android.R.color.holo_green_light
            }
            VerificationStatus.Verdict.FAILED -> {
                resourceIconId = R.drawable.ic_close_circle_outline
                resourceColorId = android.R.color.holo_red_light
            }
            VerificationStatus.Verdict.NOT_PRESENT -> {
                resourceIconId = R.drawable.ic_close_circle_outline
                resourceColorId = android.R.color.darker_gray
            }
            VerificationStatus.Verdict.NOT_CHECKED -> {
                resourceIconId = R.drawable.ic_help_circle_outline
                resourceColorId = android.R.color.holo_orange_light
            }
            VerificationStatus.Verdict.UNKNOWN -> {
                resourceIconId = R.drawable.ic_close_circle_outline
                resourceColorId = android.R.color.darker_gray
            }
        }
        imageView.setImageResource(resourceIconId)
        imageView.setColorFilter(ContextCompat.getColor(requireActivity(), resourceColorId), android.graphics.PorterDuff.Mode.SRC_IN)
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
        val activity = activity
        if (activity is PassportDetailsFragmentListener) {
            passportDetailsFragmentListener = activity
        }
    }

    override fun onDetach() {
        super.onDetach()
        passportDetailsFragmentListener = null
        activity?.finish()
    }

    interface PassportDetailsFragmentListener {
        fun onImageSelected(bitmap: Bitmap?)
    }

    companion object {
        fun newInstance(passport: Passport?, language: String?, locale: String?): PassportDetailsFragment {
            val myFragment = PassportDetailsFragment()
            val args = Bundle()
            args.putString(IntentData.KEY_LANGUAGE, language)
            args.putString(IntentData.KEY_LOCALE, locale)
            args.putParcelable(IntentData.KEY_PASSPORT, passport)
            myFragment.arguments = args
            return myFragment
        }
    }
}