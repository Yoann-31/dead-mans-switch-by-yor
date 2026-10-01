package com.example.veille

import android.content.Context
import com.example.veille.Prefs.enabled
import com.example.veille.Prefs.lastStatus
import com.example.veille.Prefs.lastValidatedAt

/**
 * Logique de validation de présence, partagée entre :
 *  - le bouton « Je suis là » de la notification (ActionReceiver)
 *  - le bouton de validation dans l'application (MainActivity)
 *
 * Enregistre la position GPS au moment de la validation, annule l'échéance
 * en cours et reprogramme la prochaine relance.
 */
object Validator {

    fun validatePresence(ctx: Context) {
        // Fige la position GPS au moment où l'utilisateur confirme sa présence
        Locator.saveCurrent(ctx)

        Scheduler.cancelDeadline(ctx)
        Notifications.cancelCheckIn(ctx)
        ctx.lastValidatedAt = System.currentTimeMillis()
        ctx.lastStatus = "Présence validée"

        if (ctx.enabled) {
            Scheduler.scheduleNextCheckIn(ctx)
        }
    }
}
