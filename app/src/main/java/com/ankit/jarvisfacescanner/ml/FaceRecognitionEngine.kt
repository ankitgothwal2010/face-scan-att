package com.ankit.jarvisfacescanner.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import com.ankit.jarvisfacescanner.storage.Profile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import org.tensorflow.lite.Interpreter

class FaceRecognitionEngine(context: Context) {

    companion object {
        const val MODEL_FILENAME = "models/mobilefacenet.tflite"
        const val INPUT_SIZE = 112
        const val EMBEDDING_DIM = 192
        private const val IMAGE_MEAN = 127.5f
        private const val IMAGE_STD = 128.0f
        private const val TAG = "FaceRecognitionEngine"
    }

    private var interpreter: Interpreter? = null
    var isModelLoaded: Boolean = false
        private set

    private val inputBuffer: ByteBuffer = ByteBuffer.allocateDirect(1 * INPUT_SIZE * INPUT_SIZE * 3 * 4).apply {
        order(ByteOrder.nativeOrder())
    }
    private val outputArray = Array(1) { FloatArray(EMBEDDING_DIM) }

    init {
        loadModel(context)
    }

    private fun loadModel(context: Context) {
        try {
            // Robust asset reading: reads raw bytes directly into direct ByteBuffer
            // Avoids openFd FileNotFoundException when assets are compressed
            val bytes = context.assets.open(MODEL_FILENAME).use { it.readBytes() }
            val modelBuffer = ByteBuffer.allocateDirect(bytes.size).apply {
                order(ByteOrder.nativeOrder())
                put(bytes)
                rewind()
            }

            val options = Interpreter.Options().apply {
                setNumThreads(4)
                setUseNNAPI(false)
            }
            interpreter = Interpreter(modelBuffer, options)
            isModelLoaded = true
            Log.i(TAG, "MobileFaceNet TFLite model loaded successfully! (${bytes.size} bytes)")
        } catch (e: Exception) {
            isModelLoaded = false
            Log.e(TAG, "Failed to load MobileFaceNet model: ${e.message}. Offline recognition disabled.", e)
        }
    }

    @Synchronized
    fun extractEmbedding(faceBitmap: Bitmap): FloatArray {
        val interp = interpreter
        if (interp == null || !isModelLoaded) {
            return FloatArray(EMBEDDING_DIM) // Return zero vector if model missing
        }

        val resized = Bitmap.createScaledBitmap(faceBitmap, INPUT_SIZE, INPUT_SIZE, true)
        inputBuffer.rewind()

        val intValues = IntArray(INPUT_SIZE * INPUT_SIZE)
        resized.getPixels(intValues, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)

        var pixel = 0
        for (i in 0 until INPUT_SIZE) {
            for (j in 0 until INPUT_SIZE) {
                val value = intValues[pixel++]
                inputBuffer.putFloat((((value shr 16) and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                inputBuffer.putFloat((((value shr 8) and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                inputBuffer.putFloat(((value and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
            }
        }

        interp.run(inputBuffer, outputArray)

        val rawEmbedding = outputArray[0].clone()
        return l2Normalize(rawEmbedding)
    }

    fun cropFace(source: Bitmap, boundingBox: Rect): Bitmap? {
        val marginX = (boundingBox.width() * 0.15f).toInt()
        val marginY = (boundingBox.height() * 0.15f).toInt()

        val left = max(0, boundingBox.left - marginX)
        val top = max(0, boundingBox.top - marginY)
        val right = min(source.width, boundingBox.right + marginX)
        val bottom = min(source.height, boundingBox.bottom + marginY)

        val width = right - left
        val height = bottom - top

        if (width <= 0 || height <= 0) return null
        return Bitmap.createBitmap(source, left, top, width, height)
    }

    fun findBestMatch(
        embedding: FloatArray,
        profiles: List<Profile>,
        threshold: Float
    ): Pair<String, Float> {
        if (!isModelLoaded) {
            return Pair("MODEL NOT LOADED", 0.0f)
        }

        var bestMatch = "UNKNOWN PERSON"
        var maxSimilarity = -1.0f

        for (profile in profiles) {
            for (sample in profile.embeddings) {
                val similarity = cosineSimilarity(embedding, sample)
                if (similarity > maxSimilarity) {
                    maxSimilarity = similarity
                    if (similarity >= threshold) {
                        bestMatch = profile.name
                    }
                }
            }
        }

        val clampedConfidence = max(0f, min(maxSimilarity, 1.0f))
        return Pair(bestMatch, clampedConfidence)
    }

    fun cosineSimilarity(v1: FloatArray, v2: FloatArray): Float {
        var dotProduct = 0.0f
        var normA = 0.0f
        var normB = 0.0f
        for (i in v1.indices) {
            dotProduct += v1[i] * v2[i]
            normA += v1[i] * v1[i]
            normB += v2[i] * v2[i]
        }
        val denominator = sqrt(normA) * sqrt(normB)
        return if (denominator > 0f) dotProduct / denominator else 0f
    }

    fun l2Normalize(vector: FloatArray): FloatArray {
        var sum = 0.0f
        for (v in vector) sum += v * v
        val norm = sqrt(sum)
        if (norm == 0f) return vector
        val normalized = FloatArray(vector.size)
        for (i in vector.indices) {
            normalized[i] = vector[i] / norm
        }
        return normalized
    }

    fun close() {
        interpreter?.close()
        interpreter = null
        isModelLoaded = false
    }
}
