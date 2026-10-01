package com.example.veille

import android.content.Context
import android.net.Uri
import android.telephony.SmsManager
import com.example.veille.Prefs.attachGps
import com.example.veille.Prefs.messageText
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
 * Envoi sur les canaux activés (SMS et/ou e-mail), avec destinataires
 * multiples et, en option, la position GPS jointe au message.
 */
object Sender {

    /** Découpe une saisie en plusieurs destinataires (séparés par , ; ou retour ligne). */
    private fun parseRecipients(raw: String): List<String> =
        raw.split(Regex("[,;\\n\\r]"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    private fun buildBody(ctx: Context): String {
        // 1) tentative d'une position fraîche (max 15 s) ; 2) sinon position figée à la
        // validation ; 3) sinon dernière position connue.
        val gps = if (ctx.attachGps) {
            Locator.getFreshLink(ctx, 15_000L)
                ?: Locator.savedLink(ctx)
                ?: Locator.getLocationLink(ctx)
        } else null
        return if (gps != null) ctx.messageText + "\n\nPosition : " + gps else ctx.messageText
    }

    fun send(ctx: Context): String {
        val body = buildBody(ctx)
        val results = mutableListOf<String>()
        if (ctx.sendSms) results += sendSms(ctx, body)
        if (ctx.sendEmail) results += sendEmail(ctx, body)
        if (results.isEmpty()) return "Aucun canal d'envoi activé."
        return results.joinToString("\n")
    }

    private fun sendSms(ctx: Context, body: String): String {
        val tos = parseRecipients(ctx.recipientSms)
        if (tos.isEmpty()) return "SMS : aucun numéro de destinataire."
        return try {
            val sms = if (android.os.Build.VERSION.SDK_INT >= 31) {
                ctx.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            var ok = 0
            val errors = mutableListOf<String>()
            for (to in tos) {
                try {
                    val parts = sms.divideMessage(body)
                    sms.sendMultipartTextMessage(to, null, parts, null, null)
                    ok++
                } catch (e: Exception) {
                    errors.add("$to (${e.message})")
                }
            }
            val base = "SMS envoyé à $ok/${tos.size} destinataire(s)"
            if (errors.isEmpty()) base else "$base — échecs : ${errors.joinToString(", ")}"
        } catch (e: Exception) {
            "Échec SMS : ${e.message}"
        }
    }

    private fun sendEmail(ctx: Context, body: String): String {
        val tos = parseRecipients(ctx.recipientEmail)
        if (tos.isEmpty()) return "E-mail : aucune adresse destinataire."
        val user = ctx.smtpUser.trim()
        val pass = ctx.smtpPass
        val host = ctx.smtpHost.trim()
        val port = ctx.smtpPort
        if (user.isEmpty() || pass.isEmpty() || host.isEmpty()) {
            return "E-mail : paramètres SMTP incomplets."
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
                for (to in tos) {
                    addRecipient(Message.RecipientType.TO, InternetAddress(to))
                }
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
            "E-mail envoyé à ${tos.size} destinataire(s)"
        } catch (e: Exception) {
            "Échec e-mail : ${e.message}"
        }
    }
}
