package com.locationwhere.smsgateway.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.locationwhere.smsgateway.service.GatewayService
import timber.log.Timber

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_LOCKED_BOOT_COMPLETED) {
            try {
                val serviceIntent = Intent(context, GatewayService::class.java).apply {
                    action = GatewayService.ACTION_START_SERVICE
                }
                context.startForegroundService(serviceIntent)
            } catch (e: Exception) {
                Timber.e(e, "Failed to start GatewayService on boot")
            }
        }
    }
}
