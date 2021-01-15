package org.idpass.smartscanner.lib.idpasslite

import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import org.idpass.smartscanner.lib.SmartScannerActivity

class IDPassLiteViewModel : ViewModel() {

    fun analyze(barcodeFormats : List<Int>, bitmap : Bitmap, imageProxy : ImageProxy, onResult: (ByteArray) -> Unit) {
        val start = System.currentTimeMillis()
        var barcodeFormat = Barcode.FORMAT_CODE_39 // Most common barcode format
        barcodeFormats.forEach {
            barcodeFormat = it or barcodeFormat // bitwise different barcode format options
        }
        val options = BarcodeScannerOptions.Builder().setBarcodeFormats(barcodeFormat).build()
        val image = InputImage.fromBitmap(bitmap, imageProxy.imageInfo.rotationDegrees)
        val scanner = BarcodeScanning.getClient(options)
        Log.d("${SmartScannerActivity.TAG}/SmartScanner", "ID PASS Lite: process")
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                val timeRequired = System.currentTimeMillis() - start
                Log.d("${SmartScannerActivity.TAG}/SmartScanner", "ID PASS Lite: success: $timeRequired ms")
                if (barcodes.isNotEmpty()) {
                    onResult.invoke(barcodes[0].rawBytes!!)
                } else {
                    Log.d("${SmartScannerActivity.TAG}/SmartScanner", "ID PASS Lite: nothing detected")
                }
            }
            .addOnFailureListener { e ->
                Log.d("${SmartScannerActivity.TAG}/SmartScanner", "ID PASS Lite: failure: ${e.message}")
            }
        imageProxy.close()
    }
}