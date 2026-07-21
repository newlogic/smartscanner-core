package org.idpass.smartscanner.lib.mrz

import org.idpass.smartscanner.mrz.parser.innovatrics.MrzParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Verifies [MRZResult.mrz] carries the exact scanned MRZ (via [MrzRecord.rawMrz]) rather than the
 * lossy [org.idpass.smartscanner.mrz.parser.innovatrics.MrzRecord.toMrz] reconstruction, which
 * rebuilds line 1 with a fixed 9-char document-number field and truncates overflow numbers such as
 * the 11-digit Dominican cédula.
 */
class MRZResultTest {

    // Real DOM cédula (TD1, 3x30); document number 40222828713 overflows the 9-char field.
    private val domRawMrz = listOf(
        "IDDOM402228287<139<<<<<<<<<<<<",
        "9401194M1404306DOM<<<<<<<<<<<7",
        "PIMENTEL<DIAZ<<MAXIMO<DAVID<<<"
    ).joinToString("\n")

    @Test
    fun `mrz is the exact scanned string, not the toMrz reconstruction`() {
        val record = MrzParser.parse(domRawMrz)
        val result = MRZResult.formatMrzResult(record, image = null)

        assertEquals(domRawMrz, result.mrz)
        assertNotEquals(record.toMrz(), result.mrz)
    }

    @Test
    fun `overflow document number is preserved`() {
        val result = MRZResult.formatMrzResult(MrzParser.parse(domRawMrz), image = null)
        assertEquals("40222828713", result.documentNumber)
    }
}
