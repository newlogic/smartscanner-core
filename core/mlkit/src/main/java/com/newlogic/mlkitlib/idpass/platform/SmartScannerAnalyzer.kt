package com.newlogic.mlkitlib.idpass.platform

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

class SmartScannerAnalyzer(private var analyze: ((ImageProxy) -> Unit)) : ImageAnalysis.Analyzer {
    override fun analyze(imageProxy: ImageProxy) {
        analyze.invoke(imageProxy)
    }
}