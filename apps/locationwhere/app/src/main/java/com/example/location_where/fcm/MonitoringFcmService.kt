package com.example.location_where.fcm

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.example.location_where.MonitoringDeviceAdminReceiver
import com.example.location_where.api.ApiService
import com.example.location_where.api.CommandExecutionRequest
import com.example.location_where.data.DeviceRegisterRequest
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MonitoringFcmService : FirebaseMessagingService() {

    @Inject
    lateinit var apiService: ApiService

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New token: $token")
        serviceScope.launch {
            try {
                val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                val adminName = ComponentName(this@MonitoringFcmService, MonitoringDeviceAdminReceiver::class.java)
                apiService.registerDevice(
                    DeviceRegisterRequest(
                        deviceModel = Build.MODEL,
                        manufacturer = Build.MANUFACTURER,
                        androidVersion = Build.VERSION.RELEASE,
                        appVersion = "1.0",
                        fcmToken = token,
                        isAdminActive = dpm.isAdminActive(adminName)
                    )
                )
            } catch (e: Exception) {
                Log.e("FCM", "Failed to sync refreshed token", e)
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        
        val type = message.data["type"]
        if (type == "REMOTE_COMMAND") {
            val commandType = message.data["command"]
            val commandId = message.data["commandId"]
            val payload = message.data["payload"]
            
            Log.d("FCM", "Received command: $commandType")
            
            executeCommand(commandType, payload)
            
            if (commandId != null) {
                markExecuted(commandId)
            }
        }
    }

    private fun executeCommand(type: String?, payload: String?) {
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminName = ComponentName(this, MonitoringDeviceAdminReceiver::class.java)

        when (type) {
            "LOCK" -> {
                if (dpm.isAdminActive(adminName)) {
                    dpm.lockNow()
                }
            }
            "WIPE" -> {
                if (dpm.isAdminActive(adminName)) {
                    dpm.wipeData(0)
                }
            }
            "SIREN" -> {
                try {
                    val player = MediaPlayer.create(this, android.provider.Settings.System.DEFAULT_RINGTONE_URI)
                    player.isLooping = true
                    player.start()
                    Handler(Looper.getMainLooper()).postDelayed({
                        player.stop()
                        player.release()
                    }, 30000)
                } catch (e: Exception) {
                    Log.e("FCM", "Siren failed", e)
                }
            }
            "MESSAGE" -> {
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(this, "ADMIN MESSAGE: $payload", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun markExecuted(id: String) {
        serviceScope.launch {
            try {
                apiService.markCommandExecuted(CommandExecutionRequest(id))
            } catch (e: Exception) {
                Log.e("FCM", "Failed to mark command executed", e)
            }
        }
    }
}
