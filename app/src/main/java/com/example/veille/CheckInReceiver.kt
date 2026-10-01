package com.example.veille

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.veille.Prefs.enabled

/**
 * Se déclenche à chaque intervalle : ouvre le délai de grâce
 * et affiche la notification de validation.
 */
class CheckInReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (!context.enabled) return
        Scheduler.scheduleDeadline(context)
        Notifications.showCheckIn(context)
    }
}
