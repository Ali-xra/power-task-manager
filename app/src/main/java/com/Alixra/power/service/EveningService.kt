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
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import com.Alixra.power.R
import com.Alixra.power.ui.EveningActivity

class EveningService : Service() {

    companion object {
        const val EVENING_SERVICE_CHANNEL_ID = "EVENING_SERVICE_CHANNEL"
        const val EVENING_SERVICE_NOTIFICATION_ID = 2003

        // متغیر static برای کنترل سرویس از خارج
        @Volatile
        private var serviceInstance: EveningService? = null

        // متد عمومی برای متوقف کردن سرویس از EveningActivity
        fun stopEveningAlarmByStep() {
            serviceInstance?.stopEveningAlarmCompletely()
        }
    }

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        serviceInstance = this // ذخیره reference برای کنترل خارجی
        acquireWakeLock()
        createNotificationChannel()
        startForegroundService()
        startEveningAlarm()
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
        try {
            // پاک‌سازی همه منابع
            handler.removeCallbacksAndMessages(null)
            serviceInstance = null
            stopEveningSound()
            stopEveningVibration()
            clearServiceNotification()
            releaseWakeLock()
        } catch (e: Exception) {
            android.util.Log.e("EveningService", "Error during service cleanup", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                EVENING_SERVICE_CHANNEL_ID,
                "سرویس یادآور شبانه",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "سرویس پخش زنگ و ویبره یادآور شبانه"
                setShowBadge(false)
                setSound(null, null) // بدون صدا برای این کانال
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun startForegroundService() {
        val intent = Intent(this, EveningActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, EVENING_SERVICE_CHANNEL_ID)
            .setContentTitle("یادآور شبانه در حال پخش")
            .setContentText("برای ادامه، مرحله اول را تکمیل کنید...")
            .setSmallIcon(R.drawable.ic_stat_name)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setSound(null) // بدون صدا برای این نوتیفیکیشن
            .setOnlyAlertOnce(true) // فقط یکبار alert
            .build()

        startForeground(EVENING_SERVICE_NOTIFICATION_ID, notification)
    }

    private fun startEveningAlarm() {
        // شروع همزمان صدا و ویبر - بدون تایمر خودکار قطع کردن
        startEveningSound()
        startEveningVibration()

        // *** تغییر اصلی: حذف تایمرهای خودکار ***
        // صدا و ویبره تا زمانی که از طریق EveningActivity متوقف نشوند، ادامه خواهند داشت

        // فقط یک تایمر امنیتی بعد از 5 دقیقه (در صورت بروز مشکل)
        handler.postDelayed({
            stopEveningAlarmCompletely()
        }, 5 * 60 * 1000) // 5 دقیقه تایمر امنیتی
    }

    private fun startEveningSound() {
        try {
            mediaPlayer = MediaPlayer().apply {
                // تلاش برای استفاده از فایل صوتی خودمان
                val uri = Uri.parse("android.resource://$packageName/${R.raw.alarm_sound}")

                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                        .build()
                )

                try {
                    setDataSource(this@EveningService, uri)
                } catch (e: Exception) {
                    // اگر فایل خودمان مشکل داشت، از رینگتون پیش‌فرض استفاده کن
                    val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

                    reset()
                    setDataSource(this@EveningService, defaultUri)
                }

                // *** تغییر اصلی: صدا باید loop باشد تا خودش متوقف نشود ***
                isLooping = true
                setVolume(0.8f, 0.8f) // کمی کم‌تر از آلارم صبح

                prepareAsync()

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
                        val fallbackUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                        setDataSource(this@EveningService, fallbackUri)
                        isLooping = true
                        setVolume(0.8f, 0.8f)
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

    private fun startEveningVibration() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        vibrator?.let { vib ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // *** تغییر اصلی: الگوی ویبره ملایم‌تر برای شب ***
                // 300ms روشن، 400ms خاموش، تکرار بی‌نهایت
                val pattern = longArrayOf(0, 300, 400)
                val effect = VibrationEffect.createWaveform(pattern, 0) // 0 = تکرار بی‌نهایت
                vib.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                val pattern = longArrayOf(0, 300, 400)
                vib.vibrate(pattern, 0) // 0 = تکرار بی‌نهایت
            }
        }
    }

    private fun stopEveningSound() {
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

    private fun stopEveningVibration() {
        vibrator?.cancel()
        vibrator = null
    }

    /**
     * پاک‌سازی نوتیفیکیشن سرویس (اما نه نوتیفیکیشن اصلی یادآور)
     */
    private fun clearServiceNotification() {
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // فقط نوتیفیکیشن سرویس را پاک می‌کنیم
            notificationManager.cancel(EVENING_SERVICE_NOTIFICATION_ID)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "PowerApp:EveningWakeLock"
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
     * این متد توسط EveningActivity هنگام رفتن به مرحله 2 فراخوانی می‌شود
     */
    fun stopEveningAlarmCompletely() {
        // حذف تمام تایمرها
        handler.removeCallbacksAndMessages(null)

        stopEveningSound()
        stopEveningVibration()
        clearServiceNotification()
        releaseWakeLock()
        stopSelf()
    }
}