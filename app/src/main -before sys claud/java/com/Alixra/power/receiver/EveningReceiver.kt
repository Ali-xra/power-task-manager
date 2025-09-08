package com.Alixra.power.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.Alixra.power.R
import com.Alixra.power.service.EveningService
import com.Alixra.power.ui.EveningActivity

class EveningReceiver : BroadcastReceiver() {

    companion object {
        const val EVENING_CHANNEL_ID = "EVENING_CHANNEL"
        const val EVENING_NOTIFICATION_ID = 2002

        // اضافه کردن متد برای پاک کردن نوتیفیکیشن از خارج
        fun clearNotification(context: Context) {
            try {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(EVENING_NOTIFICATION_ID)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        // ابتدا پاک کردن نوتیفیکیشن‌های قبلی (در صورت وجود)
        clearPreviousNotifications(context)

        // شروع سرویس صدا و ویبره - عیناً مثل AlarmReceiver
        val serviceIntent = Intent(context, EveningService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }

        // ایجاد Intent برای باز کردن EveningActivity - عیناً مثل AlarmReceiver
        val fullScreenIntent = Intent(context, EveningActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_NO_USER_ACTION or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            EVENING_NOTIFICATION_ID, // استفاده از همان ID برای یکسان‌سازی
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // ایجاد کانال نوتیفیکیشن
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                EVENING_CHANNEL_ID,
                "یادآور شبانه",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "کانال نوتیفیکیشن برای یادآور شبانه"
                setBypassDnd(true) // حتی در حالت Do Not Disturb کار کند
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                enableLights(true)
                enableVibration(false) // ویبر از سرویس مدیریت می‌شود
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // ساخت نوتیفیکیشن با Full Screen Intent - عیناً مثل AlarmReceiver
        val notificationBuilder = NotificationCompat.Builder(context, EVENING_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_name)
            .setContentTitle("🌙 وقت ارزیابی شبانه")
            .setContentText("امروز چقدر از خودتان راضی بودید؟")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false) // جلوگیری از حذف خودکار با swipe
            .setOngoing(true) // نوتیفیکیشن را غیرقابل حذف می‌کند
            .setContentIntent(fullScreenPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true) // این خط کلیدی است!
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_LIGHTS) // فقط نور، بدون صدا اضافی
            .setTimeoutAfter(10 * 60 * 1000) // timeout بعد از 10 دقیقه

        // اضافه کردن تنظیمات بیشتر برای Android های جدید
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationBuilder.setChannelId(EVENING_CHANNEL_ID)
        }

        // اضافه کردن اکشن برای باز کردن مستقیم
        val openIntent = Intent(context, EveningActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            EVENING_NOTIFICATION_ID + 1,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        notificationBuilder.addAction(
            R.drawable.ic_stat_name,
            "شروع ارزیابی",
            openPendingIntent
        )

        // *** تغییر اصلی: حذف دکمه "رد کردن" برای اجباری کردن ***
        // قبلاً دکمه "رد کردن" وجود داشت که کاربر می‌توانست یادآور را رد کند
        // حالا این دکمه حذف شده و کاربر مجبور است مرحله اول را تکمیل کند

        // کد قدیمی (حذف شده):
        /*
        val dismissIntent = Intent(context, EveningDismissReceiver::class.java)
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            EVENING_NOTIFICATION_ID + 2,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        notificationBuilder.addAction(
            R.drawable.ic_stat_name,
            "رد کردن",
            dismissPendingIntent
        )
        */

        // نمایش نوتیفیکیشن
        notificationManager.notify(EVENING_NOTIFICATION_ID, notificationBuilder.build())

        // **تغییر اصلی**: حذف باز کردن مستقیم Activity
        // فقط نوتیفیکیشن با fullScreenIntent کافی است - مثل AlarmReceiver
    }

    private fun clearPreviousNotifications(context: Context) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            // پاک کردن نوتیفیکیشن‌های قبلی یادآور شبانه
            notificationManager.cancel(EVENING_NOTIFICATION_ID)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * *** تغییر اصلی: EveningDismissReceiver دیگر استفاده نمی‌شود ***
     *
     * قبلاً این BroadcastReceiver برای مدیریت رد کردن یادآور شبانه استفاده می‌شد
     * اما حالا که سیستم اجباری شده، این قسمت غیرفعال شده است
     *
     * اگر در آینده نیاز به بازگرداندن این قابلیت بود، می‌توان کد زیر را فعال کرد:
     */

    /*
    class EveningDismissReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // متوقف کردن سرویس یادآور شبانه (اگر هنوز اجرا است)
            val serviceIntent = Intent(context, EveningService::class.java)
            context.stopService(serviceIntent)

            // پاک کردن نوتیفیکیشن
            clearNotification(context)

            // نمایش Toast
            android.widget.Toast.makeText(
                context,
                "یادآور شبانه رد شد. فردا شب دوباره می‌بینمت!",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }
    */

    /**
     * *** اضافه شدن: EveningEmergencyDismissReceiver ***
     *
     * در صورت نیاز اورژانسی، می‌توان از این receiver استفاده کرد
     * اما در حالت عادی فعال نیست
     */
    class EveningEmergencyDismissReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // فقط در شرایط اورژانسی استفاده شود
            // این receiver در نوتیفیکیشن عادی نمایش داده نمی‌شود

            try {
                // متوقف کردن سرویس یادآور شبانه
                EveningService.stopEveningAlarmByStep()

                // پاک کردن نوتیفیکیشن
                clearNotification(context)

                // نمایش Toast
                android.widget.Toast.makeText(
                    context,
                    "⚠️ یادآور شبانه به صورت اورژانسی متوقف شد",
                    android.widget.Toast.LENGTH_LONG
                ).show()

            } catch (e: Exception) {
                e.printStackTrace()

                // fallback method
                try {
                    val serviceIntent = Intent(context, EveningService::class.java)
                    context.stopService(serviceIntent)
                    clearNotification(context)
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        }
    }
}