package org.idpass.smartscanner.lib.ocr

data class OCRResult(
    val imagePath: String?,
    val image: String? = null,
    val regex: String,
    val country: String? = null,
    val value: Any?,
    val type: String?
)