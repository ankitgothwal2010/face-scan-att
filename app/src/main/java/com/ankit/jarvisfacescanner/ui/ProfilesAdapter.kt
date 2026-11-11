package com.ankit.jarvisfacescanner.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ankit.jarvisfacescanner.databinding.ItemProfileBinding
import com.ankit.jarvisfacescanner.storage.Profile

class ProfilesAdapter(
    private var profiles: List<Profile>,
    private val onDeleteClicked: (Profile) -> Unit
) : RecyclerView.Adapter<ProfilesAdapter.ProfileViewHolder>() {

    class ProfileViewHolder(val binding: ItemProfileBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfileViewHolder {
        val binding = ItemProfileBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProfileViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProfileViewHolder, position: Int) {
        val profile = profiles[position]
        holder.binding.tvProfileName.text = profile.name
        holder.binding.tvSampleCount.text = "EMBEDDINGS: ${profile.embeddings.size} SAMPLES"
        holder.binding.btnDelete.setOnClickListener { onDeleteClicked(profile) }
    }

    override fun getItemCount(): Int = profiles.size

    fun updateData(newProfiles: List<Profile>) {
        this.profiles = newProfiles
        notifyDataSetChanged()
    }
}
