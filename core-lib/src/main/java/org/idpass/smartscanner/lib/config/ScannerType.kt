package org.idpass.smartscanner.lib.config

import org.idpass.smartscanner.lib.ScannerConstants

enum class ScannerType (val value : String) {
    BARCODE(ScannerConstants.BARCODE),
    IDPASS_LITE(ScannerConstants.IDPASS_LITE),
    MRZ(ScannerConstants.MRZ);

    companion object {
        val barcodeOptions = ScannerOptions.defaultForBarcode
        val idPassLiteOptions = ScannerOptions.defaultForIdPassLite
        val mrzOptions = ScannerOptions.defaultForMRZ
    }
}