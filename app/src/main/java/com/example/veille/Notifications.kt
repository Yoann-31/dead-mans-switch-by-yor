package com.example.veille

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.veille.Prefs.deadlineAt
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Notifications {

    const val CHANNEL_CHECKIN = "checkin"
    const val CHANNEL_STATUS = "status"
    const val NOTIF_CHECKIN = 2001
    const val NOTIF_STATUS = 2002

    fun ensureChannels(ctx: Context) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val checkin = NotificationChannel(
            CHANNEL_CHECKIN, "Relance de validation",
            NotificationManager.IMPORTANCE_HIGH
        ).apply { description = "Vous demande de confirmer votre présence" }
        val status = NotificationChannel(
            CHANNEL_STATUS, "État",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        nm.createNotificationChannel(checkin)
        nm.createNotificationChannel(status)
    }

    /** Notification de relance avec le bouton « Je suis là ». */
    fun showCheckIn(ctx: Context) {
        ensureChannels(ctx)

        val validateIntent = Intent(ctx, ActionReceiver::class.java).apply {
            action = ActionReceiver.ACTION_VALIDATE
        }
        val validatePI = PendingIntent.getBroadcast(
            ctx, 0, validateIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openPI = PendingIntent.getActivity(
            ctx, 1, Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
        val before = fmt.format(Date(ctx.deadlineAt))

        val n = NotificationCompat.Builder(ctx, CHANNEL_CHECKIN)
            .setSmallIcon(R.drawable.ic_stat_shield)
            .setContentTitle("Confirmez votre présence")
            .setContentText("Validez avant $before, sinon le message configuré sera envoyé.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Appuyez sur « Je suis là » avant $before. " +
                    "Sans validation, le message que vous avez paramétré sera envoyé au destinataire."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(openPI)
            .setAutoCancel(false)
            .setOngoing(true)
            .addAction(R.drawable.ic_stat_shield, "Je suis là", validatePI)
            .build()

        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_CHECKIN, n)
    }

    fun cancelCheckIn(ctx: Context) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(NOTIF_CHECKIN)
    }

    fun showStatus(ctx: Context, title: String, text: String) {
        ensureChannels(ctx)
        val n = NotificationCompat.Builder(ctx, CHANNEL_STATUS)
            .setSmallIcon(R.drawable.ic_stat_shield)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .build()
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_STATUS, n)
    }
}
