package com.Alixra.power.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import com.Alixra.power.R
import com.Alixra.power.receiver.AlarmReceiver
import com.Alixra.power.ui.AlarmActivity

class AlarmService : Service() {

    companion object {
        const val ALARM_SERVICE_CHANNEL_ID = "ALARM_SERVICE_CHANNEL"
        const val ALARM_SERVICE_NOTIFICATION_ID = 1003
    }

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        acquireWakeLock()
        createNotificationChannel()
        startForegroundService()
        startAlarmSound()
        startVibration()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // اگر سرویس از قبل در حال اجرا بود، دوباره شروع نکن
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        // پاک‌سازی کامل هنگام توقف سرویس
        stopAlarmSound()
        stopVibration()
        clearAllNotifications()
        releaseWakeLock()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ALARM_SERVICE_CHANNEL_ID,
                "سرویس آلارم",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "سرویس پخش صدا و ویبره آلارم"
                setShowBadge(false)
                setSound(null, null) // بدون صدا برای این کانال
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun startForegroundService() {
        val intent = Intent(this, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, ALARM_SERVICE_CHANNEL_ID)
            .setContentTitle("آلارم در حال پخش")
            .setContentText("برای خاموش کردن، چالش را تکمیل کنید")
            .setSmallIcon(R.drawable.ic_stat_name)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setSound(null) // بدون صدا برای این نوتیفیکیشن
            .setOnlyAlertOnce(true) // فقط یکبار alert
            .build()

        startForeground(ALARM_SERVICE_NOTIFICATION_ID, notification)
    }

    private fun startAlarmSound() {
        try {
            mediaPlayer = MediaPlayer().apply {
                // تنظیم AudioAttributes ابتدا
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                        .build()
                )

                // تلاش برای استفاده از فایل صوتی خودمان
                try {
                    val uri = Uri.parse("android.resource://$packageName/${R.raw.alarm_sound}")
                    setDataSource(this@AlarmService, uri)
                } catch (e: Exception) {
                    // اگر فایل خودمان مشکل داشت، از رینگتون پیش‌فرض استفاده کن
                    val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

                    reset()
                    setDataSource(this@AlarmService, defaultUri)
                }

                // اطمینان از loop بودن برای زنگ صبحگاهی
                isLooping = true
                setVolume(1.0f, 1.0f)

                prepareAsync() // استفاده از prepareAsync بجای prepare

                setOnPreparedListener { mediaPlayer ->
                    try {
                        mediaPlayer.start()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                setOnErrorListener { _, what, extra ->
                    // در صورت خطا، سعی کن با رینگتون پیش‌فرض
                    try {
                        reset()
                        val fallbackUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        setDataSource(this@AlarmService, fallbackUri)
                        isLooping = true
                        setVolume(1.0f, 1.0f)
                        prepareAsync()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    true // خطا پردازش شد
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // اگر MediaPlayer کار نکرد، حداقل ویبره کار کند
        }
    }

    private fun startVibration() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        vibrator?.let { vib ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // الگوی ویبره برای زنگ صبح: قوی‌تر و مداوم‌تر
                // 500ms روشن، 300ms خاموش، تکرار بی‌نهایت
                val pattern = longArrayOf(0, 500, 300)
                val effect = VibrationEffect.createWaveform(pattern, 0) // 0 = تکرار بی‌نهایت
                vib.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                val pattern = longArrayOf(0, 500, 300)
                vib.vibrate(pattern, 0) // 0 = تکرار بی‌نهایت
            }
        }
    }

    private fun stopAlarmSound() {
        mediaPlayer?.let {
            try {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                mediaPlayer = null
            }
        }
    }

    private fun stopVibration() {
        vibrator?.cancel()
        vibrator = null
    }

    /**
     * پاک‌سازی تمام نوتیفیکیشن‌های مربوط به آلارم
     */
    private fun clearAllNotifications() {
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // پاک کردن نوتیفیکیشن اصلی آلارم
            notificationManager.cancel(AlarmReceiver.ALARM_NOTIFICATION_ID)

            // پاک کردن نوتیفیکیشن سرویس
            notificationManager.cancel(ALARM_SERVICE_NOTIFICATION_ID)

            // اختیاری: پاک کردن تمام نوتیفیکیشن‌های این اپ
            // notificationManager.cancelAll()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "PowerApp:AlarmWakeLock"
            )
            wakeLock?.acquire(10 * 60 * 1000L) // 10 دقیقه maximum
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
            wakeLock = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * متد عمومی برای متوقف کردن سرویس از خارج
     */
    fun stopAlarmCompletely() {
        stopAlarmSound()
        stopVibration()
        clearAllNotifications()
        releaseWakeLock()
        stopSelf()
    }
}