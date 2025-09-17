package com.Alixra.power.data

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * مدیر backup و restore داده‌های اپلیکیشن
 */
class BackupManager(private val context: Context) {
    
    private val preferencesManager = PreferencesManager(context)
    private val gson = GsonBuilder()
        .disableHtmlEscaping()
        .setPrettyPrinting()
        .serializeNulls()
        .create()
    
    /**
     * داده‌های کامل اپلیکیشن برای backup
     */
    data class AppBackupData(
        val version: String = "1.0",
        val createdAt: Long = System.currentTimeMillis(),
        val deviceInfo: String = android.os.Build.MODEL,
        
        // داده‌های اصلی
        val tasks: List<Task>,
        val categories: List<TaskCategory>,
        
        // تنظیمات زنگ‌ها
        val alarmSettings: AlarmSettings,
        
        // تنظیمات evening
        val eveningSettings: EveningSettings,
        
        // آمار و تنظیمات
        val statistics: StatisticsData,
        
        // سایر تنظیمات
        val preferences: AppPreferences
    )
    
    /**
     * تنظیمات زنگ‌ها
     */
    data class AlarmSettings(
        val isAlarmEnabled: Boolean,
        val alarmHour: Int,
        val alarmMinute: Int,
        val selectedDays: List<Int>,
        val isVibrationEnabled: Boolean,
        val alarmSoundUri: String?,
        val motivationalTexts: List<String>
    )
    
    /**
     * تنظیمات evening
     */
    data class EveningSettings(
        val isEveningEnabled: Boolean,
        val eveningHour: Int,
        val eveningMinute: Int,
        val currentStep: Int,
        val successCount: Int
    )
    
    /**
     * داده‌های آماری
     */
    data class StatisticsData(
        val totalTasksCreated: Int,
        val totalTasksCompleted: Int,
        val categoryRatings: Map<String, List<Int>>,
        val lastBackupDate: Long
    )
    
    /**
     * سایر تنظیمات اپلیکیشن
     */
    data class AppPreferences(
        val isDarkMode: Boolean,
        val language: String,
        val notificationsEnabled: Boolean,
        val autoBackupEnabled: Boolean
    )
    
    /**
     * تولید backup از تمام داده‌های اپلیکیشن
     */
    fun createBackup(): AppBackupData {
        return AppBackupData(
            tasks = preferencesManager.getAllTasks(),
            categories = preferencesManager.getAllTaskCategories(),
            alarmSettings = getAlarmSettings(),
            eveningSettings = getEveningSettings(),
            statistics = getStatisticsData(),
            preferences = getAppPreferences()
        )
    }
    
    /**
     * ذخیره backup در فایل
     */
    fun exportBackupToFile(uri: Uri): BackupResult {
        return try {
            val backupData = createBackup()
            val jsonString = gson.toJson(backupData)
            
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(jsonString)
                    writer.flush()
                }
            }
            
            BackupResult.Success("✅ بکاپ با موفقیت ذخیره شد")
        } catch (e: Exception) {
            BackupResult.Error("❌ خطا در ذخیره فایل: ${e.message}")
        }
    }
    
    /**
     * بازیابی backup از فایل
     */
    fun importBackupFromFile(uri: Uri): BackupResult {
        return try {
            val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                InputStreamReader(inputStream, Charsets.UTF_8).use { reader ->
                    reader.readText()
                }
            } ?: return BackupResult.Error("❌ نمی‌توان فایل را خواند")
            
            val backupData = gson.fromJson(jsonString, AppBackupData::class.java)
            
            // اعتبارسنجی داده‌ها
            if (!isValidBackupData(backupData)) {
                return BackupResult.Error("❌ فایل backup معتبر نیست")
            }
            
            // بازیابی داده‌ها
            restoreData(backupData)
            
            BackupResult.Success("✅ بازیابی با موفقیت انجام شد")
        } catch (e: JsonSyntaxException) {
            BackupResult.Error("❌ فرمت فایل نامعتبر است")
        } catch (e: Exception) {
            BackupResult.Error("❌ خطا در بازیابی: ${e.message}")
        }
    }
    
    /**
     * تولید نام فایل backup
     */
    fun generateBackupFileName(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault())
        val currentDate = dateFormat.format(Date())
        return "Power_App_Backup_$currentDate.json"
    }
    
    /**
     * دریافت اطلاعات فایل backup
     */
    fun getBackupInfo(uri: Uri): BackupInfo? {
        return try {
            val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                InputStreamReader(inputStream, Charsets.UTF_8).use { reader ->
                    reader.readText()
                }
            } ?: return null
            
            val backupData = gson.fromJson(jsonString, AppBackupData::class.java)
            
            BackupInfo(
                version = backupData.version,
                createdAt = backupData.createdAt,
                deviceInfo = backupData.deviceInfo,
                tasksCount = backupData.tasks.size,
                categoriesCount = backupData.categories.size,
                fileSize = jsonString.toByteArray(Charsets.UTF_8).size.toLong()
            )
        } catch (e: Exception) {
            null
        }
    }
    
    // === Helper Methods ===
    
    private fun getAlarmSettings(): AlarmSettings {
        return AlarmSettings(
            isAlarmEnabled = preferencesManager.isAlarmEnabled(),
            alarmHour = preferencesManager.getAlarmHour(),
            alarmMinute = preferencesManager.getAlarmMinute(),
            selectedDays = preferencesManager.getSelectedDays(),
            isVibrationEnabled = preferencesManager.isVibrationEnabled(),
            alarmSoundUri = preferencesManager.getAlarmSoundUri(),
            motivationalTexts = preferencesManager.getMotivationalTexts()
        )
    }
    
    private fun getEveningSettings(): EveningSettings {
        return EveningSettings(
            isEveningEnabled = preferencesManager.isEveningEnabled(),
            eveningHour = preferencesManager.getEveningHour(),
            eveningMinute = preferencesManager.getEveningMinute(),
            currentStep = preferencesManager.getEveningStep(),
            successCount = preferencesManager.getSuccessCount()
        )
    }
    
    private fun getStatisticsData(): StatisticsData {
        return StatisticsData(
            totalTasksCreated = preferencesManager.getAllTasks().size,
            totalTasksCompleted = preferencesManager.getAllTasks().count { it.isCompleted },
            categoryRatings = preferencesManager.getAllCategoryRatings(),
            lastBackupDate = System.currentTimeMillis()
        )
    }
    
    private fun getAppPreferences(): AppPreferences {
        return AppPreferences(
            isDarkMode = preferencesManager.isDarkMode(),
            language = preferencesManager.getLanguage(),
            notificationsEnabled = preferencesManager.areNotificationsEnabled(),
            autoBackupEnabled = preferencesManager.isAutoBackupEnabled()
        )
    }
    
    private fun isValidBackupData(data: AppBackupData?): Boolean {
        if (data == null) return false
        
        // بررسی اعتبار ساختار داده‌ها
        return try {
            data.tasks.forEach { task ->
                if (task.id.isEmpty() || task.title.isEmpty()) return false
            }
            data.categories.forEach { category ->
                if (category.id.isEmpty() || category.name.isEmpty()) return false
            }
            true
        } catch (e: Exception) {
            false
        }
    }
    
    private fun restoreData(backupData: AppBackupData) {
        // بازیابی کارها
        backupData.tasks.forEach { task ->
            preferencesManager.saveTask(task)
        }
        
        // بازیابی دسته‌بندی‌ها
        backupData.categories.forEach { category ->
            preferencesManager.saveTaskCategory(category)
        }
        
        // بازیابی تنظیمات زنگ
        with(backupData.alarmSettings) {
            preferencesManager.setAlarmEnabled(isAlarmEnabled)
            preferencesManager.setAlarmTime(alarmHour, alarmMinute)
            preferencesManager.setSelectedDays(selectedDays)
            preferencesManager.setVibrationEnabled(isVibrationEnabled)
            alarmSoundUri?.let { preferencesManager.setAlarmSoundUri(it) }
            preferencesManager.setMotivationalTexts(motivationalTexts)
        }
        
        // بازیابی تنظیمات evening
        with(backupData.eveningSettings) {
            preferencesManager.setEveningEnabled(isEveningEnabled)
            preferencesManager.setEveningTime(eveningHour, eveningMinute)
            preferencesManager.saveEveningStep(currentStep)
            preferencesManager.saveSuccessCount(successCount)
        }
        
        // بازیابی تنظیمات اپلیکیشن
        with(backupData.preferences) {
            preferencesManager.setDarkMode(isDarkMode)
            preferencesManager.setLanguage(language)
            preferencesManager.setNotificationsEnabled(notificationsEnabled)
            preferencesManager.setAutoBackupEnabled(autoBackupEnabled)
        }
    }
    
    // === Data Classes ===
    
    /**
     * نتیجه عملیات backup/restore
     */
    sealed class BackupResult {
        data class Success(val message: String) : BackupResult()
        data class Error(val message: String) : BackupResult()
    }
    
    /**
     * اطلاعات فایل backup
     */
    data class BackupInfo(
        val version: String,
        val createdAt: Long,
        val deviceInfo: String,
        val tasksCount: Int,
        val categoriesCount: Int,
        val fileSize: Long
    ) {
        fun getFormattedDate(): String {
            val formatter = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
            return formatter.format(Date(createdAt))
        }
        
        fun getFormattedFileSize(): String {
            return when {
                fileSize < 1024 -> "$fileSize B"
                fileSize < 1024 * 1024 -> "${fileSize / 1024} KB"
                else -> "${fileSize / (1024 * 1024)} MB"
            }
        }
    }
}
