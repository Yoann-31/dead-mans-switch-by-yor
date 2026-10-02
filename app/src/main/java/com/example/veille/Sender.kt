package com.example.veille

import android.content.Context
import android.net.Uri
import android.telephony.SmsManager
import com.example.veille.Prefs.attachGps
import com.example.veille.Prefs.messageEmail
import com.example.veille.Prefs.messageSms
import com.example.veille.Prefs.photoUri
import com.example.veille.Prefs.recipientEmail
import com.example.veille.Prefs.recipientSms
import com.example.veille.Prefs.sendEmail
import com.example.veille.Prefs.sendSms
import com.example.veille.Prefs.smtpHost
import com.example.veille.Prefs.smtpPass
import com.example.veille.Prefs.smtpPort
import com.example.veille.Prefs.smtpUser
import com.example.veille.Prefs.subject
import java.util.Properties
import javax.activation.DataHandler
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart
import javax.mail.util.ByteArrayDataSource

/**
 * Envoi par canal. Chaque fonction renvoie un résultat (ok + message) pour que
 * l'appelant (SendService) sache quoi re-essayer.
 */
object Sender {

    data class Res(val ok: Boolean, val msg: String)

    private fun parseRecipients(raw: String): List<String> =
        raw.split(Regex("[,;\\n\\r]"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    /** Lien de position (fraîche → validation → dernière connue). */
    fun gpsLink(ctx: Context): String? {
        if (!ctx.attachGps) return null
        return Locator.getFreshLink(ctx, 15_000L)
            ?: Locator.savedLink(ctx)
            ?: Locator.getLocationLink(ctx)
    }

    fun withGps(message: String, gps: String?): String =
        if (gps != null) message + "\n\nPosition : " + gps else message

    /** Envoi immédiat des deux canaux (utilisé par le bouton « Tester l'envoi »). */
    fun send(ctx: Context): String {
        val gps = gpsLink(ctx)
        val results = mutableListOf<String>()
        if (ctx.sendSms) results += sendSms(ctx, withGps(ctx.messageSms, gps)).msg
        if (ctx.sendEmail) results += sendEmail(ctx, withGps(ctx.messageEmail, gps)).msg
        if (results.isEmpty()) return "Aucun canal d'envoi activé."
        return results.joinToString("\n")
    }

    fun sendSms(ctx: Context, body: String): Res {
        val tos = parseRecipients(ctx.recipientSms)
        if (tos.isEmpty()) return Res(false, "SMS : aucun numéro de destinataire.")
        return try {
            val sms = if (android.os.Build.VERSION.SDK_INT >= 31) {
                ctx.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            for (to in tos) {
                val parts = sms.divideMessage(body)
                sms.sendMultipartTextMessage(to, null, parts, null, null)
            }
            Res(true, "SMS envoyé à ${tos.size} destinataire(s)")
        } catch (e: Exception) {
            Res(false, "Échec SMS : ${e.message}")
        }
    }

    fun sendEmail(ctx: Context, body: String): Res {
        val tos = parseRecipients(ctx.recipientEmail)
        if (tos.isEmpty()) return Res(false, "E-mail : aucune adresse destinataire.")
        val user = ctx.smtpUser.trim()
        val pass = ctx.smtpPass
        val host = ctx.smtpHost.trim()
        val port = ctx.smtpPort
        if (user.isEmpty() || pass.isEmpty() || host.isEmpty()) {
            return Res(false, "E-mail : paramètres SMTP incomplets.")
        }

        return try {
            val props = Properties().apply {
                put("mail.smtp.auth", "true")
                put("mail.smtp.host", host)
                put("mail.smtp.port", port.toString())
                if (port == 465) {
                    put("mail.smtp.socketFactory.port", port.toString())
                    put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
                    put("mail.smtp.ssl.enable", "true")
                } else {
                    put("mail.smtp.starttls.enable", "true")
                }
            }

            val session = Session.getInstance(props, object : Authenticator() {
                override fun getPasswordAuthentication() =
                    PasswordAuthentication(user, pass)
            })

            val msg = MimeMessage(session).apply {
                setFrom(InternetAddress(user))
                for (to in tos) addRecipient(Message.RecipientType.TO, InternetAddress(to))
                subject = ctx.subject
            }

            val textPart = MimeBodyPart().apply { setText(body, "utf-8") }
            val multipart = MimeMultipart().apply { addBodyPart(textPart) }

            val uriStr = ctx.photoUri
            if (uriStr.isNotEmpty()) {
                try {
                    val uri = Uri.parse(uriStr)
                    val bytes = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    if (bytes != null) {
                        val type = ctx.contentResolver.getType(uri) ?: "image/jpeg"
                        val attach = MimeBodyPart().apply {
                            dataHandler = DataHandler(ByteArrayDataSource(bytes, type))
                            fileName = "photo." + (type.substringAfter('/').ifEmpty { "jpg" })
                        }
                        multipart.addBodyPart(attach)
                    }
                } catch (_: Exception) { }
            }

            msg.setContent(multipart)
            Transport.send(msg)
            Res(true, "E-mail envoyé à ${tos.size} destinataire(s)")
        } catch (e: Exception) {
            Res(false, "Échec e-mail : ${e.message}")
        }
    }
}
