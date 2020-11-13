package com.newlogic.mlkitlib.newlogic.platform

import android.os.Parcelable
import com.newlogic.mlkitlib.newlogic.config.Config
import com.newlogic.mlkitlib.newlogic.config.Modes
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ScannerOptions(
        val mode: String? = Modes.MRZ.value,
        val config: Config? = null,
        val mrzFormat: String? = null,
        val barcodeOptions: List<String>? = null,
) : Parcelable {
    companion object {
        val defaultForMRZ = ScannerOptions(Modes.MRZ.value, Config.default)
        val defaultForBarcode = ScannerOptions(Modes.BARCODE.value, Config.default)

        fun sampleMrz(config: Config? = null, mrzFormat: String? = null) = ScannerOptions(mode = Modes.MRZ.value, config = config, mrzFormat = mrzFormat)
        fun sampleBarcode(config: Config? = null, barcodeOptions: List<String>?) = ScannerOptions(Modes.BARCODE.value, config = config, barcodeOptions = barcodeOptions)
    }
}