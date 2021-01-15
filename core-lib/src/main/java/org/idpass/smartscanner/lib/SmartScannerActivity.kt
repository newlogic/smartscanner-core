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
package org.idpass.smartscanner.lib

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.util.Size
import android.view.MotionEvent
import android.view.Surface
import android.view.View
import android.view.View.*
import android.view.Window
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Guideline
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import org.idpass.lite.Card
import org.idpass.lite.IDPassReader
import org.idpass.lite.exceptions.InvalidCardException
import org.idpass.lite.exceptions.InvalidKeyException
import org.idpass.smartscanner.api.ScannerConstants
import org.idpass.smartscanner.lib.barcode.BarcodeResult
import org.idpass.smartscanner.lib.barcode.BarcodeViewModel
import org.idpass.smartscanner.lib.idpasslite.IDPassLiteViewModel
import org.idpass.smartscanner.lib.mrz.MRZResult
import org.idpass.smartscanner.lib.mrz.MRZViewModel
import org.idpass.smartscanner.lib.platform.BaseActivity
import org.idpass.smartscanner.lib.platform.extension.*
import org.idpass.smartscanner.lib.platform.utils.CameraUtils.isLedFlashAvailable
import org.idpass.smartscanner.lib.platform.utils.DateUtils.formatDate
import org.idpass.smartscanner.lib.platform.utils.DateUtils.isValidDate
import org.idpass.smartscanner.lib.scanner.AnalyzerType
import org.idpass.smartscanner.lib.scanner.SmartScannerAnalyzer
import org.idpass.smartscanner.lib.scanner.SmartScannerException
import org.idpass.smartscanner.lib.scanner.config.*
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs

class SmartScannerActivity : BaseActivity(), OnClickListener {

    companion object {
        val TAG: String = SmartScannerActivity::class.java.simpleName
        const val SCANNER_OPTIONS = "scanner_options"
        const val SCANNER_RESULT = "scanner_result"
        const val SCANNER_RESULT_BYTES = "scanner_result_bytes"
    }

    private val REQUEST_CODE_PERMISSIONS = 10
    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    private val clickThreshold = 5

    private var x = 0f
    private var y = 0f
    private var config: Config? = null
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var startScanTime: Long = 0
    private var scannerOptions: ScannerOptions? = null
    private var mode: String? = null
    private var barcodeOptions: BarcodeOptions = BarcodeOptions.default
    private var barcodeFormats: List<Int> = listOf()
    private var barcodeStrings: List<String> = listOf()
    private var mrzFormat: String? = null
    private var isMLKitUsable = true

    private var flashButton: View? = null
    private var closeButton: View? = null
    private var rectangle: View? = null
    private var debugLayout: View? = null
    private var manualCapture: View? = null
    private var brandingImage: ImageView? = null
    private var captureLabelText: TextView? = null
    private var modelText: TextView? = null
    private var mlkitText: TextView? = null
    private var mlkitMS: TextView? = null
    private var mlkitTime: TextView? = null
    private var loading: ProgressBar? = null
    private var barcodeVM: BarcodeViewModel? = null
    private var idPassLiteVM: IDPassLiteViewModel? = null
    private var mrzVM: MRZViewModel? = null

    private lateinit var modelLayoutView: View
    private lateinit var coordinatorLayoutView: View
    private lateinit var viewFinder: PreviewView

    private lateinit var cameraExecutor: ExecutorService

    override fun layoutId(): Int  = R.layout.activity_smart_scanner

    override fun onCreate(savedInstanceState: Bundle?) {
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)
        // assign view ids
        coordinatorLayoutView = findViewById(R.id.coordinatorLayout)
        modelLayoutView = findViewById(R.id.viewLayout)
        viewFinder = findViewById(R.id.viewFinder)
        flashButton = findViewById(R.id.flash_button)
        closeButton = findViewById(R.id.close_button)
        rectangle = findViewById(R.id.rectimage)
        debugLayout = findViewById(R.id.debugLayout)
        modelText = findViewById(R.id.modelText)
        brandingImage = findViewById(R.id.brandingImage)
        manualCapture = findViewById(R.id.manualCapture)
        captureLabelText = findViewById(R.id.captureLabelText)
        mlkitText = findViewById(R.id.mlkitText)
        mlkitMS = findViewById(R.id.mlkitMS)
        mlkitTime = findViewById(R.id.mlkitTime)
        loading = findViewById(R.id.loading)
        // Scanner setup from intent
        hideActionBar()
        if (intent.action != null) {
            scannerOptions = when (intent.action) {
                // barcode
                ScannerConstants.IDPASS_SMARTSCANNER_BARCODE_INTENT,
                ScannerConstants.IDPASS_SMARTSCANNER_ODK_BARCODE_INTENT -> ScannerType.barcodeOptions
                // idpass lite
                ScannerConstants.IDPASS_SMARTSCANNER_IDPASS_LITE_INTENT,
                ScannerConstants.IDPASS_SMARTSCANNER_ODK_IDPASS_LITE_INTENT -> ScannerType.idPassLiteOptions
                // mrz
                ScannerConstants.IDPASS_SMARTSCANNER_MRZ_INTENT,
                ScannerConstants.IDPASS_SMARTSCANNER_ODK_MRZ_INTENT -> ScannerType.mrzOptions
                else -> throw SmartScannerException("Error: Wrong intent action. Please see ScannerConstants.kt for proper intent action strings.")
            }
        } else {
            // Use scanner options directly if no scanner type is called
            val options : ScannerOptions? = intent.getParcelableExtra(SCANNER_OPTIONS)
            options?.let {
                Log.d(TAG, "scannerOptions: $it")
                scannerOptions = options
            } ?: run {
                throw SmartScannerException("Please set proper scanner options to be able to use ID PASS Smart Scanner.")
            }
        }
        // setup modes & config for reader
        mode = scannerOptions?.mode
        config = scannerOptions?.config ?: Config.default
        isMLKitUsable = isPlayServicesAvailable()
        // initialize view models
        barcodeVM = ViewModelProvider(this).get(BarcodeViewModel::class.java)
        idPassLiteVM = ViewModelProvider(this).get(IDPassLiteViewModel::class.java)
        mrzVM = ViewModelProvider(this).get(MRZViewModel::class.java)
        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
            setupConfiguration()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
        // assign click listeners
        closeButton?.setOnClickListener(this)
        flashButton?.setOnClickListener(this)
        manualCapture?.setOnClickListener(this)
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun isPlayServicesAvailable(): Boolean {
        val availability = GoogleApiAvailability.getInstance()
        val resultCode = availability.isGooglePlayServicesAvailable(this)
        return resultCode == ConnectionResult.SUCCESS
    }

    private fun setupConfiguration() {
        if (mode == Modes.MRZ.value) {
            // initialize Tesseract if MLKit is unavailable
            if (!isMLKitUsable) mrzVM?.initializeTesseract(this)
            // initialize mrz format to use
            mrzFormat = scannerOptions?.mrzFormat ?: intent.getStringExtra(ScannerConstants.MRZ_FORMAT_EXTRA)
        }
        if (mode == Modes.BARCODE.value) {
            // barcode view layout
            barcodeOptions = scannerOptions?.barcodeOptions ?: BarcodeOptions.default
            barcodeStrings = barcodeOptions.barcodeFormats ?: BarcodeFormat.default
            barcodeFormats = barcodeStrings.map { BarcodeFormat.valueOf(it).value }
            val layoutParams = modelLayoutView.layoutParams as ConstraintLayout.LayoutParams
            val topGuideline = findViewById<Guideline>(R.id.top)
            when (barcodeOptions.barcodeScannerSize) {
                ScannerSize.CUSTOM_QR.value -> {
                    layoutParams.dimensionRatio = "4:6"
                    layoutParams.marginStart = 96 // Approx. 48dp
                    layoutParams.marginEnd = 96 // Approx. 48dp
                    modelLayoutView.layoutParams = layoutParams
                }
                ScannerSize.LARGE.value -> {
                    topGuideline.setGuidelinePercent(0.1F)
                    layoutParams.dimensionRatio = "3:4"
                    modelLayoutView.layoutParams = layoutParams
                }
                ScannerSize.SMALL.value -> {
                    layoutParams.dimensionRatio = "4:4"
                    modelLayoutView.layoutParams = layoutParams
                }
                else -> {
                    layoutParams.dimensionRatio = "3:4"
                    modelLayoutView.layoutParams = layoutParams
                }
            }
        }
        // flash
        flashButton?.visibility = if (isLedFlashAvailable(this)) VISIBLE else GONE
        // capture text label
        captureLabelText?.text = config?.label ?: String.empty()
        // font to use
        captureLabelText?.typeface = when (config?.font) {
            Fonts.NOTO_SANS_ARABIC.value -> ResourcesCompat.getFont(this, R.font.notosansarabic_bold)
            Fonts.ROBOTO.value -> ResourcesCompat.getFont(this, R.font.roboto_regular)
            else -> ResourcesCompat.getFont(this, R.font.sourcesanspro_medium)
        }
        // Background reader
        try {
            config?.background?.let {
                if (it.isNotEmpty()) {
                    val color = Color.parseColor(config?.background)
                    coordinatorLayoutView.setBackgroundColor(color)
                }
            } ?: run {
                coordinatorLayoutView.setBackgroundColor(ContextCompat.getColor(this, R.color.transparent_grey))
            }
        } catch (iae: IllegalArgumentException) {
            // This color string is not valid
            throw SmartScannerException("Please set proper color string in setting background. Example: '#ffc234' " )
        }
        // branding
        brandingImage?.visibility = config?.branding?.let { if (it) VISIBLE else GONE } ?: run { VISIBLE }
        // manual capture
        manualCapture?.visibility = config?.isManualCapture?.let {
            if (it) VISIBLE else GONE
        } ?: run {
            if (intent.getBooleanExtra(ScannerConstants.MRZ_MANUAL_CAPTURE_EXTRA, false)) VISIBLE else GONE
        }
    }

    private fun startCamera() {
        this.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            // Preview
            preview = Preview.Builder().build()
            val size = Size(480, 640)
            imageAnalyzer = ImageAnalysis.Builder()
                .setTargetResolution(size)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, getAnalyzer())
                }
            // Create configuration object for the image capture use case
            imageCapture = ImageCapture.Builder()
                .setTargetResolution(size)
                .setTargetRotation(Surface.ROTATION_0)
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build()
            // Select back camera
            val cameraSelector = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()
                // Bind use cases to camera
                camera = cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageAnalyzer,
                    imageCapture
                )
                preview?.setSurfaceProvider(viewFinder.surfaceProvider)
                Log.d(
                    TAG,
                    "Measured size: ${viewFinder.width}x${viewFinder.height}"
                )
                startScanTime = System.currentTimeMillis()
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
                setupConfiguration()
            } else {
                val snackBar: Snackbar = Snackbar.make(coordinatorLayoutView, R.string.required_perms_not_given, Snackbar.LENGTH_INDEFINITE)
                snackBar.setAction(R.string.settings) {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                }
                snackBar.show()
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun getAnalyzer(): SmartScannerAnalyzer {
        return SmartScannerAnalyzer (
            mode = mode,
            barcodeAnalysis = { bf, imageProxy ->
                runOnUiThread {
                    if (barcodeOptions.idPassLiteSupport == true) {
                        idPassLiteVM?.analyze(
                            barcodeFormats = barcodeFormats,
                            bitmap = bf,
                            imageProxy = imageProxy,
                            onResult = { raw ->
                                if (intent.action == ScannerConstants.IDPASS_SMARTSCANNER_IDPASS_LITE_INTENT ||
                                    intent.action == ScannerConstants.IDPASS_SMARTSCANNER_ODK_IDPASS_LITE_INTENT) {
                                        sendBundleResult(AnalyzerType.IDPASS_LITE, idPassLiteRaw = raw)
                                } else {
                                    sendAnalyzerResult(AnalyzerType.IDPASS_LITE, null, raw)
                                }
                            }
                        )
                    } else {
                        barcodeVM?.analyze(
                            barcodeFormats = barcodeFormats,
                            bitmap = bf,
                            filePath = cacheImagePath(),
                            imageProxy = imageProxy,
                            onResult = { result ->
                                if (intent.action == ScannerConstants.IDPASS_SMARTSCANNER_BARCODE_INTENT ||
                                    intent.action == ScannerConstants.IDPASS_SMARTSCANNER_ODK_BARCODE_INTENT) { sendBundleResult(AnalyzerType.BARCODE, barcodeResult = result)
                                }
                                else {
                                    val jsonString = Gson().toJson(result)
                                    sendAnalyzerResult(AnalyzerType.BARCODE, jsonString)
                                }
                            }
                        )
                    }
                }
            },
            mrzAnalysis = { bf, imageProxy ->
                runOnUiThread{
                    if (isMLKitUsable) {
                        mrzVM?.analyzeByMLKit(
                            context = this,
                            bitmap = bf,
                            imageProxy = imageProxy,
                            config = config,
                            mrzFormat = mrzFormat,
                            onStartTime = { start ->
                                getAnalyzerStat(start, System.currentTimeMillis())
                            },
                            onResult = { result ->
                                sendMRZResult(result)
                            },
                            onConnectFail = { connectionText ->
                                modelText?.visibility =  if (connectionText.isNotEmpty()) VISIBLE else INVISIBLE
                                modelText?.text = connectionText
                            }
                        )
                    } else {
                        mrzVM?.analyzeByTesseract(
                            context = this,
                            bitmap = bf,
                            imageProxy = imageProxy,
                            config = config,
                            mrzFormat = mrzFormat,
                            onStartTime = { start ->
                                getAnalyzerStat(start, System.currentTimeMillis())
                            },
                            onResult = { result ->
                                sendMRZResult(result)
                            }
                        )
                    }
                }
            }
        )
    }

    private fun sendMRZResult (result : MRZResult) {
        if (intent.action == ScannerConstants.IDPASS_SMARTSCANNER_MRZ_INTENT ||
            intent.action == ScannerConstants.IDPASS_SMARTSCANNER_ODK_MRZ_INTENT) {
            sendBundleResult(AnalyzerType.MLKIT, mrzResult = result)
        }
        else {
            val jsonString = Gson().toJson(result)
            sendAnalyzerResult(AnalyzerType.MLKIT, jsonString)
        }
    }

    private fun getAnalyzerStat(startTime: Long, endTime: Long) {
        val analyzerTime = endTime - startTime
        "Frame processing time: $analyzerTime ms".also { mlkitText?.text = it }
        val scanTime = ((System.currentTimeMillis().toDouble() - startScanTime.toDouble()) / 1000)
        "Total scan time: $scanTime s".also { mlkitText?.text = it }
    }

    private fun sendAnalyzerResult(analyzerType: AnalyzerType, result: String? = null, rawBytes: ByteArray? = null) {
        val data = Intent()
        when (analyzerType) {
            AnalyzerType.MLKIT -> {
                Log.d(TAG, "Success from MLKit")
                Log.d(TAG, "value: $result")
                mlkitText?.text = result
            }
            AnalyzerType.TESSERACT -> {
                Log.d(TAG, "Success from TESSERACT")
                Log.d(TAG, "value: $result")
                mlkitText?.text = result
            }
            AnalyzerType.BARCODE -> {
                Log.d(TAG, "Success from BARCODE")
                Log.d(TAG, "value: $result")
            }
            AnalyzerType.IDPASS_LITE -> {
                Log.d(TAG, "Success from IDPASS_LITE")
                Log.d(TAG, "rawBytes ${rawBytes.contentToString()}")
            }
        }
        data.putExtra(SCANNER_RESULT_BYTES, rawBytes)
        data.putExtra(SCANNER_RESULT, result)
        setResult(Activity.RESULT_OK, data)
        finish()
    }

    private fun sendBundleResult(analyzerType: AnalyzerType, barcodeResult: BarcodeResult? = null, mrzResult : MRZResult? = null, idPassLiteRaw: ByteArray? = null) {
        val bundle = Bundle()
        bundle.putString(ScannerConstants.MODE, mode)
        val prefix = if (intent.hasExtra(ScannerConstants.IDPASS_ODK_PREFIX_EXTRA)) {
            intent.getStringExtra(ScannerConstants.IDPASS_ODK_PREFIX_EXTRA)
        } else { "" }
        when (analyzerType) {
            AnalyzerType.MLKIT, AnalyzerType.TESSERACT -> {
                Log.d(TAG, "Success from MRZ")
                mrzResult?.let { result ->
                    if (intent.action == ScannerConstants.IDPASS_SMARTSCANNER_ODK_MRZ_INTENT) {
                        bundle.putString(ScannerConstants.IDPASS_ODK_INTENT_DATA, result.documentNumber)
                    }
                    // TODO implement proper image passing
                    //  bundle.putString(ScannerConstants.MRZ_IMAGE, result.image)
                    bundle.putString(ScannerConstants.MRZ_CODE, result.code)
                    bundle.putShort(ScannerConstants.MRZ_CODE_1, result.code1 ?: -1)
                    bundle.putShort(ScannerConstants.MRZ_CODE_2, result.code2 ?: -1)
                    bundle.putString(ScannerConstants.MRZ_DATE_OF_BIRTH, result.dateOfBirth)
                    bundle.putString(ScannerConstants.MRZ_DOCUMENT_NUMBER, result.documentNumber)
                    bundle.putString(ScannerConstants.MRZ_EXPIRY_DATE, result.expirationDate)
                    bundle.putString(ScannerConstants.MRZ_FORMAT, result.format)
                    bundle.putString(ScannerConstants.MRZ_GIVEN_NAMES, result.givenNames)
                    bundle.putString(ScannerConstants.MRZ_SURNAME, result.surname)
                    bundle.putString(ScannerConstants.MRZ_ISSUING_COUNTRY, result.issuingCountry)
                    bundle.putString(ScannerConstants.MRZ_NATIONALITY, result.nationality)
                    bundle.putString(ScannerConstants.MRZ_SEX, result.sex)
                    bundle.putString(ScannerConstants.MRZ_RAW, result.mrz)
                }
            }
            AnalyzerType.BARCODE -> {
                Log.d(TAG, "Success from BARCODE")
                if (intent.action == ScannerConstants.IDPASS_SMARTSCANNER_ODK_BARCODE_INTENT) {
                    bundle.putString(ScannerConstants.IDPASS_ODK_INTENT_DATA, barcodeResult?.value)
                }
                bundle.putString(ScannerConstants.BARCODE_IMAGE, barcodeResult?.imagePath)
                bundle.putString(ScannerConstants.BARCODE_CORNERS, barcodeResult?.corners)
                bundle.putString(ScannerConstants.BARCODE_VALUE, barcodeResult?.value)
            }
            AnalyzerType.IDPASS_LITE -> {
                Log.d(TAG, "Success from IDPASS_LITE")
                val idPassReader = IDPassReader()
                var card: Card?
                try {
                    try {
                        card = idPassReader.open(idPassLiteRaw)
                    } catch (ice: InvalidCardException) {
                        card = idPassReader.open(idPassLiteRaw, true)
                        ice.printStackTrace()
                    }
                    if (card != null) {
                        val fullName = card.getfullName()
                        val givenName = card.givenName
                        val surname = card.surname
                        val dateOfBirth = card.dateOfBirth
                        val placeOfBirth = card.placeOfBirth
                        val uin = card.uin
                        val address = card.postalAddress

                        if (intent.action == ScannerConstants.IDPASS_SMARTSCANNER_ODK_IDPASS_LITE_INTENT) {
                            if (uin != null) {
                                bundle.putString(ScannerConstants.IDPASS_ODK_INTENT_DATA, uin)
                            }
                        }
                        if (fullName != null) {
                            bundle.putString(ScannerConstants.IDPASS_LITE_FULL_NAME, fullName)
                        }

                        if (givenName != null) {
                            bundle.putString(ScannerConstants.IDPASS_LITE_GIVEN_NAMES, givenName)
                        }
                        if (surname != null) {
                            bundle.putString(ScannerConstants.IDPASS_LITE_SURNAME, surname)
                        }
//                        if (card.gender != 0) {
//                            bundle.putString(ScannerConstants.IDPASS_LITE_GENDER, card.gender.toString())
//                        }
                        if (dateOfBirth != null) {
                            val birthday = if (isValidDate(formatDate(dateOfBirth))) formatDate(dateOfBirth) else ""
                            bundle.putString(ScannerConstants.IDPASS_LITE_DATE_OF_BIRTH, birthday)
                        }
                        if (placeOfBirth.isNotEmpty()) {
                            bundle.putString(ScannerConstants.IDPASS_LITE_PLACE_OF_BIRTH, placeOfBirth)
                        }
                        if (uin != null) {
                            bundle.putString(ScannerConstants.IDPASS_LITE_UIN, uin)
                        }

                        if (address != null) {
                            val addressLines = address.addressLinesList.joinToString("\n")
                            bundle.putString(ScannerConstants.IDPASS_LITE_ADDRESS_POSTAL_CODE, address.postalCode)
                            bundle.putString(ScannerConstants.IDPASS_LITE_ADDRESS_ADMINISTRATIVE_AREA, address.administrativeArea)
                            bundle.putString(ScannerConstants.IDPASS_LITE_ADDRESS_ADDRESS_LINES, address.languageCode)
                            bundle.putString(ScannerConstants.IDPASS_LITE_ADDRESS_LANGUAGE_CODE, addressLines)
                            bundle.putString(ScannerConstants.IDPASS_LITE_ADDRESS_SORTING_CODE, address.sortingCode)
                            bundle.putString(ScannerConstants.IDPASS_LITE_ADDRESS_LOCALITY, address.locality)
                            bundle.putString(ScannerConstants.IDPASS_LITE_ADDRESS_SUBLOCALITY, address.sublocality)
                            bundle.putString(ScannerConstants.IDPASS_LITE_ADDRESS_ORGANIZATION, address.organization)
                        }

                        bundle.putByteArray(ScannerConstants.IDPASS_LITE_RAW, idPassLiteRaw)
                    }
                } catch (ike: InvalidKeyException) {
                    ike.printStackTrace()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        val result = Intent()
        result.putExtra(ScannerConstants.RESULT, bundle)
        // Copy all the values in the intent result to be compatible with other implementations than commcare
        for (key in bundle.keySet()) {
            result.putExtra(prefix + key, bundle.getString(key))
        }
        setResult(Activity.RESULT_OK, result)
        finish()
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.close_button -> onBackPressed()
            R.id.flash_button -> {
                flashButton?.let {
                    if (it.isSelected) {
                        it.isSelected = false
                        camera?.cameraControl?.enableTorch(false)
                    } else {
                        it.isSelected = true
                        camera?.cameraControl?.enableTorch(true)
                    }
                }
            }
            R.id.manualCapture -> {
                val imageFile = File(cacheImagePath())
                val outputFileOptions = ImageCapture.OutputFileOptions.Builder(imageFile).build()
                imageCapture?.takePicture(outputFileOptions, ContextCompat.getMainExecutor(baseContext),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                            val data = Intent()
                            val bf = imageFile.path.toBitmap()
                            bf.cacheImageToLocal(imageFile.path, 90)
                            val imageString = if (config?.imageResultType == ImageResultType.BASE_64.value) bf.encodeBase64(90) else imageFile.path
                            val result = Gson().toJson(MRZResult.getImageOnly(imageString))
                            data.putExtra(SCANNER_RESULT, result)
                            setResult(Activity.RESULT_OK, data)
                            finish()
                        }
                        override fun onError(exception: ImageCaptureException) {
                            exception.printStackTrace()
                        }
                    }
                )
            }
        }
    }

    private fun isAClick(
        startX: Float,
        endX: Float,
        startY: Float,
        endY: Float
    ): Boolean {
        val differenceX = abs(startX - endX)
        val differenceY = abs(startY - endY)
        return !(differenceX > clickThreshold || differenceY > clickThreshold)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val minDistance = 600
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                y = event.y
                x = event.x
            }
            MotionEvent.ACTION_UP -> {
                if (isAClick(x, event.x, y, event.y)) {
                    val factory: MeteringPointFactory = SurfaceOrientedMeteringPointFactory(
                        viewFinder.width.toFloat(), viewFinder.height.toFloat()
                    )
                    val autoFocusPoint = factory.createPoint(event.x, event.y)
                    try {
                        camera!!.cameraControl.startFocusAndMetering(
                            FocusMeteringAction.Builder(
                                autoFocusPoint,
                                FocusMeteringAction.FLAG_AF
                            ).apply {
                                //focus only when the user tap the preview
                                disableAutoCancel()
                            }.build()
                        )
                    } catch (e: CameraInfoUnavailableException) {
                        Log.d("ERROR", "cannot access camera", e)
                    }
                } else {
                    val deltaY = event.y - y
                    if (deltaY < -minDistance) {
                        debugLayout?.visibility = VISIBLE
                    } else if (deltaY > minDistance) {
                        debugLayout?.visibility = INVISIBLE
                    }
                }
            }
        }
        return super.onTouchEvent(event)
    }
}