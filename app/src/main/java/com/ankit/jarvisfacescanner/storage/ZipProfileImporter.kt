package com.ankit.jarvisfacescanner.storage

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import com.ankit.jarvisfacescanner.ml.FaceDetectorEngine
import com.ankit.jarvisfacescanner.ml.FaceRecognitionEngine
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ZipProfileImporter(
    private val context: Context,
    private val detector: FaceDetectorEngine,
    private val recognizer: FaceRecognitionEngine,
    private val repository: ProfileRepository
) {
    data class ImportSummary(
        val totalImagesFound: Int,
        val facesProcessed: Int,
        val profilesCreated: Int,
        val errors: List<String>
    )

    suspend fun importFromZip(zipUri: Uri): ImportSummary = withContext(Dispatchers.IO) {
        val tempDir = File(context.cacheDir, "imported_faces_${System.currentTimeMillis()}").apply { mkdirs() }
        val errors = mutableListOf<String>()
        var totalImages = 0
        var processedFaces = 0

        try {
            context.contentResolver.openInputStream(zipUri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        val file = File(tempDir, entry.name)
                        if (entry.isDirectory) {
                            file.mkdirs()
                        } else {
                            file.parentFile?.mkdirs()
                            FileOutputStream(file).use { fos -> zis.copyTo(fos) }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            }

            val personEmbeddingsMap = mutableMapOf<String, MutableList<FloatArray>>()

            tempDir.walkTopDown().filter { it.isFile && isImageFile(it) }.forEach { imgFile ->
                totalImages++
                val personName = imgFile.parentFile?.name ?: "Unknown"

                if (personName.equals("known_faces", ignoreCase = true) || personName.isBlank()) {
                    errors.add("Skipped image without person directory: ${imgFile.name}")
                    return@forEach
                }

                try {
                    val bitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
                    if (bitmap == null) {
                        errors.add("Corrupted image file: ${imgFile.name}")
                        return@forEach
                    }

                    val detectedFaces = detector.detectFaces(bitmap)
                    if (detectedFaces.isEmpty()) {
                        errors.add("No face found in ${personName}/${imgFile.name}")
                        return@forEach
                    }

                    val face = detectedFaces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }!!
                    val cropped = recognizer.cropFace(bitmap, face.boundingBox)
                    if (cropped != null) {
                        val embedding = recognizer.extractEmbedding(cropped)
                        personEmbeddingsMap.getOrPut(personName) { mutableListOf() }.add(embedding)
                        processedFaces++
                    }
                } catch (e: Exception) {
                    errors.add("Error processing ${imgFile.name}: ${e.message}")
                }
            }

            var createdProfiles = 0
            for ((personName, embeddings) in personEmbeddingsMap) {
                if (embeddings.isNotEmpty()) {
                    repository.addProfile(personName, embeddings)
                    createdProfiles++
                }
            }

            ImportSummary(totalImages, processedFaces, createdProfiles, errors)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    private fun isImageFile(file: File): Boolean {
        val ext = file.extension.lowercase()
        return ext == "jpg" || ext == "jpeg" || ext == "png" || ext == "webp"
    }
}
