package com.example.veille

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.veille.Prefs.awaitingValidation
import com.example.veille.Prefs.deadlineAt
import com.example.veille.Prefs.enabled

/**
 * Reprogramme les alarmes après un redémarrage ou une mise à jour :
 * sinon Android les efface et la veille s'arrêterait silencieusement.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (!context.enabled) return

        if (context.awaitingValidation && context.deadlineAt > System.currentTimeMillis()) {
            // On attendait une validation : on rétablit l'échéance restante.
            val remaining = context.deadlineAt - System.currentTimeMillis()
            val minutes = (remaining / 60_000L).coerceAtLeast(1)
            // Reprogramme l'échéance directement
            Scheduler.scheduleDeadlineIn(context, minutes)
            Notifications.showCheckIn(context)
        } else {
            Scheduler.scheduleNextCheckIn(context)
        }
    }
}
