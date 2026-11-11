package com.ankit.jarvisfacescanner

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.ankit.jarvisfacescanner.databinding.ActivityProfilesBinding
import com.ankit.jarvisfacescanner.ml.FaceDetectorEngine
import com.ankit.jarvisfacescanner.ml.FaceRecognitionEngine
import com.ankit.jarvisfacescanner.storage.ProfileRepository
import com.ankit.jarvisfacescanner.storage.ZipProfileImporter
import com.ankit.jarvisfacescanner.ui.ProfilesAdapter
import kotlinx.coroutines.launch

class ProfilesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfilesBinding
    private lateinit var repository: ProfileRepository
    private lateinit var adapter: ProfilesAdapter

    private val openZipLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            importZip(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfilesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = ProfileRepository(this)

        adapter = ProfilesAdapter(repository.getAllProfiles()) { profile ->
            repository.deleteProfile(profile.id)
            refreshList()
        }

        binding.rvProfiles.layoutManager = LinearLayoutManager(this)
        binding.rvProfiles.adapter = adapter

        binding.btnImportZip.setOnClickListener {
            openZipLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed"))
        }

        binding.btnClearAll.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("CONFIRM PURGE")
                .setMessage("Delete all biometric profiles? This action cannot be undone.")
                .setPositiveButton("DELETE ALL") { _, _ ->
                    repository.deleteAll()
                    refreshList()
                }
                .setNegativeButton("CANCEL", null)
                .show()
        }
    }

    private fun refreshList() {
        adapter.updateData(repository.getAllProfiles())
    }

    private fun importZip(uri: android.net.Uri) {
        binding.importProgressBar.visibility = View.VISIBLE
        binding.btnImportZip.isEnabled = false

        lifecycleScope.launch {
            val detector = FaceDetectorEngine()
            val recognizer = FaceRecognitionEngine(this@ProfilesActivity)
            val importer = ZipProfileImporter(this@ProfilesActivity, detector, recognizer, repository)

            try {
                val summary = importer.importFromZip(uri)
                val msg = """
                    Import complete!
                    Processed: ${summary.facesProcessed} faces
                    Profiles: ${summary.profilesCreated}
                    Errors: ${summary.errors.size}
                """.trimIndent()
                AlertDialog.Builder(this@ProfilesActivity)
                    .setTitle("IMPORT RESULTS")
                    .setMessage(msg)
                    .setPositiveButton("OK", null)
                    .show()
                refreshList()
            } catch (e: Exception) {
                Toast.makeText(this@ProfilesActivity, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.importProgressBar.visibility = View.GONE
                binding.btnImportZip.isEnabled = true
                detector.stop()
                recognizer.close()
            }
        }
    }
}
