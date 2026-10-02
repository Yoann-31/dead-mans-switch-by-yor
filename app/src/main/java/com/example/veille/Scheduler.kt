package com.example.veille

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.veille.Prefs.awaitingValidation
import com.example.veille.Prefs.deadlineAt
import com.example.veille.Prefs.intervalMinutes
import com.example.veille.Prefs.nextCheckInAt
import com.example.veille.Prefs.sendDelayMinutes

/**
 * Modèle :
 *  - rappels RÉCURRENTS toutes les `intervalMinutes` (CheckInReceiver se réarme)
 *  - échéance d'envoi à `now + sendDelayMinutes`, RÉINITIALISÉE à chaque validation
 *  - suivi GPS actif pendant les 10 min avant l'échéance (PreTrackReceiver)
 */
object Scheduler {

    private const val REQ_CHECKIN = 1001
    private const val REQ_DEADLINE = 1002
    private const val REQ_PRETRACK = 1003
    private const val REQ_RETRY = 1004

    private const val PRE_TRACK_MINUTES = 10L
    private const val RETRY_INTERVAL_MS = 120_000L // ré-essai toutes les 2 min

    private fun am(ctx: Context) =
        ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private fun checkInPI(ctx: Context): PendingIntent {
        val i = Intent(ctx, CheckInReceiver::class.java)
        return PendingIntent.getBroadcast(
            ctx, REQ_CHECKIN, i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun deadlinePI(ctx: Context): PendingIntent {
        val i = Intent(ctx, DeadlineReceiver::class.java)
        return PendingIntent.getBroadcast(
            ctx, REQ_DEADLINE, i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun preTrackPI(ctx: Context): PendingIntent {
        val i = Intent(ctx, PreTrackReceiver::class.java)
        return PendingIntent.getBroadcast(
            ctx, REQ_PRETRACK, i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun retryPI(ctx: Context): PendingIntent {
        val i = Intent(ctx, RetryReceiver::class.java)
        return PendingIntent.getBroadcast(
            ctx, REQ_RETRY, i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** Reprogramme une tentative d'envoi dans 2 min (quand un canal attend la connexion). */
    fun scheduleRetry(ctx: Context) {
        setExact(ctx, System.currentTimeMillis() + RETRY_INTERVAL_MS, retryPI(ctx))
    }

    fun canScheduleExact(ctx: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            am(ctx).canScheduleExactAlarms()
        } else true
    }

    private fun setExact(ctx: Context, triggerAt: Long, pi: PendingIntent) {
        val manager = am(ctx)
        try {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } catch (se: SecurityException) {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    /** Démarre la surveillance : échéance dès l'activation + 1er rappel. */
    fun start(ctx: Context) {
        val now = System.currentTimeMillis()
        ctx.awaitingValidation = true
        armDeadline(ctx, now + ctx.sendDelayMinutes * 60_000L)
        armNextReminder(ctx, now)
    }

    /** Programme le prochain rappel à (fromNow + intervalle). */
    fun armNextReminder(ctx: Context, fromNow: Long) {
        val at = fromNow + ctx.intervalMinutes * 60_000L
        ctx.nextCheckInAt = at
        setExact(ctx, at, checkInPI(ctx))
    }

    /** (Ré)arme l'échéance d'envoi à l'instant absolu [at], et le pré-suivi GPS. */
    fun armDeadline(ctx: Context, at: Long) {
        ctx.deadlineAt = at
        setExact(ctx, at, deadlinePI(ctx))

        // (re)programme le pré-suivi GPS à T−10 min
        am(ctx).cancel(preTrackPI(ctx))
        LocationTrackingService.stop(ctx)
        val now = System.currentTimeMillis()
        val trackAt = at - PRE_TRACK_MINUTES * 60_000L
        if (trackAt <= now) {
            LocationTrackingService.start(ctx)
        } else {
            setExact(ctx, trackAt, preTrackPI(ctx))
        }
    }

    /** Appelé à chaque validation : réinitialise l'échéance et le prochain rappel (option A). */
    fun onValidated(ctx: Context) {
        val now = System.currentTimeMillis()
        armDeadline(ctx, now + ctx.sendDelayMinutes * 60_000L)
        armNextReminder(ctx, now)
    }

    fun cancelAll(ctx: Context) {
        am(ctx).cancel(checkInPI(ctx))
        am(ctx).cancel(deadlinePI(ctx))
        am(ctx).cancel(preTrackPI(ctx))
        am(ctx).cancel(retryPI(ctx))
        LocationTrackingService.stop(ctx)
        ctx.awaitingValidation = false
    }
}
