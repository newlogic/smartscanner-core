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
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.Size
import android.util.TypedValue
import android.view.*
import android.view.View.*
import android.widget.*
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.constraintlayout.widget.Guideline
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions.bitmapTransform
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.zxing.BarcodeFormat.*
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.journeyapps.barcodescanner.ViewfinderView
import io.sentry.Sentry
import io.sentry.SentryOptions
import org.idpass.lite.android.IDPassLite
import org.idpass.smartscanner.api.ScannerConstants
import org.idpass.smartscanner.lib.barcode.BarcodeAnalyzer
import org.idpass.smartscanner.lib.barcode.BarcodeResult
import org.idpass.smartscanner.lib.barcode.pdf417.PDF417DecoderFactory
import org.idpass.smartscanner.lib.barcode.qr.QRCodeAnalyzer
import org.idpass.smartscanner.lib.idpasslite.IDPassLiteAnalyzer
import org.idpass.smartscanner.lib.idpasslite.IDPassManager
import org.idpass.smartscanner.lib.mrz.MRZAnalyzer
import org.idpass.smartscanner.lib.mrz.MrzUtils
import org.idpass.smartscanner.lib.nfc.NFCScanAnalyzer
import org.idpass.smartscanner.lib.ocr.OCRAnalyzer
import org.idpass.smartscanner.lib.platform.utils.PlayStoreUtils
import org.idpass.smartscanner.lib.scanner.BaseActivity
import org.idpass.smartscanner.lib.scanner.ImageResult
import org.idpass.smartscanner.lib.scanner.SmartScannerException
import org.idpass.smartscanner.lib.scanner.config.*
import org.idpass.smartscanner.lib.utils.CameraUtils.isLedFlashAvailable
import org.idpass.smartscanner.lib.utils.LanguageUtils
import org.idpass.smartscanner.lib.utils.extension.*
import org.idpass.smartscanner.lib.utils.transform.CropTransformation
import java.io.File
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.roundToInt


class SmartScannerActivity : BaseActivity(), OnClickListener {

    companion object {
        val TAG: String = SmartScannerActivity::class.java.simpleName
        const val SCANNER_OPTIONS = "scanner_options"
        const val SCANNER_RAW_RESULT = "scanner_raw_result"
        const val SCANNER_HEADER_RESULT = "scanner_header_result"
        const val SCANNER_RESULT = "scanner_result"
        const val SCANNER_INTENT_EXTRAS = "scanner_intent_extras"
        const val SCANNER_FAIL_RESULT = "scanner_fail_result"
        const val SCANNER_RESULT_BYTES = "scanner_result_bytes"
        const val SCANNER_IMAGE_TYPE = "scanner_image_type"
        const val SCANNER_SIGNATURE_VERIFICATION = "scanner_signature_verification"
        const val SCANNER_JWT_CONFIG_UPDATE = "scanner_jwt_config_update"
        const val SCANNER_SETTINGS_CALL = "scanner_settings"
    }

    private val DEFAULT_HEIGHT = 70
    private val REQUEST_CODE_PERMISSIONS = 10
    private val REQUEST_CODE_PERMISSIONS_VERSION_R = 2296
    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.CAMERA
    )
    private var config: Config? = null
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var scannerOptions: ScannerOptions? = null
    private var captureOptions: CaptureOptions? = null
    private var mode: String? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var orientation: String? = null

    private var flashButton: View? = null
    private var settingsButton: View? = null
    private var closeButton: View? = null
    private var rectangle: View? = null
    private var rectangleGuide: View? = null
    private var guideContainer: View? = null
    private var guideWidth: View? = null
    private var xGuideView: View? = null
    private var yGuideView: View? = null
    private var manualCapture: View? = null
    private var brandingImage: ImageView? = null
    private var captureLabelText: TextView? = null
    private var captureHeaderText: TextView? = null
    private var captureSubHeaderText: TextView? = null
    private var barcodeScannerView: DecoratedBarcodeView? = null

    private lateinit var modelLayoutView: View
    private lateinit var coordinatorLayoutView: View
    private lateinit var viewFinder: PreviewView
    private lateinit var cameraExecutor: ExecutorService

    private val orientationEventListener by lazy {
        object : OrientationEventListener(this) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) {
                    return
                }

                val rotation = when (orientation) {
                    in 45 until 135 -> Surface.ROTATION_270
                    in 135 until 225 -> Surface.ROTATION_180
                    in 225 until 315 -> Surface.ROTATION_90
                    else -> Surface.ROTATION_0
                }

                imageAnalyzer?.targetRotation = rotation
                imageCapture?.targetRotation = rotation
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_smart_scanner)

        // assign view ids
        coordinatorLayoutView = findViewById(R.id.coordinator_layout)
        modelLayoutView = findViewById(R.id.view_layout)
        viewFinder = findViewById(R.id.view_finder)
        barcodeScannerView = findViewById(R.id.view_finder_barcode)
        flashButton = findViewById(R.id.flash_button)
        settingsButton = findViewById(R.id.settings_button)
        closeButton = findViewById(R.id.close_button)
        rectangle = findViewById(R.id.rect_image)
        rectangleGuide = findViewById(R.id.scanner_overlay)
        guideContainer = findViewById(R.id.guide_layout)
        guideWidth = findViewById(R.id.guide_width)
        xGuideView = findViewById(R.id.x_guide)
        yGuideView = findViewById(R.id.y_guide)
        brandingImage = findViewById(R.id.branding_image)
        manualCapture = findViewById(R.id.manual_capture)
        captureLabelText = findViewById(R.id.capture_label_text)
        captureHeaderText = findViewById(R.id.capture_header_text)
        captureSubHeaderText = findViewById(R.id.capture_sub_header_text)

        // Scanner setup from intent
        hideActionBar()
        if (intent.action != null) {
            scannerOptions = ScannerOptions.defaultForODK(intent.action).also { options ->
                if (options == null) throw SmartScannerException("Error: Wrong intent action. Please see smartscanner-android-api for proper intent action strings.")
            }
        } else {
            // Use scanner options directly if no scanner type is called
            val options: ScannerOptions? = intent.getParcelableExtra(SCANNER_OPTIONS)
            options?.let {
                Log.d(TAG, "scannerOptions: $it")
                scannerOptions = options
            } ?: run {
                throw SmartScannerException("Please set proper scanner options to be able to use ID PASS Smart Scanner.")
            }
        }
        // Setup modes & config for reader
        mode = scannerOptions?.mode
        config = scannerOptions?.config ?: Config.default
        // Set orientation to PORTRAIT as default
        orientation = config?.orientation ?: Orientation.PORTRAIT.value
        // Request camera permissions
        if (allPermissionsGranted()) {
            setupConfiguration()
        } else {
            requestPermissions()
        }
        cameraExecutor = Executors.newSingleThreadExecutor()
        // Setup Sentry
        val captureLog = scannerOptions?.sentryLogger?.captureLog
        val dsn = scannerOptions?.sentryLogger?.dsn
        if (captureLog == true) {
            if (dsn != null && dsn.isNotEmpty() && dsn.isNotBlank() && dsn.isValidUrl()) {
                // Sentry DSN init
                Sentry.init { options: SentryOptions ->
                    options.dsn = dsn
                }
                Log.i(TAG, "Sentry DSN: $dsn")
            } else {
                throw SmartScannerException("Please set proper dsn value for Sentry to use")
            }
            // Sentry test message
            val testMsg = scannerOptions?.sentryLogger?.testMsg
            if (testMsg != null && testMsg.isNotEmpty() && dsn.isNotBlank()) {
                Sentry.captureMessage(testMsg)
            }
        }
    }

    private fun setupConfiguration() {
        runOnUiThread {
            val isMLKit = PlayStoreUtils.isPlayServicesAvailable(this)
            var analyzer: ImageAnalysis.Analyzer? = null
            var hasPDF417 = false

            checkGuideView()

            if (mode == Modes.BARCODE.value) {
                val barcodeStrings =
                    scannerOptions?.barcodeOptions?.barcodeFormats ?: BarcodeFormat.default
                val barcodeFormats = barcodeStrings.map { BarcodeFormat.valueOf(it).value }
                hasPDF417 = barcodeStrings.find { it == "PDF_417" }?.isNotEmpty() == true
                analyzer = BarcodeAnalyzer(
                    activity = this,
                    intent = intent,
                    imageResultType = config?.imageResultType ?: ImageResultType.PATH.value,
                    hasPDF417 = hasPDF417,
                    barcodeFormats = barcodeFormats
                )
                viewFinder.visibility = VISIBLE
                barcodeScannerView?.visibility = GONE
            }
            if (mode == Modes.QRCODE.value) {
                val qrCodeOptions = scannerOptions?.qrCodeOptions
                analyzer = QRCodeAnalyzer(
                    activity = this,
                    intent = intent,
                    imageResultType = config?.imageResultType ?: ImageResultType.PATH.value,
                    isGzipped = qrCodeOptions?.isGzipped ?: false,
                    isJson = qrCodeOptions?.isJson ?: false,
                    jsonPath = qrCodeOptions?.jsonPath
                )
            }
            if (mode == Modes.QRCODE_CONFIG.value) {
                analyzer = QRCodeAnalyzer(
                    activity = this,
                    intent = intent,
                    mode = Modes.QRCODE_CONFIG.value,
                    "",
                    null
                )
                viewFinder.visibility = VISIBLE
                barcodeScannerView?.visibility = GONE
            }
            if (mode == Modes.IDPASS_LITE.value) {
                val loaded = IDPassLite.initialize(cacheDir, assets)
                if (!loaded) Log.d("${TAG}/SmartScanner", "ID PASS Lite: Load models Failure")
                analyzer = IDPassLiteAnalyzer(
                    activity = this,
                    intent = intent,
                    onVerify = { raw ->
                        raw?.let {
                            showIDPassLiteVerification(it)
                        }
                    }
                )
                viewFinder.visibility = VISIBLE
                barcodeScannerView?.visibility = GONE
            }
            if (mode == Modes.MRZ.value) {
                analyzer = MRZAnalyzer(
                    activity = this,
                    intent = intent,
                    isMLKit = isMLKit,
                    imageResultType = config?.imageResultType ?: ImageResultType.PATH.value,
                    format = scannerOptions?.mrzFormat
                        ?: intent.getStringExtra(ScannerConstants.MRZ_FORMAT_EXTRA),
                    analyzeStart = System.currentTimeMillis(),
                    isShowGuide = config?.showGuide
                )
                viewFinder.visibility = VISIBLE
                barcodeScannerView?.visibility = GONE
            }
            if (mode == Modes.OCR.value) {
                analyzer = OCRAnalyzer(
                    activity = this,
                    intent = intent,
                    imageResultType = config?.imageResultType ?: ImageResultType.PATH.value,
                    analyzeStart = scannerOptions?.ocrOptions?.analyzeStart ?: 0,
                    //when scanning only the first text that will match the regex will be returned as value. Default value is .*
                    regex = scannerOptions?.ocrOptions?.regex
                        ?: intent.getStringExtra(ScannerConstants.OCR_REGEX),
                    //specifies the type of value being scanned. eg. firstname, lastname, etc.
                    type = scannerOptions?.ocrOptions?.type
                        ?: intent.getStringExtra(ScannerConstants.OCR_TYPE),
                    isShowGuide = config?.showOcrGuide ?: false,
                    //when manual capture is set to true. User is required to tap the capture button to analyze the image.
                    manualCapture = config?.isManualCapture ?: false
                )
                viewFinder.visibility = VISIBLE
                barcodeScannerView?.visibility = GONE
            }
            if (mode == Modes.NFC_SCAN.value) {
                val nfcOptions = scannerOptions?.nfcOptions
                analyzer = NFCScanAnalyzer(
                    activity = this,
                    intent = intent,
                    isMLKit = isMLKit,
                    imageResultType = config?.imageResultType ?: ImageResultType.PATH.value,
                    label = nfcOptions?.label,
                    language = scannerOptions?.language
                        ?: intent.getStringExtra(ScannerConstants.LANGUAGE),
                    locale = nfcOptions?.locale
                        ?: intent.getStringExtra(ScannerConstants.NFC_LOCALE),
                    withPhoto = nfcOptions?.withPhoto
                        ?: true, // default is true, NFC results with photo from NFC
                    withMrzPhoto = nfcOptions?.withMrzPhoto
                        ?: false, // default is false, NFC results with photo from MRZ
                    captureLog = scannerOptions?.sentryLogger?.captureLog
                        ?: false, // default is false, capture log and send to Sentry
                    enableLogging = nfcOptions?.enableLogging
                        ?: false, // default is false, logging is disabled
                    analyzeStart = System.currentTimeMillis(),
                    isShowGuide = config?.showGuide
                )
                viewFinder.visibility = VISIBLE
                barcodeScannerView?.visibility = GONE
            }
            if (mode == Modes.PDF_417.value) {
                // Set zxing barcode view finder
                val viewFinderBarcode = findViewById<ViewfinderView>(R.id.zxing_viewfinder_view)
                viewFinder.visibility = GONE
                barcodeScannerView?.visibility = VISIBLE
                barcodeScannerView?.initializeFromIntent(intent)
                // remove black border, text info and laser
                barcodeScannerView?.setStatusText("")
                barcodeScannerView?.viewFinder?.visibility = GONE
                viewFinderBarcode.setLaserVisibility(false)
                viewFinderBarcode.setMaskColor(ContextCompat.getColor(this, R.color.transparent))
                // set PDF417 decoder and autofocus settings
                barcodeScannerView?.barcodeView?.decoderFactory = PDF417DecoderFactory()
                barcodeScannerView?.cameraSettings?.isContinuousFocusEnabled = true
                barcodeScannerView?.cameraSettings?.isAutoFocusEnabled = true
                barcodeScannerView?.decodeContinuous { barcodePdf417 ->
                    Log.d(TAG, "Success from PDF417")
                    Log.d(TAG, "value: $barcodePdf417")
                    // Add checking to only output PDF417 barcode format response
                    if (barcodePdf417.barcodeFormat == PDF_417) {
                        val bitmapResult = barcodePdf417.bitmap
                        val filePath = this.cacheImagePath()
                        bitmapResult?.cropCenter()?.cacheImageToLocal(
                            filePath,
                            0,
                            if (config?.imageResultType == ImageResultType.BASE_64.value) 30 else 80
                        )
                        val corners = barcodePdf417.resultPoints
                        val builder = StringBuilder()
                        for (corner in corners) {
                            builder.append("${corner?.x},${corner?.y} ")
                        }
                        val cornersString = builder.toString()
                        val rawValue = barcodePdf417.text
                        val imageFile = File(filePath)
                        val imageResult =
                            if (config?.imageResultType == ImageResultType.BASE_64.value) imageFile.encodeBase64() else filePath
                        val barcodeResult = BarcodeResult(
                            imagePath = filePath,
                            image = imageResult,
                            corners = cornersString,
                            value = rawValue
                        )

                        val data = Intent()
                        val result = Gson().toJson(barcodeResult)
                        data.putExtra(SCANNER_RESULT, result)
                        data.putExtra(SCANNER_IMAGE_TYPE, config?.imageResultType)

                        setResult(Activity.RESULT_OK, data)
                        this.finish()
                    }
                }
                barcodeScannerView?.resume()
            } else {
                // Set Analyzer and start camera
                analyzer?.let {
                    startCamera(analyzer, hasPDF417)
                } ?: run {
                    if (mode == Modes.CAPTURE_ONLY.value) {
                        startCamera()
                        captureOptions = scannerOptions?.captureOptions ?: CaptureOptions.default
                    } else throw SmartScannerException("Image Analysis Scanner is null. Please check the scanner options sent to SmartScanner.")
                }
            }
        }
        setupViews()
    }

    override fun onStart() {
        super.onStart()
        orientationEventListener.enable()
    }

    override fun onResume() {
        super.onResume()
        if (mode != null && mode == Modes.PDF_417.value) {
            if (barcodeScannerView != null) {
                barcodeScannerView?.resume()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (mode != null && mode == Modes.PDF_417.value) {
            if (barcodeScannerView != null) {
                barcodeScannerView?.pause()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        orientationEventListener.disable()
    }

    @SuppressLint("ClickableViewAccessibility", "UnsafeOptInUsageError")
    private fun startCamera(analyzer: ImageAnalysis.Analyzer? = null, hasPDF417: Boolean = false) {
        viewFinder.post {
            if (viewFinder.display == null) return@post
            this.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
            cameraProviderFuture.addListener({
                val resolution = when {
                    hasPDF417 -> Size(1080, 1920)
                    mode == Modes.QRCODE.value || mode == Modes.IDPASS_LITE.value -> Size(720, 1280)
                    else -> Size(640, 480)
                }
                val rotation = viewFinder.display.rotation
                // Used to bind the lifecycle of cameras to the lifecycle owner
                cameraProvider = cameraProviderFuture.get()
                // Preview
                preview = Preview.Builder()
                    .setTargetResolution(resolution)
                    .setTargetRotation(rotation)
                    .build()
                val imageAnalysisBuilder = ImageAnalysis.Builder()

                imageAnalyzer = imageAnalysisBuilder
                    .setTargetResolution(resolution)
                    .setTargetRotation(rotation)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        analyzer?.let { analysis -> it.setAnalyzer(cameraExecutor, analysis) }
                    }

                // Create configuration object for the image capture use case
                imageCapture = ImageCapture.Builder()
                    .setTargetResolution(Size(1080, 1920))
                    .setTargetRotation(Surface.ROTATION_0)
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                    .build()
                // Select back camera
                val cameraSelector =
                    CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build()
                try {
                    // Unbind use cases before rebinding
                    cameraProvider?.unbindAll()
                    // Bind use cases to camera
                    camera = if (analyzer != null) {
                        cameraProvider?.bindToLifecycle(
                            this,
                            cameraSelector,
                            preview,
                            imageAnalyzer,
                            imageCapture
                        )
                    } else {
                        cameraProvider?.bindToLifecycle(
                            this,
                            cameraSelector,
                            preview,
                            imageCapture
                        )
                    }
                    // Adjust initial zoom ratio of camera to aid high resolution capture of Pdf417 or QR Code or ID PASS Lite
                    if (hasPDF417 || mode == Modes.QRCODE.value || mode == Modes.IDPASS_LITE.value) {
                        camera?.cameraControl?.setZoomRatio(
                            when {
                                hasPDF417 -> 0.5F
                                else -> 1.2F
                            }
                        )
                    }
                    preview?.setSurfaceProvider(viewFinder.createSurfaceProvider())
                    Log.d(
                        TAG,
                        "Measured size: ${viewFinder.width}x${viewFinder.height}"
                    )
                    // Autofocus modes and Tap to focus
                    val camera2InterOp = Camera2Interop.Extender(imageAnalysisBuilder)
                    camera2InterOp.setCaptureRequestOption(
                        CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_AUTO
                    )
                    camera2InterOp.setCaptureRequestOption(
                        CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON
                    )
                    viewFinder.afterMeasured {
                        viewFinder.setOnTouchListener { _, event ->
                            return@setOnTouchListener when (event.action) {
                                MotionEvent.ACTION_DOWN -> {
                                    true
                                }

                                MotionEvent.ACTION_UP -> {
                                    val factory: MeteringPointFactory =
                                        SurfaceOrientedMeteringPointFactory(
                                            viewFinder.width.toFloat(), viewFinder.height.toFloat()
                                        )
                                    val autoFocusPoint = factory.createPoint(event.x, event.y)
                                    try {
                                        camera?.cameraControl?.startFocusAndMetering(
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
                                    true
                                }

                                else -> false // Unhandled event.
                            }
                        }
                    }

                } catch (exc: Exception) {
                    Log.e(TAG, "Use case binding failed", exc)
                }
            }, ContextCompat.getMainExecutor(this))
        }
    }

    private fun setupViews() {
        // scanner layout size
        val topGuideline = findViewById<Guideline>(R.id.top)
        val bottomGuideline = findViewById<Guideline>(R.id.bottom)
        // scanner sizes available for Portrait only
        if (orientation == Orientation.PORTRAIT.value) {
            when (scannerOptions?.scannerSize) {
                ScannerSize.LARGE.value -> {
                    bottomGuideline.setGuidelinePercent(0.7F)
                    topGuideline.setGuidelinePercent(0.25F)
                }

                ScannerSize.SMALL.value -> {
                    bottomGuideline.setGuidelinePercent(0.6F)
                    topGuideline.setGuidelinePercent(0.375F)
                }

                else -> {
                    bottomGuideline.setGuidelinePercent(0.625F)
                    topGuideline.setGuidelinePercent(0.275F)
                }
            }
        }
        //settings
        settingsButton?.visibility = if (config?.showSettings == true) VISIBLE else GONE
        // flash
        flashButton?.visibility = if (isLedFlashAvailable(this)) VISIBLE else GONE
        // capture text label
        captureLabelText?.text = config?.label ?: String.empty()
        // capture text header
        captureHeaderText?.text = config?.header ?: String.empty()
        // capture text sub-header
        captureSubHeaderText?.text = config?.subHeader ?: String.empty()
        // font to use
        val defaultFont = when (config?.font) {
            Fonts.NOTO_SANS_ARABIC.value -> ResourcesCompat.getFont(
                this,
                R.font.notosansarabic_regular
            )

            Fonts.ROBOTO.value -> ResourcesCompat.getFont(this, R.font.roboto_regular)
            else -> ResourcesCompat.getFont(this, R.font.sourcesanspro_regular)
        }
        captureLabelText?.setTypeface(defaultFont, Typeface.BOLD)
        captureHeaderText?.setTypeface(defaultFont, Typeface.BOLD)
        captureSubHeaderText?.typeface = defaultFont
        // Background reader
        try {
            config?.background?.let {
                if (it.isNotEmpty()) {
                    val color = Color.parseColor(config?.background)
                    coordinatorLayoutView.setBackgroundColor(color)
                }
            } ?: run {
                coordinatorLayoutView.setBackgroundColor(
                    ContextCompat.getColor(
                        this,
                        R.color.transparent_grey
                    )
                )
            }
        } catch (iae: IllegalArgumentException) {
            // This color string is not valid
            throw SmartScannerException("Please set proper color string in setting background. Example: '#ffc234' ")
        }
        // branding
        brandingImage?.visibility =
            config?.branding?.let { if (it) VISIBLE else INVISIBLE } ?: run { INVISIBLE }
        // manual capture
        manualCapture?.visibility = config?.isManualCapture?.let {
            if (it) VISIBLE else GONE
        } ?: run {
            if (intent.getBooleanExtra(
                    ScannerConstants.MRZ_MANUAL_CAPTURE_EXTRA,
                    false
                )
            ) VISIBLE else GONE
        }
        // language locale
        scannerOptions?.language?.let { language ->
            LanguageUtils.changeLanguage(this, language)
        }
        // Device orientation
        if (orientation == Orientation.LANDSCAPE.value) {
            this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
        }
        // assign camera click listeners
        closeButton?.setOnClickListener(this)
        settingsButton?.setOnClickListener(this)
        flashButton?.setOnClickListener(this)
        manualCapture?.setOnClickListener(this)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_CODE_PERMISSIONS_VERSION_R,
            REQUEST_CODE_PERMISSIONS -> {
                if (allPermissionsGranted()) {
                    setupConfiguration()
                } else {
                    val snackBar: Snackbar = Snackbar.make(
                        coordinatorLayoutView,
                        R.string.required_perms_not_given,
                        Snackbar.LENGTH_INDEFINITE
                    )
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
    }

    private fun requestPermissions() =
        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.close_button -> onBackPressed()
            R.id.settings_button -> showSettings()
            R.id.flash_button -> {
                flashButton?.let {
                    if (it.isSelected) {
                        it.isSelected = false
                        enableFlashlight(false)
                    } else {
                        it.isSelected = true
                        enableFlashlight(true)
                    }
                }
            }

            R.id.manual_capture -> {
                // hide capture button during image capture
                manualCapture?.isEnabled = false
                val imageFile = File(cacheImagePath())
                val outputFileOptions = ImageCapture.OutputFileOptions.Builder(imageFile).build()
                imageCapture?.takePicture(outputFileOptions, cameraExecutor,
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                            val data = Intent()
                            val width: Int = captureOptions?.width?.px ?: run {
                                when (captureOptions?.type) {
                                    CaptureType.ID.value -> 285.px
                                    CaptureType.DOCUMENT.value -> 180.px
                                    else -> 285.px // default width for MRZ
                                }
                            }
                            val height: Int = captureOptions?.height?.px ?: run {
                                when (captureOptions?.type) {
                                    CaptureType.ID.value -> 180.px
                                    CaptureType.DOCUMENT.value -> 285.px
                                    else -> 180.px // default height for MRZ
                                }
                            }
                            // Initial MRZ Card Size
                            val transform = bitmapTransform(
                                CropTransformation(
                                    width,
                                    height,
                                    CropTransformation.CropType.CENTER
                                )
                            )
                            val bf = Glide.with(this@SmartScannerActivity)
                                .asBitmap()
                                .load(imageFile.path)
                                .apply(transform)
                                .submit()
                                .get()
                            bf.cacheImageToLocal(imageFile.path)
                            val imageString =
                                if (config?.imageResultType == ImageResultType.BASE_64.value) imageFile.encodeBase64() else imageFile.path
                            val result: Any =
                                if (mode == Modes.MRZ.value) MrzUtils.getImageOnly(imageString) else ImageResult(
                                    imageString
                                )
                            data.putExtra(SCANNER_IMAGE_TYPE, config?.imageResultType)
                            data.putExtra(SCANNER_RESULT, Gson().toJson(result))
                            setResult(Activity.RESULT_OK, data)
                            finish()
                        }

                        override fun onError(exception: ImageCaptureException) {
                            exception.printStackTrace()
                            manualCapture?.isEnabled = true
                        }
                    }
                )
            }
        }
    }

    private fun enableFlashlight(torch: Boolean) {
        if (barcodeScannerView != null && barcodeScannerView?.visibility == VISIBLE) {
            if (torch) barcodeScannerView?.setTorchOn() else barcodeScannerView?.setTorchOff()
        } else {
            camera?.cameraControl?.enableTorch(torch)
        }
    }
    private fun showSettings() {
        val data = Intent()
        data.putExtra(SCANNER_SETTINGS_CALL, true)
        data.putExtra(ScannerConstants.MODE, mode)
        data.putExtra(SCANNER_INTENT_EXTRAS, scannerOptions)

        setResult(Activity.RESULT_OK, data)
        this.finish()
    }
    @SuppressLint("InflateParams")
    private fun showIDPassLiteVerification(qrBytes: ByteArray) {
        val bottomSheetDialog = BottomSheetDialog(this)
        val sheetView = layoutInflater.inflate(R.layout.dialog_idpass_verify, null)
        bottomSheetDialog.setContentView(sheetView)
        // id pass reader
        val reader = IDPassManager.getIDPassReader()
        // bottom sheet ids
        val pinCodeInpt = sheetView.findViewById<EditText>(R.id.card_pin_code)
        val verifyBtn = sheetView.findViewById<Button>(R.id.pin_code_verify)
        val skipBtn = sheetView.findViewById<Button>(R.id.pin_code_skip)
        // stop smartscanner camera scanning
        cameraProvider?.unbindAll()
        // bottom sheet listeners
        pinCodeInpt.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(text: Editable) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                verifyBtn.isEnabled = s?.isEmpty() != true
            }
        })
        verifyBtn.setOnClickListener {
            val pinCode = pinCodeInpt.text.trim().toString()
            IDPassManager.verifyCard(
                activity = this,
                idPassReader = reader,
                intent = intent,
                raw = qrBytes,
                pinCode = pinCode,
                onResult = { bottomSheetDialog.dismiss() }
            )
        }
        skipBtn.setOnClickListener {
            IDPassManager.verifyCard(
                activity = this,
                idPassReader = reader,
                intent = intent,
                raw = qrBytes,
                onResult = { bottomSheetDialog.dismiss() }
            )
        }
        bottomSheetDialog.setOnDismissListener {
            setupConfiguration()
        }
        bottomSheetDialog.show()
    }

    private inline fun View.afterMeasured(crossinline block: () -> Unit) {
        if (measuredWidth > 0 && measuredHeight > 0) {
            block()
        } else {
            viewTreeObserver.addOnGlobalLayoutListener(object :
                ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    if (measuredWidth > 0 && measuredHeight > 0) {
                        viewTreeObserver.removeOnGlobalLayoutListener(this)
                        block()
                    }
                }
            })
        }
    }

    private fun checkGuideView() {
        if (config?.showGuide == true) {
            showMRZGuide()
        } else if (config?.showOcrGuide == true) {
            showOCRGuide()
        } else {
            guideContainer?.alpha = 0f
        }
    }

    private fun showMRZGuide() {
        guideContainer?.alpha = 1f

        config?.let { conf ->
            viewFinder.post {
                val width = if (conf.widthGuide == 0) {
                    //set the default width
                   (viewFinder.width - 21.toPx)
                } else {
                    TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        conf.widthGuide.toFloat(),
                        resources.displayMetrics
                    ).roundToInt()
                }

                rectangleGuide?.layoutParams?.width = width
                guideWidth?.layoutParams?.width = width

                //set height
                val nHeight = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    conf.heightGuide.toFloat(),
                    resources.displayMetrics
                ).roundToInt()
                rectangleGuide?.layoutParams?.height = nHeight

                //set default position

                val xPercentage = (viewFinder.width - width).div(2)
                val yPercentage = viewFinder.height - nHeight - 30.toPx

                xGuideView?.layoutParams?.width = xPercentage
                yGuideView?.layoutParams?.height = yPercentage

                xGuideView?.requestLayout()
                yGuideView?.requestLayout()
            }
            rectangleGuide?.requestLayout()
        }
    }
    private fun showOCRGuide() {
        guideContainer?.alpha = 1f

        config?.let { conf ->
            if (conf.widthGuide != 0) {
                val nWidth = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    conf.widthGuide.toFloat(),
                    resources.displayMetrics
                ).roundToInt()
                rectangleGuide?.layoutParams?.width = nWidth
                guideWidth?.layoutParams?.width = nWidth
            }

            // if height guide is not by default
            if (conf.heightGuide != 0) {
                val nHeight = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    conf.heightGuide.toFloat(),
                    resources.displayMetrics
                ).roundToInt()
                rectangleGuide?.layoutParams?.height = nHeight
            }

            if (conf.xGuide != null && conf.yGuide != null) {
                viewFinder.post {
                    //prevent xGuide from exceeding values 0.0-1.0
                    val x = when {
                        conf.xGuide.toFloat() > 1 -> 1f
                        conf.xGuide.toFloat() < 0 -> 0f
                        else -> conf.xGuide.toFloat()
                    }

                    //prevent yGuide from exceeding values 0.0-1.0
                    val y = when {
                        conf.yGuide.toFloat() > 1 -> 1f
                        conf.yGuide.toFloat() < 0 -> 0f
                        else -> conf.yGuide.toFloat()
                    }

                    //take into consideration the center point of the OCR guide.
                    val xPercentage = (viewFinder.width.toFloat() * x).roundToInt() - (rectangleGuide?.width?.div(2) ?: 0)
                    val yPercentage = (viewFinder.height.toFloat() * y).roundToInt() - (rectangleGuide?.height?.div(2) ?:0)

                    //set OCR guide center point to specified x and y coordinates
                    xGuideView?.layoutParams?.width = xPercentage
                    yGuideView?.layoutParams?.height = yPercentage

                    xGuideView?.requestLayout()
                    yGuideView?.requestLayout()
                }
            }
            rectangleGuide?.requestLayout()
        }
    }
}
