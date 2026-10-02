package com.example.veille

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.veille.Prefs.deadlineAt
import com.example.veille.Prefs.enabled
import com.example.veille.Prefs.sendDelayMinutes

/**
 * Reprogramme les alarmes après un redémarrage ou une mise à jour.
 *
 * Garde-fou (n°2) : si l'échéance d'envoi est TOMBÉE pendant que le téléphone
 * était éteint (batterie vide, redémarrage…), on déclenche l'envoi juste après
 * le démarrage — via une alarme quasi immédiate, qui bénéficie de l'exemption
 * permettant de lancer le service d'envoi en avant-plan depuis l'arrière-plan.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (!context.enabled) return

        val now = System.currentTimeMillis()
        val dl = context.deadlineAt

        if (dl in 1..now) {
            // Échéance manquée pendant l'extinction → envoi au redémarrage (en léger différé).
            Scheduler.armDeadline(context, now + 5_000L)
            return
        }

        // Sinon : on conserve l'échéance en cours (ou on en repart une) et on relance les rappels.
        val target = if (dl > now) dl else now + context.sendDelayMinutes * 60_000L
        Scheduler.armDeadline(context, target)
        Scheduler.armNextReminder(context, now)
    }
}
