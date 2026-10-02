package com.example.veille

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.veille.Prefs.enabled
import com.example.veille.Prefs.lastStatus
import com.example.veille.Prefs.messageEmail
import com.example.veille.Prefs.messageSms
import com.example.veille.Prefs.pendingEmail
import com.example.veille.Prefs.pendingSms
import com.example.veille.Prefs.retryCount
import kotlin.concurrent.thread

/**
 * Traite l'envoi des canaux encore en attente :
 *  - e-mail dès qu'Internet (Wi-Fi ou données) est disponible,
 *  - SMS dès que le réseau mobile (SIM) est prêt.
 * Tant qu'un canal reste en attente, on reprogramme un ré-essai (RetryReceiver).
 * La surveillance n'est désactivée qu'une fois tout envoyé (ou le délai de ré-essai dépassé).
 */
class SendService : Service() {

    companion object {
        private const val MAX_RETRIES = 180 // ~6 h à raison d'un essai / 2 min
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Notifications.ensureChannels(this)
        val notif: Notification = NotificationCompat.Builder(this, Notifications.CHANNEL_STATUS)
            .setSmallIcon(R.drawable.ic_stat_shield)
            .setContentTitle("Envoi en cours")
            .setContentText("Tentative d'envoi du message de sécurité…")
            .setOngoing(true)
            .build()

        val hasLocation = androidx.core.content.ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
            androidx.core.content.ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val type = if (hasLocation)
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            else
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            startForeground(3001, notif, type)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && hasLocation) {
            startForeground(3001, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(3001, notif)
        }

        thread {
            process()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }

        return START_NOT_STICKY
    }

    private fun process() {
        val gps = Sender.gpsLink(this)
        val parts = mutableListOf<String>()

        // E-mail : dès qu'une connexion Internet existe (Wi-Fi ou données)
        if (pendingEmail) {
            if (Net.hasInternet(this)) {
                val r = Sender.sendEmail(this, Sender.withGps(messageEmail, gps))
                parts += r.msg
                if (r.ok) pendingEmail = false
            } else {
                parts += "E-mail : en attente d'une connexion Internet"
            }
        }

        // SMS : dès que le réseau mobile (SIM) est prêt
        if (pendingSms) {
            if (Net.cellularReady(this)) {
                val r = Sender.sendSms(this, Sender.withGps(messageSms, gps))
                parts += r.msg
                if (r.ok) pendingSms = false
            } else {
                parts += "SMS : en attente du réseau mobile"
            }
        }

        if (!pendingSms && !pendingEmail) {
            // Tout est parti
            enabled = false
            Scheduler.cancelAll(this)
            ServerWatch.disarm(this) // le téléphone a envoyé → on évite le doublon serveur
            lastStatus = "Alerte envoyée\n" + parts.joinToString("\n")
            Notifications.showStatus(this, "Alerte envoyée", parts.joinToString("\n"))
            return
        }

        // Il reste au moins un canal à envoyer → ré-essai
        retryCount += 1
        if (retryCount >= MAX_RETRIES) {
            enabled = false
            Scheduler.cancelAll(this)
            lastStatus = "Envoi incomplet (délai de ré-essai dépassé)\n" + parts.joinToString("\n")
            Notifications.showStatus(
                this,
                "Envoi incomplet",
                "Impossible d'envoyer tous les canaux dans le délai imparti.\n" + parts.joinToString("\n")
            )
        } else {
            Scheduler.scheduleRetry(this)
            lastStatus = "Envoi en attente de connexion (essai $retryCount)\n" + parts.joinToString("\n")
            Notifications.showStatus(
                this,
                "Envoi en attente",
                "En attente d'une connexion pour terminer l'envoi.\n" + parts.joinToString("\n")
            )
        }
    }
}
