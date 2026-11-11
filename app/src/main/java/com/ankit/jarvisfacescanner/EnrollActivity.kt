package com.ankit.jarvisfacescanner

import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ankit.jarvisfacescanner.camera.CameraManager
import com.ankit.jarvisfacescanner.databinding.ActivityEnrollBinding
import com.ankit.jarvisfacescanner.ml.FaceDetectorEngine
import com.ankit.jarvisfacescanner.ml.FaceRecognitionEngine
import com.ankit.jarvisfacescanner.storage.ProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EnrollActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEnrollBinding
    private lateinit var detector: FaceDetectorEngine
    private lateinit var recognizer: FaceRecognitionEngine
    private lateinit var repository: ProfileRepository

    private var cameraManager: CameraManager? = null
    private var latestBitmap: Bitmap? = null
    private val collectedEmbeddings = mutableListOf<FloatArray>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEnrollBinding.inflate(layoutInflater)
        setContentView(binding.root)

        detector = FaceDetectorEngine()
        recognizer = FaceRecognitionEngine(this)
        repository = ProfileRepository(this)

        cameraManager = CameraManager(this, this, binding.enrollPreview) { bmp ->
            latestBitmap = bmp
        }
        cameraManager?.startCamera()

        binding.btnCaptureSample.setOnClickListener { captureSample() }
        binding.btnSaveProfile.setOnClickListener { saveProfile() }
    }

    private fun captureSample() {
        val bmp = latestBitmap
        if (bmp == null) {
            Toast.makeText(this, "Camera initializing...", Toast.LENGTH_SHORT).show()
            return
        }

        if (!recognizer.isModelLoaded) {
            Toast.makeText(this, "Recognition model asset missing! Check app assets.", Toast.LENGTH_LONG).show()
            return
        }

        lifecycleScope.launch(Dispatchers.Default) {
            val faces = detector.detectFaces(bmp)
            if (faces.isEmpty()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@EnrollActivity, "No face detected in frame", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }
            if (faces.size > 1) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@EnrollActivity, "Multiple faces detected! Frame only one person.", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            val face = faces[0]
            val cropped = recognizer.cropFace(bmp, face.boundingBox)
            if (cropped != null) {
                val embedding = recognizer.extractEmbedding(cropped)
                collectedEmbeddings.add(embedding)

                withContext(Dispatchers.Main) {
                    binding.tvSampleCounter.text = "SAMPLES CAPTURED: ${collectedEmbeddings.size} / 5"
                    Toast.makeText(this@EnrollActivity, "Sample ${collectedEmbeddings.size} captured", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveProfile() {
        val name = binding.etPersonName.text.toString().trim()
        if (name.isBlank()) {
            Toast.makeText(this, "Please enter a valid name", Toast.LENGTH_SHORT).show()
            return
        }
        if (collectedEmbeddings.isEmpty()) {
            Toast.makeText(this, "Capture at least one face sample first", Toast.LENGTH_SHORT).show()
            return
        }

        repository.addProfile(name, collectedEmbeddings)
        Toast.makeText(this, "Profile '$name' enrolled successfully", Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraManager?.stopCamera()
        detector.stop()
        recognizer.close()
    }
}
