package org.idpass.smartscanner.lib.mrz

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.googlecode.tesseract.android.TessBaseAPI
import org.idpass.smartscanner.lib.R
import org.idpass.smartscanner.lib.SmartScannerActivity
import org.idpass.smartscanner.lib.platform.extension.cacheImagePath
import org.idpass.smartscanner.lib.platform.extension.cacheImageToLocal
import org.idpass.smartscanner.lib.platform.extension.encodeBase64
import org.idpass.smartscanner.lib.platform.extension.getConnectionType
import org.idpass.smartscanner.lib.platform.utils.FileUtils
import org.idpass.smartscanner.lib.scanner.config.Config
import org.idpass.smartscanner.lib.scanner.config.ImageResultType
import org.idpass.smartscanner.lib.scanner.config.MrzFormat
import java.net.URLEncoder
import kotlin.concurrent.thread

class MRZViewModel : ViewModel() {

    private lateinit var tessBaseAPI: TessBaseAPI

    fun initializeTesseract(context : Context) {
        val extDirPath: String = context.getExternalFilesDir(null)!!.absolutePath
        Log.d(SmartScannerActivity.TAG, "path: $extDirPath")
        FileUtils.copyAssets(context, "tessdata", extDirPath)
        tessBaseAPI = TessBaseAPI()
        tessBaseAPI.init(extDirPath, "ocrb_int", TessBaseAPI.OEM_DEFAULT)
        tessBaseAPI.pageSegMode = TessBaseAPI.PageSegMode.PSM_SINGLE_BLOCK
    }

    fun analyzeByMLKit(context: Context,
                       bitmap : Bitmap,
                       imageProxy : ImageProxy,
                       config : Config?,
                       mrzFormat: String?,
                       onStartTime: (Long) -> Unit,
                       onResult: (MRZResult) -> Unit,
                       onConnectFail: (String) -> Unit
    ) {
        // Pass image to an ML Kit Vision API
        Log.d("${SmartScannerActivity.TAG}/SmartScanner", "MRZ MLKit: start")
        val start = System.currentTimeMillis()
        onStartTime.invoke(start)
        val rotation = imageProxy.imageInfo.rotationDegrees
        val cropped = getCroppedBitmap(bitmap, rotation)
        val image = InputImage.fromBitmap(cropped, rotation)
        val recognizer = TextRecognition.getClient()
        Log.d("${SmartScannerActivity.TAG}/SmartScanner", "MRZ MLKit TextRecognition: process")
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val timeRequired = System.currentTimeMillis() - start
                Log.d("${SmartScannerActivity.TAG}/SmartScanner", "MRZ MLKit TextRecognition: success: $timeRequired ms")
                var rawFullRead = ""
                val blocks = visionText.textBlocks
                for (i in blocks.indices) {
                    val lines = blocks[i].lines
                    for (j in lines.indices) {
                        if (lines[j].text.contains('<')) {
                            rawFullRead += lines[j].text + "\n"
                        }
                    }
                }
                try {
                    Log.d(
                        "${SmartScannerActivity.TAG}/SmartScanner",
                        "Before cleaner: [${
                            URLEncoder.encode(rawFullRead, "UTF-8")
                                .replace("%3C", "<").replace("%0A", "↩")
                        }]"
                    )
                    val mrz = MRZCleaner.clean(rawFullRead)
                    Log.d(
                        "${SmartScannerActivity.TAG}/SmartScanner",
                        "After cleaner = [${
                            URLEncoder.encode(mrz, "UTF-8")
                                .replace("%3C", "<").replace("%0A", "↩")
                        }]"
                    )
                    val result = processMRZResult(context = context, mrz = mrz, bitmap = bitmap, rotation = rotation, config = config, format = mrzFormat)
                    onResult.invoke(result)
                    imageProxy.close()
                } catch (e: Exception) { // MrzParseException, IllegalArgumentException
                    Log.d("${SmartScannerActivity.TAG}/SmartScanner", e.toString())
                    imageProxy.close()
                }
            }
            .addOnFailureListener { e ->
                Log.d("${SmartScannerActivity.TAG}/SmartScanner", "MRZ MLKit TextRecognition: failure: ${e.message}")
                val connectionId = if (context.getConnectionType() == 0) R.string.connection_text else R.string.model_text
                onConnectFail.invoke(context.getString(connectionId))
                imageProxy.close()
            }
    }

    fun analyzeByTesseract(context: Context,
                           bitmap : Bitmap,
                           imageProxy : ImageProxy,
                           config : Config?,
                           mrzFormat: String?,
                           onStartTime: (Long) -> Unit,
                           onResult: (MRZResult) -> Unit
    ) {
        if (::tessBaseAPI.isInitialized) {
            Log.d("${SmartScannerActivity.TAG}/SmartScanner", "MRZ Tesseract: start")
            val start = System.currentTimeMillis()
            onStartTime.invoke(start)
            thread(start = true) {
                Log.d("${SmartScannerActivity.TAG}/SmartScanner", "MRZ Tesseract: process")
                val rotation = imageProxy.imageInfo.rotationDegrees
                val cropped = getCroppedBitmap(bitmap, rotation)
                val matrix = Matrix()
                matrix.postRotate(rotation.toFloat())
                val rotatedBitmap = Bitmap.createBitmap(
                    cropped,
                    0,
                    0,
                    cropped.width,
                    cropped.height,
                    matrix,
                    true
                )
                tessBaseAPI.setImage(rotatedBitmap)
                val tessResult = tessBaseAPI.utF8Text
                try {
                    Log.d(
                        "${SmartScannerActivity.TAG}/Tesseract",
                        "Before cleaner: [${
                            URLEncoder.encode(tessResult, "UTF-8")
                                .replace("%3C", "<").replace("%0A", "↩")
                        }]"
                    )
                    val mrz = MRZCleaner.clean(tessResult)
                    Log.d(
                        "${SmartScannerActivity.TAG}/Tesseract",
                        "After cleaner = [${
                            URLEncoder.encode(mrz, "UTF-8")
                                .replace("%3C", "<")
                                .replace("%0A", "↩")
                        }]"
                    )
                    val result = processMRZResult(context = context, mrz = mrz, bitmap = bitmap, rotation = imageProxy.imageInfo.rotationDegrees , config = config, format = mrzFormat)
                    onResult.invoke(result)
                    imageProxy.close()
                } catch (e: Exception) { // MrzParseException, IllegalArgumentException
                    Log.d("${SmartScannerActivity.TAG}/Tesseract", e.toString())
                    imageProxy.close()
                }
            }
        }

    }

    private fun getCroppedBitmap(bf : Bitmap, rot: Int) : Bitmap {
        val cropped = if (rot == 90 || rot == 270) Bitmap.createBitmap(
            bf,
            bf.width / 2,
            0,
            bf.width / 2,
            bf.height
        )
        else Bitmap.createBitmap(bf, 0, bf.height / 2, bf.width, bf.height / 2)
        Log.d(SmartScannerActivity.TAG, "Cropped: (${cropped.width}, ${cropped.height}), Rotation: $rot")
        return cropped
    }

    private fun processMRZResult(context: Context, mrz : String, bitmap : Bitmap, rotation : Int, config : Config?, format: String?) : MRZResult{
        val imagePathFile = context.cacheImagePath()
        bitmap.cacheImageToLocal(imagePathFile, rotation)
        val imageString = if (config?.imageResultType == ImageResultType.BASE_64.value) bitmap.encodeBase64(rotation) else imagePathFile
        val mrzResult: MRZResult = when (format) {
            MrzFormat.MRTD_TD1.value -> MRZResult.formatMrtdTd1Result(
                MRZCleaner.parseAndCleanMrtdTd1(
                    mrz
                ), imageString
            )
            else -> MRZResult.formatMrzResult(
                MRZCleaner.parseAndClean(mrz),
                imageString
            )
        }
        return mrzResult
    }
}