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

    // --- Intervalles (en minutes) ---
    var Context.intervalMinutes: Long
        get() = p(this).getLong("intervalMinutes", 24 * 60)
        set(v) { p(this).edit().putLong("intervalMinutes", v).apply() }

    var Context.graceMinutes: Long
        get() = p(this).getLong("graceMinutes", 6 * 60)
        set(v) { p(this).edit().putLong("graceMinutes", v).apply() }

    // --- Canaux d'envoi (indépendants : SMS, e-mail, ou les deux) ---
    var Context.sendSms: Boolean
        get() = p(this).getBoolean("sendSms", false)
        set(v) { p(this).edit().putBoolean("sendSms", v).apply() }

    var Context.sendEmail: Boolean
        get() = p(this).getBoolean("sendEmail", true)
        set(v) { p(this).edit().putBoolean("sendEmail", v).apply() }

    // --- Destinataires ---
    var Context.recipientSms: String
        get() = p(this).getString("recipientSms", "") ?: ""
        set(v) { p(this).edit().putString("recipientSms", v).apply() }

    var Context.recipientEmail: String
        get() = p(this).getString("recipientEmail", "") ?: ""
        set(v) { p(this).edit().putString("recipientEmail", v).apply() }

    // --- Contenu ---
    var Context.messageText: String
        get() = p(this).getString("messageText", "") ?: ""
        set(v) { p(this).edit().putString("messageText", v).apply() }

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
}
