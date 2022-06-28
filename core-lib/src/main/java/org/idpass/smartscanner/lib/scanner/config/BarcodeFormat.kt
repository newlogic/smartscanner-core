/*
 * Copyright (C) 2020 Newlogic Pte. Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 *
 */
package org.idpass.smartscanner.lib.scanner.config

import com.google.mlkit.vision.barcode.common.Barcode.*


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
    PDF_417("PDF_417", FORMAT_PDF417),
    QR_CODE("QR_CODE", FORMAT_QR_CODE),
    UPC_A("UPC_A", FORMAT_UPC_A),
    UPC_E("UPC_E", FORMAT_UPC_E);

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
                PDF_417.label,
                QR_CODE.label,
                UPC_A.label,
                UPC_E.label,
            )
    }
}
