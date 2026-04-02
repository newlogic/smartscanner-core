package org.idpass.smartscanner.lib.scanner.config

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SearchBox(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
) : Parcelable

@Parcelize
data class ScanIDOCRField(
    val label: String,
    val anchorValue: String,
    val searchBox: SearchBox,
    val regex: String? = null,
    // Standardized output key (e.g. "documentNumber", "firstName"). When set,
    // the extracted value is emitted under this key instead of the label.
    val key: String? = null
) : Parcelable

@Parcelize
data class ScanIDOCRCountryOptions(
    val countryLabelKey: String,
    val ocrData: List<ScanIDOCRField>,
    // Minimum number of distinct anchors that must be found per frame.
    val minAnchorsRequired: Int? = null,
    // Number of valid frames to accumulate before returning results.
    val maxFrames: Int? = null,
    // Enable cell-level OCR for fields with no text from full-image OCR.
    val cellLevelOcr: Boolean? = true,
    // Ratio of row height to column width for grid-based field detection.
    // rowHeight = columnWidth * rowHeightRatio. null = 1.0.
    val rowHeightRatio: Float? = null,
    // Date format for parsing Birth Date / Expiry Date fields into yyyy-MM-dd.
    // Uses SimpleDateFormat patterns (e.g. "dd/MM/yyyy", "dd-MM-yyyy", "ddMMMyyyy").
    val dateFormat: String? = null,
    // Maps raw gender values to standardized values (e.g. {"M": "Male", "MASCULINO": "Male"}).
    val genderMap: Map<String, String>? = null
) : Parcelable
