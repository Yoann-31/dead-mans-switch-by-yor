package com.example.veille

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.veille.Prefs.deadlineAt
import com.example.veille.Prefs.enabled
import com.example.veille.Prefs.graceMinutes
import com.example.veille.Prefs.intervalMinutes
import com.example.veille.Prefs.lastStatus
import com.example.veille.Prefs.messageText
import com.example.veille.Prefs.method
import com.example.veille.Prefs.nextCheckInAt
import com.example.veille.Prefs.photoUri
import com.example.veille.Prefs.recipient
import com.example.veille.Prefs.smtpHost
import com.example.veille.Prefs.smtpPass
import com.example.veille.Prefs.smtpPort
import com.example.veille.Prefs.smtpUser
import com.example.veille.Prefs.subject
import com.example.veille.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding

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

    private val requestPerms =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        Notifications.ensureChannels(this)
        loadIntoUi()

        b.methodGroup.setOnCheckedChangeListener { _, _ -> updateSmtpVisibility() }
        updateSmtpVisibility()

        b.pickPhoto.setOnClickListener {
            pickPhoto.launch(arrayOf("image/*"))
        }

        b.saveBtn.setOnClickListener {
            saveFromUi()
            Toast.makeText(this, "Configuration enregistrée", Toast.LENGTH_SHORT).show()
        }

        b.toggleBtn.setOnClickListener {
            if (enabled) {
                enabled = false
                Scheduler.cancelAll(this)
                Notifications.cancelCheckIn(this)
                lastStatus = "Veille désactivée"
            } else {
                if (!validate()) return@setOnClickListener
                saveFromUi()
                ensurePermissions()
                if (!Scheduler.canScheduleExact(this)) {
                    promptExactAlarm()
                    return@setOnClickListener
                }
                enabled = true
                Scheduler.scheduleNextCheckIn(this)
                lastStatus = "Veille active"
            }
            refreshStatus()
        }

        b.testBtn.setOnClickListener {
            saveFromUi()
            Toast.makeText(this, "Envoi de test en cours…", Toast.LENGTH_SHORT).show()
            thread {
                val r = Sender.send(this)
                runOnUiThread {
                    Toast.makeText(this, r, Toast.LENGTH_LONG).show()
                    lastStatus = "Test : $r"
                    refreshStatus()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun updateSmtpVisibility() {
        val email = b.radioEmail.isChecked
        b.smtpBlock.visibility = if (email) View.VISIBLE else View.GONE
        b.subject.visibility = if (email) View.VISIBLE else View.GONE
        b.pickPhoto.visibility = if (email) View.VISIBLE else View.GONE
        b.photoLabel.visibility = if (email) View.VISIBLE else View.GONE
    }

    private fun loadIntoUi() {
        b.radioEmail.isChecked = method != Prefs.METHOD_SMS
        b.radioSms.isChecked = method == Prefs.METHOD_SMS
        b.recipient.setText(recipient)
        b.subject.setText(subject)
        b.messageText.setText(messageText)
        b.interval.setText(intervalMinutes.toString())
        b.grace.setText(graceMinutes.toString())
        b.smtpHost.setText(smtpHost)
        b.smtpPort.setText(smtpPort.toString())
        b.smtpUser.setText(smtpUser)
        b.smtpPass.setText(smtpPass)
        if (photoUri.isNotEmpty()) {
            b.photoLabel.text = "Photo configurée"
        }
    }

    private fun saveFromUi() {
        method = if (b.radioSms.isChecked) Prefs.METHOD_SMS else Prefs.METHOD_EMAIL
        recipient = b.recipient.text.toString().trim()
        subject = b.subject.text.toString().trim().ifEmpty { "Message important" }
        messageText = b.messageText.text.toString()
        intervalMinutes = b.interval.text.toString().toLongOrNull()?.coerceAtLeast(1) ?: 1440
        graceMinutes = b.grace.text.toString().toLongOrNull()?.coerceAtLeast(1) ?: 360
        smtpHost = b.smtpHost.text.toString().trim()
        smtpPort = b.smtpPort.text.toString().toIntOrNull() ?: 587
        smtpUser = b.smtpUser.text.toString().trim()
        smtpPass = b.smtpPass.text.toString()
    }

    private fun validate(): Boolean {
        if (b.recipient.text.toString().isBlank()) {
            Toast.makeText(this, "Renseignez un destinataire", Toast.LENGTH_SHORT).show()
            return false
        }
        if (b.messageText.text.toString().isBlank()) {
            Toast.makeText(this, "Renseignez un message", Toast.LENGTH_SHORT).show()
            return false
        }
        if (b.radioEmail.isChecked && b.smtpUser.text.toString().isBlank()) {
            Toast.makeText(this, "Renseignez les paramètres SMTP", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun ensurePermissions() {
        val need = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                need += Manifest.permission.POST_NOTIFICATIONS
            }
        }
        if (b.radioSms.isChecked &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED) {
            need += Manifest.permission.SEND_SMS
        }
        if (need.isNotEmpty()) requestPerms.launch(need.toTypedArray())
    }

    private fun promptExactAlarm() {
        Toast.makeText(
            this,
            "Autorisez les alarmes exactes pour une veille fiable.",
            Toast.LENGTH_LONG
        ).show()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    Uri.parse("package:$packageName")))
            } catch (_: Exception) {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            }
        }
    }

    private fun refreshStatus() {
        b.toggleBtn.text = if (enabled) "Désactiver la veille" else "Activer la veille"
        val fmt = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
        val sb = StringBuilder()
        sb.append(if (enabled) "● Veille ACTIVE\n" else "○ Veille inactive\n")
        if (enabled && nextCheckInAt > 0) {
            sb.append("Prochaine relance : ${fmt.format(Date(nextCheckInAt))}\n")
        }
        if (lastStatus.isNotEmpty()) sb.append(lastStatus)
        b.statusView.text = sb.toString()
    }
}
