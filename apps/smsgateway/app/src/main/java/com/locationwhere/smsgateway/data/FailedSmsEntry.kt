package com.locationwhere.smsgateway.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "failed_sms")
data class FailedSmsEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val sender: String,
    val recipient: String,
    val body: String,
    val retryCount: Int = 0,
    val nextRetryAt: Long = System.currentTimeMillis() + 60_000 // 1 min initial delay
)
