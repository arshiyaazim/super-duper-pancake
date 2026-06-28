package com.locationwhere.smsgateway.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsMessage
import com.locationwhere.smsgateway.service.GatewayService
import com.locationwhere.smsgateway.util.Prefs

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val prefs = Prefs.getInstance(context)
            if (!prefs.isForwardingEnabled) return

            val bundle = intent.extras ?: return
            val pdus = bundle.get("pdus") as? Array<*> ?: return
            val format = bundle.getString("format")

            // Reassemble multi-part SMS: group PDUs by sender, concatenate bodies
            val messageMap = mutableMapOf<String, StringBuilder>()

            for (pdu in pdus) {
                val sms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    SmsMessage.createFromPdu(pdu as ByteArray, format)
                } else {
                    @Suppress("DEPRECATION")
                    SmsMessage.createFromPdu(pdu as ByteArray)
                }

                val sender = sms.displayOriginatingAddress ?: "unknown"
                val body = sms.displayMessageBody ?: ""

                messageMap.getOrPut(sender) { StringBuilder() }.append(body)
            }

            // Forward each complete reassembled message
            for ((sender, bodyBuilder) in messageMap) {
                val fullBody = bodyBuilder.toString()
                val recipient = prefs.adminNumber

                // Flexible regex to match: ID:, ID :, I D :, I D: (case-insensitive)
                val idPattern = Regex("^(?i)\\s*I\\s*D\\s*[:\\s]")
                
                if (idPattern.containsMatchIn(fullBody)) {
                    val serviceIntent = Intent(context, GatewayService::class.java).apply {
                        action = GatewayService.ACTION_FORWARD_SMS
                        putExtra("sender", sender)
                        putExtra("recipient", recipient)
                        putExtra("body", fullBody)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                }
            }
        }
    }
}
