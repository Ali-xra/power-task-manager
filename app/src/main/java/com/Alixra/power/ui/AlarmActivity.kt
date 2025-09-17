package com.Alixra.power.ui

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.Alixra.power.R
import com.Alixra.power.data.PreferencesManager
import com.Alixra.power.databinding.ActivityAlarmBinding
import com.Alixra.power.receiver.AlarmReceiver
import com.Alixra.power.service.AlarmService
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

/**
 * Activity چالش صبحگاهی - برای خاموش کردن زنگ باید جمله انگیزشی تایپ شود
 * بهبود: چک کردن real-time متن و خاموش شدن خودکار زنگ
 */
class AlarmActivity : BaseActivity() {

    private lateinit var binding: ActivityAlarmBinding
    private lateinit var prefsManager: PreferencesManager
    private var currentQuote = ""
    private var isAlarmActive = true
    private var isTypingEnabled = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // تنظیم Activity برای نمایش روی صفحه قفل
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                        android.view.WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }

        binding = ActivityAlarmBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefsManager = PreferencesManager(this)
        setupChallenge()
        setupClickListeners()
        setupRealTimeTextChecker()
    }

    private fun setupChallenge() {
        // انتخاب جمله تصادفی
        val quotes = prefsManager.getQuotes()
        if (quotes.isNotEmpty()) {
            currentQuote = quotes[Random.nextInt(quotes.size)]
            binding.challengeQuoteText.text = currentQuote
        } else {
            // اگر جمله‌ای ذخیره نشده، از جملات پیش‌فرض استفاده کن
            currentQuote = getString(R.string.default_motivational_quote)
            binding.challengeQuoteText.text = currentQuote
        }

        // تنظیم placeholder برای EditText
        binding.userInputEditText.hint = getString(R.string.typing_hint_placeholder)

        // نمایش تاریخ فعلی
        val formatter = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault())
        binding.currentDateText.text = formatter.format(Date())
    }

    private fun setupRealTimeTextChecker() {
        // اضافه کردن TextWatcher برای بررسی real-time متن
        binding.userInputEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // نیازی نیست کاری انجام دهیم
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // نیازی نیست کاری انجام دهیم
            }

            override fun afterTextChanged(s: Editable?) {
                if (isTypingEnabled && isAlarmActive) {
                    val userInput = s?.toString()?.trim() ?: ""

                    // بررسی اینکه آیا متن کامل و درست تایپ شده یا نه
                    if (userInput == currentQuote) {
                        // متن کامل و درست است - خاموش کردن فوری زنگ
                        challengeCompleted()
                    } else if (userInput.isNotEmpty()) {
                        // بررسی اینکه آیا شروع متن درست است یا نه
                        if (currentQuote.startsWith(userInput)) {
                            // تا اینجا درست تایپ شده - تغییر رنگ به سبز
                            binding.feedbackText.text = "✅ در حال تایپ صحیح..."
                            binding.feedbackText.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
                            binding.feedbackText.visibility = View.VISIBLE
                        } else {
                            // متن اشتباه - نمایش خطا
                            binding.feedbackText.text = "❌ متن اشتباه است!"
                            binding.feedbackText.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
                            binding.feedbackText.visibility = View.VISIBLE
                        }
                    } else {
                        // متن خالی - مخفی کردن feedback
                        binding.feedbackText.visibility = View.GONE
                    }
                }
            }
        })
    }

    private fun setupClickListeners() {
        // مخفی کردن دکمه تأیید چون دیگر نیازی نداریم
        binding.submitButton.visibility = View.GONE

        // مخفی کردن دکمه لغو برای جلوگیری از خاموش کردن تصادفی
        // این دکمه می‌تواند در حالت نیمه خواب به اشتباه فشرده شود
        binding.cancelButton.visibility = View.GONE

        // اختیاری: اگر همچنان می‌خواهیم دکمه تأیید موجود باشد
        binding.submitButton.setOnClickListener {
            if (isTypingEnabled) {
                val userInput = binding.userInputEditText.text.toString().trim()
                manualCheckUserInput(userInput)
            }
        }
    }

    private fun manualCheckUserInput(userInput: String) {
        if (userInput.isEmpty()) {
            showToast(getString(R.string.enter_sentence_message))
            return
        }

        // بررسی دقت تایپ (حساس به فاصله و نقطه‌گذاری)
        if (userInput == currentQuote) {
            // موفقیت در چالش
            challengeCompleted()
        } else {
            // خطا در تایپ
            challengeFailed(userInput)
        }
    }

    private fun challengeCompleted() {
        if (!isTypingEnabled) return // جلوگیری از اجرای مکرر

        isTypingEnabled = false // غیرفعال کردن تایپ

        // نمایش پیام موفقیت
        binding.feedbackText.text = "✅ عالی! زنگ خاموش شد. روز خوبی داشته باشید!"
        binding.feedbackText.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
        binding.feedbackText.visibility = View.VISIBLE

        // غیرفعال کردن EditText
        binding.userInputEditText.isEnabled = false
        binding.userInputEditText.alpha = 0.6f

        // ثبت موفقیت در آمار
        saveSuccessStats()

        // خاموش کردن سرویس آلارم و پاک کردن نوتیفیکیشن
        stopAlarmService()
        clearAlarmNotifications()

        // تأخیر برای نمایش پیام و بستن Activity
        binding.feedbackText.postDelayed({
            finishChallenge(true)
        }, 2000)
    }

    private fun challengeFailed(userInput: String) {
        // نمایش پیام خطا
        binding.feedbackText.text = "❌ متن اشتباه است. دوباره دقت کنید!"
        binding.feedbackText.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
        binding.feedbackText.visibility = View.VISIBLE

        // پاک کردن ورودی کاربر
        binding.userInputEditText.text.clear()

        // مخفی کردن پیام خطا بعد از 3 ثانیه
        binding.feedbackText.postDelayed({
            if (binding.feedbackText.text.toString().contains("❌")) {
                binding.feedbackText.visibility = View.GONE
            }
        }, 3000)
    }

    private fun saveSuccessStats() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        // بررسی اینکه آیا امروز قبلاً موفق شده یا نه
        val lastSuccessDate = prefsManager.getLastSuccessDate()
        if (lastSuccessDate != today) {
            // اولین موفقیت امروز
            val currentCount = prefsManager.getSuccessCount()
            prefsManager.saveSuccessCount(currentCount + 1)
            prefsManager.saveLastSuccessDate(today)
        }
    }

    private fun stopAlarmService() {
        try {
            val serviceIntent = Intent(this, AlarmService::class.java)
            val result = stopService(serviceIntent)
            isAlarmActive = false

            if (!result) {
                // Log if service wasn't running
                android.util.Log.w("AlarmActivity", "AlarmService was not running")
            }
        } catch (e: Exception) {
            android.util.Log.e("AlarmActivity", "Failed to stop AlarmService", e)
        }
    }

    private fun clearAlarmNotifications() {
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // پاک کردن نوتیفیکیشن آلارم با ID مشخص
            notificationManager.cancel(AlarmReceiver.ALARM_NOTIFICATION_ID)

            // پاک کردن تمام نوتیفیکیشن‌های مربوط به آلارم
            notificationManager.cancelAll()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // حذف شده: showCancelWarning() - دیگر لازم نیست چون دکمه لغو مخفی شده

    private fun finishChallenge(success: Boolean) {
        if (success) {
            showToast(getString(R.string.success_message))
        }

        // بستن Activity
        finish()
    }

    override fun onBackPressed() {
        if (isAlarmActive) {
            // جلوگیری از خروج با دکمه Back فقط اگر زنگ هنوز فعال است
            showToast(getString(R.string.complete_typing_message))
        } else {
            // اگر زنگ خاموش شده، اجازه خروج
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // اطمینان از خاموش شدن سرویس و پاک شدن نوتیفیکیشن
        if (isAlarmActive) {
            stopAlarmService()
            clearAlarmNotifications()
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}