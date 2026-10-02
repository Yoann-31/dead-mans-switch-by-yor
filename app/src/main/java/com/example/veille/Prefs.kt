package com.example.veille

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Stockage local CHIFFRÉ de la configuration.
 * Les valeurs (y compris le mot de passe SMTP) sont chiffrées au repos
 * via EncryptedSharedPreferences (clé maître dans le Keystore Android).
 * En cas d'indisponibilité du chiffrement, repli transparent sur un stockage simple.
 */
object Prefs {

    @Volatile private var cached: SharedPreferences? = null

    private fun p(ctx: Context): SharedPreferences {
        cached?.let { return it }
        synchronized(this) {
            cached?.let { return it }
            val c = ctx.applicationContext
            val sp = try {
                val mk = MasterKey.Builder(c)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
                EncryptedSharedPreferences.create(
                    c,
                    "veille_secure",
                    mk,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (e: Exception) {
                c.getSharedPreferences("veille_fallback", Context.MODE_PRIVATE)
            }
            cached = sp
            return sp
        }
    }

    // --- Activation ---
    var Context.enabled: Boolean
        get() = p(this).getBoolean("enabled", false)
        set(v) { p(this).edit().putBoolean("enabled", v).apply() }

    // --- Récurrence des notifications (valeur canonique en minutes) ---
    var Context.intervalMinutes: Long
        get() = p(this).getLong("intervalMinutes", 60) // défaut : 1 h
        set(v) { p(this).edit().putLong("intervalMinutes", v).apply() }

    // --- Temps d'absence avant envoi (valeur canonique en minutes) ---
    var Context.sendDelayMinutes: Long
        get() = p(this).getLong("sendDelayMinutes", 24 * 60) // défaut : 24 h
        set(v) { p(this).edit().putLong("sendDelayMinutes", v).apply() }

    // --- Saisie utilisateur : quantité + unité (min / h / j) pour l'affichage ---
    var Context.intervalAmount: Int
        get() = p(this).getInt("intervalAmount", 1)
        set(v) { p(this).edit().putInt("intervalAmount", v).apply() }

    var Context.intervalUnit: String
        get() = p(this).getString("intervalUnit", "h") ?: "h"
        set(v) { p(this).edit().putString("intervalUnit", v).apply() }

    var Context.sendDelayAmount: Int
        get() = p(this).getInt("sendDelayAmount", 24)
        set(v) { p(this).edit().putInt("sendDelayAmount", v).apply() }

    var Context.sendDelayUnit: String
        get() = p(this).getString("sendDelayUnit", "h") ?: "h"
        set(v) { p(this).edit().putString("sendDelayUnit", v).apply() }

    // --- Canaux d'envoi (indépendants : SMS, e-mail, ou les deux) ---
    var Context.sendSms: Boolean
        get() = p(this).getBoolean("sendSms", false)
        set(v) { p(this).edit().putBoolean("sendSms", v).apply() }

    var Context.sendEmail: Boolean
        get() = p(this).getBoolean("sendEmail", true)
        set(v) { p(this).edit().putBoolean("sendEmail", v).apply() }

    // Joindre la position GPS au message
    var Context.attachGps: Boolean
        get() = p(this).getBoolean("attachGps", true)
        set(v) { p(this).edit().putBoolean("attachGps", v).apply() }

    // --- Filet serveur (Google Apps Script) ---
    var Context.serverEnabled: Boolean
        get() = p(this).getBoolean("serverEnabled", false)
        set(v) { p(this).edit().putBoolean("serverEnabled", v).apply() }

    var Context.serverUrl: String
        get() = p(this).getString("serverUrl", "") ?: ""
        set(v) { p(this).edit().putString("serverUrl", v).apply() }

    var Context.serverToken: String
        get() = p(this).getString("serverToken", "") ?: ""
        set(v) { p(this).edit().putString("serverToken", v).apply() }

    // Identifiant unique de cette installation (généré une fois) : permet à un même
    // compte/script Google de suivre plusieurs téléphones/applications séparément.
    val Context.serverDeviceId: String
        get() {
            val cur = p(this).getString("serverDeviceId", "") ?: ""
            if (cur.isNotEmpty()) return cur
            val id = java.util.UUID.randomUUID().toString()
            p(this).edit().putString("serverDeviceId", id).apply()
            return id
        }

    // --- Destinataires ---
    var Context.recipientSms: String
        get() = p(this).getString("recipientSms", "") ?: ""
        set(v) { p(this).edit().putString("recipientSms", v).apply() }

    var Context.recipientEmail: String
        get() = p(this).getString("recipientEmail", "") ?: ""
        set(v) { p(this).edit().putString("recipientEmail", v).apply() }

    // --- Contenu : message spécifique par canal ---
    var Context.messageSms: String
        get() = p(this).getString("messageSms", "") ?: ""
        set(v) { p(this).edit().putString("messageSms", v).apply() }

    var Context.messageEmail: String
        get() = p(this).getString("messageEmail", "") ?: ""
        set(v) { p(this).edit().putString("messageEmail", v).apply() }

    var Context.subject: String
        get() = p(this).getString("subject", "Message important") ?: "Message important"
        set(v) { p(this).edit().putString("subject", v).apply() }

    var Context.photoUri: String
        get() = p(this).getString("photoUri", "") ?: ""
        set(v) { p(this).edit().putString("photoUri", v).apply() }

    // --- Paramètres SMTP (e-mail) ---
    var Context.smtpHost: String
        get() = p(this).getString("smtpHost", "smtp.gmail.com") ?: "smtp.gmail.com"
        set(v) { p(this).edit().putString("smtpHost", v).apply() }

    var Context.smtpPort: Int
        get() = p(this).getInt("smtpPort", 587)
        set(v) { p(this).edit().putInt("smtpPort", v).apply() }

    var Context.smtpUser: String
        get() = p(this).getString("smtpUser", "") ?: ""
        set(v) { p(this).edit().putString("smtpUser", v).apply() }

    var Context.smtpPass: String
        get() = p(this).getString("smtpPass", "") ?: ""
        set(v) { p(this).edit().putString("smtpPass", v).apply() }

    // --- État d'exécution ---
    var Context.nextCheckInAt: Long
        get() = p(this).getLong("nextCheckInAt", 0)
        set(v) { p(this).edit().putLong("nextCheckInAt", v).apply() }

    var Context.deadlineAt: Long
        get() = p(this).getLong("deadlineAt", 0)
        set(v) { p(this).edit().putLong("deadlineAt", v).apply() }

    var Context.awaitingValidation: Boolean
        get() = p(this).getBoolean("awaitingValidation", false)
        set(v) { p(this).edit().putBoolean("awaitingValidation", v).apply() }

    var Context.lastValidatedAt: Long
        get() = p(this).getLong("lastValidatedAt", 0)
        set(v) { p(this).edit().putLong("lastValidatedAt", v).apply() }

    var Context.lastStatus: String
        get() = p(this).getString("lastStatus", "") ?: ""
        set(v) { p(this).edit().putString("lastStatus", v).apply() }

    // --- Envoi en attente (ré-essai jusqu'à ce que la connexion soit dispo) ---
    var Context.pendingSms: Boolean
        get() = p(this).getBoolean("pendingSms", false)
        set(v) { p(this).edit().putBoolean("pendingSms", v).apply() }

    var Context.pendingEmail: Boolean
        get() = p(this).getBoolean("pendingEmail", false)
        set(v) { p(this).edit().putBoolean("pendingEmail", v).apply() }

    var Context.retryCount: Int
        get() = p(this).getInt("retryCount", 0)
        set(v) { p(this).edit().putInt("retryCount", v).apply() }

    // Dernière position enregistrée lors d'une validation de présence
    var Context.savedLat: String
        get() = p(this).getString("savedLat", "") ?: ""
        set(v) { p(this).edit().putString("savedLat", v).apply() }

    var Context.savedLng: String
        get() = p(this).getString("savedLng", "") ?: ""
        set(v) { p(this).edit().putString("savedLng", v).apply() }

    var Context.savedLocAt: Long
        get() = p(this).getLong("savedLocAt", 0)
        set(v) { p(this).edit().putLong("savedLocAt", v).apply() }
}
