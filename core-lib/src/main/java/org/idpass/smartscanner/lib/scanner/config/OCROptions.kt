package org.idpass.smartscanner.lib.scanner.config

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class OCROptions(
    val regex: String? = ".*",
    val country: String? = "",
    val type: String? = "",
    val analyzeStart: Long? = 0,
    val ocrRegions: List<OcrRegion>? = null,
    val scanIDOCRCountryOptions: List<ScanIDOCRCountryOptions>? = null,
    val overrideDefaultCountryConfigs: Boolean = false,
    val width: Int? = null,
    val height: Int? = null
) : Parcelable {
    companion object {
        val default = OCROptions(
            regex = ".*",
            country = "N/A",
            type = "",
            analyzeStart = 0
        )
    }
}