package org.idpass.smartscanner.lib.config

enum class ScannerType (val value : String) {
    BARCODE("barcode"),
    IDPASS_LITE("idpass-lite"),
    MRZ("mrz");

    companion object {
        val barcodeOptions = ScannerOptions.defaultForBarcode
        val idPassLiteOptions = ScannerOptions.defaultForIdPassLite
        val mrzOptions = ScannerOptions.defaultForMRZ
    }
}