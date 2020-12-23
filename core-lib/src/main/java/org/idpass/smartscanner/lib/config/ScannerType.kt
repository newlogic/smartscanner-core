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
package org.idpass.smartscanner.lib.config

import org.idpass.smartscanner.lib.ScannerConstants

enum class ScannerType (val value : String) {
    BARCODE(ScannerConstants.BARCODE),
    IDPASS_LITE(ScannerConstants.IDPASS_LITE),
    MRZ(ScannerConstants.MRZ);

    companion object {
        val barcodeOptions = ScannerOptions.defaultForBarcode
        val idPassLiteOptions = ScannerOptions.defaultForIdPassLite
        val mrzOptions = ScannerOptions.defaultForMRZ64
    }
}