package com.newlogic.mlkitlib.newlogic.platform

import android.util.Log
import com.newlogic.mlkitlib.innovatrics.mrz.MrzParser
import com.newlogic.mlkitlib.innovatrics.mrz.MrzRecord
import com.newlogic.mlkitlib.innovatrics.mrz.records.MrtdTd1
import com.newlogic.mlkitlib.newlogic.MLKitActivity
import java.net.URLEncoder

object MRZCleaner {
    private var previousMRZString:String? = null

    fun clean(mrz: String): String {
        var result = mrz
            .replace(Regex("^[^PIACV]*"), "") // Remove everything before P, I, A or C
            .replace(Regex("[ \\t\\r]+"), "") // Remove any white space
            .replace(Regex("\\n+"), "\n") // Remove extra new lines
            .replace("«", "<")
            .replace("<c<", "<<<")
            .replace("<e<", "<<<")
            .replace("<E<", "<<<") // Good idea? Maybe not.
            .replace("<K<", "<<<") // Good idea? Maybe not.
            .replace("<¢<", "<<<")
            .replace("<(<", "<<<")
            .replace("<{<", "<<<")
            .replace("<[<", "<<<")
            .replace(Regex("^P[KC]"), "P<")
            .replace(Regex("[^A-Z0-9<\\n]"), "") // Remove any other char
            .trim()

        if (result.contains("<") && (
                    result.startsWith("P") ||
                    result.startsWith("I") ||
                    result.startsWith("A") ||
                    result.startsWith("C") ||
                    result.startsWith("V"))
        ) {
            when (result.filter{ it == '\n' }.count()) {
                1 -> {
                    if (result.length > 89) {
                        result = result.slice(IntRange(0, 88))
                    }
                }
                2 -> {
                    if (result.length > 92) {
                        result = result.slice(IntRange(0, 91))
                    }
                }
                else -> throw IllegalArgumentException("Invalid MRZ string. Wrong number of lines.")
            }
        } else {
            Log.d(MLKitActivity.TAG, "Error = [${URLEncoder.encode(result, "UTF-8").replace("%3C", "<").replace("%0A", "↩")}]")
            throw IllegalArgumentException("Invalid MRZ string. No '<' or 'P', 'I', 'A', 'C', 'V' detected.")
        }

        return result
    }

    private fun String.replaceNumbertoChar(): String {
        return this
            .replace('0', 'O')
            .replace("1", "I")
            .replace("8", "B")
            .replace("5", "S")
            .replace("2", "Z")
    }

    fun parseAndClean(mrz: String): MrzRecord {
        val record = MrzParser.parse(mrz)

        Log.d(MLKitActivity.TAG, "Previous Scan: $previousMRZString")
        if (!record.validComposite || !record.validDateOfBirth || !record.validDocumentNumber || !record.validExpirationDate) {
            if (mrz != previousMRZString) {
                previousMRZString = mrz
                throw IllegalArgumentException("Invalid check digits.")
            }
            Log.d(MLKitActivity.TAG, "Still accept scanning.")
        }

        record.givenNames = record.givenNames.replaceNumbertoChar()
        record.surname = record.surname.replaceNumbertoChar()
        record.issuingCountry = record.issuingCountry.replaceNumbertoChar()
        record.nationality = record.nationality.replaceNumbertoChar()
        return record
    }

    fun parseAndCleanMrtdTd1(mrz: String): MrtdTd1 {
        val record = MrzParser.parseToMrtdTd1(mrz)

        Log.d(MLKitActivity.TAG, "Previous Scan: $previousMRZString")
        if (!record.validComposite || !record.validDateOfBirth || !record.validDocumentNumber || !record.validExpirationDate) {
            if (mrz != previousMRZString) {
                previousMRZString = mrz
                throw IllegalArgumentException("Invalid check digits.")
            }
            Log.d(MLKitActivity.TAG, "Still accept scanning.")
        }

        record.givenNames = record.givenNames.replaceNumbertoChar()
        record.surname = record.surname.replaceNumbertoChar()
        record.issuingCountry = record.issuingCountry.replaceNumbertoChar()
        record.nationality = record.nationality.replaceNumbertoChar()
        return record
    }

}