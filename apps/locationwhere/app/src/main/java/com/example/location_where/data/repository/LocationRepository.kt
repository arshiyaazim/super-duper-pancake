package com.example.location_where.data.repository

import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.os.Build
import com.example.location_where.api.ApiService
import com.example.location_where.data.LocationUpdate
import com.example.location_where.data.local.LocationDao
import com.example.location_where.data.local.LocationEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepository @Inject constructor(
    private val apiService: ApiService,
    private val locationDao: LocationDao,
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("LocationRepositoryPrefs", Context.MODE_PRIVATE)
    private val repositoryMutex = Mutex()

    companion object {
        private const val MIN_PERSIST_INTERVAL_MS = 30_000L
        private const val MIN_PERSIST_DISTANCE_METERS = 20f
        private const val MIN_UPLOAD_INTERVAL_MS = 25_000L
        private const val RATE_LIMIT_COOLDOWN_MS = 60_000L
        private const val SYNC_RETENTION_MS = 7L * 24 * 60 * 60 * 1000
        private const val KEY_LAST_SAVED_AT = "last_saved_at"
        private const val KEY_LAST_SAVED_LAT = "last_saved_lat"
        private const val KEY_LAST_SAVED_LNG = "last_saved_lng"
        private const val KEY_LAST_UPLOAD_SUCCESS_AT = "last_upload_success_at"
        private const val KEY_UPLOAD_BLOCKED_UNTIL = "upload_blocked_until"
    }

    suspend fun saveLocation(
        latitude: Double,
        longitude: Double,
        accuracy: Float,
        batteryLevel: Int
    ) = repositoryMutex.withLock {
        if (!shouldPersistLocation(latitude, longitude)) return

        val address = getAddress(latitude, longitude)
        val now = System.currentTimeMillis()
        val entity = LocationEntity(
            latitude = latitude,
            longitude = longitude,
            accuracy = accuracy,
            batteryLevel = batteryLevel,
            address = address,
            timestamp = now
        )

        locationDao.insertLocation(entity)
        rememberSavedLocation(latitude, longitude, now)

        if (shouldAttemptUpload(now)) {
            uploadPendingLocationsLocked()
        }
    }

    suspend fun uploadPendingLocations() = repositoryMutex.withLock {
        uploadPendingLocationsLocked()
    }

    private suspend fun uploadPendingLocationsLocked() {
        val now = System.currentTimeMillis()
        if (now < prefs.getLong(KEY_UPLOAD_BLOCKED_UNTIL, 0L)) return

        val unsynced = locationDao.getUnsyncedLocations()
        if (unsynced.isEmpty()) return

        for (loc in unsynced) {
            val lastUploadSuccessAt = prefs.getLong(KEY_LAST_UPLOAD_SUCCESS_AT, 0L)
            val waitForNextUploadMs = MIN_UPLOAD_INTERVAL_MS - (System.currentTimeMillis() - lastUploadSuccessAt)
            if (lastUploadSuccessAt > 0L && waitForNextUploadMs > 0L) break

            try {
                val update = LocationUpdate(
                    latitude = loc.latitude,
                    longitude = loc.longitude,
                    accuracy = loc.accuracy,
                    batteryLevel = loc.batteryLevel,
                    address = loc.address
                )
                
                val response = apiService.updateLocation(update)
                if (response.isSuccessful) {
                    locationDao.markAsSynced(listOf(loc.id))
                    prefs.edit().putLong(KEY_LAST_UPLOAD_SUCCESS_AT, System.currentTimeMillis()).apply()
                    locationDao.deleteOldSyncedLocations(System.currentTimeMillis() - SYNC_RETENTION_MS)
                } else if (response.code() == 429) {
                    prefs.edit().putLong(
                        KEY_UPLOAD_BLOCKED_UNTIL,
                        System.currentTimeMillis() + RATE_LIMIT_COOLDOWN_MS
                    ).apply()
                    break
                }
            } catch (e: Exception) {
                // Network error, will retry later
                break 
            }
        }
    }

    private fun shouldPersistLocation(latitude: Double, longitude: Double): Boolean {
        val lastSavedAt = prefs.getLong(KEY_LAST_SAVED_AT, 0L)
        val now = System.currentTimeMillis()
        if (lastSavedAt == 0L) return true

        val elapsedMs = now - lastSavedAt
        if (elapsedMs >= MIN_PERSIST_INTERVAL_MS) return true

        val lastLat = prefs.getLong(KEY_LAST_SAVED_LAT, 0L).takeIf { it != 0L }?.let(Double::fromBits)
        val lastLng = prefs.getLong(KEY_LAST_SAVED_LNG, 0L).takeIf { it != 0L }?.let(Double::fromBits)
        if (lastLat == null || lastLng == null) return true

        val distanceMeters = FloatArray(1)
        Location.distanceBetween(lastLat, lastLng, latitude, longitude, distanceMeters)
        return distanceMeters[0] >= MIN_PERSIST_DISTANCE_METERS
    }

    private fun rememberSavedLocation(latitude: Double, longitude: Double, timestamp: Long) {
        prefs.edit()
            .putLong(KEY_LAST_SAVED_AT, timestamp)
            .putLong(KEY_LAST_SAVED_LAT, latitude.toBits())
            .putLong(KEY_LAST_SAVED_LNG, longitude.toBits())
            .apply()
    }

    private fun shouldAttemptUpload(now: Long): Boolean {
        val blockedUntil = prefs.getLong(KEY_UPLOAD_BLOCKED_UNTIL, 0L)
        if (now < blockedUntil) return false

        val lastUploadSuccessAt = prefs.getLong(KEY_LAST_UPLOAD_SUCCESS_AT, 0L)
        return lastUploadSuccessAt == 0L || now - lastUploadSuccessAt >= MIN_UPLOAD_INTERVAL_MS
    }

    private suspend fun getAddress(lat: Double, lng: Double): String? = withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val completableDeferred = CompletableDeferred<String?>()
                geocoder.getFromLocation(lat, lng, 1) { addresses ->
                    if (addresses.isNotEmpty()) {
                        completableDeferred.complete(addresses[0].getAddressLine(0))
                    } else {
                        completableDeferred.complete(null)
                    }
                }
                completableDeferred.await()
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                if (addresses?.isNotEmpty() == true) {
                    addresses[0].getAddressLine(0)
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }
}
