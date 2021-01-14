package org.idpass.smartscanner.lib.barcode

import android.app.Application
import android.content.Context
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
import org.idpass.smartscanner.lib.platform.extension.cacheImagePath
import org.idpass.smartscanner.lib.platform.extension.cacheImageToLocal

class BarcodeViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context
        get() = getApplication<Application>().applicationContext

    private val barcodeResultData = MutableLiveData<BarcodeResult>()

    @MainThread
    fun result() : LiveData<BarcodeResult> = barcodeResultData

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
                    val imageCachePathFile = context.cacheImagePath()
                    bf.cacheImageToLocal(
                        imageCachePathFile,
                        imageProxy.imageInfo.rotationDegrees
                    )
                    val barcodeResult = BarcodeResult(imageCachePathFile, cornersString, rawValue)
                    barcodeResultData.postValue(barcodeResult)
                } else {
                    Log.d("${SmartScannerActivity.TAG}/SmartScanner", "barcode: nothing detected")
                }
            }
            .addOnFailureListener { e ->
                Log.d("${SmartScannerActivity.TAG}/SmartScanner", "barcode: failure: ${e.message}")
            }
    }
}