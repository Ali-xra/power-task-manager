package com.Alixra.power

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import android.widget.TextView
import com.Alixra.power.data.PreferencesManager
import com.Alixra.power.ui.AlarmActivity
import com.Alixra.power.ui.AlarmsActivity
import com.Alixra.power.ui.BackupActivity
import com.Alixra.power.ui.GoalsActivity
import com.Alixra.power.ui.LoginActivity
import com.Alixra.power.ui.TasksActivity
import com.Alixra.power.ui.StatsActivity
import com.google.android.material.card.MaterialCardView

class MainActivity : AppCompatActivity() {

    private lateinit var alarmsBtn: MaterialCardView
    private lateinit var goalsBtn: MaterialCardView
    private lateinit var tasksBtn: MaterialCardView
    private lateinit var reportsBtn: MaterialCardView
    private lateinit var backupBtn: MaterialCardView
    
    private lateinit var userEmailHeader: TextView
    private lateinit var preferencesManager: PreferencesManager

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            showPermissionRationaleDialog(
                "مجوز نوتیفیکیشن",
                "برای نمایش یادآور شبانه و عملکرد صحیح سرویس زنگ، به این مجوز نیاز داریم."
            ) {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            }
        }
    }

    private fun showPermissionRationaleDialog(title: String, message: String, onPositive: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("باشه، برو به تنظیمات") { _, _ ->
                onPositive()
            }
            .setNegativeButton("فعلا نه") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        preferencesManager = PreferencesManager(this)
        
        // بررسی وضعیت ورود کاربر
        if (!preferencesManager.isUserLoggedIn()) {
            goToLoginActivity()
            return
        }
        
        setContentView(R.layout.activity_main)

        initViews()
        setupUserHeader()
        checkAllPermissions()
        setupClickListeners()
    }
    
    private fun goToLoginActivity() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun initViews() {
        alarmsBtn = findViewById(R.id.alarmsBtn)
        goalsBtn = findViewById(R.id.goalsBtn)
        tasksBtn = findViewById(R.id.tasksBtn)
        reportsBtn = findViewById(R.id.reportsBtn)
        backupBtn = findViewById(R.id.backupBtn)
        userEmailHeader = findViewById(R.id.userEmailHeader)
    }
    
    private fun setupUserHeader() {
        val userEmail = preferencesManager.getUserEmail()
        val userName = preferencesManager.getUserDisplayName()
        
        userEmailHeader.text = "👋 سلام $userName"
        
        // اضافه کردن قابلیت کلیک برای نمایش منو کاربر
        userEmailHeader.setOnClickListener {
            showUserMenu()
        }
    }
    
    private fun showUserMenu() {
        val userEmail = preferencesManager.getUserEmail() ?: ""
        
        val options = arrayOf(
            "👤 $userEmail",
            "🔄 تغییر کاربر", 
            "🚪 خروج از حساب"
        )
        
        AlertDialog.Builder(this)
            .setTitle("منوی کاربر")
            .setItems(options) { _, which ->
                when (which) {
                    1 -> changeUser() // تغییر کاربر
                    2 -> logoutUser() // خروج
                }
            }
            .setNegativeButton("بستن", null)
            .show()
    }
    
    private fun changeUser() {
        preferencesManager.logoutUser()
        goToLoginActivity()
    }
    
    private fun logoutUser() {
        AlertDialog.Builder(this)
            .setTitle("خروج از حساب")
            .setMessage("آیا مطمئن هستید که می‌خواهید از حساب خود خارج شوید؟")
            .setPositiveButton("بله، خروج") { _, _ ->
                preferencesManager.logoutUser()
                goToLoginActivity()
            }
            .setNegativeButton("لغو", null)
            .show()
    }

    private fun checkAllPermissions() {
        checkNotificationPermission()
        checkExactAlarmPermission()
        checkBatteryOptimization()
        checkFullScreenIntentPermission()
    }

    private fun setupClickListeners() {
        // دکمه زنگ‌ها
        alarmsBtn.setOnClickListener {
            val intent = Intent(this, AlarmsActivity::class.java)
            startActivity(intent)
        }

        // دکمه اهداف (جدید)
        goalsBtn.setOnClickListener {
            val intent = Intent(this, GoalsActivity::class.java)
            startActivity(intent)
        }

        // دکمه لیست کارها (تغییر یافته)
        tasksBtn.setOnClickListener {
            val intent = Intent(this, TasksActivity::class.java)
            startActivity(intent)
        }

        // دکمه گزارشات
        reportsBtn.setOnClickListener {
            val intent = Intent(this, StatsActivity::class.java)
            startActivity(intent)
        }

        // دکمه پشتیبان‌گیری
        backupBtn.setOnClickListener {
            val intent = Intent(this, BackupActivity::class.java)
            startActivity(intent)
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                showPermissionRationaleDialog(
                    "مجوز زنگ دقیق",
                    "برای اینکه زنگ‌ها دقیقا سر وقت به صدا درآیند، به این مجوز ویژه نیاز داریم."
                ) {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    startActivity(intent)
                }
            }
        }
    }

    private fun checkBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                showPermissionRationaleDialog(
                    "بهینه‌سازی باتری",
                    "برای جلوگیری از توقف برنامه توسط سیستم، لطفاً آن را از لیست بهینه‌سازی باتری خارج کنید."
                ) {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                    intent.data = Uri.parse("package:$packageName")
                    startActivity(intent)
                }
            }
        }
    }

    private fun checkFullScreenIntentPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // بررسی مجوز نمایش تمام صفحه در اندروید 14 به بالا
        }
    }
}