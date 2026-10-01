package com.example.veille

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.veille.Prefs.awaitingValidation
import com.example.veille.Prefs.enabled
import com.example.veille.Prefs.lastStatus
import kotlin.concurrent.thread

/**
 * Service en avant-plan qui effectue l'envoi hors du thread principal
 * (le réseau est interdit sur le thread principal), puis désactive la veille.
 */
class SendService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Notifications.ensureChannels(this)
        val notif: Notification = NotificationCompat.Builder(this, Notifications.CHANNEL_STATUS)
            .setSmallIcon(R.drawable.ic_stat_shield)
            .setContentTitle("Envoi en cours")
            .setContentText("Le message de sécurité est en cours d'envoi.")
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(3001, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(3001, notif)
        }

        thread {
            val result = try {
                Sender.send(this)
            } catch (e: Exception) {
                "Échec inattendu : ${e.message}"
            }

            lastStatus = result
            awaitingValidation = false
            // Déclenchement unique : on désactive la veille après l'envoi.
            enabled = false
            Scheduler.cancelAll(this)

            Notifications.showStatus(
                this,
                "Veille déclenchée",
                "$result\n\nLa veille a été désactivée. Rouvrez l'application pour la réactiver."
            )

            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }

        return START_NOT_STICKY
    }
}
