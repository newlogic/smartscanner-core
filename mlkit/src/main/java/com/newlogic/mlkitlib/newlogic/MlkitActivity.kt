package com.newlogic.mlkitlib.newlogic

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.util.Size
import android.view.MotionEvent
import android.view.View
import android.view.View.*
import android.view.Window
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.newlogic.mlkitlib.R
import com.newlogic.mlkitlib.innovatrics.barcode.BarcodeResult
import com.newlogic.mlkitlib.newlogic.config.Config
import com.newlogic.mlkitlib.newlogic.config.Fonts
import com.newlogic.mlkitlib.newlogic.config.Modes.*
import com.newlogic.mlkitlib.newlogic.extension.cacheImageToLocal
import com.newlogic.mlkitlib.newlogic.extension.empty
import com.newlogic.mlkitlib.newlogic.extension.getConnectionType
import com.newlogic.mlkitlib.newlogic.extension.toBitmap
import com.newlogic.mlkitlib.newlogic.utils.AnalyzerType
import com.newlogic.mlkitlib.newlogic.utils.CameraUtils.isLedFlashAvailable
import com.newlogic.mlkitlib.newlogic.utils.MLKitAnalyzer
import com.newlogic.mlkitlib.newlogic.utils.MRZCleaner
import com.newlogic.mlkitlib.newlogic.utils.MRZResult
import kotlinx.android.synthetic.main.activity_mrz.*
import kotlinx.android.synthetic.main.activity_mrz.view.*
import java.io.File
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs

class MLKitActivity : AppCompatActivity(), View.OnClickListener {

    companion object {
        const val TAG = "MLKitActivity"
        const val MLKIT_RESULT = "MLKIT_RESULT"
        const val CONFIG = "CONFIG"
        const val MODE = "MODE"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)

        fun defaultConfig() = Config(
            font = String.empty(),
            language = String.empty(),
            label = String.empty()
        )
    }

    private var x = 0f
    private var y = 0f
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var flashButton: View? = null
    private var closeButton: View? = null
    private var startScanTime: Long = 0
    private var mode: String? = null
    private var rectangle: View? = null

    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var modelLayoutView: View
    private lateinit var coordinatorLayoutView: View
    private lateinit var context: Context

    private val clickThreshold = 5

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_mrz)
        context = applicationContext
        // assign layout ids
        modelLayoutView = findViewById(R.id.viewLayout)
        coordinatorLayoutView =  findViewById(R.id.coordinatorLayout)
        flashButton = findViewById(R.id.flash_button)
        closeButton = findViewById(R.id.close_button)
        rectangle = findViewById(R.id.rectimage)
        // hide actionbar
        supportActionBar?.hide()
        supportActionBar?.setDisplayShowTitleEnabled(false)
        actionBar?.hide()
        actionBar?.setDisplayShowTitleEnabled(false)
        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
        // setup config for reader
        mode = intent.getStringExtra(MODE)
        setupConfiguration(intent.getParcelableExtra(CONFIG) ?: defaultConfig())
        // assign click listeners
        closeButton?.setOnClickListener(this)
        flashButton?.setOnClickListener(this)

        outputDirectory = getOutputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()
        val extDirPath: String = getExternalFilesDir(null)!!.absolutePath
        Log.d(TAG, "path: $extDirPath")
    }

    private fun setupConfiguration(config: Config) {
        flashButton?.visibility = if (isLedFlashAvailable(this)) VISIBLE else GONE
        captureLabelText.text = config.label
        if (config.font == Fonts.NOTO_SANS_ARABIC.value) captureLabelText.typeface = ResourcesCompat.getFont(this, R.font.notosansarabic_bold)
        val layoutParams = modelLayoutView.layoutParams as ConstraintLayout.LayoutParams
        when (mode) {
            PDF_417.value -> {
                layoutParams.dimensionRatio = "9:21"
                modelLayoutView.layoutParams = layoutParams
            }
            BARCODE.value, QR_CODE.value -> {
                layoutParams.marginStart = 64 // Approx. 30dp
                layoutParams.marginEnd = 64 // Approx. 30dp
                modelLayoutView.layoutParams = layoutParams
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
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

    private fun startCamera() {

        this.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            preview = Preview.Builder().build()
            var size = Size(480, 640)
            if (mode == PDF_417.value) size = Size(1080, 1920)
            imageAnalyzer = ImageAnalysis.Builder()
                .setTargetResolution(size)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, getMrzAnalyzer())
                }

            // Select back camera
            val cameraSelector = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()
                // Bind use cases to camera
                camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
                preview?.setSurfaceProvider(viewFinder.createSurfaceProvider())
                Log.d(TAG, "Measured size: ${viewFinder.width}x${viewFinder.height}")

                startScanTime = System.currentTimeMillis()
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun getAnalyzerResult(analyzerType: AnalyzerType, result: String) {
        runOnUiThread {
            when (analyzerType) {
                AnalyzerType.MLKIT -> {
                    Log.d(TAG, "Success from MLKit")
                    Log.d(TAG, "value: $result")
                    mlkitText.text = result
                }
                AnalyzerType.BARCODE -> {
                    Log.d(TAG, "Success from Barcode")
                    Log.d(TAG, "value: $result")
                }
            }
            val data = Intent()
            data.putExtra(MLKIT_RESULT, result)
            setResult(Activity.RESULT_OK, data)
            finish()
        }
    }

    private fun getAnalyzerStat (analyzerType: AnalyzerType, startTime: Long, endTime: Long) {
        runOnUiThread {
            val analyzerTime = endTime - startTime
            if (analyzerType == AnalyzerType.MLKIT) {
                mlkitMS.text = "Frame processing time: $analyzerTime ms"
                val scanTime = ((System.currentTimeMillis().toDouble() - startScanTime.toDouble()) / 1000)
                mlkitTime.text = "Total scan time: $scanTime s"
            }
        }
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    private fun getMrzAnalyzer(): MLKitAnalyzer {
        var barcodeBusy = false
        var mlkitBusy = false

        return MLKitAnalyzer { imageProxy ->
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val rot = imageProxy.imageInfo.rotationDegrees
                val bf = mediaImage.toBitmap(rot)
                val cropped = if (rot == 90 || rot == 270) Bitmap.createBitmap(bf, bf.width / 2, 0, bf.width / 2, bf.height)
                else Bitmap.createBitmap(bf, 0, bf.height / 2, bf.width, bf.height / 2)
                Log.d(TAG, "Bitmap: (${mediaImage.width}, ${mediaImage.height}) Cropped: (${cropped.width}, ${cropped.height}), Rotation: ${imageProxy.imageInfo.rotationDegrees}")

                //barcode, qr and pdf417
                if (!barcodeBusy && ((mode == PDF_417.value) || (mode == BARCODE.value)|| (mode == QR_CODE.value))) {
                    barcodeBusy = true
                    Log.d("$TAG/MLKit", "barcode: mode is $mode")
                    val start = System.currentTimeMillis()
                    var options: BarcodeScannerOptions = BarcodeScannerOptions.Builder()
                            .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                            .build()
                    when (mode) {
                        PDF_417.value -> {
                            options = BarcodeScannerOptions.Builder()
                                .setBarcodeFormats(Barcode.FORMAT_PDF417)
                                .build()
                        }
                        QR_CODE.value -> {
                            options = BarcodeScannerOptions.Builder()
                                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                                .build()
                        }
                        BARCODE.value -> {
                            options = BarcodeScannerOptions.Builder()
                                .setBarcodeFormats(
                                    Barcode.FORMAT_CODE_128,
                                    Barcode.FORMAT_CODE_39,
                                    Barcode.FORMAT_CODE_93,
                                    Barcode.FORMAT_CODABAR,
                                    Barcode.FORMAT_DATA_MATRIX,
                                    Barcode.FORMAT_EAN_13,
                                    Barcode.FORMAT_EAN_8,
                                    Barcode.FORMAT_ITF,
                                    Barcode.FORMAT_UPC_A,
                                    Barcode.FORMAT_UPC_E,
                                    Barcode.FORMAT_QR_CODE,
                                    Barcode.FORMAT_AZTEC
                                )
                                .build()
                        }
                    }
                    val image = InputImage.fromBitmap(bf, imageProxy.imageInfo.rotationDegrees)
                    val scanner = BarcodeScanning.getClient(options)
                    Log.d("$TAG/MLKit", "barcode: process")
                    scanner.process(image)
                            .addOnSuccessListener { barcodes ->
                                val timeRequired = System.currentTimeMillis() - start
                                val rawValue: String
                                val cornersString: String
                                Log.d("$TAG/MLKit", "barcode: success: $timeRequired ms")
                                if (barcodes.isNotEmpty()) {
                                    //                                val bounds = barcode.boundingBox
                                    val corners = barcodes[0].cornerPoints
                                    val builder = StringBuilder()
                                    if (corners != null) {
                                        for (corner in corners) {
                                            builder.append("${corner.x},${corner.y} ")
                                        }
                                    }
                                    cornersString = builder.toString()
                                    rawValue = barcodes[0].rawValue!!
                                    //                                val valueType = barcode.valueType
                                    val date = Calendar.getInstance().time
                                    val formatter = SimpleDateFormat("yyyyMMddHHmmss", Locale.ROOT)
                                    val currentDateTime = formatter.format(date)
                                    val imageCachePathFile = "${context.cacheDir}/Scanner-$currentDateTime.jpg"
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
                                    getAnalyzerResult(AnalyzerType.BARCODE, jsonString)
                                } else {
                                    Log.d("$TAG/MLKit", "barcode: nothing detected")
                                }
                                barcodeBusy = false
                            }
                            .addOnFailureListener { e ->
                                Log.d("$TAG/MLKit", "barcode: failure: ${e.message}")
                                barcodeBusy = false
                            }
                }
                //MRZ
                if (!mlkitBusy) {
                    mlkitBusy = true
                    val mlStartTime = System.currentTimeMillis()
                    val image = InputImage.fromBitmap(cropped, imageProxy.imageInfo.rotationDegrees)

                    // Pass image to an ML Kit Vision API
                    val recognizer = TextRecognition.getClient()
                    Log.d("$TAG/MLKit", "TextRecognition: process")
                    val start = System.currentTimeMillis()
                    recognizer.process(image)
                            .addOnSuccessListener { visionText ->
                                modelLayoutView.modelText.visibility = INVISIBLE
                                val timeRequired = System.currentTimeMillis() - start

                                Log.d("$TAG/MLKit", "TextRecognition: success: $timeRequired ms")
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
                                rectangle!!.isSelected = rawFullRead != ""

                                try {
                                    Log.d(
                                        "$TAG/MLKit",
                                        "Before cleaner: [${
                                            URLEncoder.encode(rawFullRead, "UTF-8")
                                                .replace("%3C", "<").replace("%0A", "↩")
                                        }]"
                                    )
                                    val mrz = MRZCleaner.clean(rawFullRead)
                                    Log.d(
                                        "$TAG/MLKit",
                                        "After cleaner = [${
                                            URLEncoder.encode(mrz, "UTF-8")
                                                .replace("%3C", "<").replace("%0A", "↩")
                                        }]"
                                    )
                                    val record = MRZCleaner.parseAndClean(mrz)
                                    val date = Calendar.getInstance().time
                                    val formatter = SimpleDateFormat("yyyyMMddHHmmss", Locale.ROOT)
                                    val currentDateTime = formatter.format(date)
                                    val imagePathFile = "${context.cacheDir}/Scanner-$currentDateTime.jpg"
                                    bf.cacheImageToLocal(imagePathFile, imageProxy.imageInfo.rotationDegrees)

                                    // record to json
                                    val gson = Gson()
                                    val jsonString = gson.toJson(MRZResult(
                                        imagePathFile,
                                        record.code.toString(),
                                        record.code1.toShort(),
                                        record.code2.toShort(),
                                        record.dateOfBirth?.toString()?.replace(Regex("[{}]"), ""),
                                        record.documentNumber.toString(),
                                        record.expirationDate?.toString()?.replace(Regex("[{}]"), ""),
                                        record.format.toString(),
                                        record.givenNames,
                                        record.issuingCountry,
                                        record.nationality,
                                        record.sex.toString(),
                                        record.surname,
                                        record.toMrz()
                                    ))
                                    getAnalyzerResult (AnalyzerType.MLKIT, jsonString)
                                } catch (e: Exception) { // MrzParseException, IllegalArgumentException
                                    Log.d("$TAG/MLKit", e.toString())
                                }
                                getAnalyzerStat (
                                    AnalyzerType.MLKIT,
                                    mlStartTime,
                                    System.currentTimeMillis()
                                )
                                mlkitBusy = false
                            }
                            .addOnFailureListener { e ->
                                Log.d("$TAG/MLKit", "TextRecognition: failure: ${e.message}")
                                if (getConnectionType() == 0) {
                                    modelLayoutView.modelText.text = context.getString(R.string.connection_text)
                                } else {
                                    modelLayoutView.modelText.text = context.getString(R.string.model_text)
                                }
                                modelLayoutView.modelText.visibility = VISIBLE
                                getAnalyzerStat (
                                    AnalyzerType.MLKIT,
                                    mlStartTime,
                                    System.currentTimeMillis()
                                )
                                mlkitBusy = false
                            }
                }
            }
            imageProxy.close()
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let { File(it, resources.getString(R.string.app_name)).apply { mkdirs() } }
        return if (mediaDir != null && mediaDir.exists()) mediaDir else filesDir
    }

    override fun onClick(view: View) {
        val id = view.id
        Log.d(TAG, "view: $id")
        if (id == R.id.close_button) {
            onBackPressed()
        } else if (id == R.id.flash_button) {
            if (flashButton!!.isSelected) {
                flashButton!!.isSelected = false
                camera?.cameraControl?.enableTorch(false)
            } else {
                flashButton!!.isSelected = true
                camera?.cameraControl?.enableTorch(true)
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
                        // Toast.makeText(this, "bottom2up swipe: $y1, $y2 -> $deltaY", Toast.LENGTH_SHORT).show()
                        debugLayout.visibility = VISIBLE
                    } else if (deltaY > minDistance) {
                        debugLayout.visibility = INVISIBLE
                    }
                }
            }
        }
        return super.onTouchEvent(event)
    }
}