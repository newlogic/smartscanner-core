package com.newlogic.lib.idpass.config

import android.os.Parcelable
import com.newlogic.lib.idpass.config.Modes.BARCODE
import com.newlogic.lib.idpass.config.Modes.MRZ
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ScannerOptions(
        val mode: String? = MRZ.value,
        val config: Config? = null,
        val mrzFormat: String? = null,
        val barcodeOptions: BarcodeOptions? = null,
) : Parcelable {
    companion object {
        // Default
        val defaultForBarcode = ScannerOptions(mode = BARCODE.value, config = Config.default)
        val defaultForMRZ = ScannerOptions(mode = MRZ.value, config = Config.default)
        val defaultForIdPassLite = ScannerOptions(mode = BARCODE.value, barcodeOptions = BarcodeOptions.defaultIdPassLite, config = Config.default)
        // Sample
        fun sampleMrz(config: Config? = null, mrzFormat: String? = null) = ScannerOptions(mode = MRZ.value, config = config, mrzFormat = mrzFormat)
        fun sampleBarcode(config: Config? = null, barcodeOptions: BarcodeOptions?) = ScannerOptions(mode = BARCODE.value, config = config, barcodeOptions = barcodeOptions)
    }
}