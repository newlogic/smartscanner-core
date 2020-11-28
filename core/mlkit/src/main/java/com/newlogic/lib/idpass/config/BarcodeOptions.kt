package com.newlogic.lib.idpass.config

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

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
    }
}
