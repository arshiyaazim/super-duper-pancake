package com.locationwhere.smsgateway.service

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.locationwhere.smsgateway.MainActivity
import com.locationwhere.smsgateway.R
import com.locationwhere.smsgateway.data.AppDatabase
import com.locationwhere.smsgateway.data.FailedSmsEntry
import com.locationwhere.smsgateway.data.LogEntry
import com.locationwhere.smsgateway.network.ApiClient
import com.locationwhere.smsgateway.util.PhoneUtil
import com.locationwhere.smsgateway.util.Prefs
import com.locationwhere.smsgateway.util.SmsSender
import kotlinx.coroutines.*
import org.json.JSONObject
import timber.log.Timber
import kotlin.time.Duration.Companion.seconds

class GatewayService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var prefs: Prefs
    private lateinit var db: AppDatabase

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs.getInstance(this)
        db = AppDatabase.getDatabase(this)
        createNotificationChannel()
        startRetryLoop()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification("Gateway is Active"))

        val action = intent?.action ?: ACTION_START_SERVICE
        
        if (action == ACTION_FORWARD_SMS) {
            val sender = intent?.getStringExtra("sender") ?: ""
            val recipient = intent?.getStringExtra("recipient") ?: ""
            val body = intent?.getStringExtra("body") ?: ""
            
            if (sender.isNotEmpty() && body.isNotEmpty()) {
                forwardSms(sender, recipient, body)
            }
        }

        return START_STICKY
    }

    private fun forwardSms(sender: String, recipient: String, body: String) {
        // Guard: skip forwarding if settings aren't configured
        if (!prefs.isConfigured) {
            Timber.w("SMS forwarding skipped — not configured")
            serviceScope.launch {
                db.logDao().insert(
                    LogEntry(
                        sender = sender,
                        recipient = recipient,
                        body = body,
                        status = "SKIPPED",
                        smsStatus = "Not configured"
                    )
                )
            }
            return
        }

        Timber.d("Forwarding SMS from: ${PhoneUtil.maskPhoneNumber(sender)}")
        serviceScope.launch {
            ApiClient.forwardSms(
                prefs.backendUrl,
                prefs.gatewaySecret,
                sender,
                body
            ) { success, response ->
                Timber.d("API result: success=$success")
                handleApiResponse(sender, recipient, body, success, response)
            }
        }
    }

    private fun handleApiResponse(sender: String, recipient: String, body: String, success: Boolean, response: String?) {
        serviceScope.launch {
            var status = if (success) "SUCCESS" else "FAILED"
            var employeeCode: String? = null
            var replyMessage: String?
            var replyTo: String? = null
            var smsStatus: String? = null

            if (success && response != null) {
                try {
                    val json = JSONObject(response)
                    val apiStatus = json.optString("status")
                    Timber.d("API status: $apiStatus")
                    if (apiStatus == "duplicate") {
                        status = "DUPLICATE"
                        employeeCode = json.optString("employeeCode")
                    } else {
                        employeeCode = json.optString("employeeCode")
                        replyTo = json.optString("replyTo")
                        replyMessage = json.optString("replyMessage")

                        if (!replyTo.isNullOrEmpty() && !replyMessage.isNullOrEmpty()) {
                            smsStatus = SmsSender.sendSms(this@GatewayService, replyTo, replyMessage)
                            Timber.d("SMS send result: $smsStatus")
                            if (smsStatus != "SENT") {
                                status = "API_OK_SMS_FAILED"
                            }
                        } else {
                            Timber.w("Missing replyTo or replyMessage in API response")
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "JSON parsing error")
                    status = "FAILED"
                    smsStatus = "JSON Error: ${e.message}"
                }
            } else if (!success) {
                // Queue for retry on API failure
                try {
                    db.failedSmsDao().insert(
                        FailedSmsEntry(
                            sender = sender,
                            recipient = recipient,
                            body = body
                        )
                    )
                    Timber.d("Queued failed SMS for retry")
                } catch (e: Exception) {
                    Timber.e(e, "Failed to queue SMS for retry")
                }
            }

            db.logDao().insert(
                LogEntry(
                    sender = sender,
                    recipient = recipient,
                    body = body,
                    status = status,
                    employeeCode = employeeCode,
                    apiResponse = response,
                    smsStatus = smsStatus
                )
            )
            db.logDao().clearOldLogs()
        }
    }

    private fun startRetryLoop() {
        serviceScope.launch {
            while (isActive) {
                delay(60.seconds) // Check every 60 seconds
                try {
                    val pending = db.failedSmsDao().getPendingRetries()
                    if (pending.isNotEmpty()) {
                        Timber.d("Retrying ${pending.size} failed SMS entries")
                    }
                    for (entry in pending) {
                        try {
                            val result = CompletableDeferred<Pair<Boolean, String?>>()
                            ApiClient.forwardSms(
                                prefs.backendUrl,
                                prefs.gatewaySecret,
                                entry.sender,
                                entry.body
                            ) { s, r ->
                                result.complete(s to r)
                            }
                            val (retrySuccess, response) = result.await()
                            if (retrySuccess) {
                                handleApiResponse(entry.sender, entry.recipient, entry.body, true, response)
                                db.failedSmsDao().delete(entry)
                                Timber.d("Retry success for entry ${entry.id}")
                            } else {
                                // Exponential backoff: 1min, 2min, 4min, 8min, 16min
                                val backoffMs = 60_000L * (1L shl entry.retryCount)
                                db.failedSmsDao().updateRetry(entry.id, System.currentTimeMillis() + backoffMs)
                                Timber.d("Retry failed for entry ${entry.id}, next in ${backoffMs / 1000}s")
                            }
                        } catch (e: Exception) {
                            Timber.e(e, "Retry error for entry ${entry.id}")
                        }
                    }
                    db.failedSmsDao().deleteExhaustedRetries()
                } catch (e: Exception) {
                    Timber.e(e, "Retry loop error")
                }
            }
        }
    }

    private fun createNotification(content: String): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SMS Gateway")
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_gateway)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            CHANNEL_ID, "Gateway Service",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START_SERVICE = "ACTION_START_SERVICE"
        const val ACTION_FORWARD_SMS = "ACTION_FORWARD_SMS"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "GatewayServiceChannel"
    }
}
