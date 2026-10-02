package com.example.veille

import android.content.Context
import com.example.veille.Prefs.messageEmail
import com.example.veille.Prefs.messageSms
import com.example.veille.Prefs.recipientEmail
import com.example.veille.Prefs.sendDelayMinutes
import com.example.veille.Prefs.sendEmail
import com.example.veille.Prefs.sendSms
import com.example.veille.Prefs.serverDeviceId
import com.example.veille.Prefs.serverEnabled
import com.example.veille.Prefs.serverToken
import com.example.veille.Prefs.serverUrl
import com.example.veille.Prefs.subject
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

/**
 * Filet serveur : "pingue" un script Google Apps Script.
 *  - checkin() : à l'activation et à chaque validation → repousse l'échéance serveur.
 *  - disarm()  : à la désactivation, ou après un envoi réussi du téléphone → évite le doublon.
 *
 * Le serveur attend un peu plus longtemps que le téléphone (marge) pour laisser
 * le téléphone envoyer en premier s'il est vivant.
 */
object ServerWatch {

    private const val MARGIN_MS = 2L * 60L * 60L * 1000L // 2 h de marge

    fun checkin(ctx: Context) {
        if (!ctx.serverEnabled || ctx.serverUrl.isBlank()) return
        val fireAt = System.currentTimeMillis() + ctx.sendDelayMinutes * 60_000L + MARGIN_MS
        val pos = Locator.savedLink(ctx) ?: Locator.getLocationLink(ctx) ?: ""

        // Cas serveur (Google) : l'e-mail d'alerte reprend le message SMS + le message e-mail.
        // (L'envoi normal par le téléphone garde, lui, un message propre à chaque canal.)
        val msgs = mutableListOf<String>()
        if (ctx.sendSms && ctx.messageSms.isNotBlank()) msgs += ctx.messageSms
        if (ctx.sendEmail && ctx.messageEmail.isNotBlank()) msgs += ctx.messageEmail
        val combined = if (msgs.isEmpty()) ctx.messageEmail else msgs.joinToString("\n\n")

        val body = JSONObject().apply {
            put("token", ctx.serverToken)
            put("id", ctx.serverDeviceId)
            put("label", "Dead Man's Switch by YRO")
            put("action", "checkin")
            put("fireAt", fireAt)
            put("to", ctx.recipientEmail)
            put("subject", ctx.subject)
            put("message", combined)
            put("position", pos)
        }
        post(ctx.serverUrl, body.toString())
    }

    fun disarm(ctx: Context) {
        if (!ctx.serverEnabled || ctx.serverUrl.isBlank()) return
        val body = JSONObject().apply {
            put("token", ctx.serverToken)
            put("id", ctx.serverDeviceId)
            put("action", "disarm")
        }
        post(ctx.serverUrl, body.toString())
    }

    private fun post(urlStr: String, payload: String) {
        thread {
            try {
                val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    connectTimeout = 15000
                    readTimeout = 15000
                    instanceFollowRedirects = true
                    setRequestProperty("Content-Type", "application/json")
                }
                conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
                conn.inputStream.use { it.readBytes() } // consomme la réponse
                conn.disconnect()
            } catch (_: Exception) {
                // silencieux : le check-in échoué sera renvoyé à la prochaine validation
            }
        }
    }
}
