package com.example.location_where.workers

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.location_where.MonitoringDeviceAdminReceiver
import com.example.location_where.api.ApiService
import com.example.location_where.api.CommandExecutionRequest
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class CommandWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val apiService: ApiService
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            Log.d("CommandWorker", "Polling for pending commands")
            val response = apiService.getPendingCommands()
            if (response.isSuccessful) {
                val commands = response.body()?.data ?: emptyList()
                Log.d("CommandWorker", "Fetched ${commands.size} pending commands")
                for (command in commands) {
                    Log.d("CommandWorker", "Executing ${command.commandType} command ${command.id}")
                    executeCommand(command)
                    val executionResponse = apiService.markCommandExecuted(CommandExecutionRequest(command.id))
                    Log.d(
                        "CommandWorker",
                        "Mark executed for ${command.id}: ${executionResponse.code()} ${executionResponse.isSuccessful}"
                    )
                }
            } else {
                Log.e("CommandWorker", "Pending commands request failed: ${response.code()} ${response.message()}")
            }
            Result.success()
        } catch (e: Exception) {
            Log.e("CommandWorker", "Command polling failed", e)
            Result.retry()
        }
    }

    private fun executeCommand(command: com.example.location_where.data.Command) {
        val dpm = applicationContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminName = ComponentName(applicationContext, MonitoringDeviceAdminReceiver::class.java)

        when (command.commandType) {
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
                val player = MediaPlayer.create(applicationContext, android.provider.Settings.System.DEFAULT_RINGTONE_URI)
                player.isLooping = true
                player.start()
                Handler(Looper.getMainLooper()).postDelayed({
                    player.stop()
                    player.release()
                }, 30000)
            }
            "MESSAGE" -> {
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(applicationContext, "ADMIN MESSAGE: ${command.commandPayload}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
