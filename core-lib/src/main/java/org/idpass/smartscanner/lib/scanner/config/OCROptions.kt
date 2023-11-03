package org.idpass.smartscanner.lib.scanner.config

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class OCROptions(
    val regex: String? = ".*",
    val country: String? = "",
    val type: String? = "",
    val analyzeStart: Long? = 0
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