package com.example.veille

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.veille.Prefs.awaitingValidation
import com.example.veille.Prefs.enabled

/**
 * Se déclenche si le délai de grâce expire sans validation :
 * lance le service d'envoi en avant-plan.
 */
class DeadlineReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (!context.enabled) return
        if (!context.awaitingValidation) return

        Notifications.cancelCheckIn(context)

        val svc = Intent(context, SendService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(context, svc)
        } else {
            context.startService(svc)
        }
    }
}
