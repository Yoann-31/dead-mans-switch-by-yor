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

        Notifications.cancelCheckIn(ctx)
        ctx.lastValidatedAt = System.currentTimeMillis()
        ctx.lastStatus = "Présence validée"

        if (ctx.enabled) {
            // Réinitialise le compte à rebours d'envoi et le prochain rappel (option A)
            Scheduler.onValidated(ctx)
            // Signale la présence au filet serveur (repousse l'échéance côté serveur)
            ServerWatch.checkin(ctx)
        }
    }
}
