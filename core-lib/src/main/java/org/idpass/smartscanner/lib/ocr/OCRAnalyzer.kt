package org.idpass.smartscanner.lib.ocr

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.camera.core.ImageProxy
import com.google.gson.Gson
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import org.idpass.smartscanner.api.ScannerConstants
import org.idpass.smartscanner.lib.R
import org.idpass.smartscanner.lib.SmartScannerActivity
import org.idpass.smartscanner.lib.scanner.BaseImageAnalyzer
import org.idpass.smartscanner.lib.scanner.config.ImageResultType
import org.idpass.smartscanner.lib.scanner.config.Modes
import org.idpass.smartscanner.lib.utils.BitmapUtils
import org.idpass.smartscanner.lib.utils.extension.cacheImagePath
import org.idpass.smartscanner.lib.utils.extension.cacheImageToLocal
import org.idpass.smartscanner.lib.utils.extension.cropCenter
import org.idpass.smartscanner.lib.utils.extension.encodeBase64
import org.idpass.smartscanner.lib.utils.extension.isImageBlur
import org.idpass.smartscanner.lib.utils.extension.setBrightness
import org.idpass.smartscanner.lib.utils.extension.setContrast
import java.io.File

open class OCRAnalyzer(
    override val activity: Activity,
    override val intent: Intent,
    override val mode: String = Modes.OCR.value,
    private val imageResultType: String,
    private val isShowGuide: Boolean = false,
    private val regex: String? = "",
    private val type: String? = "",
    private val manualCapture: Boolean = false,
    private val analyzeStart: Long = 0
) : BaseImageAnalyzer() {

    private var captured = false
    private var startAnalyze = false

    @SuppressLint("UnsafeExperimentalUsageError", "UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        captured = !manualCapture
        Handler(Looper.getMainLooper()).postDelayed({
            startAnalyze = true
        }, analyzeStart)
        val bitmap = BitmapUtils.getBitmap(imageProxy)
        bitmap?.let { bf ->
            val rotation = imageProxy.imageInfo.rotationDegrees
            bf.apply {
                // Increase brightness and contrast for clearer image to be processed
                setContrast(1.1F)
                setBrightness(3F)
            }

            val rectGuide = activity.findViewById<ImageView>(R.id.scanner_overlay)
            val xGuide = activity.findViewById<View>(R.id.x_guide)
            val yGuide = activity.findViewById<View>(R.id.y_guide)
            val viewFinder = activity.findViewById<View>(R.id.view_finder)
            val capture = activity.findViewById<View>(R.id.manual_capture)
            var inputBitmap = bf
            var inputRot = rotation
            var rotatedBF = BitmapUtils.rotateImage(bf, rotation)

            capture.setOnClickListener {
                captured = true
            }

            if (isShowGuide) {
                // try to cropped forcefully

                // Crop preview area
                val cropHeight = if (rotatedBF.width < viewFinder.width) {
                    // if preview area larger than analysing image
                    val koeff = rotatedBF.width.toFloat() / viewFinder!!.width.toFloat()
                    viewFinder.height.toFloat() * koeff
                } else {
                    // if preview area smaller than analysing image
                    val prc =
                        100 - (viewFinder.width.toFloat() / (rotatedBF.width.toFloat() / 100f))
                    viewFinder.height + ((viewFinder.height.toFloat() / 100f) * prc)
                }
                val cropTop = (rotatedBF.height / 2) - (cropHeight / 2)
                rotatedBF = Bitmap.createBitmap(
                    rotatedBF,
                    0,
                    cropTop.toInt(),
                    rotatedBF.width,
                    cropHeight.toInt()
                )

                // Crop OCR area
                val ratio = rotatedBF.width.toFloat() / viewFinder.width.toFloat()
                val width = rectGuide.width * ratio
                val height = rectGuide.height * ratio
                var x = (xGuide.width) * ratio
                var y = (yGuide.height) * ratio

                if (x + width > rotatedBF.width) {
                    val diff = x + width - rotatedBF.width
                    x -= diff
                }

                if (y + height > rotatedBF.height) {
                    val diff = y + height - rotatedBF.height
                    y -= diff
                }

                inputBitmap = Bitmap.createBitmap(
                    rotatedBF,
                    x.toInt(),
                    y.toInt(),
                    width.toInt(),
                    height.toInt()
                )
                inputRot = 0
            }

            // Pass image to an ML Kit Vision API
            Log.d("${SmartScannerActivity.TAG}/SmartScanner", "OCR MLKit: start")
            val start = System.currentTimeMillis()
            val image = InputImage.fromBitmap(inputBitmap, inputRot)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            Log.d("${SmartScannerActivity.TAG}/SmartScanner", "OCR MLKit TextRecognition: process")

            recognizer.process(image)

                .addOnSuccessListener { visionText ->

                    val timeRequired = System.currentTimeMillis() - start
                    Log.d(
                        "${SmartScannerActivity.TAG}/SmartScanner",
                        "OCR MLKit TextRecognition: success: $timeRequired ms"
                    )
                    var value = ""
                    var array = ArrayList<String>()
                    val blocks = visionText.textBlocks

                    for (i in blocks.indices) {
                        val lines = blocks[i].lines
                        for (j in lines.indices) {
                            //check if text matches the given regex
                            if (OCRChecker.check(lines[j].text, regex)) {
                                value += if (value.isNotEmpty()) " " + lines[j].text else lines[j].text
                                array.add(lines[j].text)
                            }
                        }
                    }
                    if (manualCapture) {
                        if (captured) processResult(
                            result = value,
                            array = array,
                            bitmap = bf,
                            rotation = rotation
                        )
                    } else if (value.isNotEmpty() && !inputBitmap.isImageBlur(50.0) && startAnalyze) {
                        processResult(
                            result = value,
                            array = array,
                            bitmap = bf,
                            rotation = rotation
                        )
                    } else {
                        Log.d(
                            "${SmartScannerActivity.TAG}/SmartScanner",
                            "OCR: nothing detected"
                        )
                    }

                    imageProxy.close()
                }
                .addOnFailureListener { e ->
                    e.printStackTrace()
                    imageProxy.close()
                }


        }
    }

    internal open fun processResult(result: String, array: ArrayList<String>, bitmap: Bitmap, rotation: Int) {
        val imagePath = activity.cacheImagePath()
        bitmap.cropCenter().cacheImageToLocal(
            imagePath,
            rotation,
            if (imageResultType == ImageResultType.BASE_64.value) 40 else 80
        )
        val imageFile = File(imagePath)
        val imageString =
            if (imageResultType == ImageResultType.BASE_64.value) imageFile.encodeBase64() else imagePath


        val result = OCRResult(
            imagePath = imagePath,
            image = imageString,
            regex = regex ?: OCRChecker.DEFAULT_REGEX_STRING,
            valuesArray = array,
            value = result,
            type = type
        )

        when (intent.action) {
            ScannerConstants.IDPASS_SMARTSCANNER_OCR_INTENT,
            ScannerConstants.IDPASS_SMARTSCANNER_ODK_OCR_INTENT -> {
                sendBundleResult(ocrResult = result)
            }

            else -> {
                val jsonString = Gson().toJson(result)
                sendAnalyzerResult(result = jsonString)
            }
        }
    }

    private fun sendAnalyzerResult(result: String) {
        val data = Intent()
        Log.d(SmartScannerActivity.TAG, "Success from OCR")
        Log.d(SmartScannerActivity.TAG, "value: $result")
        data.putExtra(SmartScannerActivity.SCANNER_IMAGE_TYPE, imageResultType)
        data.putExtra(SmartScannerActivity.SCANNER_RESULT, result)
        data.putExtra(ScannerConstants.MODE, mode)
        activity.setResult(Activity.RESULT_OK, data)
        activity.finish()
    }

    private fun sendBundleResult(ocrResult: OCRResult? = null) {
        val bundle = Bundle()
        Log.d(SmartScannerActivity.TAG, "Success from BARCODE")
        if (intent.action == ScannerConstants.IDPASS_SMARTSCANNER_ODK_OCR_INTENT) {
            bundle.putString(
                ScannerConstants.IDPASS_ODK_INTENT_DATA,
                ocrResult?.value?.toString()
            )
        }
        bundle.putString(ScannerConstants.MODE, mode)
        bundle.putString(ScannerConstants.OCR_IMAGE, ocrResult?.imagePath)
        bundle.putString(ScannerConstants.OCR_TYPE, ocrResult?.type)
        bundle.putString(ScannerConstants.OCR_VALUE, ocrResult?.value?.toString())
        val result = Intent()
        val prefix = if (intent.hasExtra(ScannerConstants.IDPASS_ODK_PREFIX_EXTRA)) {
            intent.getStringExtra(ScannerConstants.IDPASS_ODK_PREFIX_EXTRA)
        } else {
            ""
        }
        result.putExtra(ScannerConstants.RESULT, bundle)
        // Copy all the values in the intent result to be compatible with other implementations than commcare
        for (key in bundle.keySet()) {
            result.putExtra(prefix + key, bundle.getString(key))
        }
        activity.setResult(Activity.RESULT_OK, result)
        activity.finish()
    }
}