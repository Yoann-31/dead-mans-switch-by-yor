package com.example.veille

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.veille.Prefs.awaitingValidation
import com.example.veille.Prefs.enabled

/**
 * Déclenché ~10 min avant l'échéance : démarre le suivi de position
 * si la surveillance est active et qu'une validation est attendue.
 */
class PreTrackReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (context.enabled && context.awaitingValidation) {
            LocationTrackingService.start(context)
        }
    }
}
