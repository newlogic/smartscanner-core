package org.idpass.smartscanner.innovatrics.mrz

import org.idpass.smartscanner.innovatrics.mrz.records.MrtdTd1
import org.idpass.smartscanner.lib.extension.empty
import org.idpass.smartscanner.lib.extension.noValue


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
                    image,
                    record.code.toString(),
                    record.code1.toShort(),
                    record.code2.toShort(),
                    record.dateOfBirth?.toString()?.replace(Regex("[{}]"), ""),
                    record.documentNumber.toString(),
                    record.expirationDate?.toString()?.replace(Regex("[{}]"), ""),
                    record.format.toString(),
                    record.givenNames,
                    record.issuingCountry,
                    record.nationality,
                    record.sex.toString(),
                    record.surname,
                    record.toMrz()
            )
        }

        fun formatMrtdTd1Result(record: MrtdTd1, image: String?) : MRZResult {
            return MRZResult(
                    image,
                    record.code.toString(),
                    record.code1.toShort(),
                    record.code2.toShort(),
                    record.dateOfBirth?.toString()?.replace(Regex("[{}]"), ""),
                    record.documentNumber.toString(),
                    record.expirationDate?.toString()?.replace(Regex("[{}]"), ""),
                    record.format.toString(),
                    record.givenNames,
                    record.issuingCountry,
                    record.nationality,
                    record.sex.toString(),
                    record.surname,
                    record.toMrz(),
                    record.optional,
                    record.optional2
            )
        }

        fun getImageOnly(image: String?) : MRZResult {
            return MRZResult(
                    image,
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