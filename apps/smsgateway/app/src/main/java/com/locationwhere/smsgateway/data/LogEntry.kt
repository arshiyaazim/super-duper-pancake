package com.locationwhere.smsgateway.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "logs")
data class LogEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val sender: String,
    val recipient: String,
    val body: String,
    val status: String, // SUCCESS, API_OK_SMS_FAILED, FAILED, SKIPPED, DUPLICATE
    val employeeCode: String? = null,
    val apiResponse: String? = null,
    val smsStatus: String? = null
)
