package com.newlogic.mlkitlib.idpass.config

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class BarcodeOptions(
    val barcodeFormats: List<String>? = null,
    val barcodeScannerSize: String? = null
) : Parcelable {
    companion object {
        val default = BarcodeOptions(
            barcodeScannerSize = ScannerSize.MEDIUM.value,
            barcodeFormats = BarcodeFormat.default
        )
    }
}
