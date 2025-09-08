package com.Alixra.power.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.Alixra.power.data.PreferencesManager
import com.Alixra.power.utils.AlarmUtils

class BootReceiver : BroadcastReceiver() {

    /**
     * این متد زمانی که گوشی به طور کامل روشن و راه‌اندازی شد، توسط سیستم عامل فراخوانی می‌شود.
     */
    override fun onReceive(context: Context, intent: Intent) {
        // ما فقط به رویداد "BOOT_COMPLETED" (اتمام راه‌اندازی) اهمیت می‌دهیم
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {

            // یک نمونه از مدیر داده‌ها می‌سازیم
            val prefsManager = PreferencesManager(context)

            // --- تنظیم مجدد زنگ صبح ---
            val morningTime = prefsManager.getMorningAlarmTime()
            // اگر زمانی برای زنگ صبح ذخیره شده بود
            if (morningTime.isNotEmpty()) {
                val parts = morningTime.split(":")
                val hour = parts[0].toInt()
                val minute = parts[1].toInt()

                // محاسبه زمان بعدی زنگ
                val timeInMillis = AlarmUtils.getNextAlarmTime(hour, minute)
                // تنظیم مجدد زنگ
                AlarmUtils.setMorningAlarm(context, timeInMillis)
            }


            // --- تنظیم مجدد یادآور شب ---
            val eveningTime = prefsManager.getEveningAlarmTime()
            // اگر زمانی برای یادآور شب ذخیره شده بود
            if (eveningTime.isNotEmpty()) {
                val parts = eveningTime.split(":")
                val hour = parts[0].toInt()
                val minute = parts[1].toInt()

                // محاسبه زمان بعدی یادآور
                val timeInMillis = AlarmUtils.getNextAlarmTime(hour, minute)
                // تنظیم مجدد یادآور
                AlarmUtils.setEveningAlarm(context, timeInMillis)
            }
        }
    }
}