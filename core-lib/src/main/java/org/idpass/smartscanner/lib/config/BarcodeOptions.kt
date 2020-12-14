package org.idpass.smartscanner.lib.config

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class BarcodeOptions(
    val barcodeFormats: List<String>? = null,
    val barcodeScannerSize: String? = null,
    val idPassLiteSupport: Boolean? = null
) : Parcelable {
    companion object {
        val default = BarcodeOptions(
            barcodeScannerSize = ScannerSize.MEDIUM.value,
            barcodeFormats = BarcodeFormat.default,
            idPassLiteSupport = false
        )
        val defaultIdPassLite = BarcodeOptions(
            barcodeScannerSize = ScannerSize.MEDIUM.value,
            barcodeFormats = BarcodeFormat.default,
            idPassLiteSupport = true
        )
    }
}
