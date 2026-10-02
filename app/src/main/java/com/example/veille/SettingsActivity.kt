package com.example.veille

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.transition.TransitionManager
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.example.veille.Prefs.attachGps
import com.example.veille.Prefs.enabled
import com.example.veille.Prefs.intervalAmount
import com.example.veille.Prefs.intervalMinutes
import com.example.veille.Prefs.intervalUnit
import com.example.veille.Prefs.lastStatus
import com.example.veille.Prefs.messageEmail
import com.example.veille.Prefs.messageSms
import com.example.veille.Prefs.photoUri
import com.example.veille.Prefs.recipientEmail
import com.example.veille.Prefs.recipientSms
import com.example.veille.Prefs.sendDelayAmount
import com.example.veille.Prefs.sendDelayMinutes
import com.example.veille.Prefs.sendDelayUnit
import com.example.veille.Prefs.sendEmail
import com.example.veille.Prefs.sendSms
import com.example.veille.Prefs.serverEnabled
import com.example.veille.Prefs.serverToken
import com.example.veille.Prefs.serverUrl
import com.example.veille.Prefs.smtpHost
import com.example.veille.Prefs.smtpPass
import com.example.veille.Prefs.smtpPort
import com.example.veille.Prefs.smtpUser
import com.example.veille.Prefs.subject
import com.example.veille.databinding.ActivitySettingsBinding
import java.io.File
import kotlin.concurrent.thread

class SettingsActivity : AppCompatActivity() {

    private lateinit var b: ActivitySettingsBinding

    private val unitCodes = listOf("min", "h", "j")
    private val unitLabels = listOf("Minutes", "Heures", "Jours")

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

        if (enabled) {
            Toast.makeText(this, "Surveillance active : réglages verrouillés.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setupUnitSpinners()
        setupCollapse(b.smsHeader, b.smsBody, b.smsChevron)
        setupCollapse(b.emailHeader, b.emailBody, b.emailChevron)
        setupCollapse(b.serverHeader, b.serverBody, b.serverChevron)
        setupCollapse(b.timerHeader, b.timerBody, b.timerChevron)
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
                Toast.makeText(this, "Impossible d'ouvrir l'appareil photo : ${e.message}", Toast.LENGTH_LONG).show()
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

    private fun unitFactor(code: String): Long = when (code) {
        "h" -> 60L
        "j" -> 1440L
        else -> 1L
    }

    private fun setupUnitSpinners() {
        val a1 = ArrayAdapter(this, R.layout.spinner_item, unitLabels)
        a1.setDropDownViewResource(R.layout.spinner_dropdown_item)
        b.intervalUnit.adapter = a1

        val a2 = ArrayAdapter(this, R.layout.spinner_item, unitLabels)
        a2.setDropDownViewResource(R.layout.spinner_dropdown_item)
        b.sendDelayUnit.adapter = a2
    }

    private fun setupCollapse(header: View, body: View, chevron: ImageView) {
        chevron.rotation = if (body.visibility == View.VISIBLE) 180f else 0f
        header.setOnClickListener {
            val show = body.visibility != View.VISIBLE
            TransitionManager.beginDelayedTransition(b.contentRoot)
            body.visibility = if (show) View.VISIBLE else View.GONE
            chevron.rotation = if (show) 180f else 0f
        }
    }

    private fun updateVisibility() {
        b.smsCard.visibility = if (b.switchSms.isChecked) View.VISIBLE else View.GONE
        b.emailCard.visibility = if (b.switchEmail.isChecked) View.VISIBLE else View.GONE
    }

    private fun loadIntoUi() {
        b.switchSms.isChecked = sendSms
        b.switchEmail.isChecked = sendEmail
        b.switchGps.isChecked = attachGps
        b.recipientSms.setText(recipientSms)
        b.messageSms.setText(messageSms)
        b.recipientEmail.setText(recipientEmail)
        b.subject.setText(subject)
        b.messageEmail.setText(messageEmail)
        b.interval.setText(intervalAmount.toString())
        b.intervalUnit.setSelection(unitCodes.indexOf(intervalUnit).coerceAtLeast(0))
        b.sendDelay.setText(sendDelayAmount.toString())
        b.sendDelayUnit.setSelection(unitCodes.indexOf(sendDelayUnit).coerceAtLeast(0))
        b.smtpHost.setText(smtpHost)
        b.smtpPort.setText(smtpPort.toString())
        b.smtpUser.setText(smtpUser)
        b.smtpPass.setText(smtpPass)
        b.switchServer.isChecked = serverEnabled
        b.serverUrl.setText(serverUrl)
        b.serverToken.setText(serverToken)
        if (photoUri.isNotEmpty()) b.photoLabel.text = "Photo configurée"
    }

    private fun saveFromUi() {
        sendSms = b.switchSms.isChecked
        sendEmail = b.switchEmail.isChecked
        attachGps = b.switchGps.isChecked
        recipientSms = b.recipientSms.text.toString().trim()
        messageSms = b.messageSms.text.toString()
        recipientEmail = b.recipientEmail.text.toString().trim()
        subject = b.subject.text.toString().trim().ifEmpty { "Message important" }
        messageEmail = b.messageEmail.text.toString()

        val iAmt = b.interval.text.toString().toIntOrNull()?.coerceAtLeast(1) ?: 1
        val iUnit = unitCodes[b.intervalUnit.selectedItemPosition]
        intervalAmount = iAmt
        intervalUnit = iUnit
        intervalMinutes = iAmt.toLong() * unitFactor(iUnit)

        val sAmt = b.sendDelay.text.toString().toIntOrNull()?.coerceAtLeast(1) ?: 1
        val sUnit = unitCodes[b.sendDelayUnit.selectedItemPosition]
        sendDelayAmount = sAmt
        sendDelayUnit = sUnit
        sendDelayMinutes = sAmt.toLong() * unitFactor(sUnit)

        smtpHost = b.smtpHost.text.toString().trim()
        smtpPort = b.smtpPort.text.toString().toIntOrNull() ?: 587
        smtpUser = b.smtpUser.text.toString().trim()
        smtpPass = b.smtpPass.text.toString()

        serverEnabled = b.switchServer.isChecked
        serverUrl = b.serverUrl.text.toString().trim()
        serverToken = b.serverToken.text.toString().trim()
    }
}
