package com.Alixra.power

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import com.Alixra.power.data.PreferencesManager
import java.util.*

/**
 * Application کلاس برای مدیریت زبان در سطح کل برنامه
 */
class PowerApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        // تنظیم زبان هنگام شروع برنامه
        updateAppLanguage()
    }
    
    override fun attachBaseContext(base: Context?) {
        val context = base?.let { updateLocale(it) } ?: base
        super.attachBaseContext(context)
    }
    
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
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
        
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)
    }
    
    companion object {
        /**
         * تغییر زبان برنامه به صورت سراسری
         */
        fun changeLanguage(context: Context, languageCode: String) {
            val prefsManager = PreferencesManager(context)
            prefsManager.setLanguage(languageCode)
            
            val locale = Locale(languageCode)
            Locale.setDefault(locale)
            
            val config = Configuration(context.resources.configuration)
            config.setLocale(locale)
            
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
        }
    }
}
