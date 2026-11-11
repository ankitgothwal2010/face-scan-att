package com.ankit.jarvisfacescanner.storage

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.util.UUID

class LogRepository(context: Context) {
    private val file = File(context.filesDir, "scan_logs.json")
    private val gson = Gson()

    @Synchronized
    fun getAllLogs(): List<ScanLog> {
        if (!file.exists()) return emptyList()
        return try {
            val json = file.readText()
            val type = object : TypeToken<List<ScanLog>>() {}.type
            val logs: List<ScanLog>? = gson.fromJson(json, type)
            logs?.sortedByDescending { it.timestamp } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    @Synchronized
    fun addLog(personName: String, isKnown: Boolean, confidence: Float, cameraSource: String): ScanLog {
        val current = getAllLogs().toMutableList()
        val confPercent = (confidence * 100).toInt()
        val log = ScanLog(
            id = UUID.randomUUID().toString(),
            personName = personName,
            isKnown = isKnown,
            confidencePercent = confPercent,
            cameraSource = cameraSource
        )
        // Keep the latest 500 records
        current.add(0, log)
        val trimmed = if (current.size > 500) current.take(500) else current
        file.writeText(gson.toJson(trimmed))
        return log
    }

    @Synchronized
    fun clearLogs() {
        if (file.exists()) file.delete()
    }
}
