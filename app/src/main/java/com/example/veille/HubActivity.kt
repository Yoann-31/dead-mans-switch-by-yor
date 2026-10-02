package com.example.veille

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.veille.Prefs.enabled

/**
 * Page d'accueil « boîte à outils » : liste les fonctionnalités de l'application.
 * Chaque fonctionnalité a un raccourci (carte) avec son nom et son statut,
 * et figure aussi dans le menu (bouton ⋮).
 *
 * Pour ajouter une future fonctionnalité : ajouter une entrée dans `features`.
 */
class HubActivity : AppCompatActivity() {

    private data class Feature(
        val name: String,
        val isActive: (Context) -> Boolean,
        val open: (HubActivity) -> Unit
    )

    private val features: List<Feature> = listOf(
        Feature(
            name = "Dead Man's Switch by YRO",
            isActive = { it.enabled },
            open = { it.startActivity(Intent(it, MainActivity::class.java)) }
        )
        // ➜ Ajouter ici les prochaines fonctionnalités.
    )

    private lateinit var container: android.widget.LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hub)

        container = findViewById(R.id.featuresContainer)
        findViewById<ImageButton>(R.id.menuBtn).setOnClickListener { showMenu(it) }
    }

    override fun onResume() {
        super.onResume()
        buildCards()
    }

    private fun buildCards() {
        container.removeAllViews()
        val inflater = LayoutInflater.from(this)
        for (f in features) {
            val card = inflater.inflate(R.layout.item_feature, container, false)
            card.findViewById<TextView>(R.id.featureName).text = f.name
            val active = f.isActive(this)
            val status = card.findViewById<TextView>(R.id.featureStatus)
            status.text = if (active) "● Actif" else "○ Inactif"
            status.setTextColor(
                ContextCompat.getColor(this, if (active) R.color.accent else R.color.text_muted)
            )
            card.setOnClickListener { f.open(this) }
            container.addView(card)
        }
    }

    private fun showMenu(anchor: View) {
        val popup = PopupMenu(this, anchor)
        features.forEachIndexed { i, f -> popup.menu.add(0, i, i, f.name) }
        popup.setOnMenuItemClickListener { item ->
            features[item.itemId].open(this)
            true
        }
        popup.show()
    }
}
