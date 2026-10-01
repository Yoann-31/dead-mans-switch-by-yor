package com.example.veille

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Reçoit l'appui sur « Je suis là » depuis la notification.
 * Délègue au validateur commun (qui enregistre la position et reprogramme).
 */
class ActionReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_VALIDATE = "com.example.veille.VALIDATE"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == ACTION_VALIDATE) {
            Validator.validatePresence(context)
        }
    }
}
