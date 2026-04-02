package org.idpass.smartscanner.lib.scanner.config

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class OcrRegion(
    val x: Int,
    val y: Int,
    val label: String? = null,
    val regex: String? = null
) : Parcelable
