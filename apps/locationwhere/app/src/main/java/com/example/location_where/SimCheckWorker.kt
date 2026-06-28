package com.example.location_where

import android.content.Context
import android.os.Build
import android.telephony.SubscriptionManager
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.location_where.api.ApiService
import com.example.location_where.data.DeviceInfoMap
import com.example.location_where.data.SimAlert
import com.example.location_where.utils.TokenManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SimCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val subscriptionManager = applicationContext.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
        
        try {
            val activeSubscriptions = subscriptionManager.activeSubscriptionInfoList
            if (activeSubscriptions != null) {
                for (info in activeSubscriptions) {
                    val currentIccId = info.iccId
                    
                    val sharedPrefs = applicationContext.getSharedPreferences("MonitoringPrefs", Context.MODE_PRIVATE)
                    val savedIccId = sharedPrefs.getString("authorized_iccid", null)

                    if (savedIccId != null && savedIccId != currentIccId) {
                        sendSimAlert(savedIccId, currentIccId, null, info.subscriptionId.toString())
                    } else if (savedIccId == null) {
                        sharedPrefs.edit().putString("authorized_iccid", currentIccId).apply()
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e("SimCheckWorker", "Permission denied", e)
        }

        return Result.success()
    }

    private suspend fun sendSimAlert(oldSim: String, newSim: String, oldImsi: String?, newImsi: String) {
        val alert = SimAlert(
            previousSim = oldSim,
            newSim = newSim,
            previousIMSI = oldImsi,
            newIMSI = newImsi,
            deviceInfo = DeviceInfoMap(
                deviceModel = Build.MODEL,
                androidVersion = Build.VERSION.RELEASE
            )
        )

        try {
            tokenManager.getAccessToken() ?: return
            val response = apiService.reportSimChange(alert) // AuthInterceptor will add the token
            if (response.isSuccessful) {
                Log.d("SimCheckWorker", "SIM alert sent")
            }
        } catch (e: Exception) {
            Log.e("SimCheckWorker", "Failed to send alert", e)
        }
    }
}
