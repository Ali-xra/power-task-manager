package com.Alixra.power.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
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

            // استفاده از setExactAndAllowWhileIdle برای آلارم‌های دقیق
            // این روش تضمین می‌کند که آلارم در زمان دقیق اجرا شود
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    // Android 6+ - setExactAndAllowWhileIdle برای عبور از Doze mode
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        timeInMillis,
                        pendingIntent
                    )
                } else {
                    // Android کمتر از 6 - setExact کافی است
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        timeInMillis,
                        pendingIntent
                    )
                }
            } catch (e: Exception) {
                // fallback به setExactAndAllowWhileIdle
                try {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        timeInMillis,
                        pendingIntent
                    )
                } catch (e2: Exception) {
                    e2.printStackTrace()
                }
            }
            
            // ذخیره وضعیت فعال بودن آلارم صبح
            if (enableRepeating) {
                prefsManager.setMorningAlarmEnabled(true)
            }
        }
    }
    
    // تابع تنظیم آلارم صبح با ساعت و دقیقه
    fun setMorningAlarmWithTime(context: Context, hour: Int, minute: Int) {
        if (hour !in 0..23 || minute !in 0..59) {
            throw IllegalArgumentException("Invalid time: hour must be 0-23, minute must be 0-59")
        }

        val prefsManager = PreferencesManager(context)
        val timeString = String.format("%02d:%02d", hour, minute)
        
        // ذخیره زمان آلارم
        prefsManager.saveMorningAlarmTime(timeString)
        
        // دریافت روزهای انتخاب شده
        val selectedDays = prefsManager.getSelectedDays()
        
        if (selectedDays.isEmpty() || selectedDays.size == 7) {
            // اگر همه روزها انتخاب شده‌اند، از روش قدیمی استفاده کن
            val timeInMillis = getNextAlarmTime(hour, minute)
            setMorningAlarm(context, timeInMillis, true)
        } else {
            // برای روزهای خاص، آلارم‌های جداگانه تنظیم کن
            setMorningAlarmForSpecificDays(context, hour, minute, selectedDays)
        }
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

            // استفاده از setExactAndAllowWhileIdle برای یادآورهای دقیق
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    // Android 6+ - setExactAndAllowWhileIdle برای عبور از Doze mode
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        timeInMillis,
                        pendingIntent
                    )
                } else {
                    // Android کمتر از 6 - setExact کافی است
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        timeInMillis,
                        pendingIntent
                    )
                }
            } catch (e: Exception) {
                // fallback به setExactAndAllowWhileIdle
                try {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        timeInMillis,
                        pendingIntent
                    )
                } catch (e2: Exception) {
                    e2.printStackTrace()
                }
            }
            
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
        
        // دریافت روزهای انتخاب شده برای یادآور شب
        val selectedDays = prefsManager.getEveningSelectedDays()
        
        if (selectedDays.isEmpty() || selectedDays.size == 7) {
            // اگر همه روزها انتخاب شده‌اند، از روش قدیمی استفاده کن
            val timeInMillis = getNextAlarmTime(hour, minute)
            setEveningAlarm(context, timeInMillis, true)
        } else {
            // برای روزهای خاص، یادآور جداگانه تنظیم کن
            setEveningAlarmForSpecificDays(context, hour, minute, selectedDays)
        }
    }

    // تابع برای لغو زنگ صبح (نسخه بهبود یافته)
    fun cancelMorningAlarm(context: Context) {
        val prefsManager = PreferencesManager(context)
        
        // لغو همه آلارم‌های صبح (روزانه و هفتگی)
        cancelAllMorningAlarms(context)
        
        // غیرفعال کردن وضعیت آلارم صبح
        prefsManager.setMorningAlarmEnabled(false)
    }

    // تابع برای لغو یادآور شب (نسخه بهبود یافته)
    fun cancelEveningAlarm(context: Context) {
        val prefsManager = PreferencesManager(context)
        
        // لغو همه یادآورهای شب (روزانه و هفتگی)
        cancelAllEveningAlarms(context)
        
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
                    setMorningAlarm(context, nextAlarmTime, enableRepeating)
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
                    setEveningAlarm(context, nextAlarmTime, enableRepeating)
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
    
    // تابع تنظیم آلارم صبح برای روزهای خاص هفته
    private fun setMorningAlarmForSpecificDays(context: Context, hour: Int, minute: Int, selectedDays: List<Int>) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val prefsManager = PreferencesManager(context)
        
        // ابتدا همه آلارم‌های قبلی را لغو کن
        cancelAllMorningAlarms(context)
        
        if (canScheduleExactAlarms(alarmManager)) {
            selectedDays.forEach { dayOfWeek ->
                val requestCode = MORNING_ALARM_REQUEST_CODE + dayOfWeek // شناسه منحصر به فرد برای هر روز
                
                val intent = Intent(context, AlarmReceiver::class.java)
                intent.putExtra("isRepeating", true)
                intent.putExtra("dayOfWeek", dayOfWeek)
                
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                
                // محاسبه زمان بعدی برای این روز خاص
                val nextAlarmTime = getNextAlarmTimeForDay(hour, minute, dayOfWeek)
                
                try {
                    // استفاده از setExactAndAllowWhileIdle برای آلارم‌های دقیق
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            nextAlarmTime,
                            pendingIntent
                        )
                    } else {
                        alarmManager.setExact(
                            AlarmManager.RTC_WAKEUP,
                            nextAlarmTime,
                            pendingIntent
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            prefsManager.setMorningAlarmEnabled(true)
        }
    }
    
    // محاسبه زمان بعدی آلارم برای یک روز خاص هفته
    private fun getNextAlarmTimeForDay(hour: Int, minute: Int, targetDayOfWeek: Int): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        var daysToAdd = targetDayOfWeek - currentDayOfWeek
        
        // اگر روز هدف امروز است و زمان گذشته، برای هفته بعد تنظیم کن
        if (daysToAdd == 0 && calendar.before(Calendar.getInstance())) {
            daysToAdd = 7
        } else if (daysToAdd < 0) {
            // اگر روز هدف در هفته گذشته است، برای هفته بعد تنظیم کن
            daysToAdd += 7
        }
        
        calendar.add(Calendar.DAY_OF_YEAR, daysToAdd)
        return calendar.timeInMillis
    }
    
    // لغو همه آلارم‌های صبح (برای همه روزها)
    private fun cancelAllMorningAlarms(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        // لغو آلارم روزانه (اگر وجود دارد)
        val dailyIntent = Intent(context, AlarmReceiver::class.java)
        val dailyPendingIntent = PendingIntent.getBroadcast(
            context,
            MORNING_ALARM_REQUEST_CODE,
            dailyIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (dailyPendingIntent != null) {
            alarmManager.cancel(dailyPendingIntent)
            dailyPendingIntent.cancel()
        }
        
        // لغو آلارم‌های مخصوص روزهای هفته
        for (dayOfWeek in Calendar.SUNDAY..Calendar.SATURDAY) {
            val requestCode = MORNING_ALARM_REQUEST_CODE + dayOfWeek
            val intent = Intent(context, AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }
    
    // تابع تنظیم یادآور شب برای روزهای خاص هفته
    private fun setEveningAlarmForSpecificDays(context: Context, hour: Int, minute: Int, selectedDays: List<Int>) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val prefsManager = PreferencesManager(context)
        
        // ابتدا همه یادآورهای قبلی را لغو کن
        cancelAllEveningAlarms(context)
        
        if (canScheduleExactAlarms(alarmManager)) {
            selectedDays.forEach { dayOfWeek ->
                val requestCode = EVENING_ALARM_REQUEST_CODE + dayOfWeek // شناسه منحصر به فرد برای هر روز
                
                val intent = Intent(context, EveningReceiver::class.java)
                intent.putExtra("isRepeating", true)
                intent.putExtra("dayOfWeek", dayOfWeek)
                
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                
                // محاسبه زمان بعدی برای این روز خاص
                val nextAlarmTime = getNextAlarmTimeForDay(hour, minute, dayOfWeek)
                
                try {
                    // استفاده از setExactAndAllowWhileIdle برای یادآورهای دقیق
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            nextAlarmTime,
                            pendingIntent
                        )
                    } else {
                        alarmManager.setExact(
                            AlarmManager.RTC_WAKEUP,
                            nextAlarmTime,
                            pendingIntent
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            prefsManager.setEveningAlarmEnabled(true)
        }
    }
    
    // لغو همه یادآورهای شب (برای همه روزها)
    private fun cancelAllEveningAlarms(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        // لغو یادآور روزانه (اگر وجود دارد)
        val dailyIntent = Intent(context, EveningReceiver::class.java)
        val dailyPendingIntent = PendingIntent.getBroadcast(
            context,
            EVENING_ALARM_REQUEST_CODE,
            dailyIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (dailyPendingIntent != null) {
            alarmManager.cancel(dailyPendingIntent)
            dailyPendingIntent.cancel()
        }
        
        // لغو یادآورهای مخصوص روزهای هفته
        for (dayOfWeek in Calendar.SUNDAY..Calendar.SATURDAY) {
            val requestCode = EVENING_ALARM_REQUEST_CODE + dayOfWeek
            val intent = Intent(context, EveningReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
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