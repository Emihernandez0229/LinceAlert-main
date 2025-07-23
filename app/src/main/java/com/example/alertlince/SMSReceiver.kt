package com.example.alertlince

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class SMSReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("SMSReceiver", "Broadcast recibido: ${intent.action}")

        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            try {
                val smsMessages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                for (sms in smsMessages) {
                    val messageBody = sms.messageBody
                    Log.d("SMSReceiver", "SMS recibido: $messageBody")

                    // Enviar el texto completo al fragmento mediante broadcast local
                    val localIntent = Intent("sms_ubicacion_recibida")
                    localIntent.putExtra("texto_sms", messageBody)
                    LocalBroadcastManager.getInstance(context).sendBroadcast(localIntent)
                }
            } catch (e: Exception) {
                Log.e("SMSReceiver", "Error al procesar el SMS: ${e.message}")
            }
        } else {
            Log.d("SMSReceiver", "Acción no coincidente: ${intent.action}")
        }
    }
}
