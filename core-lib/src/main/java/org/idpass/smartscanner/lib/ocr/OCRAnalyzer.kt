package org.idpass.smartscanner.lib.ocr

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.android.material.progressindicator.LinearProgressIndicator
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
import org.idpass.smartscanner.lib.scanner.config.ScanIDOCRCountryOptions
import org.idpass.smartscanner.lib.scanner.config.ScanIDOCRField
import org.idpass.smartscanner.lib.utils.BitmapUtils
import org.idpass.smartscanner.lib.utils.extension.cacheImagePath
import org.idpass.smartscanner.lib.utils.extension.cacheImageToLocal
import org.idpass.smartscanner.lib.utils.extension.cropCenter
import org.idpass.smartscanner.lib.utils.extension.encodeBase64
import org.idpass.smartscanner.lib.utils.extension.isImageBlur
import org.idpass.smartscanner.lib.utils.extension.setBrightness
import org.idpass.smartscanner.lib.utils.extension.setContrast
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

open class OCRAnalyzer(
    override val activity: Activity,
    override val intent: Intent,
    override val mode: String = Modes.OCR.value,
    private val imageResultType: String,
    private val isShowGuide: Boolean = false,
    private val regex: String? = "",
    private val type: String? = "",
    private val manualCapture: Boolean = false,
    private val analyzeStart: Long = 0,
    // ID scan parameters (optional — when provided, enables anchor-based ID scanning)
    private val scanIDOCRCountryOptions: List<ScanIDOCRCountryOptions>? = null,
    private val showDebugOverlay: Boolean = false
) : BaseImageAnalyzer() {

    // --- Simple OCR state (accessed from both UI and analyzer threads) ---
    @Volatile private var captured = false
    @Volatile private var startAnalyze = false

    // --- ID scan state (only used when scanIDOCRCountryOptions is non-null) ---
    private var detectedCountry: ScanIDOCRCountryOptions? = null
    private var validFramesCollected = 0
    private var totalAttempts = 0
    private val accumulatedResults = mutableMapOf<String, MutableMap<String, Int>>()
    private val isProcessing = AtomicBoolean(false)
    private var lastAnalyzedTimestamp = 0L
    private val textRecognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    // Shared executor for cell-level OCR callbacks. Hoisted out of the per-field loop
    // so each frame doesn't spin up N executors. Lazy so non-ID-scan modes pay nothing.
    private val cellExecutor by lazy { java.util.concurrent.Executors.newSingleThreadExecutor() }

    // Cached view references for ID scan (initialized on UI thread in init)
    private var rectImageView: View? = null
    private var headerTextView: TextView? = null
    private var subHeaderTextView: TextView? = null
    private var progressIndicator: LinearProgressIndicator? = null
    private var debugOverlayView: DebugOverlayView? = null

    private val isIDScanMode get() = scanIDOCRCountryOptions != null && scanIDOCRCountryOptions.isNotEmpty()

    private companion object {
        const val FRAME_INTERVAL_MS = 500L
        // rect_image border colors for scanning states
        const val COLOR_NEUTRAL = 0xFFDDDDDD.toInt()    // Light grey (default)
        const val COLOR_ACTIVE = 0xFF1976D2.toInt()     // Material Blue 700
        const val COLOR_COMPLETE = 0xFF4CAF50.toInt()   // Material Green 500
        const val RECT_CORNER_RADIUS = 8f               // dp — matches rectangle_white.xml
        const val RECT_STROKE_WIDTH = 6f                // dp — matches rectangle_white.xml
    }

    init {
        if (isIDScanMode) {
            activity.runOnUiThread {
                // Cache view references
                rectImageView = activity.findViewById(R.id.rect_image)
                headerTextView = activity.findViewById(R.id.capture_header_text)
                subHeaderTextView = activity.findViewById(R.id.capture_sub_header_text)
                progressIndicator = activity.findViewById(R.id.scan_progress)

                // Hide the scanner_overlay guide and its semi-transparent overlay —
                // for ID scan we use rect_image (the preview border) as the visual frame.
                activity.findViewById<View>(R.id.guide_layout)?.visibility = View.GONE

                // Set initial state
                headerTextView?.text = "Position your ID card"
                subHeaderTextView?.text = "Align the front of your ID within the frame"
                progressIndicator?.visibility = View.VISIBLE
                progressIndicator?.isIndeterminate = true

                // Clear the static src drawable so our programmatic background is visible
                (rectImageView as? ImageView)?.setImageDrawable(null)

                // Set initial rect color
                setRectColor(COLOR_NEUTRAL)

                // Add debug overlay if enabled
                if (showDebugOverlay) {
                    val root = activity.findViewById<ViewGroup>(R.id.view_layout)
                    if (root != null) {
                        debugOverlayView = DebugOverlayView(activity).also {
                            root.addView(it, ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            ))
                            it.elevation = 20f
                        }
                    }
                }
            }
        }
    }

    /**
     * Changes the rect_image border color for scanning feedback.
     */
    private fun setRectColor(color: Int) {
        val rect = rectImageView ?: return
        val density = activity.resources.displayMetrics.density
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = RECT_CORNER_RADIUS * density
            setStroke((RECT_STROKE_WIDTH * density).toInt(), color)
            setColor(0x00000000) // transparent fill
        }
        activity.runOnUiThread {
            rect.background = drawable
        }
    }

    @SuppressLint("UnsafeExperimentalUsageError", "UnsafeOptInUsageError")
    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        // Branch: ID scan mode vs simple OCR mode
        if (isIDScanMode) {
            analyzeIDScan(imageProxy)
            return
        }

        // --- Simple OCR logic (unchanged) ---
        captured = !manualCapture
        Handler(Looper.getMainLooper()).postDelayed({
            startAnalyze = true
        }, analyzeStart)
        val bitmap = BitmapUtils.getBitmap(imageProxy)
        if (bitmap == null) {
            imageProxy.close()
            return
        }
        bitmap.let { rawBf ->
            val rotation = imageProxy.imageInfo.rotationDegrees
            // Increase brightness and contrast for clearer image to be processed
            val bf = (rawBf.setContrast(1.1F) ?: rawBf).let { it.setBrightness(3F) ?: it }

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
                    val koeff = rotatedBF.width.toFloat() / viewFinder.width.toFloat()
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

    // ==================== ID Scan Logic ====================

    /**
     * Transforms text to match the given regex pattern by extracting all substrings
     * that match and joining them.
     */
    private fun transformTextToMatchRegex(text: String, regexPattern: String): String {
        return try {
            val regex = Regex(regexPattern)
            regex.findAll(text).map { it.value }.joinToString("").trim()
        } catch (e: Exception) {
            Log.e(SmartScannerActivity.TAG, "Invalid regex pattern: $regexPattern", e)
            text.trim()
        }
    }

    /**
     * Fuzzy match: checks if any substring of [text] is close to [anchor]
     * using Levenshtein distance. Allows up to ~30% character errors.
     */
    private fun fuzzyContains(text: String, anchor: String): Boolean {
        if (anchor.isEmpty()) return false
        val t = text.lowercase()
        val a = anchor.lowercase()
        // Exact substring match first (fast path)
        if (t.contains(a)) return true
        // Sliding window fuzzy match
        val maxDist = (a.length * 0.35f).toInt().coerceAtLeast(1)
        val windowSize = a.length
        if (t.length < windowSize) {
            return levenshtein(t, a) <= maxDist
        }
        for (i in 0..t.length - windowSize) {
            val window = t.substring(i, (i + windowSize + 1).coerceAtMost(t.length))
            if (levenshtein(window, a) <= maxDist) return true
        }
        return false
    }

    private fun levenshtein(a: String, b: String): Int {
        val m = a.length
        val n = b.length
        var prev = IntArray(n + 1) { it }
        var curr = IntArray(n + 1)
        for (i in 1..m) {
            curr[0] = i
            for (j in 1..n) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                curr[j] = minOf(curr[j - 1] + 1, prev[j] + 1, prev[j - 1] + cost)
            }
            val tmp = prev; prev = curr; curr = tmp
        }
        return prev[n]
    }

    /**
     * Removes the best fuzzy match of [anchor] from [text] and returns the remainder.
     * Returns null if no fuzzy match is found or if the remainder is too short.
     */
    private fun fuzzyStripAnchor(text: String, anchor: String): String? {
        if (anchor.isEmpty() || text.length <= anchor.length) return null
        val t = text.lowercase()
        val a = anchor.lowercase()
        // Exact match first (fast path)
        val exactIdx = t.indexOf(a)
        if (exactIdx >= 0) {
            val remainder = text.removeRange(exactIdx, exactIdx + anchor.length).trim()
            return if (remainder.length > 1) remainder else null
        }
        // Fuzzy sliding window — find best match position
        val maxDist = (a.length * 0.35f).toInt().coerceAtLeast(1)
        var bestIdx = -1
        var bestLen = a.length
        var bestDist = Int.MAX_VALUE
        for (i in 0..(t.length - a.length)) {
            for (len in a.length..(a.length + 1).coerceAtMost(t.length - i)) {
                val window = t.substring(i, i + len)
                val dist = levenshtein(window, a)
                if (dist < bestDist) {
                    bestDist = dist
                    bestIdx = i
                    bestLen = len
                }
            }
        }
        if (bestDist > maxDist || bestIdx < 0) return null
        val remainder = text.removeRange(bestIdx, bestIdx + bestLen).trim()
        return if (remainder.length > 1) remainder else null
    }

    @ExperimentalGetImage
    private fun analyzeIDScan(imageProxy: ImageProxy) {
        val currentTimestamp = System.currentTimeMillis()
        if (isProcessing.get() || (currentTimestamp - lastAnalyzedTimestamp < FRAME_INTERVAL_MS)) {
            imageProxy.close()
            return
        }
        isProcessing.set(true)
        lastAnalyzedTimestamp = currentTimestamp

        val bitmap = BitmapUtils.getBitmap(imageProxy)
        if (bitmap == null) {
            isProcessing.set(false)
            imageProxy.close()
            return
        }

        // Increase brightness and contrast for clearer OCR
        val enhanced = (bitmap.setContrast(1.1F) ?: bitmap).let { it.setBrightness(3F) ?: it }

        val viewFinder = activity.findViewById<View>(R.id.view_finder)
        // Activity is portrait-locked; camera sensor is landscape-mounted.
        // Always rotate 90° to get a portrait bitmap regardless of device rotation reporting.
        var rotatedBF = BitmapUtils.rotateImage(enhanced, 90)

        // Crop to exactly what's visible in the viewfinder (= what's inside rect_image).
        // CameraX PreviewView uses FILL_CENTER: scales bitmap so the smaller dimension
        // fills the view, then center-crops the overflow.
        var croppedBitmap: Bitmap? = null
        val bw = rotatedBF.width.toFloat()
        val bh = rotatedBF.height.toFloat()
        val vw = viewFinder.width.toFloat()
        val vh = viewFinder.height.toFloat()

        if (vw > 0 && vh > 0) {
            val scale = maxOf(vw / bw, vh / bh)
            val visibleW = (vw / scale).toInt()
            val visibleH = (vh / scale).toInt()
            val cropX = ((bw.toInt() - visibleW) / 2).coerceAtLeast(0)
            val cropY = ((bh.toInt() - visibleH) / 2).coerceAtLeast(0)
            val cropW = visibleW.coerceAtMost(rotatedBF.width - cropX)
            val cropH = visibleH.coerceAtMost(rotatedBF.height - cropY)

            if (cropW > 0 && cropH > 0) {
                try {
                    croppedBitmap = Bitmap.createBitmap(rotatedBF, cropX, cropY, cropW, cropH)
                } catch (e: Exception) {
                    Log.e(SmartScannerActivity.TAG, "Error cropping bitmap: ${e.message}")
                    croppedBitmap = rotatedBF
                }
            } else {
                croppedBitmap = rotatedBF
            }
        } else {
            croppedBitmap = rotatedBF
        }

        if (croppedBitmap == null) {
            Log.w(SmartScannerActivity.TAG, "ID Scan: Cropped bitmap is null")
            isProcessing.set(false)
            imageProxy.close()
            return
        }

        val image = InputImage.fromBitmap(croppedBitmap, 0)
        val countries = scanIDOCRCountryOptions!!

        textRecognizer.process(image)
            .addOnSuccessListener { visionText ->
                if (activity.isFinishing || activity.isDestroyed) return@addOnSuccessListener

                val allLines = visionText.textBlocks.flatMap { it.lines }
                Log.d(SmartScannerActivity.TAG, "ID Scan: found ${allLines.size} lines")
                val debugBoxes = if (showDebugOverlay) mutableListOf<DebugOverlayView.DebugBox>() else null

                // Debug: draw all detected text lines in yellow
                if (debugBoxes != null) {
                    allLines.forEach { line ->
                        line.boundingBox?.let { bb ->
                            debugBoxes.add(DebugOverlayView.DebugBox(bb, 0xAAFFFF00.toInt(), line.text))
                        }
                    }
                }

                if (detectedCountry == null) {
                    // Search for country
                    for (country in countries) {
                        if (visionText.text.contains(country.countryLabelKey, ignoreCase = true)) {
                            detectedCountry = country
                            validFramesCollected = 0
                            totalAttempts = 0
                            accumulatedResults.clear()
                            Log.d(SmartScannerActivity.TAG, "Country Detected: ${country.countryLabelKey}")
                            activity.runOnUiThread {
                                setRectColor(COLOR_ACTIVE)
                                headerTextView?.text = "ID Detected"
                                subHeaderTextView?.text = country.countryLabelKey
                            }
                            break
                        }
                    }

                    if (detectedCountry == null) {
                        // Debug: show detected text even when searching for country
                        if (debugBoxes != null && debugOverlayView != null) {
                            val bmpW = croppedBitmap.width.toFloat()
                            val bmpH = croppedBitmap.height.toFloat()
                            val overlay = debugOverlayView!!
                            activity.runOnUiThread {
                                val ow = overlay.width.toFloat()
                                val oh = overlay.height.toFloat()
                                if (ow > 0 && oh > 0) {
                                    val mapped = debugBoxes.map { box ->
                                        DebugOverlayView.DebugBox(
                                            android.graphics.Rect(
                                                (box.rect.left * ow / bmpW).toInt(),
                                                (box.rect.top * oh / bmpH).toInt(),
                                                (box.rect.right * ow / bmpW).toInt(),
                                                (box.rect.bottom * oh / bmpH).toInt()
                                            ), box.color, box.label
                                        )
                                    }
                                    overlay.updateBoxes(mapped)
                                }
                            }
                        }
                        activity.runOnUiThread {
                            setRectColor(COLOR_NEUTRAL)
                        }
                    }
                } else {
                    val country = detectedCountry!!
                    val maxFrames = country.maxFrames ?: 2
                    totalAttempts++

                    // Check if all anchors are found in this frame
                    val allAnchorsFound = country.ocrData.all { field ->
                        allLines.any { fuzzyContains(it.text, field.anchorValue) }
                    }

                    if (allAnchorsFound) {
                        // Cell-OCR tasks are gathered during the per-field sweep and awaited once below,
                        // so the camera analyzer thread blocks for a single overall timeout instead of
                        // serialising N per-field waits.
                        data class PendingCellOcr(
                            val field: ScanIDOCRField,
                            val searchRect: Rect,
                            val task: com.google.android.gms.tasks.Task<com.google.mlkit.vision.text.Text>
                        )
                        val pendingCellOcr = mutableListOf<PendingCellOcr>()

                        for (field in country.ocrData) {
                            val anchors = allLines.filter { fuzzyContains(it.text, field.anchorValue) }
                            for (anchor in anchors) {
                                val ab = anchor.boundingBox ?: continue

                                // Debug: draw anchor in cyan
                                debugBoxes?.add(DebugOverlayView.DebugBox(ab, 0xFF00FFFF.toInt(), "ANCHOR: ${field.label}"))

                                // Define search box relative to anchor
                                val sl = ab.left + (ab.width() * field.searchBox.x)
                                val st = ab.top + (ab.height() * field.searchBox.y)
                                val sr = sl + (ab.width() * field.searchBox.width)
                                val sb = st + (ab.height() * field.searchBox.height)

                                // Clamp to bitmap bounds
                                val cropLeft = sl.toInt().coerceIn(0, croppedBitmap.width)
                                val cropTop = st.toInt().coerceIn(0, croppedBitmap.height)
                                val cropRight = sr.toInt().coerceIn(cropLeft, croppedBitmap.width)
                                val cropBottom = sb.toInt().coerceIn(cropTop, croppedBitmap.height)
                                val cropW = cropRight - cropLeft
                                val cropH = cropBottom - cropTop

                                val searchRect = Rect(cropLeft, cropTop, cropRight, cropBottom)

                                // Debug: draw search box in red
                                debugBoxes?.add(DebugOverlayView.DebugBox(searchRect, 0xFFFF0000.toInt(), "SEARCH: ${field.label}"))

                                val useCellOcr = country.cellLevelOcr != false

                                if (useCellOcr && cropW > 10 && cropH > 10) {
                                    val cellBitmap = Bitmap.createBitmap(croppedBitmap, cropLeft, cropTop, cropW, cropH)
                                    val cellImage = InputImage.fromBitmap(cellBitmap, 0)
                                    pendingCellOcr.add(
                                        PendingCellOcr(field, searchRect, textRecognizer.process(cellImage))
                                    )
                                } else {
                                    // Line-intersection: find best matching line from full-image OCR
                                    val bestLine = allLines
                                        .filter { it != anchor && it.boundingBox != null && Rect.intersects(it.boundingBox!!, searchRect) }
                                        .maxByOrNull { line ->
                                            val lb = line.boundingBox!!
                                            val intersection = Rect()
                                            if (intersection.setIntersect(searchRect, lb)) {
                                                (intersection.width() * intersection.height()).toFloat()
                                            } else 0f
                                        }

                                    var targetText: String? = null

                                    // Check if value is on the same line as the anchor (fuzzy match)
                                    val strippedText = fuzzyStripAnchor(anchor.text, field.anchorValue)
                                    if (strippedText != null) {
                                        targetText = strippedText
                                    }

                                    if (targetText == null && bestLine != null) {
                                        targetText = bestLine.text
                                        bestLine.boundingBox?.let { bb ->
                                            debugBoxes?.add(DebugOverlayView.DebugBox(bb, 0xFF00FF00.toInt(), "VALUE: $targetText"))
                                        }
                                    }

                                    if (targetText != null) {
                                        val processedText = if (field.regex != null) {
                                            transformTextToMatchRegex(targetText, field.regex)
                                        } else targetText.trim()

                                        if (processedText.isNotEmpty()) {
                                            val map = accumulatedResults.getOrPut(field.label) { mutableMapOf() }
                                            map[processedText] = map.getOrDefault(processedText, 0) + 1
                                        }
                                    }
                                }
                            }
                        }

                        // Await all enqueued cell-OCR tasks in parallel under a single overall timeout.
                        if (pendingCellOcr.isNotEmpty()) {
                            val cellLatch = java.util.concurrent.CountDownLatch(pendingCellOcr.size)
                            val cellResults = arrayOfNulls<String>(pendingCellOcr.size)
                            pendingCellOcr.forEachIndexed { idx, p ->
                                p.task
                                    .addOnSuccessListener(cellExecutor) { result ->
                                        cellResults[idx] = result.text
                                            .replace('\n', ' ')
                                            .replace("  ", " ")
                                            .trim()
                                        cellLatch.countDown()
                                    }
                                    .addOnFailureListener(cellExecutor) {
                                        cellLatch.countDown()
                                    }
                            }
                            try {
                                cellLatch.await(2000, java.util.concurrent.TimeUnit.MILLISECONDS)
                            } catch (_: Exception) {}

                            pendingCellOcr.forEachIndexed { idx, p ->
                                val cellText = cellResults[idx] ?: return@forEachIndexed
                                if (cellText.isEmpty()) return@forEachIndexed
                                val processedText = if (p.field.regex != null) {
                                    transformTextToMatchRegex(cellText, p.field.regex)
                                } else cellText

                                Log.d(SmartScannerActivity.TAG, "Cell OCR [${p.field.label}]: raw=\"$cellText\" after regex=\"$processedText\"")
                                debugBoxes?.add(DebugOverlayView.DebugBox(p.searchRect, 0xFF00FF00.toInt(), "VALUE: $processedText"))

                                if (processedText.isNotEmpty()) {
                                    val map = accumulatedResults.getOrPut(p.field.label) { mutableMapOf() }
                                    map[processedText] = map.getOrDefault(processedText, 0) + 1
                                }
                            }
                        }

                        validFramesCollected++
                    }

                    // Debug: map bitmap-space boxes to screen coordinates and draw
                    if (debugBoxes != null && debugOverlayView != null) {
                        val bmpW = croppedBitmap.width.toFloat()
                        val bmpH = croppedBitmap.height.toFloat()
                        val overlay = debugOverlayView!!
                        activity.runOnUiThread {
                            val ow = overlay.width.toFloat()
                            val oh = overlay.height.toFloat()
                            if (ow > 0 && oh > 0) {
                                val scaleX = ow / bmpW
                                val scaleY = oh / bmpH
                                val mapped = debugBoxes.map { box ->
                                    DebugOverlayView.DebugBox(
                                        android.graphics.Rect(
                                            (box.rect.left * scaleX).toInt(),
                                            (box.rect.top * scaleY).toInt(),
                                            (box.rect.right * scaleX).toInt(),
                                            (box.rect.bottom * scaleY).toInt()
                                        ),
                                        box.color,
                                        box.label
                                    )
                                }
                                overlay.updateBoxes(mapped)
                            }
                        }
                    }

                    // Update UI
                    activity.runOnUiThread {
                        setRectColor(COLOR_ACTIVE)
                        headerTextView?.text = "Reading your ID..."
                        subHeaderTextView?.text = "Hold steady \u2014 $validFramesCollected/$maxFrames"
                        progressIndicator?.isIndeterminate = false
                        progressIndicator?.setProgressCompat(
                            validFramesCollected * 100 / maxFrames, true
                        )
                    }

                    if (validFramesCollected >= maxFrames) {
                        // Process final result
                        val rawFields = mutableMapOf<String, String>()
                        val finalValues = ArrayList<String>()

                        accumulatedResults.forEach { (label, counts) ->
                            val best = counts.maxByOrNull { it.value }
                            if (best != null) {
                                rawFields[label] = best.key
                                finalValues.add(best.key)
                            }
                        }

                        val finalFields = normalizeFields(rawFields, country)

                        activity.runOnUiThread {
                            setRectColor(COLOR_COMPLETE)
                            headerTextView?.text = "Done!"
                            subHeaderTextView?.text = ""
                            progressIndicator?.setProgressCompat(100, true)
                        }

                        processResult(
                            result = finalValues.firstOrNull() ?: "",
                            array = finalValues,
                            bitmap = croppedBitmap ?: rotatedBF,
                            rotation = 0,
                            fields = finalFields,
                            rawFields = rawFields,
                            country = country.countryLabelKey
                        )
                    } else if (totalAttempts > maxFrames * 3) {
                        // Verify country is still visible
                        if (!visionText.text.contains(country.countryLabelKey, ignoreCase = true)) {
                            detectedCountry = null
                            validFramesCollected = 0
                            totalAttempts = 0
                            accumulatedResults.clear()
                            activity.runOnUiThread {
                                setRectColor(COLOR_NEUTRAL)
                                headerTextView?.text = "Position your ID card"
                                subHeaderTextView?.text = "Align the front of your ID within the frame"
                                progressIndicator?.isIndeterminate = true
                            }
                            Log.d(SmartScannerActivity.TAG, "Lost Country - Resetting")
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                if (!activity.isFinishing && !activity.isDestroyed) {
                    Log.e(SmartScannerActivity.TAG, "Text recognition failed", e)
                }
            }
            .addOnCompleteListener {
                isProcessing.set(false)
            }

        imageProxy.close()
    }

    // ==================== Field Normalization ====================

    /**
     * Parses a date string into yyyy-MM-dd based on the country's dateFormat.
     * Handles: dd/MM/yyyy, dd-MM-yyyy, ddMMMyyyy (with month name lookup).
     * Returns the original value if parsing fails.
     */
    private fun parseDate(value: String, dateFormat: String?, monthMap: Map<String, String>): String {
        if (dateFormat == null) return value
        try {
            return when (dateFormat) {
                "dd/MM/yyyy" -> {
                    val parts = value.split("/")
                    if (parts.size == 3) "${parts[2]}-${parts[1]}-${parts[0]}" else value
                }
                "dd-MM-yyyy" -> {
                    val parts = value.split("-")
                    if (parts.size == 3) "${parts[2]}-${parts[1]}-${parts[0]}" else value
                }
                "ddMMMyyyy" -> {
                    // e.g. "19SEP2000" or "25NOV1980"
                    val match = Regex("^(\\d{2})([A-Za-z]{3})(\\d{4})$").find(value) ?: return value
                    val day = match.groupValues[1]
                    val monthStr = match.groupValues[2].uppercase()
                    val year = match.groupValues[3]
                    val month = monthMap[monthStr] ?: return value
                    "$year-$month-$day"
                }
                else -> value
            }
        } catch (e: Exception) {
            return value
        }
    }

    /**
     * Remaps extracted fields from display labels to standardized keys,
     * and normalizes date and gender values based on the country config.
     */
    private fun normalizeFields(
        rawFields: Map<String, String>,
        country: ScanIDOCRCountryOptions
    ): Map<String, String> {
        val labelToField = country.ocrData.associateBy { it.label }
        val normalized = mutableMapOf<String, String>()

        // Month name mapping for manual date parsing (covers Spanish and English)
        val monthMap = mapOf(
            "ENE" to "01", "FEB" to "02", "MAR" to "03", "ABR" to "04",
            "MAY" to "05", "JUN" to "06", "JUL" to "07", "AGO" to "08",
            "SEP" to "09", "OCT" to "10", "NOV" to "11", "DIC" to "12",
            "JAN" to "01", "AUG" to "08", "DEC" to "12"
        )

        for ((label, value) in rawFields) {
            val field = labelToField[label]
            val outputKey = field?.key ?: label

            val normalizedValue = when (outputKey) {
                "dateOfBirth", "expiryDate" -> {
                    parseDate(value.trim(), country.dateFormat, monthMap)
                }
                "gender" -> {
                    val genderMap = country.genderMap
                    if (genderMap != null) {
                        genderMap[value.trim()] ?: genderMap[value.trim().uppercase()] ?: value
                    } else value
                }
                else -> value
            }

            normalized[outputKey] = normalizedValue
        }

        return normalized
    }

    // ==================== Result Processing ====================

    internal open fun processResult(
        result: String,
        array: ArrayList<String>,
        bitmap: Bitmap,
        rotation: Int,
        fields: Map<String, String>? = null,
        rawFields: Map<String, String>? = null,
        country: String? = null
    ) {
        val imagePath = activity.cacheImagePath()
        // ID scan: save the full bitmap (already cropped to guide region).
        // Simple OCR: crop to center square for backwards compatibility.
        val outputBitmap = if (fields != null) bitmap else bitmap.cropCenter()
        outputBitmap.cacheImageToLocal(
            imagePath,
            rotation,
            if (imageResultType == ImageResultType.BASE_64.value) 40 else 80
        )
        val imageFile = File(imagePath)
        val imageString =
            if (imageResultType == ImageResultType.BASE_64.value) imageFile.encodeBase64() else imagePath

        val ocrResult = OCRResult(
            imagePath = imagePath,
            image = imageString,
            regex = regex ?: OCRChecker.DEFAULT_REGEX_STRING,
            valuesArray = array,
            value = result,
            type = type,
            fields = fields,
            rawFields = rawFields,
            country = country
        )

        when (intent.action) {
            ScannerConstants.IDPASS_SMARTSCANNER_OCR_INTENT,
            ScannerConstants.IDPASS_SMARTSCANNER_ODK_OCR_INTENT -> {
                sendBundleResult(ocrResult = ocrResult)
            }

            else -> {
                val jsonString = Gson().toJson(ocrResult)
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
        Log.d(SmartScannerActivity.TAG, "Success from OCR (bundle)")
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
        // Include extracted fields (ID scan results) in the bundle
        ocrResult?.fields?.forEach { (key, value) ->
            bundle.putString("field_$key", value)
        }
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

    // ==================== Debug Overlay ====================

    /**
     * Debug overlay that draws colored boxes on the camera preview:
     * - YELLOW: all detected OCR text lines
     * - CYAN: anchor matches
     * - RED: search box regions (where the analyzer looks for field values)
     * - GREEN: extracted field value text
     */
    class DebugOverlayView(context: android.content.Context) : View(context) {
        data class DebugBox(val rect: android.graphics.Rect, val color: Int, val label: String? = null)

        private var boxes: List<DebugBox> = emptyList()
        private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            textSize = 28f
            setShadowLayer(2f, 1f, 1f, 0xFF000000.toInt())
        }

        fun updateBoxes(newBoxes: List<DebugBox>) {
            boxes = newBoxes
            postInvalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            boxes.forEach { box ->
                strokePaint.color = box.color
                canvas.drawRect(box.rect, strokePaint)
                box.label?.let { label ->
                    canvas.drawText(label, box.rect.left.toFloat(), box.rect.top.toFloat() - 4f, textPaint)
                }
            }
        }
    }
}
