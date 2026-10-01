package com.example.veille

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import com.example.veille.Prefs.savedLat
import com.example.veille.Prefs.savedLng
import com.example.veille.Prefs.savedLocAt
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Gestion de la position :
 *  - saveCurrent()   : enregistre la dernière position connue (appelé à la validation)
 *  - savedLink()     : lien Maps de la position enregistrée à la validation
 *  - getFreshLink()  : tente une acquisition GPS fraîche (avec délai), sinon null
 *  - getLocationLink(): lien Maps de la dernière position connue (repli)
 */
object Locator {

    private fun hasPermission(ctx: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            ctx, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            ctx, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    private fun link(loc: Location) = "https://maps.google.com/?q=${loc.latitude},${loc.longitude}"

    private fun bestLastKnown(ctx: Context): Location? {
        if (!hasPermission(ctx)) return null
        return try {
            val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val providers = listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER
            )
            var best: Location? = null
            for (p in providers) {
                if (!lm.isProviderEnabled(p)) continue
                @Suppress("MissingPermission")
                val loc = lm.getLastKnownLocation(p) ?: continue
                if (best == null || loc.time > best!!.time) best = loc
            }
            best
        } catch (e: Exception) {
            null
        }
    }

    /** Enregistre la dernière position connue (quand l'utilisateur valide). */
    fun saveCurrent(ctx: Context) {
        val loc = bestLastKnown(ctx) ?: return
        ctx.savedLat = loc.latitude.toString()
        ctx.savedLng = loc.longitude.toString()
        ctx.savedLocAt = loc.time
    }

    /** Lien Maps de la position enregistrée à la dernière validation, ou null. */
    fun savedLink(ctx: Context): String? {
        val la = ctx.savedLat
        val lo = ctx.savedLng
        return if (la.isNotEmpty() && lo.isNotEmpty()) "https://maps.google.com/?q=$la,$lo" else null
    }

    /** Lien Maps de la dernière position connue, ou null. */
    fun getLocationLink(ctx: Context): String? {
        val loc = bestLastKnown(ctx) ?: return null
        return link(loc)
    }

    /**
     * Tente d'obtenir une position FRAÎCHE en déclenchant une acquisition active,
     * en attendant au plus [timeoutMs]. Retourne le lien Maps, ou null si échec/délai.
     * À appeler depuis un thread secondaire (bloquant).
     */
    fun getFreshLink(ctx: Context, timeoutMs: Long): String? {
        if (!hasPermission(ctx)) return null
        return try {
            val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
                .filter { try { lm.isProviderEnabled(it) } catch (e: Exception) { false } }
            if (providers.isEmpty()) return null

            val latch = CountDownLatch(1)
            val holder = arrayOfNulls<Location>(1)
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    if (holder[0] == null) {
                        holder[0] = location
                        latch.countDown()
                    }
                }
                override fun onProviderDisabled(provider: String) {}
                override fun onProviderEnabled(provider: String) {}
                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            }

            val looper = Looper.getMainLooper()
            for (p in providers) {
                try {
                    @Suppress("MissingPermission", "DEPRECATION")
                    lm.requestSingleUpdate(p, listener, looper)
                } catch (_: Exception) { }
            }

            latch.await(timeoutMs, TimeUnit.MILLISECONDS)
            try { lm.removeUpdates(listener) } catch (_: Exception) { }

            holder[0]?.let { link(it) }
        } catch (e: Exception) {
            null
        }
    }
}
