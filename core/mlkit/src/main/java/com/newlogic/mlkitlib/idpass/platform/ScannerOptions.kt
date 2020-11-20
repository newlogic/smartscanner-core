package com.newlogic.mlkitlib.idpass.platform

import android.os.Parcelable
import com.newlogic.mlkitlib.idpass.config.Config
import com.newlogic.mlkitlib.idpass.config.Modes.BARCODE
import com.newlogic.mlkitlib.idpass.config.Modes.MRZ
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ScannerOptions(
    val mode: String? = MRZ.value,
    val config: Config? = null,
    val mrzFormat: String? = null,
    val barcodeOptions: List<String>? = null,
) : Parcelable {
    companion object {
        // Default
        val defaultForMRZ = ScannerOptions(MRZ.value, Config.default)
        val defaultForBarcode = ScannerOptions(BARCODE.value, Config.default)
        // Sample
        fun sampleMrz(config: Config? = null, mrzFormat: String? = null) = ScannerOptions(mode = MRZ.value, config = config, mrzFormat = mrzFormat)
        fun sampleBarcode(config: Config? = null, barcodeOptions: List<String>?) = ScannerOptions(BARCODE.value, config = config, barcodeOptions = barcodeOptions)
    }
}