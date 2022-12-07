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
import org.idpass.smartscanner.lib.SmartScannerActivity
import org.idpass.smartscanner.lib.mrz.MRZAnalyzer
import org.idpass.smartscanner.lib.mrz.MRZCleaner
import org.idpass.smartscanner.lib.mrz.MRZResult
import org.idpass.smartscanner.lib.nfc.NFCActivity.Companion.FOR_SMARTSCANNER_APP
import org.idpass.smartscanner.lib.nfc.details.IntentData
import org.idpass.smartscanner.lib.scanner.config.ImageResultType
import org.idpass.smartscanner.lib.scanner.config.Modes
import org.idpass.smartscanner.lib.utils.extension.cacheImagePath
import org.idpass.smartscanner.lib.utils.extension.cacheImageToLocal
import org.idpass.smartscanner.lib.utils.extension.cropCenter
import org.idpass.smartscanner.lib.utils.extension.encodeBase64
import java.io.File

open class NFCScanAnalyzer(
    override val activity: Activity,
    override val intent: Intent,
    override val mode: String = Modes.NFC_SCAN.value,
    private val label: String?,
    private val language: String?,
    private val locale: String?,
    private val withPhoto: Boolean,
    private val withMrzPhoto: Boolean,
    private val captureLog: Boolean,
    private val enableLogging: Boolean,
    private val imageResultType: String,
    isMLKit: Boolean,
    format: String? = null,
    analyzeStart: Long,
    isShowGuide: Boolean? = false
) : MRZAnalyzer(activity, intent, mode, label, language, locale, withMrzPhoto, withPhoto, captureLog, enableLogging, isMLKit, imageResultType, format, analyzeStart, isShowGuide) {

    override fun processResult(result: String, bitmap: Bitmap, rotation: Int) {
        val mrzResult =  MRZResult.formatMrzResult(MRZCleaner.parseAndClean(result))
        mrzResult.mrz?.let { mrzString ->
            Log.d(SmartScannerActivity.TAG, "Success from NFC -- SCAN")
            val nfcIntent = Intent(activity, NFCActivity::class.java)
            when {
                intent.hasExtra(FOR_SMARTSCANNER_APP) -> nfcIntent.putExtra(FOR_SMARTSCANNER_APP, true)
                intent.action == ScannerConstants.IDPASS_SMARTSCANNER_NFC_INTENT ||
                intent.action == ScannerConstants.IDPASS_SMARTSCANNER_ODK_NFC_INTENT -> nfcIntent.putExtra(ScannerConstants.NFC_ACTION, intent.action)
            }
            if (withMrzPhoto) {
                val imagePath = activity.cacheImagePath()
                bitmap.cropCenter().cacheImageToLocal(imagePath,
                    rotation,
                    if (imageResultType == ImageResultType.BASE_64.value) 40 else 80
                )
                val imageFile = File(imagePath)
                val imageString = if (imageResultType == ImageResultType.BASE_64.value) imageFile.encodeBase64() else imagePath
                nfcIntent.putExtra(IntentData.KEY_MRZ_PHOTO, imageString)
            }
            nfcIntent.putExtra(ScannerConstants.NFC_MRZ_STRING, mrzString)
            nfcIntent.putExtra(ScannerConstants.NFC_LOCALE, locale)
            nfcIntent.putExtra(ScannerConstants.LANGUAGE, language)
            nfcIntent.putExtra(IntentData.KEY_LABEL, label)
            nfcIntent.putExtra(IntentData.KEY_WITH_PHOTO, withPhoto)
            nfcIntent.putExtra(IntentData.KEY_CAPTURE_LOG, captureLog)
            nfcIntent.putExtra(IntentData.KEY_ENABLE_LOGGGING, enableLogging)
            nfcIntent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
            activity.startActivity(nfcIntent)
            activity.finish()
        }
    }
}