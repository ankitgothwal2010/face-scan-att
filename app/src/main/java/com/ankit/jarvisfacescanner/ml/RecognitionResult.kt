package com.ankit.jarvisfacescanner.ml

import android.graphics.Rect

data class RecognitionResult(
    val boundingBox: Rect,
    val matchedName: String,
    val confidenceEstimate: Float,
    val isKnown: Boolean,
    val embedding: FloatArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as RecognitionResult
        return boundingBox == other.boundingBox && matchedName == other.matchedName
    }

    override fun hashCode(): Int {
        var result = boundingBox.hashCode()
        result = 31 * result + matchedName.hashCode()
        return result
    }
}
