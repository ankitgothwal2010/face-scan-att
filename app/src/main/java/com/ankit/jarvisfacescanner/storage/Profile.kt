package com.ankit.jarvisfacescanner.storage

data class Profile(
    val id: String,
    val name: String,
    val embeddings: List<FloatArray>,
    val createdAt: Long = System.currentTimeMillis()
)
