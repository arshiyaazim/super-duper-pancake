package com.locationwhere.smsgateway.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.locationwhere.smsgateway.R
import com.locationwhere.smsgateway.data.LogEntry
import com.locationwhere.smsgateway.databinding.ItemLogBinding
import com.locationwhere.smsgateway.util.PhoneUtil
import java.text.SimpleDateFormat
import java.util.*

class LogAdapter : ListAdapter<LogEntry, LogAdapter.LogViewHolder>(LogDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val binding = ItemLogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LogViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class LogViewHolder(private val binding: ItemLogBinding) : RecyclerView.ViewHolder(binding.root) {
        private val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())

        fun bind(log: LogEntry) {
            val context = binding.root.context
            binding.tvSender.text = context.getString(R.string.log_sender_format, PhoneUtil.maskPhoneNumber(log.sender))
            binding.tvTime.text = dateFormat.format(Date(log.timestamp))
            binding.tvStatus.text = log.status
            binding.tvBody.text = log.body
            binding.tvEmployeeCode.text = log.employeeCode ?: "N/A"

            val statusColor = when (log.status) {
                "SUCCESS" -> "#4CAF50".toColorInt()
                "DUPLICATE" -> "#FF9800".toColorInt()
                "FAILED" -> "#F44336".toColorInt()
                "API_OK_SMS_FAILED" -> "#FFC107".toColorInt()
                else -> Color.GRAY
            }
            binding.statusIndicator.setBackgroundColor(statusColor)
            binding.tvStatus.setTextColor(statusColor)

            binding.detailsLayout.isGone = true
            binding.root.setOnClickListener {
                binding.detailsLayout.isVisible = !binding.detailsLayout.isVisible
            }

            binding.tvApiResponse.text = context.getString(R.string.log_api_response_format, log.apiResponse ?: "None")
            binding.tvSmsStatus.text = context.getString(R.string.log_sms_status_format, log.smsStatus ?: "None")
        }
    }

    class LogDiffCallback : DiffUtil.ItemCallback<LogEntry>() {
        override fun areItemsTheSame(oldItem: LogEntry, newItem: LogEntry): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: LogEntry, newItem: LogEntry): Boolean = oldItem == newItem
    }
}
