package com.Alixra.power.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import com.Alixra.power.receiver.AlarmReceiver
import com.Alixra.power.receiver.EveningReceiver
import com.Alixra.power.data.PreferencesManager
import java.util.Calendar

object AlarmUtils {

    // یک شناسه منحصر به فرد برای هر نوع زنگ تعریف می‌کنیم
    private const val MORNING_ALARM_REQUEST_CODE = 1001
    private const val EVENING_ALARM_REQUEST_CODE = 1002

    // تابع تنظیم زنگ صبح (نسخه جدید با پشتیبانی از تکرار)
    fun setMorningAlarm(context: Context, timeInMillis: Long, enableRepeating: Boolean = true) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val prefsManager = PreferencesManager(context)

        // ابتدا چک می‌کنیم که آیا مجوز لازم برای تنظیم زنگ دقیق را داریم یا نه
        if (canScheduleExactAlarms(alarmManager)) {
            val intent = Intent(context, AlarmReceiver::class.java)
            intent.putExtra("isRepeating", enableRepeating) // اضافه کردن فلگ تکرار
            
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                MORNING_ALARM_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // تنظیم زنگ دقیق برای زمان مشخص شده
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                timeInMillis,
                pendingIntent
            )
            
            // ذخیره وضعیت فعال بودن آلارم صبح
            if (enableRepeating) {
                prefsManager.setMorningAlarmEnabled(true)
            }
        }
    }
    
    // تابع تنظیم آلارم صبح با ساعت و دقیقه
    fun setMorningAlarmWithTime(context: Context, hour: Int, minute: Int) {
        val prefsManager = PreferencesManager(context)
        val timeString = String.format("%02d:%02d", hour, minute)
        
        // ذخیره زمان آلارم
        prefsManager.saveMorningAlarmTime(timeString)
        
        // محاسبه زمان بعدی آلارم
        val timeInMillis = getNextAlarmTime(hour, minute)
        
        // تنظیم آلارم
        setMorningAlarm(context, timeInMillis, true)
    }

    // تابع تنظیم یادآور شب (نسخه جدید با پشتیبانی از تکرار)
    fun setEveningAlarm(context: Context, timeInMillis: Long, enableRepeating: Boolean = true) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val prefsManager = PreferencesManager(context)

        if (canScheduleExactAlarms(alarmManager)) {
            val intent = Intent(context, EveningReceiver::class.java)
            intent.putExtra("isRepeating", enableRepeating) // اضافه کردن فلگ تکرار
            
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                EVENING_ALARM_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                timeInMillis,
                pendingIntent
            )
            
            // ذخیره وضعیت فعال بودن یادآور شب
            if (enableRepeating) {
                prefsManager.setEveningAlarmEnabled(true)
            }
        }
    }
    
    // تابع تنظیم یادآور شب با ساعت و دقیقه
    fun setEveningAlarmWithTime(context: Context, hour: Int, minute: Int) {
        val prefsManager = PreferencesManager(context)
        val timeString = String.format("%02d:%02d", hour, minute)
        
        // ذخیره زمان یادآور
        prefsManager.saveEveningAlarmTime(timeString)
        
        // محاسبه زمان بعدی یادآور
        val timeInMillis = getNextAlarmTime(hour, minute)
        
        // تنظیم یادآور
        setEveningAlarm(context, timeInMillis, true)
    }

    // تابع برای لغو زنگ صبح (نسخه بهبود یافته)
    fun cancelMorningAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val prefsManager = PreferencesManager(context)
        
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            MORNING_ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        // اگر زنگی با این شناسه وجود داشت، لغو کن
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
        
        // غیرفعال کردن وضعیت آلارم صبح
        prefsManager.setMorningAlarmEnabled(false)
    }

    // تابع برای لغو یادآور شب (نسخه بهبود یافته)
    fun cancelEveningAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val prefsManager = PreferencesManager(context)
        
        val intent = Intent(context, EveningReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            EVENING_ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
        
        // غیرفعال کردن وضعیت یادآور شب
        prefsManager.setEveningAlarmEnabled(false)
    }

    // تابع کمکی برای بررسی مجوز تنظیم زنگ دقیق (مورد نیاز برای اندروید 12 و بالاتر)
    private fun canScheduleExactAlarms(alarmManager: AlarmManager): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            // در نسخه‌های قدیمی‌تر اندروید این مجوز لازم نیست
            true
        }
    }

    // تابع برای محاسبه زمان بعدی زنگ (برای تنظیم مجدد پس از ریستارت)
    fun getNextAlarmTime(hour: Int, minute: Int): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // اگر زمان گذشته بود، برای روز بعد تنظیم کن
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return calendar.timeInMillis
    }
    
    // تابع تنظیم مجدد آلارم صبح برای فردا (پس از اجرا)
    fun scheduleNextMorningAlarm(context: Context) {
        val prefsManager = PreferencesManager(context)
        
        // بررسی اینکه آلارم صبح فعال است
        if (!prefsManager.isMorningAlarmEnabled()) {
            return
        }
        
        val morningTime = prefsManager.getMorningAlarmTime()
        if (morningTime.isNotEmpty()) {
            val parts = morningTime.split(":")
            if (parts.size == 2) {
                try {
                    val hour = parts[0].toInt()
                    val minute = parts[1].toInt()
                    
                    // محاسبه زمان فردا برای آلارم
                    val nextAlarmTime = getTomorrowAlarmTime(hour, minute)
                    
                    // تنظیم آلارم فردا
                    setMorningAlarm(context, nextAlarmTime, true)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    
    // تابع تنظیم مجدد یادآور شب برای فردا (پس از اجرا)
    fun scheduleNextEveningAlarm(context: Context) {
        val prefsManager = PreferencesManager(context)
        
        // بررسی اینکه یادآور شب فعال است
        if (!prefsManager.isEveningAlarmEnabled()) {
            return
        }
        
        val eveningTime = prefsManager.getEveningAlarmTime()
        if (eveningTime.isNotEmpty()) {
            val parts = eveningTime.split(":")
            if (parts.size == 2) {
                try {
                    val hour = parts[0].toInt()
                    val minute = parts[1].toInt()
                    
                    // محاسبه زمان فردا برای یادآور
                    val nextAlarmTime = getTomorrowAlarmTime(hour, minute)
                    
                    // تنظیم یادآور فردا
                    setEveningAlarm(context, nextAlarmTime, true)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    
    // تابع محاسبه زمان فردا (مخصوص آلارم‌های تکراری)
    private fun getTomorrowAlarmTime(hour: Int, minute: Int): Long {
        val calendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1) // فردا
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }
    
    // تابع بررسی وضعیت آلارم‌ها
    fun getMorningAlarmStatus(context: Context): Pair<Boolean, String> {
        val prefsManager = PreferencesManager(context)
        val isEnabled = prefsManager.isMorningAlarmEnabled()
        val time = prefsManager.getMorningAlarmTime()
        return Pair(isEnabled, time)
    }
    
    fun getEveningAlarmStatus(context: Context): Pair<Boolean, String> {
        val prefsManager = PreferencesManager(context)
        val isEnabled = prefsManager.isEveningAlarmEnabled()
        val time = prefsManager.getEveningAlarmTime()
        return Pair(isEnabled, time)
    }
    
    // تابع فعال/غیرفعال کردن آلارم‌ها بدون حذف زمان‌شان
    fun toggleMorningAlarm(context: Context, enable: Boolean) {
        val prefsManager = PreferencesManager(context)
        
        if (enable) {
            val morningTime = prefsManager.getMorningAlarmTime()
            if (morningTime.isNotEmpty()) {
                val parts = morningTime.split(":")
                if (parts.size == 2) {
                    try {
                        val hour = parts[0].toInt()
                        val minute = parts[1].toInt()
                        setMorningAlarmWithTime(context, hour, minute)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        } else {
            cancelMorningAlarm(context)
        }
    }
    
    fun toggleEveningAlarm(context: Context, enable: Boolean) {
        val prefsManager = PreferencesManager(context)
        
        if (enable) {
            val eveningTime = prefsManager.getEveningAlarmTime()
            if (eveningTime.isNotEmpty()) {
                val parts = eveningTime.split(":")
                if (parts.size == 2) {
                    try {
                        val hour = parts[0].toInt()
                        val minute = parts[1].toInt()
                        setEveningAlarmWithTime(context, hour, minute)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        } else {
            cancelEveningAlarm(context)
        }
    }
}