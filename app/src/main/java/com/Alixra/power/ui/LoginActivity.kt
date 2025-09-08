package com.Alixra.power.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.Alixra.power.MainActivity
import com.Alixra.power.PowerApplication
import com.Alixra.power.R
import com.Alixra.power.data.PreferencesManager
import com.google.android.material.button.MaterialButton

/**
 * صفحه ورود به برنامه
 */
class LoginActivity : BaseActivity() {
    
    private lateinit var emailEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var persianButton: MaterialButton
    private lateinit var englishButton: MaterialButton
    private lateinit var preferencesManager: PreferencesManager
    
    private var selectedLanguage = "fa" // default Persian
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        preferencesManager = PreferencesManager(this)
        
        // تنظیم زبان ذخیره شده
        selectedLanguage = preferencesManager.getLanguage()
        
        // بررسی اینکه آیا کاربر قبلاً وارد شده یا نه
        if (preferencesManager.isUserLoggedIn()) {
            goToMainActivity()
            return
        }
        
        setContentView(R.layout.activity_login)
        
        initViews()
        setupLanguageButtons()
        setupClickListeners()
    }
    
    private fun initViews() {
        emailEditText = findViewById(R.id.emailEditText)
        loginButton = findViewById(R.id.loginButton)
        persianButton = findViewById(R.id.persianButton)
        englishButton = findViewById(R.id.englishButton)
    }
    
    private fun setupLanguageButtons() {
        // تنظیم وضعیت اولیه دکمه‌ها
        updateLanguageButtons()
        
        persianButton.setOnClickListener {
            if (selectedLanguage != "fa") {
                selectedLanguage = "fa"
                PowerApplication.changeLanguage(this, selectedLanguage)
                preferencesManager.setLanguage(selectedLanguage)
                recreate() // راه‌اندازی مجدد برای اعمال تغییرات
            }
        }
        
        englishButton.setOnClickListener {
            if (selectedLanguage != "en") {
                selectedLanguage = "en"
                PowerApplication.changeLanguage(this, selectedLanguage)
                preferencesManager.setLanguage(selectedLanguage)
                recreate() // راه‌اندازی مجدد برای اعمال تغییرات
            }
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
    
    
    private fun setupClickListeners() {
        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            
            if (validateEmail(email)) {
                loginUser(email)
            } else {
                showError(getString(R.string.email_error))
            }
        }
    }
    
    private fun validateEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
    
    private fun loginUser(email: String) {
        // ذخیره ایمیل کاربر
        preferencesManager.saveUserEmail(email)
        preferencesManager.setUserLoggedIn(true)
        
        Toast.makeText(this, getString(R.string.welcome_message), Toast.LENGTH_SHORT).show()
        
        goToMainActivity()
    }
    
    private fun goToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
    
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
