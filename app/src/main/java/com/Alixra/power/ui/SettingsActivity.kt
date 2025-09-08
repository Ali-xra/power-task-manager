package com.Alixra.power.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.Alixra.power.R
import com.Alixra.power.data.PreferencesManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

/**
 * صفحه تنظیمات - شامل پشتیبان‌گیری و تغییر زبان
 */
class SettingsActivity : BaseActivity() {
    
    private lateinit var preferencesManager: PreferencesManager
    
    // Views
    private lateinit var backButton: TextView
    private lateinit var titleTextView: TextView
    private lateinit var backupCard: MaterialCardView
    private lateinit var languageCard: MaterialCardView
    private lateinit var persianButton: MaterialButton
    private lateinit var englishButton: MaterialButton
    
    private var selectedLanguage: String = "fa"
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        preferencesManager = PreferencesManager(this)
        selectedLanguage = preferencesManager.getLanguage()
        
        initViews()
        setupClickListeners()
        updateLanguageButtons()
    }
    
    private fun initViews() {
        backButton = findViewById(R.id.backButton)
        titleTextView = findViewById(R.id.titleTextView)
        backupCard = findViewById(R.id.backupCard)
        languageCard = findViewById(R.id.languageCard)
        persianButton = findViewById(R.id.persianButton)
        englishButton = findViewById(R.id.englishButton)
    }
    
    private fun setupClickListeners() {
        backButton.setOnClickListener {
            finish()
        }
        
        backupCard.setOnClickListener {
            val intent = Intent(this, BackupActivity::class.java)
            startActivity(intent)
        }
        
        languageCard.setOnClickListener {
            showLanguageDialog()
        }
        
        persianButton.setOnClickListener {
            changeLanguage("fa")
        }
        
        englishButton.setOnClickListener {
            changeLanguage("en")
        }
    }
    
    private fun showLanguageDialog() {
        val currentLanguage = preferencesManager.getLanguage()
        val items = arrayOf(
            getString(R.string.language_persian),
            getString(R.string.language_english)
        )
        val checkedItem = if (currentLanguage == "fa") 0 else 1
        
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.language_selection_title))
            .setSingleChoiceItems(items, checkedItem) { dialog, which ->
                val newLanguage = if (which == 0) "fa" else "en"
                changeLanguage(newLanguage)
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
    
    private fun changeLanguage(languageCode: String) {
        if (selectedLanguage != languageCode) {
            selectedLanguage = languageCode
            preferencesManager.setLanguage(selectedLanguage)
            
            Toast.makeText(
                this,
                getString(R.string.language_changed_message),
                Toast.LENGTH_SHORT
            ).show()
            
            // بازسازی Activity برای اعمال زبان جدید
            recreate()
        }
    }
    
    private fun updateLanguageButtons() {
        if (selectedLanguage == "fa") {
            persianButton.backgroundTintList = getColorStateList(android.R.color.holo_blue_light)
            persianButton.setTextColor(getColor(android.R.color.white))
            englishButton.backgroundTintList = getColorStateList(android.R.color.transparent)
            englishButton.setTextColor(getColor(android.R.color.holo_blue_light))
        } else {
            englishButton.backgroundTintList = getColorStateList(android.R.color.holo_blue_light)
            englishButton.setTextColor(getColor(android.R.color.white))
            persianButton.backgroundTintList = getColorStateList(android.R.color.transparent)
            persianButton.setTextColor(getColor(android.R.color.holo_blue_light))
        }
    }
}
