package com.example.location_where.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.location_where.GeofenceBroadcastReceiver
import com.example.location_where.MainActivity
import com.example.location_where.R
import com.example.location_where.api.ApiService
import com.example.location_where.api.CallLogRequest
import com.example.location_where.utils.EncryptionUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import android.Manifest

@AndroidEntryPoint
class CallRecordingService : Service() {

    @Inject
    lateinit var apiService: ApiService

    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private var audioFile: File? = null
    private var currentPhoneNumber: String? = null
    private var callStartTime: Long = 0
    private var callType: String = "UNKNOWN"

    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private lateinit var telephonyManager: TelephonyManager

    companion object {
        private const val NOTIFICATION_ID = 67890
        private const val CHANNEL_ID = "call_recording_tracking"
    }

    override fun onCreate() {
        super.onCreate()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            stopSelf()
            return
        }

        createNotificationChannel()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                startForeground(
                    NOTIFICATION_ID,
                    createNotification(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                )
            } else {
                startForeground(NOTIFICATION_ID, createNotification())
            }
        } catch (e: Exception) {
            Log.e("CallRecordService", "Failed to start foreground", e)
            stopSelf()
            return
        }

        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            registerTelephonyCallback()
        } else {
            @Suppress("DEPRECATION")
            telephonyManager.listen(object : android.telephony.PhoneStateListener() {
                override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                    handleCallState(state, phoneNumber)
                }
            }, android.telephony.PhoneStateListener.LISTEN_CALL_STATE)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun registerTelephonyCallback() {
        telephonyManager.registerTelephonyCallback(mainExecutor, object : TelephonyCallback(), TelephonyCallback.CallStateListener {
            override fun onCallStateChanged(state: Int) {
                handleCallState(state, null)
            }
        })
    }

    private fun handleCallState(state: Int, phoneNumber: String?) {
        when (state) {
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                if (!isRecording) {
                    currentPhoneNumber = phoneNumber
                    callStartTime = System.currentTimeMillis()
                    startRecording()
                }
            }
            TelephonyManager.CALL_STATE_IDLE -> {
                if (isRecording) {
                    stopRecording()
                    saveAndUploadCall(phoneNumber)
                }
            }
            TelephonyManager.CALL_STATE_RINGING -> {
                currentPhoneNumber = phoneNumber
                callType = "INCOMING"
            }
        }
    }

    private fun startRecording() {
        try {
            audioFile = File(externalCacheDir, "call_${System.currentTimeMillis()}.mp4")
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(audioFile?.absolutePath)
                prepare()
                start()
            }
            isRecording = true
            Log.d("CallRecord", "Recording started")
        } catch (e: Exception) {
            Log.e("CallRecord", "Failed to start recording", e)
        }
    }

    private fun stopRecording() {
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
            mediaRecorder = null
            isRecording = false
            Log.d("CallRecord", "Recording stopped")
        } catch (e: Exception) {
            Log.e("CallRecord", "Failed to stop recording", e)
        }
    }

    private fun saveAndUploadCall(phoneNumber: String?) {
        val number = phoneNumber ?: currentPhoneNumber ?: "Unknown"
        val duration = ((System.currentTimeMillis() - callStartTime) / 1000).toInt()
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val startedAt = sdf.format(Date(callStartTime))
        val endedAt = sdf.format(Date())

        val request = CallLogRequest(
            callType = if (callType == "INCOMING") "INCOMING" else "OUTGOING",
            phoneNumber = number,
            duration = duration,
            startedAt = startedAt,
            endedAt = endedAt
        )

        serviceScope.launch {
            try {
                val logResponse = apiService.uploadCallLog(request)
                if (logResponse.isSuccessful) {
                    val callLogId = logResponse.body()?.data?.id
                    if (callLogId != null && audioFile != null) {
                        uploadRecording(callLogId, audioFile!!)
                    }
                }
            } catch (e: Exception) {
                Log.e("CallRecord", "Failed to upload call log", e)
            }
        }
    }

    private suspend fun uploadRecording(callLogId: String, file: File) {
        if (!file.exists()) return
        val encryptedFile = File(file.parent, file.name + ".enc")
        EncryptionUtils.encryptFile(file, encryptedFile)
        val checksum = EncryptionUtils.calculateChecksum(encryptedFile)

        val requestFile = encryptedFile.asRequestBody("audio/mp4".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("recording", encryptedFile.name, requestFile)
        val idBody = callLogId.toRequestBody("text/plain".toMediaTypeOrNull())
        val checksumBody = checksum.toRequestBody("text/plain".toMediaTypeOrNull())

        try {
            val response = apiService.uploadCallRecording(body, idBody, checksumBody)
            if (response.isSuccessful) {
                Log.d("CallRecord", "Recording uploaded successfully")
                file.delete()
                encryptedFile.delete()
            }
        } catch (e: Exception) {
            Log.e("CallRecord", "Failed to upload recording", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Call Monitoring Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Call Monitoring Active")
            .setContentText("Policy compliance: Monitoring calls.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
