package com.ankit.jarvisfacescanner.storage

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.util.UUID

class ProfileRepository(private val context: Context) {
    private val file = File(context.filesDir, "profiles.json")
    private val gson = Gson()

    @Synchronized
    fun getAllProfiles(): List<Profile> {
        if (!file.exists()) return emptyList()
        return try {
            val json = file.readText()
            val type = object : TypeToken<List<Profile>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    @Synchronized
    fun addProfile(name: String, embeddings: List<FloatArray>): Profile {
        val current = getAllProfiles().toMutableList()
        val existingIndex = current.indexOfFirst { it.name.equals(name, ignoreCase = true) }

        val profile = if (existingIndex >= 0) {
            val existing = current[existingIndex]
            val merged = (existing.embeddings + embeddings)
            Profile(existing.id, existing.name, merged)
        } else {
            Profile(UUID.randomUUID().toString(), name.trim(), embeddings)
        }

        if (existingIndex >= 0) {
            current[existingIndex] = profile
        } else {
            current.add(profile)
        }

        saveAll(current)
        return profile
    }

    @Synchronized
    fun deleteProfile(id: String) {
        val current = getAllProfiles().filterNot { it.id == id }
        saveAll(current)
    }

    @Synchronized
    fun deleteAll() {
        if (file.exists()) file.delete()
    }

    private fun saveAll(profiles: List<Profile>) {
        val json = gson.toJson(profiles)
        file.writeText(json)
    }
}
