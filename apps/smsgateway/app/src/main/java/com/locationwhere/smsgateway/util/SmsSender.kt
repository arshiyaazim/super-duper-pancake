package com.locationwhere.smsgateway.util

import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.telephony.SmsManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

object SmsSender {

    suspend fun sendSms(context: Context, phoneNumber: String, message: String): String {
        Timber.d("Sending SMS to: ${PhoneUtil.maskPhoneNumber(phoneNumber)}")
        return try {
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            val parts = smsManager.divideMessage(message)
            val results = mutableListOf<CompletableDeferred<Int>>()

            for (i in parts.indices) {
                val deferred = CompletableDeferred<Int>()
                results.add(deferred)

                val sentAction = "SMS_SENT_${System.currentTimeMillis()}_$i"
                val sentIntent = PendingIntent.getBroadcast(
                    context, i, Intent(sentAction),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT
                )

                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(ctx: Context, intent: Intent) {
                        try {
                            ctx.unregisterReceiver(this)
                        } catch (e: Exception) {
                            Timber.w("Receiver already unregistered")
                        }
                        deferred.complete(resultCode)
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.registerReceiver(receiver, IntentFilter(sentAction), Context.RECEIVER_NOT_EXPORTED)
                } else {
                    @Suppress("UnspecifiedRegisterReceiverFlag")
                    context.registerReceiver(receiver, IntentFilter(sentAction))
                }

                if (parts.size > 1) {
                    val sentIntents = ArrayList<PendingIntent>()
                    for (j in parts.indices) {
                        val partAction = "SMS_SENT_${System.currentTimeMillis()}_$j"
                        val partIntent = PendingIntent.getBroadcast(
                            context, j, Intent(partAction),
                            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT
                        )
                        sentIntents.add(partIntent)
                    }
                    smsManager.sendMultipartTextMessage(phoneNumber, null, parts, sentIntents, null)
                } else {
                    smsManager.sendTextMessage(phoneNumber, null, message, sentIntent, null)
                }
            }

            // Wait up to 30 seconds for all parts to report back
            val allSuccess = withTimeoutOrNull(30_000L) {
                results.all { it.await() == Activity.RESULT_OK }
            }

            when (allSuccess) {
                true -> "SENT"
                false -> "SEND_FAILED"
                null -> "TIMEOUT"
            }
        } catch (e: Exception) {
            Timber.e(e, "Error sending SMS")
            "FAILED: ${e.message}"
        }
    }
}
