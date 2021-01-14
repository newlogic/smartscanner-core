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
package org.idpass.smartscanner.lib.mrz

import org.idpass.smartscanner.lib.platform.extension.empty
import org.idpass.smartscanner.lib.platform.extension.noValue
import org.idpass.smartscanner.mrz.parser.innovatrics.MrzRecord
import org.idpass.smartscanner.mrz.parser.innovatrics.records.MrtdTd1


data class MRZResult(
        val image: String?,
        val code: String?,
        val code1: Short?,
        val code2: Short?,
        val dateOfBirth: String?,
        val documentNumber: String?,
        val expirationDate: String?,
        val format: String?,
        val givenNames: String?,
        val issuingCountry: String?,
        val nationality: String?,
        val sex: String?,
        val surname: String?,
        var mrz: String?,
        val optional: String? = null,
        val optional2: String? = null
) {
    companion object {
        fun formatMrzResult(record: MrzRecord, image: String?) : MRZResult {
            return MRZResult(
                    image = image,
                    code = record.code.toString(),
                    code1 = record.code1.toShort(),
                    code2 = record.code2.toShort(),
                    dateOfBirth = record.dateOfBirth?.toString()?.replace(Regex("[{}]"), ""),
                    documentNumber = record.documentNumber.toString(),
                    expirationDate = record.expirationDate?.toString()?.replace(Regex("[{}]"), ""),
                    format = record.format.toString(),
                    givenNames = record.givenNames,
                    issuingCountry = record.issuingCountry,
                    nationality = record.nationality,
                    sex = record.sex.toString(),
                    surname = record.surname,
                    mrz = record.toMrz()
            )
        }

        fun formatMrtdTd1Result(record: MrtdTd1, image: String?) : MRZResult {
            return MRZResult(
                    image = image,
                    code = record.code.toString(),
                    code1 = record.code1.toShort(),
                    code2 = record.code2.toShort(),
                    dateOfBirth = record.dateOfBirth?.toString()?.replace(Regex("[{}]"), ""),
                    documentNumber = record.documentNumber.toString(),
                    expirationDate = record.expirationDate?.toString()?.replace(Regex("[{}]"), ""),
                    format = record.format.toString(),
                    givenNames = record.givenNames,
                    issuingCountry = record.issuingCountry,
                    nationality = record.nationality,
                    sex = record.sex.toString(),
                    surname = record.surname,
                    mrz = record.toMrz(),
                    optional = record.optional,
                    optional2 = record.optional2
            )
        }

        fun getImageOnly(image: String?) : MRZResult {
            return MRZResult(
                    image = image,
                    String.empty(),
                    Int.noValue().toShort(),
                    Int.noValue().toShort(),
                    String.empty(),
                    String.empty(),
                    String.empty(),
                    String.empty(),
                    String.empty(),
                    String.empty(),
                    String.empty(),
                    String.empty(),
                    String.empty(),
                    String.empty()
            )
        }
    }
}