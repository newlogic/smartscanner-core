package org.idpass.smartscanner.lib.ocr

import org.idpass.smartscanner.lib.scanner.config.DefaultCountryConfigs
import org.idpass.smartscanner.lib.scanner.config.ScanIDOCRCountryOptions
import org.idpass.smartscanner.lib.scanner.config.ScanIDOCRField
import org.idpass.smartscanner.lib.scanner.config.SearchBox
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the pure OCR decision logic extracted from OCRAnalyzer:
 * gender value resolution (exact -> fuzzy -> glyph-confusion -> omit),
 * frame vote-winner selection, and field normalization.
 */
class OCRScanLogicTest {

    private val letterGenderMap = mapOf("M" to "Male", "F" to "Female")
    private val wordGenderMap = mapOf("MASCULINO" to "Male", "FEMENINO" to "Female")

    // ---------- resolveGender: exact ----------

    @Test
    fun `exact single-letter maps to canonical`() {
        assertEquals("Male", OCRScanLogic.resolveGender("M", letterGenderMap))
        assertEquals("Female", OCRScanLogic.resolveGender("F", letterGenderMap))
    }

    @Test
    fun `lowercase single-letter maps via uppercased key`() {
        assertEquals("Male", OCRScanLogic.resolveGender("m", letterGenderMap))
        assertEquals("Female", OCRScanLogic.resolveGender(" f ", letterGenderMap))
    }

    @Test
    fun `exact full word maps to canonical`() {
        assertEquals("Male", OCRScanLogic.resolveGender("MASCULINO", wordGenderMap))
        assertEquals("Female", OCRScanLogic.resolveGender("femenino", wordGenderMap))
    }

    // ---------- resolveGender: fuzzy (multi-char words) ----------

    @Test
    fun `fuzzy rescues a misread word within threshold`() {
        // MASCUUNO is edit-distance 2 from MASCULINO (tol floor(9*0.3)=2).
        assertEquals("Male", OCRScanLogic.resolveGender("MASCUUNO", wordGenderMap))
    }

    @Test
    fun `fuzzy does not match a word that is too corrupted`() {
        assertNull(OCRScanLogic.resolveGender("XYZABCDE", wordGenderMap))
    }

    // ---------- resolveGender: single-glyph confusion ----------

    @Test
    fun `confusion table maps look-alike glyphs to the intended sex`() {
        assertEquals("Female", OCRScanLogic.resolveGender("E", letterGenderMap)) // E -> F
        assertEquals("Female", OCRScanLogic.resolveGender("P", letterGenderMap)) // P -> F
        assertEquals("Female", OCRScanLogic.resolveGender("T", letterGenderMap)) // T -> F
        assertEquals("Male", OCRScanLogic.resolveGender("N", letterGenderMap))   // N -> M
        assertEquals("Male", OCRScanLogic.resolveGender("H", letterGenderMap))   // H -> M
        assertEquals("Male", OCRScanLogic.resolveGender("W", letterGenderMap))   // W -> M
    }

    @Test
    fun `unknown single glyph is omitted rather than coin-flipped`() {
        assertNull(OCRScanLogic.resolveGender("X", letterGenderMap))
        assertNull(OCRScanLogic.resolveGender("O", letterGenderMap))
    }

    @Test
    fun `single letter never fuzzy-flips F to M`() {
        // F is exact-Female; must never resolve to Male, and confusion is directional.
        assertEquals("Female", OCRScanLogic.resolveGender("F", letterGenderMap))
        // A short garbage string must not fuzzy-snap to a single-letter key.
        assertNull(OCRScanLogic.resolveGender("MASC", letterGenderMap))
    }

    @Test
    fun `word map never triggers single-glyph confusion`() {
        // "E" would be Female under the letter map, but must NOT resolve under a word map.
        assertNull(OCRScanLogic.resolveGender("E", wordGenderMap))
    }

    // ---------- resolveGender: edge cases ----------

    @Test
    fun `empty or blank value is omitted`() {
        assertNull(OCRScanLogic.resolveGender("", letterGenderMap))
        assertNull(OCRScanLogic.resolveGender("   ", letterGenderMap))
    }

    @Test
    fun `null gender map passes the trimmed raw value through`() {
        assertEquals("M", OCRScanLogic.resolveGender(" M ", null))
        assertNull(OCRScanLogic.resolveGender("  ", null))
    }

    // ---------- pickBestValues: voting ----------

    @Test
    fun `voting picks the majority value over three frames`() {
        val accumulated = mapOf(
            "Document ID" to mapOf("123" to 2, "128" to 1)
        )
        assertEquals("123", OCRScanLogic.pickBestValues(accumulated)["Document ID"])
    }

    @Test
    fun `voting on a tie keeps the first-seen value deterministically`() {
        // LinkedHashMap preserves insertion order; maxByOrNull returns the first max.
        val counts = LinkedHashMap<String, Int>()
        counts["A"] = 1
        counts["B"] = 1
        val accumulated = mapOf("Field" to counts)
        assertEquals("A", OCRScanLogic.pickBestValues(accumulated)["Field"])
    }

    // ---------- normalizeFields ----------

    private fun nicaraguaLike(): ScanIDOCRCountryOptions = ScanIDOCRCountryOptions(
        countryLabelKey = "NICARAGUA",
        dateFormat = "dd-MM-yyyy",
        genderMap = letterGenderMap,
        ocrData = listOf(
            ScanIDOCRField("Document ID", "IDENTIDAD", SearchBox(0f, 0f, 1f, 1f), key = "documentNumber"),
            ScanIDOCRField("Birth Date", "Nacimiento", SearchBox(0f, 0f, 1f, 1f), key = "dateOfBirth"),
            ScanIDOCRField("Sex", "Sexo", SearchBox(0f, 0f, 1f, 1f), key = "gender")
        )
    )

    @Test
    fun `normalizeFields remaps keys and normalizes date and gender`() {
        val raw = mapOf(
            "Document ID" to "001-123",
            "Birth Date" to "05-11-1990",
            "Sex" to "m"
        )
        val out = OCRScanLogic.normalizeFields(raw, nicaraguaLike())
        assertEquals("001-123", out["documentNumber"])
        assertEquals("1990-11-05", out["dateOfBirth"])
        assertEquals("Male", out["gender"])
    }

    @Test
    fun `normalizeFields rescues a look-alike gender glyph`() {
        val out = OCRScanLogic.normalizeFields(mapOf("Sex" to "E"), nicaraguaLike())
        assertEquals("Female", out["gender"])
    }

    @Test
    fun `normalizeFields omits unresolved gender instead of emitting garbage`() {
        val out = OCRScanLogic.normalizeFields(mapOf("Sex" to "SEXO"), nicaraguaLike())
        assertFalse("gender should be omitted when unresolved", out.containsKey("gender"))
    }

    // ---------- config regression guards ----------

    @Test
    fun `nicaragua default gender regex is permissive for glyph rescue`() {
        val sex = DefaultCountryConfigs.NICARAGUA.ocrData.first { it.key == "gender" }
        assertEquals("[A-Za-z]", sex.regex)
    }

    @Test
    fun `built-in configs leave maxFrames unset so the 3-frame default applies`() {
        assertTrue(DefaultCountryConfigs.ALL.all { it.maxFrames == null })
    }
}
