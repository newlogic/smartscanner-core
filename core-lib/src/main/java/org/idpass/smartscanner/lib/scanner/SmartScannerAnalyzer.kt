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
package org.idpass.smartscanner.lib.scanner

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import org.idpass.smartscanner.lib.SmartScannerActivity
import org.idpass.smartscanner.lib.platform.extension.toBitmap
import org.idpass.smartscanner.lib.scanner.config.Modes

class SmartScannerAnalyzer (private val mode : String?,
                            private val barcodeAnalysis: ((Bitmap, ImageProxy) -> Unit),
                            private val mrzAnalysis: ((Bitmap, ImageProxy) -> Unit)) : ImageAnalysis.Analyzer {

    @SuppressLint("UnsafeExperimentalUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            Log.d(SmartScannerActivity.TAG, "Bitmap: (${mediaImage.width}, ${mediaImage.height})")
            val rot = imageProxy.imageInfo.rotationDegrees
            val bf = mediaImage.toBitmap(rot, mode)
            if (mode == Modes.BARCODE.value) {
                barcodeAnalysis.invoke(bf, imageProxy)
            }
            if (mode == Modes.MRZ.value) {
                mrzAnalysis.invoke(bf, imageProxy)
            }
        }
    }
}