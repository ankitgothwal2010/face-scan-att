package com.ankit.jarvisfacescanner.storage

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ScanLog(
    val id: String,
    val personName: String,
    val isKnown: Boolean,
    val confidencePercent: Int,
    val cameraSource: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    val formattedTime: String
        get() {
            val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm:ss a", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
}
