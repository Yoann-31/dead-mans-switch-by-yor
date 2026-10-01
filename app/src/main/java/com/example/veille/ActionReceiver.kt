package com.example.veille

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.veille.Prefs.enabled
import com.example.veille.Prefs.lastStatus
import com.example.veille.Prefs.lastValidatedAt

/**
 * Reçoit l'appui sur « Je suis là » : annule l'échéance,
 * enlève la notification et reprogramme la prochaine relance.
 */
class ActionReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_VALIDATE = "com.example.veille.VALIDATE"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == ACTION_VALIDATE) {
            Scheduler.cancelDeadline(context)
            Notifications.cancelCheckIn(context)
            context.lastValidatedAt = System.currentTimeMillis()
            context.lastStatus = "Présence validée"
            if (context.enabled) {
                Scheduler.scheduleNextCheckIn(context)
            }
        }
    }
}
