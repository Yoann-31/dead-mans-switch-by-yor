package com.example.veille

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.veille.Prefs.awaitingValidation
import com.example.veille.Prefs.deadlineAt
import com.example.veille.Prefs.graceMinutes
import com.example.veille.Prefs.intervalMinutes
import com.example.veille.Prefs.nextCheckInAt

/**
 * Programme les deux alarmes exactes :
 *  - la relance périodique (CheckInReceiver)
 *  - l'échéance de grâce (DeadlineReceiver)
 */
object Scheduler {

    private const val REQ_CHECKIN = 1001
    private const val REQ_DEADLINE = 1002
    private const val REQ_PRETRACK = 1003

    /** Minutes de suivi GPS actif avant l'échéance. */
    private const val PRE_TRACK_MINUTES = 10L

    private fun am(ctx: Context) =
        ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private fun preTrackPI(ctx: Context): PendingIntent {
        val i = Intent(ctx, PreTrackReceiver::class.java)
        return PendingIntent.getBroadcast(
            ctx, REQ_PRETRACK, i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

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
            // Repli si l'autorisation d'alarme exacte manque
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    /** Programme la prochaine relance à maintenant + intervalle. */
    fun scheduleNextCheckIn(ctx: Context) {
        val at = System.currentTimeMillis() + ctx.intervalMinutes * 60_000L
        ctx.nextCheckInAt = at
        ctx.awaitingValidation = false
        cancelDeadline(ctx)
        setExact(ctx, at, checkInPI(ctx))
    }

    /** Appelé quand la relance se déclenche : ouvre le délai de grâce. */
    fun scheduleDeadline(ctx: Context) {
        val now = System.currentTimeMillis()
        val at = now + ctx.graceMinutes * 60_000L
        ctx.deadlineAt = at
        ctx.awaitingValidation = true
        setExact(ctx, at, deadlinePI(ctx))
        schedulePreTrack(ctx, now, at)
    }

    /** Rétablit une échéance dans N minutes (utilisé après un redémarrage). */
    fun scheduleDeadlineIn(ctx: Context, minutes: Long) {
        val now = System.currentTimeMillis()
        val at = now + minutes * 60_000L
        ctx.deadlineAt = at
        ctx.awaitingValidation = true
        setExact(ctx, at, deadlinePI(ctx))
        schedulePreTrack(ctx, now, at)
    }

    /** Programme le suivi GPS à T−10 min (ou le démarre tout de suite si la fenêtre est plus courte). */
    private fun schedulePreTrack(ctx: Context, now: Long, deadline: Long) {
        val trackAt = deadline - PRE_TRACK_MINUTES * 60_000L
        if (trackAt <= now) {
            LocationTrackingService.start(ctx)
        } else {
            setExact(ctx, trackAt, preTrackPI(ctx))
        }
    }

    fun cancelDeadline(ctx: Context) {
        am(ctx).cancel(deadlinePI(ctx))
        am(ctx).cancel(preTrackPI(ctx))
        LocationTrackingService.stop(ctx)
    }

    fun cancelAll(ctx: Context) {
        am(ctx).cancel(checkInPI(ctx))
        am(ctx).cancel(deadlinePI(ctx))
        am(ctx).cancel(preTrackPI(ctx))
        LocationTrackingService.stop(ctx)
        ctx.awaitingValidation = false
    }
}
