package com.ankit.jarvisfacescanner

import android.os.Bundle
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ankit.jarvisfacescanner.databinding.ActivitySettingsBinding
import com.ankit.jarvisfacescanner.storage.EncryptedPreferencesManager

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: EncryptedPreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = EncryptedPreferencesManager(this)
        loadExistingSettings()

        binding.sbThreshold.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = progress / 100.0f
                binding.tvThresholdLabel.text = "MATCH THRESHOLD: ${"%.2f".format(value)}"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.btnSaveSettings.setOnClickListener {
            val source = if (binding.rbTapo.isChecked) "TAPO" else "PHONE"
            prefs.setCameraSource(source)

            val ip = binding.etTapoIp.text.toString().trim()
            val user = binding.etTapoUser.text.toString().trim()
            val pass = binding.etTapoPass.text.toString().trim()
            val stream = binding.etTapoStream.text.toString().trim()
            val threshold = binding.sbThreshold.progress / 100.0f

            prefs.saveTapoConfig(ip, 554, user, pass, stream)
            prefs.setThreshold(threshold)

            Toast.makeText(this, "Settings saved securely to Keystore", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun loadExistingSettings() {
        if (prefs.getCameraSource() == "TAPO") {
            binding.rbTapo.isChecked = true
        } else {
            binding.rbPhone.isChecked = true
        }

        binding.etTapoIp.setText(prefs.getTapoIp())
        binding.etTapoUser.setText(prefs.getTapoUser())
        binding.etTapoPass.setText(prefs.getTapoPass())
        binding.etTapoStream.setText(prefs.getTapoStream())

        val currentThresh = (prefs.getThreshold() * 100).toInt()
        binding.sbThreshold.progress = currentThresh
        binding.tvThresholdLabel.text = "MATCH THRESHOLD: ${"%.2f".format(prefs.getThreshold())}"
    }
}
