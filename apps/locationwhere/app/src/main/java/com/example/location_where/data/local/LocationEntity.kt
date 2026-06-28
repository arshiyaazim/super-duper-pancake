package com.example.location_where.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "location_logs")
data class LocationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val batteryLevel: Int,
    val address: String?,
    val timestamp: Long,
    val isSynced: Boolean = false
)
