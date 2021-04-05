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

import org.idpass.smartscanner.lib.nfc.passport.Passport
import org.idpass.smartscanner.lib.platform.extension.arrayToString
import org.idpass.smartscanner.lib.platform.utils.DateUtils
import org.idpass.smartscanner.lib.platform.utils.DateUtils.formatStandardDate
import org.jmrtd.lds.icao.MRZInfo


data class NFCResult(
        val image: String? = null,
        var givenNames: String? = null,
        var surname: String? = null,
        var nameOfHolder: String? = null,
        var dateOfBirth: String? = null,
        var gender: String? = null,
        var documentNumber: String? = null,
        var dateOfExpiry: String? = null,
        var issuingState: String? = null,
        var nationality: String? = null,
        var otherNames: String? = null,
        var custodyInformation: String? = null,
        var profession: String? = null,
        var telephone: String? = null,
        var title: String? = null,
        var dateAndTimeOfPersonalization: String? = null,
        var dateOfIssue: String? = null,
        var endorsementsAndObservations: String? = null,
        var issuingAuthority: String? = null,
        var personalizationSystemSerialNumber: String? = null,
        var taxOrExitRequirements: String? = null,
        var mrzOptional: String? = null,
        var mrzOptional2: String? = null
) {
    companion object {

        fun formatResult(passport: Passport?, mrzInfo: MRZInfo? = null): NFCResult {
            val personDetails = passport?.personDetails
            val additionalPersonDetails = passport?.additionalPersonDetails
            val additionalDocumentDetails = passport?.additionalDocumentDetails
            // Note: Get proper names
            // NFCResult.nameOfHolder --> LAST_NAME<<GIVEN_NAMES (RTL for Arabic)
            // we split nameOfHolder to two parts separated by '<<' (double chevron)
            val nameParts  = additionalPersonDetails?.nameOfHolder?.split("<<")?.toMutableList()
            // then first part of nameOfHolder should contain LAST_NAME
            val surname = nameParts?.get(0)?.replace("<", " ")
            // other parts remaining of nameOfHolder should contain GIVEN_NAMES
            val givenNames = nameParts?.get(1)?.replace("<", " ")
            // in which multiple names are separated by '<' (single chevron)
            // Get proper date of birth
            val dateOfBirth = if (additionalPersonDetails?.fullDateOfBirth.isNullOrEmpty()) {
                DateUtils.toAdjustedDate (formatStandardDate(personDetails?.dateOfBirth))
            } else formatStandardDate(additionalPersonDetails?.fullDateOfBirth, "yyyyMMdd")
            return NFCResult(
                    givenNames = givenNames,
                    surname = surname,
                    nameOfHolder = additionalPersonDetails?.nameOfHolder,
                    gender = personDetails?.gender?.name,
                    documentNumber = personDetails?.documentNumber,
                    dateOfExpiry = DateUtils.toReadableDate(formatStandardDate(personDetails?.dateOfExpiry)),
                    issuingState = personDetails?.issuingState,
                    nationality = personDetails?.nationality,
                    otherNames = additionalPersonDetails?.otherNames?.arrayToString(),
                    dateOfBirth = dateOfBirth,
                    custodyInformation = additionalPersonDetails?.custodyInformation,
                    profession = additionalPersonDetails?.profession,
                    telephone = additionalPersonDetails?.telephone,
                    title = additionalPersonDetails?.title,
                    dateAndTimeOfPersonalization = additionalDocumentDetails?.dateAndTimeOfPersonalization,
                    dateOfIssue = formatStandardDate(additionalDocumentDetails?.dateOfIssue, "yyyyMMdd"),
                    endorsementsAndObservations = additionalDocumentDetails?.endorsementsAndObservations,
                    issuingAuthority = additionalDocumentDetails?.issuingAuthority,
                    personalizationSystemSerialNumber = additionalDocumentDetails?.personalizationSystemSerialNumber,
                    taxOrExitRequirements = additionalDocumentDetails?.taxOrExitRequirements,
                    mrzOptional = mrzInfo?.optionalData1,
                    mrzOptional2 = mrzInfo?.optionalData2
            )
        }
    }
}