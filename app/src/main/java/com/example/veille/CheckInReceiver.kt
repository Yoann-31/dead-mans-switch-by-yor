package com.example.veille

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.veille.Prefs.enabled

/**
 * Rappel récurrent : affiche la notification de présence puis reprogramme
 * le rappel suivant. L'échéance d'envoi est gérée séparément (DeadlineReceiver).
 */
class CheckInReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (!context.enabled) return
        Notifications.showCheckIn(context)
        Scheduler.armNextReminder(context, System.currentTimeMillis())
    }
}
