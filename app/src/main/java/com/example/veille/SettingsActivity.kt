package com.example.veille

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File
import com.example.veille.Prefs.attachGps
import com.example.veille.Prefs.enabled
import com.example.veille.Prefs.graceMinutes
import com.example.veille.Prefs.intervalMinutes
import com.example.veille.Prefs.lastStatus
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
import com.example.veille.databinding.ActivitySettingsBinding
import kotlin.concurrent.thread

class SettingsActivity : AppCompatActivity() {

    private lateinit var b: ActivitySettingsBinding

    private var cameraUri: Uri? = null

    private val takePhoto =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { ok: Boolean ->
            if (ok && cameraUri != null) {
                photoUri = cameraUri.toString()
                b.photoLabel.text = "Photo prise avec l'appareil"
            }
        }

    private val pickPhoto =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null) {
                try {
                    contentResolver.takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {}
                photoUri = uri.toString()
                b.photoLabel.text = "Photo : ${uri.lastPathSegment}"
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(b.root)

        // Sécurité : pas de modif pendant une surveillance active
        if (enabled) {
            Toast.makeText(this, "Surveillance active : réglages verrouillés.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        loadIntoUi()

        b.switchSms.setOnCheckedChangeListener { _, _ -> updateVisibility() }
        b.switchEmail.setOnCheckedChangeListener { _, _ -> updateVisibility() }
        updateVisibility()

        b.pickPhoto.setOnClickListener { pickPhoto.launch(arrayOf("image/*")) }

        b.takePhotoBtn.setOnClickListener {
            try {
                val dir = File(filesDir, "photos").apply { mkdirs() }
                val f = File(dir, "capture_${System.currentTimeMillis()}.jpg")
                val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", f)
                cameraUri = uri
                takePhoto.launch(uri)
            } catch (e: Exception) {
                Toast.makeText(
                    this,
                    "Impossible d'ouvrir l'appareil photo : ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        b.saveBtn.setOnClickListener {
            saveFromUi()
            Toast.makeText(this, "Configuration enregistrée", Toast.LENGTH_SHORT).show()
            finish()
        }

        b.testBtn.setOnClickListener {
            saveFromUi()
            Toast.makeText(this, "Envoi de test en cours…", Toast.LENGTH_SHORT).show()
            thread {
                val r = Sender.send(this)
                runOnUiThread {
                    Toast.makeText(this, r, Toast.LENGTH_LONG).show()
                    lastStatus = "Test : $r"
                }
            }
        }
    }

    private fun updateVisibility() {
        b.smsBlock.visibility = if (b.switchSms.isChecked) View.VISIBLE else View.GONE
        b.emailBlock.visibility = if (b.switchEmail.isChecked) View.VISIBLE else View.GONE
    }

    private fun loadIntoUi() {
        b.switchSms.isChecked = sendSms
        b.switchEmail.isChecked = sendEmail
        b.switchGps.isChecked = attachGps
        b.recipientSms.setText(recipientSms)
        b.recipientEmail.setText(recipientEmail)
        b.subject.setText(subject)
        b.messageText.setText(messageText)
        b.interval.setText(intervalMinutes.toString())
        b.grace.setText(graceMinutes.toString())
        b.smtpHost.setText(smtpHost)
        b.smtpPort.setText(smtpPort.toString())
        b.smtpUser.setText(smtpUser)
        b.smtpPass.setText(smtpPass)
        if (photoUri.isNotEmpty()) b.photoLabel.text = "Photo configurée"
    }

    private fun saveFromUi() {
        sendSms = b.switchSms.isChecked
        sendEmail = b.switchEmail.isChecked
        attachGps = b.switchGps.isChecked
        recipientSms = b.recipientSms.text.toString().trim()
        recipientEmail = b.recipientEmail.text.toString().trim()
        subject = b.subject.text.toString().trim().ifEmpty { "Message important" }
        messageText = b.messageText.text.toString()
        intervalMinutes = b.interval.text.toString().toLongOrNull()?.coerceAtLeast(1) ?: 1440
        graceMinutes = b.grace.text.toString().toLongOrNull()?.coerceAtLeast(1) ?: 360
        smtpHost = b.smtpHost.text.toString().trim()
        smtpPort = b.smtpPort.text.toString().toIntOrNull() ?: 587
        smtpUser = b.smtpUser.text.toString().trim()
        smtpPass = b.smtpPass.text.toString()
    }
}
