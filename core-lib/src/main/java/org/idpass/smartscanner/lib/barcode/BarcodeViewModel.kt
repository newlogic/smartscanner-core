package org.idpass.smartscanner.lib.barcode

import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import org.idpass.smartscanner.lib.SmartScannerActivity
import org.idpass.smartscanner.lib.platform.extension.cacheImageToLocal

class BarcodeViewModel : ViewModel() {

    fun analyze(barcodeFormats : List<Int>, bitmap : Bitmap, filePath : String, imageProxy : ImageProxy, onResult: (BarcodeResult) -> Unit) {
        val start = System.currentTimeMillis()
        var barcodeFormat = Barcode.FORMAT_CODE_39 // Most common barcode format
        barcodeFormats.forEach {
            barcodeFormat = it or barcodeFormat // bitwise different barcode format options
        }
        val options = BarcodeScannerOptions.Builder().setBarcodeFormats(barcodeFormat).build()
        val image = InputImage.fromBitmap(bitmap, imageProxy.imageInfo.rotationDegrees)
        val scanner = BarcodeScanning.getClient(options)
        Log.d("${SmartScannerActivity.TAG}/SmartScanner", "barcode: process")
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                val timeRequired = System.currentTimeMillis() - start
                val rawValue: String
                val cornersString: String
                Log.d("${SmartScannerActivity.TAG}/SmartScanner", "barcode: success: $timeRequired ms")
                if (barcodes.isNotEmpty()) {
                    val corners = barcodes[0].cornerPoints
                    val builder = StringBuilder()
                    if (corners != null) {
                        for (corner in corners) {
                            builder.append("${corner.x},${corner.y} ")
                        }
                    }
                    cornersString = builder.toString()
                    rawValue = barcodes[0].rawValue!!
                    bitmap.cacheImageToLocal(
                        filePath,
                        imageProxy.imageInfo.rotationDegrees
                    )
                    val barcodeResult = BarcodeResult(filePath, cornersString, rawValue)
                    onResult.invoke(barcodeResult)
                } else {
                    Log.d("${SmartScannerActivity.TAG}/SmartScanner", "barcode: nothing detected")
                }
            }
            .addOnFailureListener { e ->
                Log.d("${SmartScannerActivity.TAG}/SmartScanner", "barcode: failure: ${e.message}")
            }
        imageProxy.close()
    }
}