package com.Alixra.power.ui

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.Alixra.power.data.PreferencesManager
import java.util.*

/**
 * Activity پایه برای تنظیم زبان در تمام صفحات
 */
abstract class BaseActivity : AppCompatActivity() {
    
    override fun attachBaseContext(newBase: Context?) {
        val context = newBase?.let { updateLocale(it) } ?: newBase
        super.attachBaseContext(context)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // اعمال زبان هنگام ایجاد Activity
        updateAppLanguage()
    }
    
    private fun updateLocale(context: Context): Context {
        val prefsManager = PreferencesManager(context)
        val language = prefsManager.getLanguage()
        
        val locale = Locale(language)
        Locale.setDefault(locale)
        
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        
        return context.createConfigurationContext(config)
    }
    
    private fun updateAppLanguage() {
        val prefsManager = PreferencesManager(this)
        val language = prefsManager.getLanguage()
        
        val locale = Locale(language)
        Locale.setDefault(locale)
        
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        
        resources.updateConfiguration(config, resources.displayMetrics)
    }
}
