package com.example.location_where.services

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.location_where.GeofenceBroadcastReceiver
import com.example.location_where.MainActivity
import com.example.location_where.R
import com.example.location_where.api.ApiService
import com.example.location_where.data.repository.LocationRepository
import com.google.android.gms.location.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject
import android.Manifest

@AndroidEntryPoint
class LocationService : Service() {

    @Inject
    lateinit var repository: LocationRepository
    
    @Inject
    lateinit var apiService: ApiService

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var geofencingClient: GeofencingClient
    private lateinit var locationCallback: LocationCallback
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        private const val NOTIFICATION_ID = 12345
        private const val CHANNEL_ID = "location_tracking"
        private const val ALERT_CHANNEL_ID = "geofence_alert"
        private const val NORMAL_UPDATE_INTERVAL_MS: Long = 60_000
        private const val LOW_BATTERY_UPDATE_INTERVAL_MS: Long = 120_000
        private const val MIN_UPDATE_DISTANCE_METERS = 25f
        private var UPDATE_INTERVAL_MS: Long = NORMAL_UPDATE_INTERVAL_MS
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        geofencingClient = LocationServices.getGeofencingClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    if (location.accuracy <= 100) {
                        handleLocationUpdate(location)
                    }
                }
            }
        }
        
        createNotificationChannels()
        setupGeofences()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            stopSelf()
            return START_NOT_STICKY
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
            } else {
                startForeground(NOTIFICATION_ID, createNotification())
            }
        } catch (e: Exception) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && e is ForegroundServiceStartNotAllowedException) {
                Log.e("LocationService", "FGS start not allowed", e)
            } else {
                Log.e("LocationService", "Failed to start foreground", e)
            }
            stopSelf()
            return START_NOT_STICKY
        }

        adjustIntervalBasedOnBattery()
        requestLocationUpdates()
        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, UPDATE_INTERVAL_MS)
            .setMinUpdateIntervalMillis(UPDATE_INTERVAL_MS)
            .setMinUpdateDistanceMeters(MIN_UPDATE_DISTANCE_METERS)
            .setWaitForAccurateLocation(false)
            .build()

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun handleLocationUpdate(location: Location) {
        serviceScope.launch {
            val batteryLevel = getBatteryLevel()
            repository.saveLocation(
                location.latitude,
                location.longitude,
                location.accuracy,
                batteryLevel
            )
            updatePersistentNotification(batteryLevel)
            
            if (batteryLevel < 30 && UPDATE_INTERVAL_MS == NORMAL_UPDATE_INTERVAL_MS) {
                UPDATE_INTERVAL_MS = LOW_BATTERY_UPDATE_INTERVAL_MS
                requestLocationUpdates()
            }
        }
    }

    private fun setupGeofences() {
        serviceScope.launch {
            try {
                val response = apiService.getGeofences()
                if (response.isSuccessful) {
                    val geofences = response.body()?.data ?: return@launch
                    registerGeofences(geofences)
                }
            } catch (e: Exception) {
                Log.e("LocationService", "Failed to fetch geofences", e)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun registerGeofences(geofenceList: List<com.example.location_where.api.GeofenceData>) {
        if (geofenceList.isEmpty()) return

        val androidGeofences = geofenceList.map {
            Geofence.Builder()
                .setRequestId(it.id)
                .setCircularRegion(it.centerLat, it.centerLng, it.radiusMeters)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_EXIT)
                .build()
        }

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_EXIT)
            .addGeofences(androidGeofences)
            .build()

        val intent = Intent(this, GeofenceBroadcastReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        geofencingClient.addGeofences(request, pendingIntent).addOnFailureListener {
            Log.e("LocationService", "Failed to add geofences", it)
        }
    }

    private fun getBatteryLevel(): Int {
        val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    private fun adjustIntervalBasedOnBattery() {
        val level = getBatteryLevel()
        UPDATE_INTERVAL_MS = if (level < 30) LOW_BATTERY_UPDATE_INTERVAL_MS else NORMAL_UPDATE_INTERVAL_MS
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            
            val trackingChannel = NotificationChannel(
                CHANNEL_ID,
                "Location Tracking",
                NotificationManager.IMPORTANCE_LOW
            )
            manager.createNotificationChannel(trackingChannel)

            val alertChannel = NotificationChannel(
                ALERT_CHANNEL_ID,
                "Geofence Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(alertChannel)
        }
    }

    private fun createNotification(battery: Int = 100): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GPS Tracking Active")
            .setContentText("Monitoring location | Battery: $battery%")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    @SuppressLint("MissingPermission")
    private fun updatePersistentNotification(battery: Int) {
        val notification = createNotification(battery)
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}
