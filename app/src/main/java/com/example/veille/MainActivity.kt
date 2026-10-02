package com.example.veille

import android.Manifest
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
import com.example.veille.Prefs.lastStatus
import com.example.veille.Prefs.messageEmail
import com.example.veille.Prefs.messageSms
import com.example.veille.Prefs.nextCheckInAt
import com.example.veille.Prefs.recipientEmail
import com.example.veille.Prefs.recipientSms
import com.example.veille.Prefs.sendEmail
import com.example.veille.Prefs.sendSms
import com.example.veille.Prefs.smtpUser
import com.example.veille.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding

    private val requestPerms =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        Notifications.ensureChannels(this)
        requestStartupPermissions()

        b.powerBtn.setOnClickListener { toggleSurveillance() }

        b.validateBtn.setOnClickListener {
            Validator.validatePresence(this)
            Toast.makeText(this, "Présence validée", Toast.LENGTH_SHORT).show()
            refreshUi()
        }

        b.settingsBtn.setOnClickListener {
            if (enabled) {
                Toast.makeText(
                    this,
                    "Désactivez la surveillance pour modifier les réglages.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                startActivity(Intent(this, SettingsActivity::class.java))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshUi()
    }

    private fun requestStartupPermissions() {
        val need = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                need += Manifest.permission.POST_NOTIFICATIONS
            }
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED) {
            need += Manifest.permission.SEND_SMS
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            need += Manifest.permission.ACCESS_FINE_LOCATION
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            need += Manifest.permission.CAMERA
        }
        if (need.isNotEmpty()) requestPerms.launch(need.toTypedArray())
    }

    private fun toggleSurveillance() {
        if (enabled) {
            enabled = false
            Scheduler.cancelAll(this)
            Notifications.cancelCheckIn(this)
            ServerWatch.disarm(this)
            lastStatus = "Surveillance désactivée"
            refreshUi()
            return
        }

        if (!validateConfig()) return

        if (!Scheduler.canScheduleExact(this)) {
            promptExactAlarm()
            return
        }

        enabled = true
        Scheduler.start(this)
        ServerWatch.checkin(this)
        lastStatus = "Surveillance activée"
        refreshUi()
    }

    private fun validateConfig(): Boolean {
        if (!sendSms && !sendEmail) {
            openSettings("Activez au moins un canal (SMS ou e-mail) dans les réglages.")
            return false
        }
        if (sendSms && (recipientSms.isBlank() || messageSms.isBlank())) {
            openSettings("Complétez le SMS (destinataire et message).")
            return false
        }
        if (sendEmail && (recipientEmail.isBlank() || messageEmail.isBlank() || smtpUser.isBlank())) {
            openSettings("Complétez l'e-mail (destinataire, message et SMTP).")
            return false
        }
        return true
    }

    private fun openSettings(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    private fun promptExactAlarm() {
        Toast.makeText(
            this,
            "Autorisez les alarmes exactes pour une surveillance fiable.",
            Toast.LENGTH_LONG
        ).show()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                startActivity(
                    Intent(
                        Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        Uri.parse("package:$packageName")
                    )
                )
            } catch (_: Exception) {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            }
        }
    }

    private fun refreshUi() {
        val active = enabled

        b.powerBtn.setBackgroundResource(if (active) R.drawable.circle_on else R.drawable.circle_off)
        b.powerBtn.text = if (active) "ACTIVE" else "INACTIVE"
        b.hint.text = if (active)
            "Appuyez pour désactiver la surveillance"
        else
            "Appuyez pour activer la surveillance"

        // Roue accessible seulement si désactivé
        b.settingsBtn.isEnabled = !active
        b.settingsBtn.alpha = if (active) 0.3f else 1f

        // Bouton de validation : disponible dès que la surveillance est active
        b.validateBtn.visibility = if (active) View.VISIBLE else View.GONE

        val fmt = SimpleDateFormat("dd/MM 'à' HH:mm", Locale.getDefault())
        if (active) {
            b.statusLine.text = "● Surveillance active"
            val sb = StringBuilder()
            if (deadlineAt > 0) sb.append("Envoi si absence avant ${fmt.format(Date(deadlineAt))}\n")
            if (nextCheckInAt > 0) sb.append("Prochain rappel : ${fmt.format(Date(nextCheckInAt))}\n")
            sb.append(channelsSummary())
            b.statusDetail.text = sb.toString().trim()
        } else {
            b.statusLine.text = "○ Surveillance inactive"
            val detail = if (lastStatus.isNotEmpty())
                lastStatus
            else
                "Configurez l'application via la roue en haut à droite."
            b.statusDetail.text = detail
        }
    }

    private fun channelsSummary(): String {
        val canaux = buildList {
            if (sendSms) add("SMS")
            if (sendEmail) add("e-mail")
        }
        return if (canaux.isEmpty()) "" else "Canaux : ${canaux.joinToString(" + ")}"
    }
}
