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
import com.Alixra.power.ui.SettingsActivity
import com.Alixra.power.ui.GoalsActivity
import com.Alixra.power.ui.BaseActivity
import com.Alixra.power.ui.LoginActivity
import com.Alixra.power.ui.TasksActivity
import com.Alixra.power.ui.StatsActivity
import com.google.android.material.card.MaterialCardView

class MainActivity : BaseActivity() {

    private lateinit var alarmsBtn: MaterialCardView
    private lateinit var goalsBtn: MaterialCardView
    private lateinit var tasksBtn: MaterialCardView
    private lateinit var reportsBtn: MaterialCardView
    private lateinit var settingsBtn: MaterialCardView
    
    private lateinit var userEmailHeader: TextView
    private lateinit var preferencesManager: PreferencesManager

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            showPermissionRationaleDialog(
                getString(R.string.notification_permission_title),
                getString(R.string.notification_permission_message)
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
            .setPositiveButton(getString(R.string.ok_goto_settings)) { _, _ ->
                onPositive()
            }
            .setNegativeButton(getString(R.string.not_now)) { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        preferencesManager = PreferencesManager(this)
        
        // زبان توسط BaseActivity تنظیم می‌شود
        
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
        settingsBtn = findViewById(R.id.settingsBtn)
        userEmailHeader = findViewById(R.id.userEmailHeader)
    }
    
    private fun setupUserHeader() {
        val userName = preferencesManager.getUserDisplayName()
        
        userEmailHeader.text = getString(R.string.hello_user, userName)
        
        // اضافه کردن قابلیت کلیک برای نمایش منو کاربر
        userEmailHeader.setOnClickListener {
            showUserMenu()
        }
    }
    
    private fun showUserMenu() {
        val userEmail = preferencesManager.getUserEmail() ?: ""
        
        val options = arrayOf(
            "👤 $userEmail",
            getString(R.string.change_user), 
            getString(R.string.logout)
        )
        
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.user_menu_title))
            .setItems(options) { _, which ->
                when (which) {
                    1 -> changeUser() // تغییر کاربر
                    2 -> logoutUser() // خروج
                }
            }
            .setNegativeButton(getString(R.string.close), null)
            .show()
    }
    
    private fun changeUser() {
        preferencesManager.logoutUser()
        goToLoginActivity()
    }
    
    private fun logoutUser() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.logout_title))
            .setMessage(getString(R.string.logout_message))
            .setPositiveButton(getString(R.string.yes_logout)) { _, _ ->
                preferencesManager.logoutUser()
                goToLoginActivity()
            }
            .setNegativeButton(getString(R.string.cancel), null)
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
            intent.putExtra(TasksActivity.EXTRA_SHOW_SHORTCUTS, true)
            intent.putExtra(TasksActivity.EXTRA_FROM_MAIN_PAGE, true)
            startActivity(intent)
        }

        // دکمه گزارشات
        reportsBtn.setOnClickListener {
            val intent = Intent(this, StatsActivity::class.java)
            startActivity(intent)
        }

        // دکمه تنظیمات
        settingsBtn.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
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
                    getString(R.string.exact_alarm_permission_title),
                    getString(R.string.exact_alarm_permission_message)
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
                    getString(R.string.battery_optimization_title),
                    getString(R.string.battery_optimization_message)
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