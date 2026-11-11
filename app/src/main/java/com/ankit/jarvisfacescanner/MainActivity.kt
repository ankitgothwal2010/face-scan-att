package com.ankit.jarvisfacescanner

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.ankit.jarvisfacescanner.camera.CameraManager
import com.ankit.jarvisfacescanner.camera.RtspStreamManager
import com.ankit.jarvisfacescanner.databinding.ActivityMainBinding
import com.ankit.jarvisfacescanner.ml.FaceDetectorEngine
import com.ankit.jarvisfacescanner.ml.FaceRecognitionEngine
import com.ankit.jarvisfacescanner.ml.RecognitionResult
import com.ankit.jarvisfacescanner.storage.EncryptedPreferencesManager
import com.ankit.jarvisfacescanner.storage.LogRepository
import com.ankit.jarvisfacescanner.storage.ProfileRepository
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefsManager: EncryptedPreferencesManager
    private lateinit var repository: ProfileRepository
    private lateinit var logRepository: LogRepository

    private var cameraManager: CameraManager? = null
    private var rtspStreamManager: RtspStreamManager? = null

    private lateinit var detector: FaceDetectorEngine
    private lateinit var recognizer: FaceRecognitionEngine

    private val isScanning = AtomicBoolean(false)
    private val isProcessingFrame = AtomicBoolean(false)
    private val lastLoggedMap = ConcurrentHashMap<String, Long>()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            setupActiveCamera()
        } else {
            Toast.makeText(this, "Camera permission needed for phone camera", Toast.LENGTH_SHORT).show()
            if (prefsManager.getCameraSource() != "TAPO") {
                binding.tvStatusBanner.text = "PERMISSION REQUIRED // CAMERA BLOCKED"
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            prefsManager = EncryptedPreferencesManager(this)
            repository = ProfileRepository(this)
            logRepository = LogRepository(this)
            detector = FaceDetectorEngine()
            recognizer = FaceRecognitionEngine(this)

            setupListeners()
            checkPermissionsAndStart()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Notice: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupListeners() {
        binding.btnToggleScan.setOnClickListener {
            if (isScanning.get()) {
                isScanning.set(false)
                binding.btnToggleScan.text = getString(R.string.btn_start_scan)
                binding.reticleOverlay.updateResults(emptyList(), 0, 0)
                binding.tvTelemetry.text = "SCANNER PAUSED // OFFLINE ENGINE IDLE"
            } else {
                isScanning.set(true)
                binding.btnToggleScan.text = getString(R.string.btn_stop_scan)
                binding.tvTelemetry.text = "SCANNING ACTIVE // AWAITING TARGET..."
            }
        }

        binding.btnLogs.setOnClickListener {
            startActivity(Intent(this, LogsActivity::class.java))
        }

        binding.btnEnroll.setOnClickListener {
            startActivity(Intent(this, EnrollActivity::class.java))
        }

        binding.btnProfiles.setOnClickListener {
            startActivity(Intent(this, ProfilesActivity::class.java))
        }

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun checkPermissionsAndStart() {
        if (prefsManager.getCameraSource() == "TAPO") {
            setupActiveCamera()
        } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            setupActiveCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun setupActiveCamera() {
        val mode = prefsManager.getCameraSource()
        if (mode == "TAPO") {
            binding.cameraPreview.visibility = View.GONE
            binding.rtspTextureView.visibility = View.VISIBLE
            cameraManager?.stopCamera()

            val rtspUrl = prefsManager.getRtspUrl()
            if (rtspUrl.isBlank()) {
                binding.tvStatusBanner.text = "TAPO CONFIG MISSING // OPEN SETTINGS"
                return
            }

            rtspStreamManager?.stopStream()
            rtspStreamManager = RtspStreamManager(
                this,
                binding.rtspTextureView,
                onFrameSampled = { bitmap -> handleIncomingFrame(bitmap) },
                onStatusChanged = { status -> runOnUiThread { binding.tvStatusBanner.text = status } }
            )
            rtspStreamManager?.startStream(rtspUrl)
        } else {
            binding.rtspTextureView.visibility = View.GONE
            binding.cameraPreview.visibility = View.VISIBLE
            rtspStreamManager?.stopStream()

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                cameraManager?.stopCamera()
                cameraManager = CameraManager(this, this, binding.cameraPreview) { bitmap ->
                    handleIncomingFrame(bitmap)
                }
                cameraManager?.startCamera()
                if (recognizer.isModelLoaded) {
                    binding.tvStatusBanner.text = "PHONE CAMERA ONLINE // HELIO G70"
                } else {
                    binding.tvStatusBanner.text = "CAMERA READY // MODEL ASSET MISSING"
                }
            } else {
                binding.tvStatusBanner.text = "PERMISSION REQUIRED // CAMERA BLOCKED"
            }
        }
    }

    private fun handleIncomingFrame(bitmap: Bitmap) {
        if (!isScanning.get() || isProcessingFrame.get()) return

        isProcessingFrame.set(true)
        val startTime = System.currentTimeMillis()

        lifecycleScope.launch(Dispatchers.Default) {
            try {
                val faces = detector.detectFaces(bitmap)
                val profiles = repository.getAllProfiles()
                val threshold = prefsManager.getThreshold()
                val results = mutableListOf<RecognitionResult>()

                for (face in faces) {
                    val cropped = recognizer.cropFace(bitmap, face.boundingBox)
                    if (cropped != null) {
                        val embedding = recognizer.extractEmbedding(cropped)
                        val (name, similarity) = recognizer.findBestMatch(embedding, profiles, threshold)
                        val isKnown = name != "UNKNOWN PERSON" && name != "MODEL NOT LOADED"

                        results.add(
                            RecognitionResult(
                                boundingBox = face.boundingBox,
                                matchedName = name,
                                confidenceEstimate = similarity,
                                isKnown = isKnown,
                                embedding = embedding
                            )
                        )

                        // Record access log with 12s debouncing
                        logAccessEvent(name, isKnown, similarity, prefsManager.getCameraSource())
                    }
                }

                val latency = System.currentTimeMillis() - startTime
                withContext(Dispatchers.Main) {
                    binding.reticleOverlay.updateResults(results, bitmap.width, bitmap.height)
                    if (results.isNotEmpty()) {
                        val primary = results[0]
                        binding.tvTelemetry.text =
                            "LATENCY: ${latency}ms | TARGET: ${primary.matchedName} | EST.CONF: ${(primary.confidenceEstimate * 100).toInt()}%"
                    } else {
                        binding.tvTelemetry.text = "LATENCY: ${latency}ms | TARGETS: 0 DETECTED | ENGINE: OK"
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isProcessingFrame.set(false)
            }
        }
    }

    private fun logAccessEvent(name: String, isKnown: Boolean, similarity: Float, source: String) {
        val now = System.currentTimeMillis()
        val lastTime = lastLoggedMap[name] ?: 0L
        if (now - lastTime > 12_000L) { // 12 seconds cooldown per person to prevent duplicate log spam
            lastLoggedMap[name] = now
            val displaySource = if (source == "TAPO") "TAPO C200" else "PHONE CAMERA"
            logRepository.addLog(name, isKnown, similarity, displaySource)
        }
    }

    override fun onResume() {
        super.onResume()
        setupActiveCamera()
    }

    override fun onPause() {
        super.onPause()
        cameraManager?.stopCamera()
        rtspStreamManager?.stopStream()
    }

    override fun onDestroy() {
        super.onDestroy()
        detector.stop()
        recognizer.close()
    }
}
