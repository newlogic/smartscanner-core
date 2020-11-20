package com.newlogic.mlkitlib.idpass.config

import com.google.mlkit.vision.barcode.Barcode.*

enum class BarcodeFormat(val label: String, val value: Int) {
    ALL("ALL", FORMAT_ALL_FORMATS),
    AZTEC("AZTEC", FORMAT_AZTEC),
    CODABAR("CODABAR", FORMAT_CODABAR),
    CODE_39("CODE_39", FORMAT_CODE_39),
    CODE_93("CODE_93", FORMAT_CODE_93),
    CODE_128("CODE_128", FORMAT_CODE_128),
    DATA_MATRIX("DATA_MATRIX", FORMAT_DATA_MATRIX),
    EAN_8("EAN_8", FORMAT_EAN_8),
    EAN_13("EAN_13", FORMAT_EAN_13),
    ITF("ITF", FORMAT_ITF),
    QR_CODE("QR_CODE", FORMAT_QR_CODE),
    UPC_A("UPC_A", FORMAT_UPC_A),
    UPC_E("UPC_E", FORMAT_UPC_E),
    PDF_417("PDF_417", FORMAT_PDF417);

    companion object {
        val default =
            arrayListOf(
                AZTEC.label,
                CODABAR.label,
                CODABAR.label,
                CODE_39.label,
                CODE_93.label,
                CODE_128.label,
                DATA_MATRIX.label,
                EAN_8.label,
                EAN_13.label,
                ITF.label,
                QR_CODE.label,
                UPC_A.label,
                UPC_E.label,
            )
    }
}
