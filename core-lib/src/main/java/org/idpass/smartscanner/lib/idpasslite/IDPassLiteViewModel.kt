package org.idpass.smartscanner.lib.idpasslite

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.annotation.MainThread
import androidx.camera.core.ImageProxy
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import org.idpass.smartscanner.lib.SmartScannerActivity

class IDPassLiteViewModel(application: Application) : AndroidViewModel(application) {

    private val idPassLiteData = MutableLiveData<ByteArray>()

    @MainThread
    fun result() : LiveData<ByteArray> = idPassLiteData

    @MainThread
    fun analyze(barcodeFormats : List<Int>, bf : Bitmap, imageProxy : ImageProxy) {
        val start = System.currentTimeMillis()
        var barcodeFormat = Barcode.FORMAT_CODE_39 // Most common barcode format
        barcodeFormats.forEach {
            barcodeFormat = it or barcodeFormat // bitwise different barcode format options
        }
        val options = BarcodeScannerOptions.Builder().setBarcodeFormats(barcodeFormat).build()
        val image = InputImage.fromBitmap(bf, imageProxy.imageInfo.rotationDegrees)
        val scanner = BarcodeScanning.getClient(options)
        Log.d("${SmartScannerActivity.TAG}/SmartScanner", "ID PASS Lite: process")
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                val timeRequired = System.currentTimeMillis() - start
                Log.d("${SmartScannerActivity.TAG}/SmartScanner", "ID PASS Lite: success: $timeRequired ms")
                if (barcodes.isNotEmpty()) {
                    idPassLiteData.postValue(barcodes[0].rawBytes!!)
                } else {
                    Log.d("${SmartScannerActivity.TAG}/SmartScanner", "ID PASS Lite: nothing detected")
                }
            }
            .addOnFailureListener { e ->
                Log.d("${SmartScannerActivity.TAG}/SmartScanner", "ID PASS Lite: failure: ${e.message}")
            }
    }
}