package org.idpass.smartscanner.lib.config

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.idpass.smartscanner.lib.config.Modes.BARCODE
import org.idpass.smartscanner.lib.config.Modes.MRZ

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