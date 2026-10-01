package com.example.veille

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.veille.Prefs.deadlineAt
import com.example.veille.Prefs.enabled
import com.example.veille.Prefs.sendDelayMinutes

/**
 * Reprogramme les alarmes après un redémarrage ou une mise à jour :
 * conserve l'échéance d'envoi en cours et relance les rappels récurrents.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (!context.enabled) return

        val now = System.currentTimeMillis()
        val dl = context.deadlineAt
        // Conserve l'échéance si elle est encore dans le futur, sinon en repart une nouvelle.
        val target = if (dl > now) dl else now + context.sendDelayMinutes * 60_000L
        Scheduler.armDeadline(context, target)
        Scheduler.armNextReminder(context, now)
    }
}
