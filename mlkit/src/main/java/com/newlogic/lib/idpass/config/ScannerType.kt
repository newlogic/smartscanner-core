package com.newlogic.lib.idpass.config

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