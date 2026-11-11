package com.ankit.jarvisfacescanner

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.ankit.jarvisfacescanner.databinding.ActivityLogsBinding
import com.ankit.jarvisfacescanner.storage.LogRepository
import com.ankit.jarvisfacescanner.ui.LogsAdapter

class LogsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLogsBinding
    private lateinit var repository: LogRepository
    private lateinit var adapter: LogsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLogsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = LogRepository(this)
        val logs = repository.getAllLogs()

        adapter = LogsAdapter(logs)
        binding.rvLogs.layoutManager = LinearLayoutManager(this)
        binding.rvLogs.adapter = adapter

        updateUi(logs.size)

        binding.btnClearLogs.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("CONFIRM PURGE")
                .setMessage("Delete all scan and login history records?")
                .setPositiveButton("DELETE ALL") { _, _ ->
                    repository.clearLogs()
                    adapter.updateData(emptyList())
                    updateUi(0)
                }
                .setNegativeButton("CANCEL", null)
                .show()
        }
    }

    private fun updateUi(count: Int) {
        binding.tvLogCount.text = "ENTRIES: $count"
        if (count == 0) {
            binding.tvEmptyLogs.visibility = View.VISIBLE
            binding.rvLogs.visibility = View.GONE
        } else {
            binding.tvEmptyLogs.visibility = View.GONE
            binding.rvLogs.visibility = View.VISIBLE
        }
    }
}
