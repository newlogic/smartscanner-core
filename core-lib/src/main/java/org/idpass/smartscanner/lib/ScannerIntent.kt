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
package org.idpass.smartscanner.lib

import android.content.Intent

/*
* TODO Remove when idpass-smarscanner-api is available in maven
* */
class ScannerIntent {

    companion object {
        @JvmStatic
        fun intentMrz(): Intent {
            val intent = Intent(ScannerConstants.IDPASS_SMARTSCANNER_INTENT)
            intent.putExtra(ScannerConstants.SCANNER, ScannerConstants.MRZ)
            return intent
        }

        @JvmStatic
        fun intentBarcode(): Intent {
            val intent = Intent(ScannerConstants.IDPASS_SMARTSCANNER_INTENT)
            intent.putExtra(ScannerConstants.SCANNER, ScannerConstants.BARCODE)
            return intent
        }

        @JvmStatic
        fun intentIDPassLite(): Intent {
            val intent = Intent(ScannerConstants.IDPASS_SMARTSCANNER_INTENT)
            intent.putExtra(ScannerConstants.SCANNER, ScannerConstants.IDPASS_LITE)
            return intent
        }
    }
}