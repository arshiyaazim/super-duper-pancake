package com.example.location_where.data

data class DeviceRegisterRequest(
    val deviceModel: String,
    val manufacturer: String,
    val androidVersion: String,
    val appVersion: String,
    val fcmToken: String? = null,
    val isAdminActive: Boolean = false
)
