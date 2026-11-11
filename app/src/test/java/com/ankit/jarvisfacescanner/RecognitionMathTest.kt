package com.ankit.jarvisfacescanner

import com.ankit.jarvisfacescanner.storage.Profile
import kotlin.math.sqrt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecognitionMathTest {

    private fun cosineSimilarity(v1: FloatArray, v2: FloatArray): Float {
        var dot = 0f
        var normA = 0f
        var normB = 0f
        for (i in v1.indices) {
            dot += v1[i] * v2[i]
            normA += v1[i] * v1[i]
            normB += v2[i] * v2[i]
        }
        val denom = sqrt(normA) * sqrt(normB)
        return if (denom > 0f) dot / denom else 0f
    }

    private fun l2Normalize(vec: FloatArray): FloatArray {
        var sum = 0f
        for (v in vec) sum += v * v
        val norm = sqrt(sum)
        if (norm == 0f) return vec
        val out = FloatArray(vec.size)
        for (i in vec.indices) out[i] = vec[i] / norm
        return out
    }

    @Test
    fun testIdenticalVectors_yieldsMaximumSimilarity() {
        val vectorA = floatArrayOf(0.5f, 0.5f, 0.5f, 0.5f)
        val similarity = cosineSimilarity(vectorA, vectorA)
        assertEquals(1.0f, similarity, 0.0001f)
    }

    @Test
    fun testOrthogonalVectors_yieldsZeroSimilarity() {
        val vectorA = floatArrayOf(1.0f, 0.0f)
        val vectorB = floatArrayOf(0.0f, 1.0f)
        val similarity = cosineSimilarity(vectorA, vectorB)
        assertEquals(0.0f, similarity, 0.0001f)
    }

    @Test
    fun testL2NormalizationProducesUnitLength() {
        val rawVector = floatArrayOf(3.0f, 4.0f)
        val normalized = l2Normalize(rawVector)
        var sumSquares = 0f
        for (v in normalized) sumSquares += v * v
        assertEquals(1.0f, sumSquares, 0.0001f)
    }

    @Test
    fun testThresholdMatchingLogic() {
        val enrolledAnkit = floatArrayOf(0.9f, 0.1f, 0.0f)
        val targetCandidate = floatArrayOf(0.88f, 0.12f, 0.01f)
        val strangerCandidate = floatArrayOf(0.0f, 0.95f, 0.1f)

        val profile = Profile("1", "Ankit Gothwal", listOf(enrolledAnkit))

        val ankitSim = cosineSimilarity(targetCandidate, enrolledAnkit)
        val strangerSim = cosineSimilarity(strangerCandidate, enrolledAnkit)

        val threshold = 0.68f

        assertTrue("Target candidate must exceed 0.68 threshold", ankitSim >= threshold)
        assertTrue("Stranger candidate must fall below 0.68 threshold", strangerSim < threshold)
    }
}
