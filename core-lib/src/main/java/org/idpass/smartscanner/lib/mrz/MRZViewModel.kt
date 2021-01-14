package org.idpass.smartscanner.lib.mrz

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.annotation.MainThread
import androidx.camera.core.ImageProxy
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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

class MRZViewModel(application: Application) : AndroidViewModel(application) {

    private lateinit var tessBaseAPI: TessBaseAPI
    private val context: Context
        get() = getApplication<Application>().applicationContext

    private val mrzResultData = MutableLiveData<MRZResult>()
    private val startTimeData = MutableLiveData<Long>()
    private val connectionTextData = MutableLiveData<String>()
    private val connectionVisibilityData = MutableLiveData<Boolean>()


    @MainThread
    fun result() : LiveData<MRZResult> = mrzResultData

    @MainThread
    fun startTime() : LiveData<Long>  = startTimeData

    @MainThread
    fun connectionText() : LiveData<String>  = connectionTextData

    @MainThread
    fun connectionVisibility() : LiveData<Boolean>  = connectionVisibilityData

    @MainThread
    fun initializeTesseract() {
        val extDirPath: String = context.getExternalFilesDir(null)!!.absolutePath
        Log.d(SmartScannerActivity.TAG, "path: $extDirPath")
        FileUtils.copyAssets(context, "tessdata", extDirPath)
        tessBaseAPI = TessBaseAPI()
        tessBaseAPI.init(extDirPath, "ocrb_int", TessBaseAPI.OEM_DEFAULT)
        tessBaseAPI.pageSegMode = TessBaseAPI.PageSegMode.PSM_SINGLE_BLOCK
    }

    @MainThread
    fun analyze(isMlKit: Boolean, original : Bitmap, cropped : Bitmap, imageProxy : ImageProxy, config : Config?, mrzFormat: String? ) {
        config?.let {
            if (isMlKit) {
                analyzeByMLKit(original = original, cropped = cropped, imageProxy = imageProxy, config = it, mrzFormat = mrzFormat)
            } else {
                analyzeByTesseract(original = original, cropped = cropped, imageProxy = imageProxy, config = it, mrzFormat = mrzFormat)
            }
        }
    }

    @MainThread
    private fun analyzeByMLKit(original : Bitmap, cropped : Bitmap, imageProxy : ImageProxy, config : Config, mrzFormat: String? ) {
        Log.d("${SmartScannerActivity.TAG}/SmartScanner", "MRZ MLKit: start")
        val image = InputImage.fromBitmap(
            cropped,
            imageProxy.imageInfo.rotationDegrees
        )
        // Pass image to an ML Kit Vision API
        val recognizer = TextRecognition.getClient()
        Log.d("${SmartScannerActivity.TAG}/SmartScanner", "MRZ MLKit TextRecognition: process")
        val start = System.currentTimeMillis()
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                connectionVisibilityData.postValue(false)
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
                    processMRZResult(mrz = mrz, bitmap = original, rotation = imageProxy.imageInfo.rotationDegrees ,config = config, format = mrzFormat)
                } catch (e: Exception) { // MrzParseException, IllegalArgumentException
                    Log.d("${SmartScannerActivity.TAG}/SmartScanner", e.toString())
                }
            }
            .addOnFailureListener { e ->
                Log.d("${SmartScannerActivity.TAG}/SmartScanner", "MRZ MLKit TextRecognition: failure: ${e.message}")
                val connectionId = if (context.getConnectionType() == 0) R.string.connection_text else R.string.model_text
                connectionTextData.postValue(context.getString(connectionId))
                connectionVisibilityData.postValue(true)
            }
        startTimeData.postValue(start)
    }

    @MainThread
    private fun analyzeByTesseract(original : Bitmap, cropped : Bitmap, imageProxy : ImageProxy, config : Config, mrzFormat: String? ) {
        if (::tessBaseAPI.isInitialized) {
            val matrix = Matrix()
            val start = System.currentTimeMillis()
            thread(start = true) {
                Log.d("${SmartScannerActivity.TAG}/SmartScanner", "MRZ Tesseract: start")
                matrix.postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
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
                    processMRZResult(mrz = mrz, bitmap = original, rotation = imageProxy.imageInfo.rotationDegrees ,config = config, format = mrzFormat)
                } catch (e: Exception) { // MrzParseException, IllegalArgumentException
                    Log.d("${SmartScannerActivity.TAG}/Tesseract", e.toString())
                }
                startTimeData.postValue(start)
            }
        }

    }

    @MainThread
    private fun processMRZResult(mrz : String, bitmap : Bitmap, rotation : Int, config : Config, format: String?) {
        val imagePathFile = context.cacheImagePath()
        bitmap.cacheImageToLocal(imagePathFile, rotation)
        val imageString = if (config.imageResultType == ImageResultType.BASE_64.value) bitmap.encodeBase64(rotation) else imagePathFile
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
        mrzResultData.postValue(mrzResult)
    }
}