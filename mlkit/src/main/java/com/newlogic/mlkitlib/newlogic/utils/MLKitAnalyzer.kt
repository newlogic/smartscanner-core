package com.newlogic.mlkitlib.newlogic.utils

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

class MLKitAnalyzer(private var analyze: ((ImageProxy) -> Unit)) : ImageAnalysis.Analyzer {
    override fun analyze(imageProxy: ImageProxy) {
        analyze.invoke(imageProxy)
    }
}