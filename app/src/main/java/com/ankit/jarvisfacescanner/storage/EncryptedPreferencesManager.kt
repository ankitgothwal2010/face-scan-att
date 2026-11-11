package com.ankit.jarvisfacescanner.storage

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.net.URLEncoder

class EncryptedPreferencesManager(context: Context) {

    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "jarvis_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        Log.e("EncryptedPreferences", "Falling back to private SharedPreferences", e)
        context.getSharedPreferences("jarvis_prefs_fallback", Context.MODE_PRIVATE)
    }

    fun saveTapoConfig(ip: String, port: Int, user: String, pass: String, stream: String) {
        // Strip any leading rtsp:// or http:// or trailing slashes if user typed them
        val cleanIp = ip.trim()
            .removePrefix("rtsp://")
            .removePrefix("http://")
            .substringBefore(":")
            .substringBefore("/")

        val cleanStream = stream.trim().removePrefix("/")

        prefs.edit()
            .putString("tapo_ip", cleanIp)
            .putInt("tapo_port", port)
            .putString("tapo_user", user.trim())
            .putString("tapo_pass", pass)
            .putString("tapo_stream", if (cleanStream.isNotBlank()) cleanStream else "stream2")
            .apply()
    }

    fun getRtspUrl(): String {
        val ip = prefs.getString("tapo_ip", "")?.trim() ?: ""
        val port = prefs.getInt("tapo_port", 554)
        val user = prefs.getString("tapo_user", "")?.trim() ?: ""
        val pass = prefs.getString("tapo_pass", "") ?: ""
        val stream = prefs.getString("tapo_stream", "stream2")?.trim() ?: "stream2"

        if (ip.isBlank() || user.isBlank() || pass.isBlank()) return ""

        // URL encode username & password to prevent syntax errors with @, #, $, :, etc.
        val encodedUser = URLEncoder.encode(user, "UTF-8")
        val encodedPass = URLEncoder.encode(pass, "UTF-8")

        return "rtsp://$encodedUser:$encodedPass@$ip:$port/$stream"
    }

    fun getThreshold(): Float = prefs.getFloat("matching_threshold", 0.68f)

    fun setThreshold(threshold: Float) {
        prefs.edit().putFloat("matching_threshold", threshold).apply()
    }

    fun getCameraSource(): String = prefs.getString("camera_source", "PHONE") ?: "PHONE"

    fun setCameraSource(source: String) {
        prefs.edit().putString("camera_source", source).apply()
    }

    fun getTapoIp(): String = prefs.getString("tapo_ip", "") ?: ""
    fun getTapoUser(): String = prefs.getString("tapo_user", "") ?: ""
    fun getTapoPass(): String = prefs.getString("tapo_pass", "") ?: ""
    fun getTapoStream(): String = prefs.getString("tapo_stream", "stream2") ?: "stream2"
}
