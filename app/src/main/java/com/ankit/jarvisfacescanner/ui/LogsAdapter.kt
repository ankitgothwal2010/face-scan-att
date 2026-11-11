package com.ankit.jarvisfacescanner.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ankit.jarvisfacescanner.databinding.ItemScanLogBinding
import com.ankit.jarvisfacescanner.storage.ScanLog

class LogsAdapter(
    private var logs: List<ScanLog>
) : RecyclerView.Adapter<LogsAdapter.LogViewHolder>() {

    class LogViewHolder(val binding: ItemScanLogBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val binding = ItemScanLogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LogViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        val item = logs[position]
        holder.binding.tvLogName.text = item.personName
        holder.binding.tvLogTime.text = "TIME: ${item.formattedTime}"
        holder.binding.tvLogTelemetry.text = "CONFIDENCE: ${item.confidencePercent}% // SOURCE: ${item.cameraSource}"

        if (item.isKnown) {
            holder.binding.tvLogBadge.text = "[AUTHORIZED]"
            holder.binding.tvLogBadge.setTextColor(Color.parseColor("#00E676")) // Green
            holder.binding.tvLogName.setTextColor(Color.parseColor("#E0F7FA"))
        } else {
            holder.binding.tvLogBadge.text = "[UNKNOWN DETECTED]"
            holder.binding.tvLogBadge.setTextColor(Color.parseColor("#FF1744")) // Red
            holder.binding.tvLogName.setTextColor(Color.parseColor("#FF8A80"))
        }
    }

    override fun getItemCount(): Int = logs.size

    fun updateData(newLogs: List<ScanLog>) {
        this.logs = newLogs
        notifyDataSetChanged()
    }
}
