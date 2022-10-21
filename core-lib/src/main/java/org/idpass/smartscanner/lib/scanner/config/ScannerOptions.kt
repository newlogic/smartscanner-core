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

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.idpass.smartscanner.api.ScannerConstants
import org.idpass.smartscanner.lib.scanner.config.Modes.*

@Parcelize
data class ScannerOptions(
    val mode: String? = MRZ.value,
    val config: Config? = null,
    val language: String? = null,
    val nfcLocale: String? = null,
    val mrzFormat: String? = null,
    val scannerSize: String? = null,
    val barcodeOptions: BarcodeOptions? = null,
    val captureOptions: CaptureOptions? = null,
    val nfcOptions: NFCOptions? = null,
    val qrCodeOptions: QRcodeOptions? = null,
    val sentryLogger: SentryLogger? = null,
) : Parcelable {
    companion object {
        val defaultForBarcode = ScannerOptions(
            mode = BARCODE.value,
            scannerSize = ScannerSize.LARGE.value,
            config = Config.default
        )

        val defaultForCaptureOnly = ScannerOptions(
            mode = CAPTURE_ONLY.value,
            config = Config.default,
            captureOptions = CaptureOptions.default
        )

        val defaultForIdPassLite = ScannerOptions(
            mode = IDPASS_LITE.value,
            scannerSize = ScannerSize.LARGE.value,
            barcodeOptions = BarcodeOptions.defaultIdPassLite,
            config = Config.default
        )

        val defaultForMRZ = ScannerOptions(
            mode = MRZ.value,
            config = Config.default
        )

        val defaultForNFCScan = ScannerOptions(
            mode = NFC_SCAN.value,
            nfcOptions = NFCOptions.default,
            config = Config(
                isManualCapture = false,
                branding = true
            )
        )

        val defaultForQRCode = ScannerOptions(
            mode = QRCODE.value,
            scannerSize = ScannerSize.LARGE.value,
            config = Config.default
        )

        fun defaultForODK(action : String?) : ScannerOptions? {
            return when (action) {
                // barcode
                ScannerConstants.IDPASS_SMARTSCANNER_BARCODE_INTENT,
                ScannerConstants.IDPASS_SMARTSCANNER_ODK_BARCODE_INTENT -> defaultForBarcode
                // idpass lite
                ScannerConstants.IDPASS_SMARTSCANNER_IDPASS_LITE_INTENT,
                ScannerConstants.IDPASS_SMARTSCANNER_ODK_IDPASS_LITE_INTENT -> defaultForIdPassLite
                // mrz
                ScannerConstants.IDPASS_SMARTSCANNER_MRZ_INTENT,
                ScannerConstants.IDPASS_SMARTSCANNER_ODK_MRZ_INTENT -> defaultForMRZ
                // nfc
                ScannerConstants.IDPASS_SMARTSCANNER_NFC_INTENT,
                ScannerConstants.IDPASS_SMARTSCANNER_ODK_NFC_INTENT -> defaultForNFCScan
                // qrcode
                ScannerConstants.IDPASS_SMARTSCANNER_QRCODE_INTENT,
                ScannerConstants.IDPASS_SMARTSCANNER_ODK_QRCODE_INTENT -> defaultForQRCode
                else -> null
            }
        }
    }
}