package com.example.veille

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.veille.Prefs.enabled

/**
 * Se déclenche quand le temps d'absence est écoulé sans validation :
 * arrête le suivi GPS et lance l'envoi.
 */
class DeadlineReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (!context.enabled) return

        Notifications.cancelCheckIn(context)
        LocationTrackingService.stop(context)

        val svc = Intent(context, SendService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(context, svc)
        } else {
            context.startService(svc)
        }
    }
}
