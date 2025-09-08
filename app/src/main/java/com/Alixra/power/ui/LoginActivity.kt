package com.Alixra.power.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.Alixra.power.MainActivity
import com.Alixra.power.R
import com.Alixra.power.data.PreferencesManager

/**
 * صفحه ورود به برنامه
 */
class LoginActivity : AppCompatActivity() {
    
    private lateinit var emailEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var preferencesManager: PreferencesManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        preferencesManager = PreferencesManager(this)
        
        // بررسی اینکه آیا کاربر قبلاً وارد شده یا نه
        if (preferencesManager.isUserLoggedIn()) {
            goToMainActivity()
            return
        }
        
        setContentView(R.layout.activity_login)
        
        initViews()
        setupClickListeners()
    }
    
    private fun initViews() {
        emailEditText = findViewById(R.id.emailEditText)
        loginButton = findViewById(R.id.loginButton)
    }
    
    private fun setupClickListeners() {
        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            
            if (validateEmail(email)) {
                loginUser(email)
            } else {
                showError("لطفاً یک آدرس ایمیل معتبر وارد کنید")
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
        
        Toast.makeText(this, "🎉 خوش آمدید!", Toast.LENGTH_SHORT).show()
        
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
