package org.idpass.smartscanner.lib

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
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
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.mlkit.vision.barcode.Barcode.FORMAT_CODE_39
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.googlecode.tesseract.android.TessBaseAPI
import org.idpass.smartscanner.innovatrics.barcode.BarcodeResult
import org.idpass.smartscanner.innovatrics.mrz.MRZResult
import org.idpass.smartscanner.innovatrics.mrz.MRZResult.Companion.formatMrtdTd1Result
import org.idpass.smartscanner.innovatrics.mrz.MRZResult.Companion.formatMrzResult
import org.idpass.smartscanner.lib.config.*
import org.idpass.smartscanner.lib.exceptions.SmartScannerException
import org.idpass.smartscanner.lib.extension.*
import org.idpass.smartscanner.lib.platform.AnalyzerType
import org.idpass.smartscanner.lib.platform.MRZCleaner
import org.idpass.smartscanner.lib.platform.SmartScannerAnalyzer
import org.idpass.smartscanner.lib.utils.CameraUtils.isLedFlashAvailable
import org.idpass.smartscanner.lib.utils.FileUtils.copyAssets
import java.io.File
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.concurrent.thread
import kotlin.math.abs

class SmartScannerActivity : AppCompatActivity(), OnClickListener {

    companion object {
        val TAG: String = SmartScannerActivity::class.java.simpleName
        const val SCANNER = "scanner"
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

    private lateinit var modelLayoutView: View
    private lateinit var coordinatorLayoutView: View
    private lateinit var viewFinder: PreviewView

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var context: Context
    private lateinit var outputDirectory: File
    private lateinit var tessBaseAPI: TessBaseAPI

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_smart_scanner)
        // get application context
        context = applicationContext
        // assign layout ids
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
        // hide actionbar
        supportActionBar?.hide()
        supportActionBar?.setDisplayShowTitleEnabled(false)
        actionBar?.hide()
        actionBar?.setDisplayShowTitleEnabled(false)
        // Scanner type or options
        val type : String? = intent.getStringExtra(SCANNER)
        type?.let {
            // Check for options on specific scanner type call out
            Log.d(TAG, "scannerType: $it")
            if (it.isNotEmpty()) {
                scannerOptions = when (it) {
                    ScannerType.BARCODE.value -> ScannerType.barcodeOptions
                    ScannerType.IDPASS_LITE.value -> ScannerType.idPassLiteOptions
                    ScannerType.MRZ.value -> ScannerType.mrzOptions
                    else -> throw SmartScannerException("Error: Wrong scanner type. Please set to either \"barcode\", \"idpass-lite\", \"mrz\" ")
                }
            } else {
                throw SmartScannerException("Scanner type cannot be null or empty.")
            }
        } ?: run {
            // Use scanner options directly if no scanner type is called
            val options : ScannerOptions? = intent.getParcelableExtra(SCANNER_OPTIONS)
            options?.let {
                Log.d(TAG, "scannerOptions: $it")
                scannerOptions = options
            } ?: run {
                throw SmartScannerException("Please set proper scanner options to be able to use ID PASS Smart Scanner.")
            }
        }
        mode = scannerOptions?.mode
        mrzFormat = scannerOptions?.mrzFormat ?: MrzFormat.MRP.value
        barcodeOptions = scannerOptions?.barcodeOptions ?: BarcodeOptions.default
        barcodeStrings = barcodeOptions.barcodeFormats ?: BarcodeFormat.default
        barcodeFormats = barcodeStrings.map { BarcodeFormat.valueOf(it).value }
        // setup config for reader
        config = scannerOptions?.config  ?: Config.default
        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera(config ?: Config.default)
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
        // assign click listeners
        closeButton?.setOnClickListener(this)
        flashButton?.setOnClickListener(this)
        // directory output paths
        outputDirectory = getOutputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()
        isMLKitUsable = checkGooglePlayServices()
        Log.d(TAG, "isMLKitUsable: $isMLKitUsable")
        // Initialize Tesseract
        if (mode == Modes.MRZ.value && !isMLKitUsable) {
            val extDirPath: String = getExternalFilesDir(null)!!.absolutePath
            Log.d(TAG, "path: $extDirPath")
            copyAssets(this, "tessdata", extDirPath)
            tessBaseAPI = TessBaseAPI()
            tessBaseAPI.init(extDirPath, "ocrb_int", TessBaseAPI.OEM_DEFAULT)
            tessBaseAPI.pageSegMode = TessBaseAPI.PageSegMode.PSM_SINGLE_BLOCK
        }
    }

    private fun setupConfiguration(config: Config) {
        // flash
        flashButton?.visibility = if (isLedFlashAvailable(this)) VISIBLE else GONE
        // capture text label
        captureLabelText?.text = config.label ?: String.empty()
        // font to use
        captureLabelText?.typeface = when (config.font) {
            Fonts.NOTO_SANS_ARABIC.value -> ResourcesCompat.getFont(this, R.font.notosansarabic_bold)
            Fonts.ROBOTO.value -> ResourcesCompat.getFont(this, R.font.roboto_regular)
            else -> ResourcesCompat.getFont(this, R.font.sourcesanspro_medium)
        }
        // Background reader
        try {
            config.background?.let {
                if (it.isNotEmpty()) {
                    val color = Color.parseColor(config.background)
                    coordinatorLayoutView.setBackgroundColor(color)
                }
            } ?: run {
                coordinatorLayoutView.setBackgroundColor(ContextCompat.getColor(this, R.color.transparent_grey))
            }
        } catch (iae: IllegalArgumentException) {
            // This color string is not valid
            coordinatorLayoutView.setBackgroundColor(ContextCompat.getColor(this, R.color.transparent_grey))
        }
        // barcode view layout
        if (mode == Modes.BARCODE.value) {
            val layoutParams = modelLayoutView.layoutParams as ConstraintLayout.LayoutParams
            when (barcodeOptions.barcodeScannerSize) {
                ScannerSize.CUSTOM_QR.value -> {
                    layoutParams.dimensionRatio = "4:6"
                    layoutParams.marginStart = 96 // Approx. 48dp
                    layoutParams.marginEnd = 96 // Approx. 48dp
                    modelLayoutView.layoutParams = layoutParams
                }
                ScannerSize.LARGE.value -> {
                    layoutParams.dimensionRatio = "4:7"
                    modelLayoutView.layoutParams = layoutParams
                }
                ScannerSize.SMALL.value -> {
                    layoutParams.dimensionRatio = "4:4"
                    modelLayoutView.layoutParams = layoutParams
                }
                else -> {
                    layoutParams.dimensionRatio = "4:5"
                    modelLayoutView.layoutParams = layoutParams
                }
            }
        }
        // branding
        brandingImage?.visibility = config.branding?.let { if (it) VISIBLE else GONE } ?: run { VISIBLE }
        // manual capture
        manualCapture?.visibility = config.isManualCapture?.let { if (it) VISIBLE else GONE } ?: run { GONE }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera(config ?: Config.default)
            } else {
                val snackBar: Snackbar = Snackbar.make(
                    coordinatorLayoutView,
                    R.string.required_perms_not_given, Snackbar.LENGTH_INDEFINITE
                )
                snackBar.setAction(R.string.settings) { openSettingsApp() }
                snackBar.show()
            }
        }
    }

    private fun openSettingsApp() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivity(intent)
    }

    private fun startCamera(config: Config) {
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
                    it.setAnalyzer(cameraExecutor, getMrzAnalyzer())
                }
            // Create configuration object for the image capture use case
            imageCapture = ImageCapture.Builder()
                .setTargetResolution(size)
                .setTargetRotation(Surface.ROTATION_0)
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build()
            manualCapture?.setOnClickListener {
                val imageFile = File(getImagePath())
                val outputFileOptions = ImageCapture.OutputFileOptions.Builder(imageFile).build()
                imageCapture?.takePicture(outputFileOptions, ContextCompat.getMainExecutor(
                    baseContext), object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        val data = Intent()
                        val bf = imageFile.path.toBitmap()
                        bf.cacheImageToLocal(imageFile.path, 90)
                        val imageString = if (config.imageResultType == ImageResultType.BASE_64.value) bf.encodeBase64() else imageFile.path
                        val result = Gson().toJson(MRZResult.getImageOnly(imageString))
                        data.putExtra(SCANNER_RESULT, result)
                        setResult(Activity.RESULT_OK, data)
                        finish()
                    }
                    override fun onError(exception: ImageCaptureException) {
                        exception.printStackTrace()
                    }
                })
            }
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
                preview?.setSurfaceProvider(viewFinder.createSurfaceProvider())
                Log.d(
                    TAG,
                    "Measured size: ${viewFinder.width}x${viewFinder.height}"
                )
                startScanTime = System.currentTimeMillis()
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
        setupConfiguration(config)
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    private fun getMrzAnalyzer(): SmartScannerAnalyzer {
        var barcodeBusy = false
        var mrzBusy = false

        return SmartScannerAnalyzer { imageProxy ->
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val rot = imageProxy.imageInfo.rotationDegrees
                val bf = mediaImage.toBitmap(rot)
                val cropped = if (rot == 90 || rot == 270) Bitmap.createBitmap(
                    bf,
                    bf.width / 2,
                    0,
                    bf.width / 2,
                    bf.height
                )
                else Bitmap.createBitmap(bf, 0, bf.height / 2, bf.width, bf.height / 2)
                Log.d(TAG, "Bitmap: (${mediaImage.width}, ${mediaImage.height}) Cropped: (${cropped.width}, ${cropped.height}), Rotation: ${imageProxy.imageInfo.rotationDegrees}")

                //barcode, qr and pdf417
                if (!barcodeBusy &&  (mode == Modes.BARCODE.value)) {
                    barcodeBusy = true
                    Log.d("$TAG/SmartScanner", "barcode: mode is $mode")
                    val start = System.currentTimeMillis()
                    var barcodeFormat = FORMAT_CODE_39 // Most common barcode format
                    barcodeFormats.forEach {
                        barcodeFormat = it or barcodeFormat // bitwise different barcode format options
                    }
                    val options = BarcodeScannerOptions.Builder().setBarcodeFormats(barcodeFormat).build()
                    val image = InputImage.fromBitmap(bf, imageProxy.imageInfo.rotationDegrees)
                    val scanner = BarcodeScanning.getClient(options)
                    Log.d("$TAG/SmartScanner", "barcode: process")
                    scanner.process(image)
                        .addOnSuccessListener { barcodes ->
                            val timeRequired = System.currentTimeMillis() - start
                            val rawValue: String
                            val cornersString: String
                            val rawBytes: ByteArray
                            Log.d("$TAG/SmartScanner", "barcode: success: $timeRequired ms")
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
                                val imageCachePathFile = getImagePath()
                                bf.cacheImageToLocal(
                                    imageCachePathFile,
                                    imageProxy.imageInfo.rotationDegrees
                                )
                                val gson = Gson()
                                val jsonString = gson.toJson(
                                    BarcodeResult(
                                        imageCachePathFile,
                                        cornersString,
                                        rawValue
                                    )
                                )
                                if (barcodeOptions.idPassLiteSupport == true) {
                                    rawBytes = barcodes[0].rawBytes!!
                                    getAnalyzerResult(AnalyzerType.IDPASS_LITE, null, rawBytes)
                                } else {
                                    getAnalyzerResult(AnalyzerType.BARCODE, jsonString)
                                }
                            } else {
                                Log.d("$TAG/SmartScanner", "barcode: nothing detected")
                            }
                            barcodeBusy = false
                        }
                        .addOnFailureListener { e ->
                            Log.d("$TAG/SmartScanner", "barcode: failure: ${e.message}")
                            barcodeBusy = false
                        }
                }
                //MRZ
                if (!mrzBusy &&  (mode == Modes.MRZ.value)) {
                    val mlStartTime = System.currentTimeMillis()
                    if (isMLKitUsable) {
                        mrzBusy = true
                        val image = InputImage.fromBitmap(
                            cropped,
                            imageProxy.imageInfo.rotationDegrees
                        )
                        // Pass image to an ML Kit Vision API
                        val recognizer = TextRecognition.getClient()
                        Log.d("$TAG/SmartScanner", "TextRecognition: process")
                        val start = System.currentTimeMillis()
                        recognizer.process(image)
                            .addOnSuccessListener { visionText ->
                                modelText?.visibility = INVISIBLE
                                val timeRequired = System.currentTimeMillis() - start
                                Log.d("$TAG/SmartScanner", "TextRecognition: success: $timeRequired ms")
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
                                        "$TAG/SmartScanner",
                                        "Before cleaner: [${
                                            URLEncoder.encode(rawFullRead, "UTF-8")
                                                .replace("%3C", "<").replace("%0A", "↩")
                                        }]"
                                    )
                                    val mrz = MRZCleaner.clean(rawFullRead)
                                    Log.d(
                                        "$TAG/SmartScanner",
                                        "After cleaner = [${
                                            URLEncoder.encode(mrz, "UTF-8")
                                                .replace("%3C", "<").replace("%0A", "↩")
                                        }]"
                                    )
                                    val imagePathFile = getImagePath()
                                    bf.cacheImageToLocal(
                                        imagePathFile,
                                        imageProxy.imageInfo.rotationDegrees
                                    )
                                    val imageString = if (config?.imageResultType == ImageResultType.BASE_64.value) bf.encodeBase64(
                                        imageProxy.imageInfo.rotationDegrees
                                    ) else imagePathFile
                                    val mrzResult: MRZResult = when (mrzFormat) {
                                        MrzFormat.MRTD_TD1.value -> formatMrtdTd1Result(MRZCleaner.parseAndCleanMrtdTd1(mrz), imageString)
                                        else -> formatMrzResult(MRZCleaner.parseAndClean(mrz), imageString)
                                    }
                                    // record to json
                                    val gson = Gson()
                                    val jsonString = gson.toJson(mrzResult)
                                    getAnalyzerResult(AnalyzerType.MRZ, jsonString)
                                } catch (e: Exception) { // MrzParseException, IllegalArgumentException
                                    Log.d("$TAG/SmartScanner", e.toString())
                                }
                                getAnalyzerStat(
                                    mlStartTime,
                                    System.currentTimeMillis()
                                )
                                mrzBusy = false
                            }
                            .addOnFailureListener { e ->
                                Log.d("$TAG/SmartScanner", "TextRecognition: failure: ${e.message}")
                                if (getConnectionType() == 0) {
                                    modelText?.text = context.getString(R.string.connection_text)
                                } else {
                                    modelText?.text = context.getString(R.string.model_text)
                                }
                                modelText?.visibility = VISIBLE
                                getAnalyzerStat(
                                    mlStartTime,
                                    System.currentTimeMillis()
                                )
                                mrzBusy = false
                            }
                    } else {
                        thread(start = true) {
                            mrzBusy = true
                            // Tesseract
                            val matrix = Matrix()
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
                                    "$TAG/Tesseract",
                                    "Before cleaner: [${
                                        URLEncoder.encode(tessResult, "UTF-8")
                                            .replace("%3C", "<").replace("%0A", "↩")
                                    }]"
                                )
                                val mrz = MRZCleaner.clean(tessResult)
                                Log.d(
                                    "$TAG/Tesseract",
                                    "After cleaner = [${
                                        URLEncoder.encode(mrz, "UTF-8")
                                            .replace("%3C", "<")
                                            .replace("%0A", "↩")
                                    }]"
                                )
                                val imagePathFile = getImagePath()
                                bf.cacheImageToLocal(
                                    imagePathFile,
                                    imageProxy.imageInfo.rotationDegrees
                                )
                                val imageString = if (config?.imageResultType == ImageResultType.BASE_64.value) bf.encodeBase64(
                                    imageProxy.imageInfo.rotationDegrees
                                ) else imagePathFile
                                val record = MRZCleaner.parseAndClean(mrz)
                                val gson = Gson()
                                val jsonString = gson.toJson(
                                    MRZResult(
                                        imageString,
                                        record.code.toString(),
                                        record.code1.toShort(),
                                        record.code2.toShort(),
                                        record.dateOfBirth.toString().replace(Regex("[{}]"), ""),
                                        record.documentNumber.toString(),
                                        record.expirationDate.toString().replace(Regex("[{}]"), ""),
                                        record.format.toString(),
                                        record.givenNames,
                                        record.issuingCountry,
                                        record.nationality,
                                        record.sex.toString(),
                                        record.surname,
                                        record.toMrz()
                                    )
                                )
                                getAnalyzerResult(AnalyzerType.MRZ, jsonString)
                            } catch (e: Exception) { // MrzParseException, IllegalArgumentException
                                Log.d("$TAG/Tesseract", e.toString())
                            }
                            getAnalyzerStat(
                                mlStartTime,
                                System.currentTimeMillis()
                            )
                            mrzBusy = false
                        }
                    }
                }
            }
            imageProxy.close()
        }
    }

    private fun getImagePath(): String {
        val date = Calendar.getInstance().time
        val formatter = SimpleDateFormat("yyyyMMddHHmmss", Locale.ROOT)
        val currentDateTime = formatter.format(date)
        return "${context.cacheDir}/Scanner-$currentDateTime.jpg"
    }

    private fun getAnalyzerResult(analyzerType: AnalyzerType, result: String? = null, rawBytes: ByteArray? = null) {
        val data = Intent()
        runOnUiThread {
            when (analyzerType) {
                AnalyzerType.MRZ -> {
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
    }

    private fun getAnalyzerStat(startTime: Long, endTime: Long) {
        runOnUiThread {
            val analyzerTime = endTime - startTime
            "Frame processing time: $analyzerTime ms".also { mlkitText?.text = it }
            val scanTime = ((System.currentTimeMillis().toDouble() - startScanTime.toDouble()) / 1000)
            "Total scan time: $scanTime s".also { mlkitText?.text = it }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let { File(
            it,
            resources.getString(R.string.app_name)
        ).apply { mkdirs() } }
        return if (mediaDir != null && mediaDir.exists()) mediaDir else filesDir
    }

    private fun checkGooglePlayServices(showErrorDialog: Boolean = false): Boolean {
        val availability = GoogleApiAvailability.getInstance()
        val resultCode = availability.isGooglePlayServicesAvailable(this)
        if (resultCode != ConnectionResult.SUCCESS) {
            val dialog = availability.getErrorDialog(this, resultCode, 0)
            if (showErrorDialog) dialog.show()
            return false
        }
        return true
    }

    override fun onClick(view: View) {
        val id = view.id
        Log.d(TAG, "view: $id")
        if (id == R.id.close_button) {
            onBackPressed()
        } else if (id == R.id.flash_button) {
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