package com.Alixra.power.ui

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import com.Alixra.power.data.PreferencesManager
import java.util.*

/**
 * Activity پایه برای تنظیم زبان در تمام صفحات
 */
abstract class BaseActivity : AppCompatActivity() {
    
    private lateinit var preferencesManager: PreferencesManager
    
    override fun attachBaseContext(newBase: Context?) {
        if (newBase != null) {
            val prefsManager = PreferencesManager(newBase)
            val language = prefsManager.getLanguage()
            val context = updateLocale(newBase, language)
            super.attachBaseContext(context)
        } else {
            super.attachBaseContext(newBase)
        }
    }
    
    private fun updateLocale(context: Context, language: String): Context {
        val locale = Locale(language)
        Locale.setDefault(locale)
        
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        
        return context.createConfigurationContext(config)
    }
}
