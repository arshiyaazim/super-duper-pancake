package com.example.location_where.api

import com.example.location_where.data.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @POST("api/v1/auth/mobile/login")
    suspend fun login(@Body body: LoginRequest): Response<ApiResponse<LoginResponse>>

    @POST("api/v1/auth/refresh")
    suspend fun refreshToken(@Body body: RefreshRequest): Response<ApiResponse<TokenResponse>>

    @POST("api/v1/location/update")
    suspend fun updateLocation(@Body body: LocationUpdate): Response<ApiResponse<Unit>>

    @POST("api/v1/sim/change-alert")
    suspend fun reportSimChange(@Body body: SimAlert): Response<ApiResponse<Unit>>

    @POST("api/v1/calls/log")
    suspend fun uploadCallLog(@Body body: CallLogRequest): Response<ApiResponse<CallLogResponse>>

    @Multipart
    @POST("api/v1/calls/upload-recording")
    suspend fun uploadCallRecording(
        @Part recording: MultipartBody.Part,
        @Part("callLogId") callLogId: RequestBody,
        @Part("checksum") checksum: RequestBody
    ): Response<ApiResponse<Unit>>

    @GET("api/v1/device/commands/pending")
    suspend fun getPendingCommands(): Response<ApiResponse<List<Command>>>

    @POST("api/v1/device/commands/executed")
    suspend fun markCommandExecuted(@Body body: CommandExecutionRequest): Response<ApiResponse<Unit>>

    @POST("api/v1/device/register")
    suspend fun registerDevice(@Body body: DeviceRegisterRequest): Response<ApiResponse<Unit>>

    @GET("api/v1/location/geofence")
    suspend fun getGeofences(): Response<ApiResponse<List<GeofenceData>>>

    @POST("api/v1/location/geofence/breach")
    suspend fun reportGeofenceBreach(@Body body: GeofenceBreachRequest): Response<ApiResponse<Unit>>

    @POST("api/v1/employees/consent")
    suspend fun submitConsent(@Body body: ConsentRequest): Response<ApiResponse<Unit>>
}

data class ConsentRequest(val consentSigned: Boolean, val consentDate: String)

data class CallLogResponse(val id: String)

data class GeofenceBreachRequest(
    val geofenceId: String,
    val alertType: String, // "EXITED"
    val latitude: Double,
    val longitude: Double
)

data class GeofenceData(
    val id: String,
    val centerLat: Double,
    val centerLng: Double,
    val radiusMeters: Float
)

data class CallLogRequest(
    val callType: String,
    val phoneNumber: String,
    val duration: Int,
    val startedAt: String,
    val endedAt: String
)

data class CommandExecutionRequest(val commandId: String)
