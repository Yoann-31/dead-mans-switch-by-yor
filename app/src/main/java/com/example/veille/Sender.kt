package com.example.veille

import android.content.Context
import android.net.Uri
import android.telephony.SmsManager
import com.example.veille.Prefs.messageText
import com.example.veille.Prefs.method
import com.example.veille.Prefs.photoUri
import com.example.veille.Prefs.recipient
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
 * Réalise l'envoi effectif selon la méthode configurée.
 * Retourne un message d'état lisible.
 */
object Sender {

    fun send(ctx: Context): String {
        return when (ctx.method) {
            Prefs.METHOD_SMS -> sendSms(ctx)
            else -> sendEmail(ctx)
        }
    }

    private fun sendSms(ctx: Context): String {
        val to = ctx.recipient.trim()
        val body = ctx.messageText
        if (to.isEmpty()) return "Échec : aucun numéro de destinataire."
        return try {
            val sms = if (android.os.Build.VERSION.SDK_INT >= 31) {
                ctx.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            val parts = sms.divideMessage(body)
            sms.sendMultipartTextMessage(to, null, parts, null, null)
            "SMS envoyé à $to"
        } catch (e: Exception) {
            "Échec de l'envoi du SMS : ${e.message}"
        }
    }

    private fun sendEmail(ctx: Context): String {
        val to = ctx.recipient.trim()
        if (to.isEmpty()) return "Échec : aucune adresse destinataire."
        val user = ctx.smtpUser.trim()
        val pass = ctx.smtpPass
        val host = ctx.smtpHost.trim()
        val port = ctx.smtpPort
        if (user.isEmpty() || pass.isEmpty() || host.isEmpty()) {
            return "Échec : paramètres SMTP incomplets."
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
                addRecipient(Message.RecipientType.TO, InternetAddress(to))
                subject = ctx.subject
            }

            val textPart = MimeBodyPart().apply {
                setText(ctx.messageText, "utf-8")
            }

            val multipart = MimeMultipart().apply { addBodyPart(textPart) }

            // Pièce jointe photo, si configurée
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
                } catch (_: Exception) {
                    // On envoie quand même l'e-mail sans la pièce jointe
                }
            }

            msg.setContent(multipart)
            Transport.send(msg)
            "E-mail envoyé à $to"
        } catch (e: Exception) {
            "Échec de l'envoi de l'e-mail : ${e.message}"
        }
    }
}
