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
package org.idpass.smartscanner.lib.nfc

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import org.idpass.smartscanner.api.ScannerConstants
import org.idpass.smartscanner.api.ScannerIntent
import org.idpass.smartscanner.lib.SmartScannerActivity
import org.idpass.smartscanner.lib.mrz.MRZAnalyzer
import org.idpass.smartscanner.lib.scanner.config.Modes

open class NFCScanAnalyzer(
    override val activity: Activity,
    override val intent: Intent,
    override val mode: String = Modes.MRZ.value,
    isMLKit: Boolean,
    imageResultType: String,
    format: String?,
    onConnectFail: (String) -> Unit
) : MRZAnalyzer(activity, intent, mode, isMLKit, imageResultType, format, onConnectFail) {

    override fun processResult(mrz: String, bitmap: Bitmap, rotation: Int) {
        Log.d(SmartScannerActivity.TAG, "Success from NFC -- SCAN mrz")
        if (intent.action == ScannerConstants.IDPASS_SMARTSCANNER_NFC_SCAN_INTENT ||
            intent.action == ScannerConstants.IDPASS_SMARTSCANNER_ODK_NFC_SCAN_INTENT) {
            ScannerIntent.intentNFC(mrz)
        } else {
            val intent = Intent(activity, NFCActivity::class.java)
            intent.putExtra(ScannerConstants.NFC_MRZ_STRING, mrz)
            activity.startActivity(intent)
            activity.finish()
        }
    }
}