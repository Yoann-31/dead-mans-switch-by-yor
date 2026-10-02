package com.example.veille

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.veille.Prefs.enabled
import com.example.veille.Prefs.pendingEmail
import com.example.veille.Prefs.pendingSms

/**
 * Relance une tentative d'envoi (toutes les 2 min) tant qu'un canal est en attente
 * de connexion. Déclenché par l'alarme programmée dans SendService via Scheduler.
 */
class RetryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (!context.enabled) return
        if (!context.pendingSms && !context.pendingEmail) return

        val svc = Intent(context, SendService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(context, svc)
        } else {
            context.startService(svc)
        }
    }
}
