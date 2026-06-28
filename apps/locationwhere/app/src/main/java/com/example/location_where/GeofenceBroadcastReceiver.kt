package com.example.location_where

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.location_where.api.ApiService
import com.example.location_where.api.GeofenceBreachRequest
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class GeofenceBroadcastReceiver : BroadcastReceiver() {

    @Inject
    lateinit var apiService: ApiService

    companion object {
        private const val ALERT_CHANNEL_ID = "geofence_alert"
        private const val NOTIFICATION_ID = 54321
    }

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return

        if (geofencingEvent.hasError()) {
            Log.e("GeofenceReceiver", "Geofencing error code: ${geofencingEvent.errorCode}")
            return
        }

        if (geofencingEvent.geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            val triggeringGeofences = geofencingEvent.triggeringGeofences
            val location = geofencingEvent.triggeringLocation
            
            triggeringGeofences?.forEach { geofence ->
                reportBreach(geofence.requestId, location?.latitude ?: 0.0, location?.longitude ?: 0.0)
            }
            
            showExitNotification(context)
        }
    }

    private fun reportBreach(id: String, lat: Double, lng: Double) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                apiService.reportGeofenceBreach(GeofenceBreachRequest(id, "EXITED", lat, lng))
            } catch (e: Exception) {
                Log.e("GeofenceReceiver", "Failed to report breach", e)
            }
        }
    }

    private fun showExitNotification(context: Context) {
        val notification = NotificationCompat.Builder(context, ALERT_CHANNEL_ID)
            .setContentTitle("Geofence Breach!")
            .setContentText("আপনি নির্ধারিত এলাকার বাইরে চলে গেছেন।")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }
}
