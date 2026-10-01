package com.example.veille

import android.content.Context

/**
 * Stockage local de la configuration (SharedPreferences).
 * Rien ne quitte l'appareil tant que le déclenchement n'a pas lieu.
 */
object Prefs {
    private const val FILE = "veille_config"

    const val METHOD_SMS = "SMS"
    const val METHOD_EMAIL = "EMAIL"

    private fun p(ctx: Context) =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    // --- Activation ---
    var Context.enabled: Boolean
        get() = p(this).getBoolean("enabled", false)
        set(v) { p(this).edit().putBoolean("enabled", v).apply() }

    // --- Intervalles (en minutes) ---
    var Context.intervalMinutes: Long
        get() = p(this).getLong("intervalMinutes", 24 * 60) // défaut : 1 jour
        set(v) { p(this).edit().putLong("intervalMinutes", v).apply() }

    var Context.graceMinutes: Long
        get() = p(this).getLong("graceMinutes", 6 * 60) // défaut : 6 h pour valider
        set(v) { p(this).edit().putLong("graceMinutes", v).apply() }

    // --- Destinataire et contenu ---
    var Context.method: String
        get() = p(this).getString("method", METHOD_EMAIL) ?: METHOD_EMAIL
        set(v) { p(this).edit().putString("method", v).apply() }

    var Context.recipient: String
        get() = p(this).getString("recipient", "") ?: ""
        set(v) { p(this).edit().putString("recipient", v).apply() }

    var Context.messageText: String
        get() = p(this).getString("messageText", "") ?: ""
        set(v) { p(this).edit().putString("messageText", v).apply() }

    var Context.subject: String
        get() = p(this).getString("subject", "Message important") ?: "Message important"
        set(v) { p(this).edit().putString("subject", v).apply() }

    var Context.photoUri: String
        get() = p(this).getString("photoUri", "") ?: ""
        set(v) { p(this).edit().putString("photoUri", v).apply() }

    // --- Paramètres SMTP (pour l'e-mail) ---
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
