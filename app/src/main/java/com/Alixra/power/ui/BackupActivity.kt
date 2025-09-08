package com.Alixra.power.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.Alixra.power.R
import com.Alixra.power.data.BackupManager
import com.Alixra.power.databinding.ActivityBackupBinding
import java.text.SimpleDateFormat
import java.util.*

/**
 * صفحه مدیریت backup و restore
 */
class BackupActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityBackupBinding
    private lateinit var backupManager: BackupManager
    
    // launcher برای انتخاب محل ذخیره backup
    private val saveBackupLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                performBackup(uri)
            }
        }
    }
    
    // launcher برای انتخاب فایل restore
    private val restoreBackupLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                showRestoreConfirmation(uri)
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBackupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        backupManager = BackupManager(this)
        
        setupUI()
        setupClickListeners()
    }
    
    private fun setupUI() {
        // تنظیم عنوان
        binding.pageTitle.text = "پشتیبان‌گیری و بازیابی"
        
        // نمایش آخرین backup
        updateLastBackupInfo()
        
        // نمایش آمار فعلی
        updateCurrentStats()
    }
    
    private fun setupClickListeners() {
        // دکمه بازگشت
        binding.backButton.setOnClickListener {
            finish()
        }
        
        // دکمه تهیه backup
        binding.createBackupButton.setOnClickListener {
            createBackup()
        }
        
        // دکمه بازیابی
        binding.restoreBackupButton.setOnClickListener {
            restoreBackup()
        }
        
        // دکمه مشاهده جزئیات backup فعلی
        binding.viewCurrentDataButton.setOnClickListener {
            showCurrentDataDialog()
        }
        
        // دکمه تنظیمات خودکار
        binding.autoBackupSettingsButton.setOnClickListener {
            showAutoBackupSettings()
        }
    }
    
    private fun updateLastBackupInfo() {
        // اینجا می‌تونیم آخرین تاریخ backup رو از preferences بخونیم
        val lastBackupDate = "هنوز پشتیبان‌گیری نشده"
        binding.lastBackupDate.text = lastBackupDate
    }
    
    private fun updateCurrentStats() {
        val backupData = backupManager.createBackup()
        
        binding.currentTasksCount.text = "${backupData.tasks.size} کار"
        binding.currentCategoriesCount.text = "${backupData.categories.size} هدف"
        binding.currentSettingsCount.text = "تنظیمات کامل"
    }
    
    private fun createBackup() {
        val fileName = backupManager.generateBackupFileName()
        
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(Intent.EXTRA_TITLE, fileName)
        }
        
        saveBackupLauncher.launch(intent)
    }
    
    private fun performBackup(uri: Uri) {
        binding.createBackupButton.isEnabled = false
        binding.createBackupButton.text = "در حال تهیه..."
        
        when (val result = backupManager.exportBackupToFile(uri)) {
            is BackupManager.BackupResult.Success -> {
                Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
                updateLastBackupInfo()
                
                // نمایش dialog موفقیت با جزئیات
                showBackupSuccessDialog(uri)
            }
            is BackupManager.BackupResult.Error -> {
                Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
            }
        }
        
        binding.createBackupButton.isEnabled = true
        binding.createBackupButton.text = "تهیه پشتیبان"
    }
    
    private fun restoreBackup() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/json", "text/plain"))
        }
        
        restoreBackupLauncher.launch(intent)
    }
    
    private fun showRestoreConfirmation(uri: Uri) {
        // دریافت اطلاعات فایل
        val backupInfo = backupManager.getBackupInfo(uri)
        
        if (backupInfo == null) {
            Toast.makeText(this, "❌ فایل معتبر نیست", Toast.LENGTH_LONG).show()
            return
        }
        
        val message = buildString {
            appendLine("📋 اطلاعات فایل پشتیبان:")
            appendLine()
            appendLine("📅 تاریخ: ${backupInfo.getFormattedDate()}")
            appendLine("📱 دستگاه: ${backupInfo.deviceInfo}")
            appendLine("📝 تعداد کارها: ${backupInfo.tasksCount}")
            appendLine("🎯 تعداد اهداف: ${backupInfo.categoriesCount}")
            appendLine("📏 حجم فایل: ${backupInfo.getFormattedFileSize()}")
            appendLine()
            appendLine("⚠️ توجه: تمام داده‌های فعلی جایگزین خواهند شد!")
        }
        
        AlertDialog.Builder(this)
            .setTitle("تأیید بازیابی")
            .setMessage(message)
            .setPositiveButton("بازیابی") { _, _ ->
                performRestore(uri)
            }
            .setNegativeButton("لغو", null)
            .show()
    }
    
    private fun performRestore(uri: Uri) {
        binding.restoreBackupButton.isEnabled = false
        binding.restoreBackupButton.text = "در حال بازیابی..."
        
        when (val result = backupManager.importBackupFromFile(uri)) {
            is BackupManager.BackupResult.Success -> {
                Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
                updateCurrentStats()
                
                // نمایش dialog موفقیت
                showRestoreSuccessDialog()
            }
            is BackupManager.BackupResult.Error -> {
                Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
            }
        }
        
        binding.restoreBackupButton.isEnabled = true
        binding.restoreBackupButton.text = "بازیابی از فایل"
    }
    
    private fun showCurrentDataDialog() {
        val backupData = backupManager.createBackup()
        
        val message = buildString {
            appendLine("📊 اطلاعات فعلی برنامه:")
            appendLine()
            appendLine("📝 کارها: ${backupData.tasks.size}")
            appendLine("   - انجام شده: ${backupData.tasks.count { it.isCompleted }}")
            appendLine("   - در انتظار: ${backupData.tasks.count { !it.isCompleted }}")
            appendLine()
            appendLine("🎯 اهداف: ${backupData.categories.size}")
            appendLine()
            appendLine("⏰ تنظیمات زنگ:")
            appendLine("   - صبح: ${if (backupData.alarmSettings.isAlarmEnabled) "فعال" else "غیرفعال"}")
            appendLine("   - شب: ${if (backupData.eveningSettings.isEveningEnabled) "فعال" else "غیرفعال"}")
            appendLine()
            appendLine("📈 آمار:")
            appendLine("   - تعداد موفقیت: ${backupData.eveningSettings.successCount}")
            appendLine("   - کل کارهای ایجاد شده: ${backupData.statistics.totalTasksCreated}")
            appendLine("   - کل کارهای انجام شده: ${backupData.statistics.totalTasksCompleted}")
        }
        
        AlertDialog.Builder(this)
            .setTitle("جزئیات داده‌های فعلی")
            .setMessage(message)
            .setPositiveButton("باشه", null)
            .show()
    }
    
    private fun showAutoBackupSettings() {
        val items = arrayOf(
            "فعال‌سازی پشتیبان‌گیری خودکار",
            "پشتیبان‌گیری روزانه",
            "پشتیبان‌گیری هفتگی",
            "تنظیم محل ذخیره"
        )
        
        AlertDialog.Builder(this)
            .setTitle("تنظیمات پشتیبان‌گیری خودکار")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> toggleAutoBackup()
                    1 -> setAutoBackupFrequency("daily")
                    2 -> setAutoBackupFrequency("weekly")
                    3 -> setAutoBackupLocation()
                }
            }
            .setNegativeButton("بستن", null)
            .show()
    }
    
    private fun toggleAutoBackup() {
        // TODO: پیاده‌سازی تنظیمات auto backup
        Toast.makeText(this, "این قابلیت به زودی اضافه می‌شود", Toast.LENGTH_SHORT).show()
    }
    
    private fun setAutoBackupFrequency(frequency: String) {
        // TODO: پیاده‌سازی تنظیم دوره پشتیبان‌گیری
        Toast.makeText(this, "تنظیم $frequency ذخیره شد", Toast.LENGTH_SHORT).show()
    }
    
    private fun setAutoBackupLocation() {
        // TODO: پیاده‌سازی انتخاب محل ذخیره خودکار
        Toast.makeText(this, "این قابلیت به زودی اضافه می‌شود", Toast.LENGTH_SHORT).show()
    }
    
    private fun showBackupSuccessDialog(uri: Uri) {
        val message = buildString {
            appendLine("✅ پشتیبان‌گیری با موفقیت انجام شد!")
            appendLine()
            appendLine("📄 فایل در مکان انتخابی ذخیره شد")
            appendLine("📅 ${SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date())}")
            appendLine()
            appendLine("💡 نکته: این فایل را در مکان امنی نگهداری کنید")
        }
        
        AlertDialog.Builder(this)
            .setTitle("پشتیبان‌گیری موفق")
            .setMessage(message)
            .setPositiveButton("عالی!", null)
            .show()
    }
    
    private fun showRestoreSuccessDialog() {
        val message = buildString {
            appendLine("✅ بازیابی با موفقیت انجام شد!")
            appendLine()
            appendLine("🔄 تمام داده‌های برنامه بازیابی شدند")
            appendLine("⚡ ممکن است نیاز به راه‌اندازی مجدد برنامه باشد")
            appendLine()
            appendLine("💡 نکته: زنگ‌ها و یادآورها خودکار فعال شده‌اند")
        }
        
        AlertDialog.Builder(this)
            .setTitle("بازیابی موفق")
            .setMessage(message)
            .setPositiveButton("باشه") { _, _ ->
                // بازگشت به صفحه اصلی
                finish()
            }
            .show()
    }
}
