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
import org.idpass.smartscanner.lib.scanner.config.Language.Locale
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
            nfcLocale = Locale.LTR,
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
    }
}