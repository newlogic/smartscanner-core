package org.idpass.smartscanner.lib.ocr

data class OCRResult(
    val imagePath: String?,
    val image: String? = null,
    val regex: String,
    val valuesArray: ArrayList<String>,
    val value: Any?,
    val type: String?
)