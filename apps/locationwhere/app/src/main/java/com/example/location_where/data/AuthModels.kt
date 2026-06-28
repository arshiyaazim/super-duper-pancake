package com.example.location_where.data

data class ApiResponse<T>(
    val success: Boolean,
    val data: T?,
    val error: String?,
    val message: String? = null
)

data class LoginRequest(
    val employeeCode: String,
    val password: String,
    val deviceId: String,
    val fcmToken: String? = null
)

data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val employee: EmployeeInfo
)

data class EmployeeInfo(
    val id: String,
    val fullName: String,
    val employeeCode: String,
    val department: String?,
    val designation: String?
)

data class RefreshRequest(
    val refreshToken: String
)

data class TokenResponse(
    val accessToken: String
)

data class Command(
    val id: String,
    val commandType: String,
    val commandPayload: String?
)

data class LocationUpdate(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val batteryLevel: Int,
    val address: String? = null
)

data class SimAlert(
    val previousSim: String?,
    val newSim: String,
    val previousIMSI: String?,
    val newIMSI: String,
    val deviceInfo: DeviceInfoMap
)

data class DeviceInfoMap(
    val deviceModel: String,
    val androidVersion: String
)
