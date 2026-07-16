package org.idpass.smartscanner.lib.ocr

import org.idpass.smartscanner.lib.scanner.config.ScanIDOCRCountryOptions

/**
 * Pure, Android-free decision logic for anchor-based ID OCR scanning.
 * Extracted from [OCRAnalyzer] so it can be unit-tested without a camera or MLKit.
 */
internal object OCRScanLogic {

    // Month name mapping for manual date parsing (covers Spanish and English)
    private val monthMap = mapOf(
        "ENE" to "01", "FEB" to "02", "MAR" to "03", "ABR" to "04",
        "MAY" to "05", "JUN" to "06", "JUL" to "07", "AGO" to "08",
        "SEP" to "09", "OCT" to "10", "NOV" to "11", "DIC" to "12",
        "JAN" to "01", "AUG" to "08", "DEC" to "12"
    )

    /**
     * Known directional single-glyph OCR confusions for letter-style gender fields
     * (e.g. Nicaragua "M"/"F"). Maps a misread glyph to the canonical letter it was
     * most likely meant to be. Conservative and directional: an F-family glyph never
     * maps toward "M". Identity M->M / F->F is handled by the exact lookup upstream.
     */
    private val GENDER_GLYPH_CONFUSIONS = mapOf(
        'N' to 'M', 'H' to 'M', 'W' to 'M',
        'E' to 'F', 'P' to 'F', 'T' to 'F'
    )

    /**
     * Picks the most-frequently-read value for each field from the accumulated
     * per-field vote tallies. Insertion order is preserved.
     */
    fun pickBestValues(accumulated: Map<String, Map<String, Int>>): LinkedHashMap<String, String> {
        val out = LinkedHashMap<String, String>()
        accumulated.forEach { (label, counts) ->
            counts.maxByOrNull { it.value }?.let { out[label] = it.key }
        }
        return out
    }

    /**
     * Remaps extracted fields from display labels to standardized keys, and normalizes
     * date and gender values based on the country config. Fields that can't be confidently
     * resolved (e.g. an unreadable gender) are omitted rather than emitted garbled.
     */
    fun normalizeFields(
        rawFields: Map<String, String>,
        country: ScanIDOCRCountryOptions
    ): Map<String, String> {
        val labelToField = country.ocrData.associateBy { it.label }
        val normalized = mutableMapOf<String, String>()

        for ((label, value) in rawFields) {
            val field = labelToField[label]
            val outputKey = field?.key ?: label

            when (outputKey) {
                "dateOfBirth", "expiryDate" ->
                    normalized[outputKey] = parseDate(value.trim(), country.dateFormat)
                "gender" ->
                    resolveGender(value, country.genderMap)?.let { normalized[outputKey] = it }
                else ->
                    normalized[outputKey] = value
            }
        }

        return normalized
    }

    /**
     * Resolves a raw OCR'd gender value to its canonical form via [genderMap].
     * Chain: exact -> fuzzy (multi-char words) -> single-glyph confusion -> unresolved.
     * Returns null when the value can't be confidently resolved, so the caller omits
     * gender rather than emitting a garbled string.
     */
    fun resolveGender(value: String, genderMap: Map<String, String>?): String? {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return null
        // No map configured: pass the raw value through (best-effort, existing behavior).
        if (genderMap == null) return trimmed
        val upper = trimmed.uppercase()

        // 1. Exact (covers clean M/F/MASCULINO, and lowercase via the uppercased key).
        (genderMap[trimmed] ?: genderMap[upper])?.let { return it }

        // 2. Fuzzy match for multi-char words. Threshold floors to 0 for short keys,
        //    so single letters never fuzzy-match here (M<->F is distance 1 — a tie).
        if (upper.length >= 4) {
            val best = genderMap.keys
                .map { key -> key to levenshtein(upper, key.uppercase()) }
                .filter { (key, dist) -> dist <= (key.length * 0.3f).toInt() }
                .minByOrNull { it.second }
            if (best != null) return genderMap[best.first]
        }

        // 3. Single-glyph confusion — only for letter-style gender (single-char keys).
        if (upper.length == 1 && genderMap.keys.any { it.length == 1 }) {
            val canonical = GENDER_GLYPH_CONFUSIONS[upper[0]]?.toString()
            if (canonical != null) {
                (genderMap[canonical] ?: genderMap[canonical.lowercase()])?.let { return it }
            }
        }

        // 4. Unresolved -> omit.
        return null
    }

    /**
     * Parses a date string into yyyy-MM-dd based on the country's dateFormat.
     * Handles: dd/MM/yyyy, dd-MM-yyyy, ddMMMyyyy (with month name lookup).
     * Returns the original value if parsing fails.
     */
    fun parseDate(value: String, dateFormat: String?): String {
        if (dateFormat == null) return value
        try {
            return when (dateFormat) {
                "dd/MM/yyyy" -> {
                    val parts = value.split("/")
                    if (parts.size == 3) "${parts[2]}-${parts[1]}-${parts[0]}" else value
                }
                "dd-MM-yyyy" -> {
                    val parts = value.split("-")
                    if (parts.size == 3) "${parts[2]}-${parts[1]}-${parts[0]}" else value
                }
                "ddMMMyyyy" -> {
                    // e.g. "19SEP2000" or "25NOV1980"
                    val match = Regex("^(\\d{2})([A-Za-z]{3})(\\d{4})$").find(value) ?: return value
                    val day = match.groupValues[1]
                    val monthStr = match.groupValues[2].uppercase()
                    val year = match.groupValues[3]
                    val month = monthMap[monthStr] ?: return value
                    "$year-$month-$day"
                }
                else -> value
            }
        } catch (e: Exception) {
            return value
        }
    }

    /** Levenshtein edit distance between [a] and [b]. */
    fun levenshtein(a: String, b: String): Int {
        val m = a.length
        val n = b.length
        var prev = IntArray(n + 1) { it }
        var curr = IntArray(n + 1)
        for (i in 1..m) {
            curr[0] = i
            for (j in 1..n) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                curr[j] = minOf(curr[j - 1] + 1, prev[j] + 1, prev[j - 1] + cost)
            }
            val tmp = prev; prev = curr; curr = tmp
        }
        return prev[n]
    }
}
