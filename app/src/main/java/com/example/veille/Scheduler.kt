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
        val at = System.currentTimeMillis() + ctx.graceMinutes * 60_000L
        ctx.deadlineAt = at
        ctx.awaitingValidation = true
        setExact(ctx, at, deadlinePI(ctx))
    }

    /** Rétablit une échéance dans N minutes (utilisé après un redémarrage). */
    fun scheduleDeadlineIn(ctx: Context, minutes: Long) {
        val at = System.currentTimeMillis() + minutes * 60_000L
        ctx.deadlineAt = at
        ctx.awaitingValidation = true
        setExact(ctx, at, deadlinePI(ctx))
    }

    fun cancelDeadline(ctx: Context) {
        am(ctx).cancel(deadlinePI(ctx))
    }

    fun cancelAll(ctx: Context) {
        am(ctx).cancel(checkInPI(ctx))
        am(ctx).cancel(deadlinePI(ctx))
        ctx.awaitingValidation = false
    }
}
