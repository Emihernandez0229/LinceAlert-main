package com.example.alertlince

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class SMSReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION == intent.action) {
            val smsMessages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (sms in smsMessages) {
                val messageBody = sms.messageBody

                // Envía el texto completo al fragmento mediante broadcast local
                val localIntent = Intent("sms_ubicacion_recibida")
                localIntent.putExtra("texto_sms", messageBody)
                LocalBroadcastManager.getInstance(context).sendBroadcast(localIntent)
            }
        }
    }
}
