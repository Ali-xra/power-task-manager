package com.Alixra.power.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import com.Alixra.power.receiver.AlarmReceiver
import com.Alixra.power.receiver.EveningReceiver
import java.util.Calendar

object AlarmUtils {

    // یک شناسه منحصر به فرد برای هر نوع زنگ تعریف می‌کنیم
    private const val MORNING_ALARM_REQUEST_CODE = 1001
    private const val EVENING_ALARM_REQUEST_CODE = 1002

    // تابع تنظیم زنگ صبح
    fun setMorningAlarm(context: Context, timeInMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // ابتدا چک می‌کنیم که آیا مجوز لازم برای تنظیم زنگ دقیق را داریم یا نه
        if (canScheduleExactAlarms(alarmManager)) {
            val intent = Intent(context, AlarmReceiver::class.java)
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
        }
    }

    // تابع تنظیم یادآور شب
    fun setEveningAlarm(context: Context, timeInMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (canScheduleExactAlarms(alarmManager)) {
            val intent = Intent(context, EveningReceiver::class.java)
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
        }
    }

    // تابع برای لغو زنگ صبح
    fun cancelMorningAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
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
        }
    }

    // تابع برای لغو یادآور شب
    fun cancelEveningAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, EveningReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            EVENING_ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
        }
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
}