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
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.View
import android.view.View.*
import android.view.Window
import android.widget.*
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Guideline
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions.bitmapTransform
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import org.idpass.lite.android.IDPassLite
import org.idpass.smartscanner.api.ScannerConstants
import org.idpass.smartscanner.lib.barcode.BarcodeAnalyzer
import org.idpass.smartscanner.lib.barcode.qr.QRCodeAnalyzer
import org.idpass.smartscanner.lib.idpasslite.IDPassLiteAnalyzer
import org.idpass.smartscanner.lib.idpasslite.IDPassManager
import org.idpass.smartscanner.lib.mrz.MRZAnalyzer
import org.idpass.smartscanner.lib.mrz.MrzUtils
import org.idpass.smartscanner.lib.nfc.NFCScanAnalyzer
import org.idpass.smartscanner.lib.platform.BaseActivity
import org.idpass.smartscanner.lib.platform.extension.*
import org.idpass.smartscanner.lib.platform.utils.CameraUtils.isLedFlashAvailable
import org.idpass.smartscanner.lib.platform.utils.transform.CropTransformation
import org.idpass.smartscanner.lib.scanner.SmartScannerException
import org.idpass.smartscanner.lib.scanner.config.*
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class SmartScannerActivity : BaseActivity(), OnClickListener {

    companion object {
        val TAG: String = SmartScannerActivity::class.java.simpleName
        const val SCANNER_OPTIONS = "scanner_options"
        const val SCANNER_RESULT = "scanner_result"
        const val SCANNER_RESULT_BYTES = "scanner_result_bytes"
    }

    private val REQUEST_CODE_PERMISSIONS = 10
    private val REQUEST_CODE_PERMISSIONS_VERSION_R = 2296
    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    private var config: Config? = null
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var scannerOptions: ScannerOptions? = null
    private var mode: String? = null
    private var cameraProvider: ProcessCameraProvider? = null

    private var flashButton: View? = null
    private var closeButton: View? = null
    private var rectangle: View? = null
    private var manualCapture: View? = null
    private var brandingImage: ImageView? = null
    private var captureLabelText: TextView? = null
    private var modelText: TextView? = null
    private var modelTextLoading: ProgressBar? = null

    private lateinit var modelLayoutView: View
    private lateinit var coordinatorLayoutView: View
    private lateinit var viewFinder: PreviewView
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_smart_scanner)
        // assign view ids
        coordinatorLayoutView = findViewById(R.id.coordinatorLayout)
        modelLayoutView = findViewById(R.id.viewLayout)
        viewFinder = findViewById(R.id.viewFinder)
        flashButton = findViewById(R.id.flash_button)
        closeButton = findViewById(R.id.close_button)
        rectangle = findViewById(R.id.rectimage)
        modelText = findViewById(R.id.modelText)
        modelTextLoading = findViewById(R.id.modelTextLoading)
        brandingImage = findViewById(R.id.brandingImage)
        manualCapture = findViewById(R.id.manualCapture)
        captureLabelText = findViewById(R.id.captureLabelText)
        // Scanner setup from intent
        hideActionBar()
        if (intent.action != null) {
            scannerOptions = when (intent.action) {
                // barcode
                ScannerConstants.IDPASS_SMARTSCANNER_BARCODE_INTENT,
                ScannerConstants.IDPASS_SMARTSCANNER_ODK_BARCODE_INTENT -> ScannerOptions.defaultForBarcode
                // idpass lite
                ScannerConstants.IDPASS_SMARTSCANNER_IDPASS_LITE_INTENT,
                ScannerConstants.IDPASS_SMARTSCANNER_ODK_IDPASS_LITE_INTENT -> ScannerOptions.defaultForIdPassLite
                // mrz
                ScannerConstants.IDPASS_SMARTSCANNER_MRZ_INTENT,
                ScannerConstants.IDPASS_SMARTSCANNER_ODK_MRZ_INTENT -> ScannerOptions.defaultForMRZ
                // nfc
                ScannerConstants.IDPASS_SMARTSCANNER_NFC_INTENT,
                ScannerConstants.IDPASS_SMARTSCANNER_ODK_NFC_INTENT -> ScannerOptions.defaultForNFCScan
                // qrcode
                ScannerConstants.IDPASS_SMARTSCANNER_QRCODE_INTENT,
                ScannerConstants.IDPASS_SMARTSCANNER_ODK_QRCODE_INTENT -> ScannerOptions.defaultForQRCode
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
        // Request camera permissions
        if (allPermissionsGranted()) {
            setupConfiguration()
        } else {
            requestPermissions()
        }
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun setupConfiguration() {
        runOnUiThread {
            val isMLKit = isPlayServicesAvailable()
            var analyzer : ImageAnalysis.Analyzer? = null
            if (mode == Modes.BARCODE.value) {
                val barcodeStrings = scannerOptions?.barcodeOptions?.barcodeFormats ?: BarcodeFormat.default
                val barcodeFormats = barcodeStrings.map { BarcodeFormat.valueOf(it).value }
                analyzer = BarcodeAnalyzer(
                    activity = this,
                    intent = intent,
                    barcodeFormats = barcodeFormats
                )
            }
            if (mode == Modes.QRCODE.value) {
                analyzer = QRCodeAnalyzer(
                    activity = this,
                    intent = intent
                )
            }
            if (mode == Modes.IDPASS_LITE.value) {
                val loaded = IDPassLite.initialize(cacheDir, assets)
                if (!loaded) Log.d("${TAG}/SmartScanner", "ID PASS Lite: Load models Failure")
                analyzer = IDPassLiteAnalyzer(
                    activity = this,
                    intent = intent,
                    onVerify = { raw ->
                        raw?.let {
                            showIDPassLiteVerification (it)
                        }
                    }
                )
            }
            if (mode == Modes.MRZ.value) {
                analyzer = MRZAnalyzer(
                        activity = this,
                        intent = intent,
                        isMLKit = isMLKit,
                        imageResultType = config?.imageResultType ?: ImageResultType.PATH.value,
                        format = scannerOptions?.mrzFormat ?: intent.getStringExtra(ScannerConstants.MRZ_FORMAT_EXTRA),
                        analyzeStart = System.currentTimeMillis(),
                        onConnectSuccess = {
                            if (modelText?.visibility == VISIBLE) {
                                modelText?.text = it
                            }
                            modelTextLoading?.visibility = INVISIBLE
                            modelText?.visibility = INVISIBLE
                        },
                        onConnectFail = {
                            modelTextLoading?.visibility = VISIBLE
                            modelText?.visibility = VISIBLE
                            modelText?.text = it
                        }
                ).also {
                    if (!isMLKit) it.initializeTesseract(this)
                }
            }
            if (mode == Modes.NFC_SCAN.value) {
                analyzer = NFCScanAnalyzer(
                        activity = this,
                        intent = intent,
                        isMLKit = isMLKit,
                        imageResultType = config?.imageResultType ?: ImageResultType.PATH.value,
                        format = scannerOptions?.mrzFormat ?: intent.getStringExtra(ScannerConstants.MRZ_FORMAT_EXTRA),
                        analyzeStart = System.currentTimeMillis(),
                        onConnectSuccess = {
                            modelTextLoading?.visibility = INVISIBLE
                            modelText?.visibility = INVISIBLE
                        },
                        onConnectFail = {
                            modelTextLoading?.visibility = VISIBLE
                            modelText?.visibility = VISIBLE
                            modelText?.text = it
                        }
                ).also {
                    if (!isMLKit) it.initializeTesseract(this)
                }
            }
            // set Analyzer and start camera
            analyzer?.let {
                startCamera(analyzer)
            } ?: run {
                throw SmartScannerException("Image Analysis Scanner is null. Please check the scanner options sent to SmartScanner.")
            }
        }
        setupViews()
    }

    private fun startCamera(analyzer: ImageAnalysis.Analyzer) {
        this.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            cameraProvider = cameraProviderFuture.get()
            // Preview
            preview = Preview.Builder().build()
            val size = Size(480, 640)
            imageAnalyzer = ImageAnalysis.Builder()
                .setTargetResolution(size)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, analyzer)
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
                cameraProvider?.unbindAll()
                // Bind use cases to camera
                camera = cameraProvider?.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageAnalyzer,
                    imageCapture
                )
                preview?.setSurfaceProvider(viewFinder.createSurfaceProvider())
                Log.d(
                    TAG,
                    "Measured size: ${viewFinder.width}x${viewFinder.height}"
                )
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
        // assign camera click listeners
        closeButton?.setOnClickListener(this)
        flashButton?.setOnClickListener(this)
        manualCapture?.setOnClickListener(this)
    }

    private fun setupViews() {
        // scanner layout size
        val layoutParams = modelLayoutView.layoutParams as ConstraintLayout.LayoutParams
        val topGuideline = findViewById<Guideline>(R.id.top)
        val bottomGuideline = findViewById<Guideline>(R.id.bottom)
        when (scannerOptions?.scannerSize) {
            ScannerSize.CUSTOM_QR.value -> {
                layoutParams.dimensionRatio = "4:6"
                layoutParams.marginStart = 96 // Approx. 48dp
                layoutParams.marginEnd = 96 // Approx. 48dp
                modelLayoutView.layoutParams = layoutParams
            }
            ScannerSize.LARGE.value -> {
                bottomGuideline.setGuidelinePercent(0.75F)
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
        brandingImage?.visibility = config?.branding?.let { if (it) VISIBLE else INVISIBLE } ?: run { INVISIBLE }
        // manual capture
        manualCapture?.visibility = config?.isManualCapture?.let {
            if (it) VISIBLE else GONE
        } ?: run {
            if (intent.getBooleanExtra(ScannerConstants.MRZ_MANUAL_CAPTURE_EXTRA, false)) VISIBLE else GONE
        }
    }

    private fun isPlayServicesAvailable(): Boolean {
        val availability = GoogleApiAvailability.getInstance()
        val resultCode = availability.isGooglePlayServicesAvailable(this)
        return resultCode == ConnectionResult.SUCCESS
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_CODE_PERMISSIONS_VERSION_R,
            REQUEST_CODE_PERMISSIONS -> {
                if (allPermissionsGranted()) {
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
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
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
                // hide capture button during image capture
                manualCapture?.isEnabled = false
                val imageFile = File(cacheImagePath())
                val outputFileOptions = ImageCapture.OutputFileOptions.Builder(imageFile).build()
                imageCapture?.takePicture(outputFileOptions, cameraExecutor,
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                            val data = Intent()
                            val transform = bitmapTransform(CropTransformation(285.px, 180.px, CropTransformation.CropType.CENTER)) // Initial MRZ Card Size
                            val bf = Glide.with(this@SmartScannerActivity)
                                            .asBitmap()
                                            .load(imageFile.path)
                                            .apply(transform)
                                            .submit()
                                            .get()
                            bf.cacheImageToLocal(imageFile.path)
                            val imageString = if (config?.imageResultType == ImageResultType.BASE_64.value) bf.encodeBase64() else imageFile.path
                            val result = if(mode == Modes.MRZ.value) Gson().toJson(MrzUtils.getImageOnly(imageString)) else imageString
                            data.putExtra(SCANNER_RESULT, result)
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

    @SuppressLint("InflateParams")
    private fun showIDPassLiteVerification(qrBytes : ByteArray)  {
        val bottomSheetDialog = BottomSheetDialog(this)
        val sheetView = layoutInflater.inflate(R.layout.dialog_idpass_verify, null)
        bottomSheetDialog.setContentView(sheetView)
        // id pass reader
        val reader = IDPassManager.getIDPassReader()
        // bottom sheet ids
        val pinCodeInpt = sheetView.findViewById<EditText>(R.id.cardPinCode)
        val verifyBtn = sheetView.findViewById<Button>(R.id.pinCodeVerify)
        val skipBtn = sheetView.findViewById<Button>(R.id.pinCodeSkip)
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
}