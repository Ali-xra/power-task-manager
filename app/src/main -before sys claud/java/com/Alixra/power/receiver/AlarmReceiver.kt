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
import com.Alixra.power.service.AlarmService
import com.Alixra.power.ui.AlarmActivity

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ALARM_CHANNEL_ID = "ALARM_CHANNEL"
        const val ALARM_NOTIFICATION_ID = 1001

        // اضافه کردن متد برای پاک کردن نوتیفیکیشن از خارج
        fun clearNotification(context: Context) {
            try {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(ALARM_NOTIFICATION_ID)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        // ابتدا پاک کردن نوتیفیکیشن‌های قبلی (در صورت وجود)
        clearPreviousNotifications(context)

        // شروع سرویس صدا و ویبره
        val serviceIntent = Intent(context, AlarmService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }

        // ایجاد Intent برای باز کردن AlarmActivity
        val fullScreenIntent = Intent(context, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_NO_USER_ACTION or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            ALARM_NOTIFICATION_ID, // استفاده از همان ID برای یکسان‌سازی
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // ایجاد کانال نوتیفیکیشن
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ALARM_CHANNEL_ID,
                "زنگ هوشمند",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "کانال نوتیفیکیشن برای زنگ صبحگاهی"
                setBypassDnd(true) // حتی در حالت Do Not Disturb کار کند
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }

        // ساخت نوتیفیکیشن با Full Screen Intent
        val notificationBuilder = NotificationCompat.Builder(context, ALARM_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_name)
            .setContentTitle("⏰ زنگ هوشمند")
            .setContentText("برای خاموش کردن، چالش را انجام دهید")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false) // جلوگیری از حذف خودکار با swipe
            .setOngoing(true) // نوتیفیکیشن را غیرقابل حذف می‌کند
            .setContentIntent(fullScreenPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true) // این خط کلیدی است!
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
            .setTimeoutAfter(5 * 60 * 1000) // timeout بعد از 5 دقیقه (اختیاری)

        // اضافه کردن اکشن برای خاموش کردن مستقیم
        val dismissIntent = Intent(context, AlarmDismissReceiver::class.java)
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_NOTIFICATION_ID + 1,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        notificationBuilder.addAction(
            R.drawable.ic_stat_name,
            "خاموش کردن",
            dismissPendingIntent
        )

        // نمایش نوتیفیکیشن
        notificationManager.notify(ALARM_NOTIFICATION_ID, notificationBuilder.build())

        // در Android 10+ اگر صفحه قفل نیست، مستقیماً Activity را باز کن
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                context.startActivity(fullScreenIntent)
            }
        } catch (e: Exception) {
            // اگر نتوانست Activity را باز کند، نوتیفیکیشن کافی است
            e.printStackTrace()
        }
    }

    private fun clearPreviousNotifications(context: Context) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            // پاک کردن نوتیفیکیشن‌های قبلی آلارم
            notificationManager.cancel(ALARM_NOTIFICATION_ID)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * BroadcastReceiver برای مدیریت خاموش کردن آلارم از نوتیفیکیشن
     */
    class AlarmDismissReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // خاموش کردن سرویس آلارم
            val serviceIntent = Intent(context, AlarmService::class.java)
            context.stopService(serviceIntent)

            // پاک کردن نوتیفیکیشن
            clearNotification(context)

            // اختیاری: نمایش Toast
            android.widget.Toast.makeText(
                context,
                "زنگ خاموش شد بدون انجام چالش",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }
}