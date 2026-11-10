package com.bodepudi.vexnotebooktracker.ui.dashboard

import com.bodepudi.vexnotebooktracker.utils.DebugLogger
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bodepudi.vexnotebooktracker.databinding.ItemUrlViewLogBinding
import com.bodepudi.vexnotebooktracker.network.UrlViewLogEntry

class UrlViewLogAdapter(
    private val entries: List<UrlViewLogEntry>,
    private val onEntryClick: (UrlViewLogEntry) -> Unit
) : RecyclerView.Adapter<UrlViewLogAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemUrlViewLogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(entries[position], position + 1) // Pass position + 1 for serial number
    }

    override fun getItemCount(): Int = entries.size

    inner class ViewHolder(private val binding: ItemUrlViewLogBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(entry: UrlViewLogEntry, serialNumber: Int) { // Accept serial number parameter
            binding.apply {
                // Set serial number
                tvSerialNumber.text = serialNumber.toString()

                tvDate.text = entry.date
                tvIpAddress.text = entry.ipAddress
                tvHostName.text = entry.hostName
                tvDeviceType.text = entry.deviceType

                root.setOnClickListener {
                    DebugLogger.d("UrlViewLogAdapter", "🔗 Item clicked at position $adapterPosition (Serial: $serialNumber)")
                    DebugLogger.d("UrlViewLogAdapter", "📋 Entry details: LogID=${entry.logId}, Date=${entry.date}, IP=${entry.ipAddress}")

                    onEntryClick(entry)
                }
            }
        }
    }
}