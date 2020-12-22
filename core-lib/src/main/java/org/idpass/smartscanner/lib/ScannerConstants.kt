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

/*
* TODO Remove when idpass-smarscanner-api is available in maven
* */
object ScannerConstants {

    // Scanner
    const val SCANNER = "SCANNER"
    const val RESULT = "RESULT"

    // Types
    const val BARCODE = "barcode"
    const val IDPASS_LITE = "idpass-lite"
    const val MRZ = "mrz"

    // BARCODE or ID PASS Lite
    const val BARCODE_IMAGE = "IMAGE"
    const val BARCODE_CORNERS = "CORNERS"
    const val BARCODE_VALUE = "RAW_VALUE"
    const val BARCODE_RAW = "RAW_BYTES"

    // MRZ
    const val MRZ_IMAGE = "IMAGE"
    const val MRZ_CODE = "CODE"
    const val MRZ_CODE_1 = "CODE_1"
    const val MRZ_CODE_2 = "CODE_2"
    const val MRZ_DATE_BIRTH = "DATE_OF_BIRTH"
    const val MRZ_DOC_NUM = "DOCUMENT_NUMBER"
    const val MRZ_EXP_DATE = "EXPIRY_DATE"
    const val MRZ_FORMAT = "FORMAT"
    const val MRZ_GIVEN_NAMES = "GIVEN_NAMES"
    const val MRZ_ISSUE_COUNTRY = "ISSUE_COUNTRY"
    const val MRZ_NATIONALITY = "NATIONALITY"
    const val MRZ_SEX = "SEX"
    const val MRZ_SURNAME = "SURNAME"

    // Intent
    const val IDPASS_SMARTSCANNER_INTENT = "org.idpass.smartscanner.SCAN"
}