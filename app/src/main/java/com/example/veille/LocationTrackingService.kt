package com.example.veille

import android.Manifest
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.veille.Prefs.savedLat
import com.example.veille.Prefs.savedLng
import com.example.veille.Prefs.savedLocAt

/**
 * Service en avant-plan qui, pendant les minutes précédant l'échéance,
 * met à jour en continu la position et enregistre la plus récente dans Prefs.
 * Démarré à T−10 min (ou au début de la fenêtre si celle-ci est plus courte),
 * arrêté à la validation ou au moment de l'envoi.
 */
class LocationTrackingService : Service() {

    companion object {
        private const val NOTIF_ID = 3002

        fun start(ctx: Context) {
            val granted =
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
            val i = Intent(ctx, LocationTrackingService::class.java)
            ContextCompat.startForegroundService(ctx, i)
        }

        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, LocationTrackingService::class.java))
        }
    }

    private var lm: LocationManager? = null
    private var listener: LocationListener? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Notifications.ensureChannels(this)
        val notif = NotificationCompat.Builder(this, Notifications.CHANNEL_STATUS)
            .setSmallIcon(R.drawable.ic_stat_shield)
            .setContentTitle("Localisation en cours")
            .setContentText("Mise à jour de la position avant l'échéance.")
            .setOngoing(true)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
            } else {
                startForeground(NOTIF_ID, notif)
            }
        } catch (e: Exception) {
            stopSelf()
            return START_NOT_STICKY
        }

        startUpdates()
        return START_STICKY
    }

    private fun startUpdates() {
        try {
            val manager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            lm = manager
            val l = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    this@LocationTrackingService.savedLat = location.latitude.toString()
                    this@LocationTrackingService.savedLng = location.longitude.toString()
                    this@LocationTrackingService.savedLocAt = location.time
                }
                override fun onProviderDisabled(provider: String) {}
                override fun onProviderEnabled(provider: String) {}
                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            }
            listener = l
            val looper = Looper.getMainLooper()
            for (p in listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)) {
                if (!manager.isProviderEnabled(p)) continue
                @Suppress("MissingPermission")
                manager.requestLocationUpdates(p, 15_000L, 0f, l, looper)
            }
        } catch (_: Exception) { }
    }

    override fun onDestroy() {
        try { listener?.let { lm?.removeUpdates(it) } } catch (_: Exception) { }
        super.onDestroy()
    }
}
