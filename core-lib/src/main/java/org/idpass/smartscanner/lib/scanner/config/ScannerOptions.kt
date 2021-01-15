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
import org.idpass.smartscanner.lib.scanner.config.Modes.*

@Parcelize
data class ScannerOptions(
    val mode: String? = MRZ.value,
    val config: Config? = null,
    val mrzFormat: String? = null,
    val scannerSize: String? = null,
    val barcodeOptions: BarcodeOptions? = null,
) : Parcelable {
    companion object {
        // Default
        val defaultForBarcode = ScannerOptions(mode = BARCODE.value, config = Config.default)
        val defaultForMRZ = ScannerOptions(mode = MRZ.value, config = Config.default)
        val defaultForIdPassLite = ScannerOptions(
            mode = IDPASS_LITE.value,
            scannerSize = ScannerSize.LARGE.value,
            barcodeOptions = BarcodeOptions.defaultIdPassLite,
            config = Config.default
        )

        // Sample
        fun sampleMrz(config: Config? = null, mrzFormat: String? = null) =
            ScannerOptions(mode = MRZ.value, config = config, mrzFormat = mrzFormat)

        fun sampleBarcode(config: Config? = null, barcodeOptions: BarcodeOptions?) =
            ScannerOptions(mode = BARCODE.value, config = config, barcodeOptions = barcodeOptions)

        fun sampleIdPassLite(config: Config? = null) =
            ScannerOptions(
                mode = IDPASS_LITE.value,
                scannerSize = ScannerSize.LARGE.value,
                config = config
            )
    }
}