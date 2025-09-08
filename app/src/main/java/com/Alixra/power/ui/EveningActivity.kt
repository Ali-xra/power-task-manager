package com.Alixra.power.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.Alixra.power.data.PreferencesManager
import com.Alixra.power.data.Task
import com.Alixra.power.data.TaskCategory
import com.Alixra.power.databinding.ActivityEveningBinding
import com.Alixra.power.receiver.EveningReceiver
import com.Alixra.power.service.EveningService
import com.Alixra.power.ui.adapters.CategoryRatingAdapter
import com.Alixra.power.ui.adapters.TodayTasksAdapter
import java.text.SimpleDateFormat
import java.util.*

class EveningActivity : BaseActivity() {

    private lateinit var binding: ActivityEveningBinding
    private lateinit var prefsManager: PreferencesManager

    // ویژگی‌های جدید برای سیستم چندمرحله‌ای
    private var currentStep = 1
    private var dailyRating = 5
    private val categoryRatings = mutableMapOf<String, Int>()
    private var todayTasks = listOf<Task>()

    // *** متغیر جدید برای کنترل وضعیت اجباری ***
    private var isEveningServiceStopped = false

    // Adapters
    private lateinit var categoryRatingAdapter: CategoryRatingAdapter
    private lateinit var todayTasksAdapter: TodayTasksAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEveningBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupPreferences()
        setupCurrentDate()

        // *** تغییر اصلی: حذف توقف خودکار سرویس در onCreate ***
        // سرویس یادآور شبانه را در اینجا متوقف نمی‌کنیم
        // فقط وقتی کاربر از مرحله 1 به مرحله 2 برود، متوقف می‌شود

        // بررسی وجود layout جدید یا قدیمی
        if (hasNewLayout()) {
            setupNewMultiStepSystem()
        } else {
            setupLegacySystem()
        }
    }

    private fun hasNewLayout(): Boolean {
        // بررسی وجود عناصر جدید در layout
        return try {
            binding.step1Layout
            binding.step2Layout
            binding.step3Layout
            binding.dailyRatingSeekBar
            binding.dailyRatingValueTextView
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun setupPreferences() {
        prefsManager = PreferencesManager(this)
    }

    private fun setupCurrentDate() {
        val formatter = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault())
        binding.dateTextView.text = formatter.format(Date())
    }

    // === متدهای سیستم جدید (Multi-Step) ===

    private fun setupNewMultiStepSystem() {
        setupRecyclerViews()
        setupStep1()
        setupClickListeners()

        // بازیابی مرحله فعلی (در صورت قطع ناگهانی)
        currentStep = prefsManager.getEveningStep()
        showCurrentStep()
        
        // تنظیم OnBackPressedCallback
        setupBackPressedCallback()
    }

    private fun setupRecyclerViews() {
        // آداپتور امتیازدهی بخش‌ها
        categoryRatingAdapter = CategoryRatingAdapter { categoryId, rating ->
            categoryRatings[categoryId] = rating
        }
        binding.categoriesRatingRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.categoriesRatingRecyclerView.adapter = categoryRatingAdapter

        // آداپتور کارهای امروز
        todayTasksAdapter = TodayTasksAdapter(prefsManager) { task, isCompleted ->
            // به‌روزرسانی وضعیت کار
            val updatedTask = task.copy(isCompleted = isCompleted)
            prefsManager.saveTask(updatedTask)

            // به‌روزرسانی لیست محلی
            val index = todayTasks.indexOfFirst { it.id == task.id }
            if (index != -1) {
                todayTasks = todayTasks.toMutableList().apply {
                    set(index, updatedTask)
                }
            }

            updateIncompleteTasksText()
        }
        binding.todayTasksRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.todayTasksRecyclerView.adapter = todayTasksAdapter
    }

    private fun setupStep1() {
        try {
            // فقط از dailyRatingSeekBar استفاده می‌کنیم و ratingSeekBar را مخفی می‌کنیم
            binding.dailyRatingSeekBar.progress = 5
            updateDailyRatingText(5)

            // مخفی کردن SeekBar قدیمی اگر وجود دارد
            try {
                binding.ratingSeekBar.visibility = View.GONE
            } catch (e: Exception) {
                // اگر وجود نداشت، نادیده بگیر
            }

            binding.dailyRatingSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        dailyRating = progress
                        updateDailyRatingText(progress)
                    }
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateDailyRatingText(rating: Int) {
        try {
            binding.dailyRatingValueTextView.text = "$rating"

            // تغییر رنگ بر اساس امتیاز
            val color = when (rating) {
                in 0..2 -> "#D32F2F"  // قرمز
                in 3..4 -> "#F57C00"  // نارنجی
                in 5..6 -> "#388E3C"  // سبز
                in 7..8 -> "#1B5E20"  // سبز پررنگ
                in 9..10 -> "#4A148C" // بنفش
                else -> "#757575"     // خاکستری
            }
            binding.dailyRatingValueTextView.setTextColor(Color.parseColor(color))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupClickListeners() {
        try {
            binding.nextButton.setOnClickListener {
                when (currentStep) {
                    1 -> goToStep2()
                    2 -> goToStep3()
                    3 -> finishEveningReview()
                }
            }

            binding.previousButton.setOnClickListener {
                when (currentStep) {
                    2 -> {
                        currentStep = 1
                        showCurrentStep()
                    }
                    3 -> {
                        currentStep = 2
                        showCurrentStep()
                    }
                }
            }

            binding.skipButton.setOnClickListener {
                Toast.makeText(this, "باشه، فردا شب دوباره می‌بینمت!", Toast.LENGTH_SHORT).show()
                clearEveningNotification()
                finish()
            }
        } catch (e: Exception) {
            // اگر دکمه‌ها موجود نبودند، نادیده بگیر
        }
    }

    private fun showCurrentStep() {
        try {
            // مخفی کردن همه مراحل
            binding.step1Layout.visibility = View.GONE
            binding.step2Layout.visibility = View.GONE
            binding.step3Layout.visibility = View.GONE

            // نمایش مرحله فعلی
            when (currentStep) {
                1 -> {
                    binding.step1Layout.visibility = View.VISIBLE
                    binding.nextButton.text = "▶ مرحله بعد"
                    updateStepIndicators(1)
                }
                2 -> {
                    binding.step2Layout.visibility = View.VISIBLE
                    binding.nextButton.text = "▶ مرحله بعد"
                    loadCategoriesForRating()
                    updateStepIndicators(2)

                    // *** تغییر اصلی: متوقف کردن سرویس هنگام ورود به مرحله 2 ***
                    stopEveningServiceOnStepChange()
                }
                3 -> {
                    binding.step3Layout.visibility = View.VISIBLE
                    binding.nextButton.text = "✅ پایان"
                    loadTodayTasks()
                    updateStepIndicators(3)
                }
            }
        } catch (e: Exception) {
            // اگر عناصر جدید موجود نبود، فقط SeekBar نمایش بده
        }
    }

    private fun updateStepIndicators(activeStep: Int) {
        try {
            val activeColor = Color.parseColor("#8E24AA")
            val inactiveColor = Color.parseColor("#E1BEE7")

            binding.step1Indicator.setBackgroundColor(if (activeStep >= 1) activeColor else inactiveColor)
            binding.step2Indicator.setBackgroundColor(if (activeStep >= 2) activeColor else inactiveColor)
            binding.step3Indicator.setBackgroundColor(if (activeStep >= 3) activeColor else inactiveColor)
        } catch (e: Exception) {
            // اگر نشانگرها موجود نبود، نادیده بگیر
        }
    }

    private fun goToStep2() {
        currentStep = 2
        prefsManager.saveEveningStep(currentStep)

        // ذخیره امتیاز کلی
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        prefsManager.saveDailyRating(today, dailyRating)

        showCurrentStep()
    }

    private fun loadCategoriesForRating() {
        val categories = prefsManager.getTaskCategories()
        categoryRatingAdapter.updateCategories(categories)
    }

    private fun goToStep3() {
        currentStep = 3
        prefsManager.saveEveningStep(currentStep)

        // ذخیره امتیازات بخش‌ها
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        categoryRatings.forEach { (categoryId, rating) ->
            prefsManager.saveCategoryRating(today, categoryId, rating)
        }

        showCurrentStep()
    }

    private fun loadTodayTasks() {
        todayTasks = prefsManager.getTodayTasks()
        todayTasksAdapter.updateTasks(todayTasks)
        updateIncompleteTasksText()
    }

    private fun updateIncompleteTasksText() {
        try {
            val incompleteTasks = todayTasks.filter { !it.isCompleted }
            if (incompleteTasks.isNotEmpty()) {
                binding.incompleteTasksText.visibility = View.VISIBLE
                binding.incompleteTasksText.text = "${incompleteTasks.size} کار انجام نشده به فردا منتقل می‌شود"
            } else {
                binding.incompleteTasksText.visibility = View.GONE
            }
        } catch (e: Exception) {
            // اگر عنصر موجود نبود، نادیده بگیر
        }
    }

    private fun finishEveningReview() {
        // انتقال کارهای ناتمام به فردا
        val incompleteTasks = todayTasks.filter { !it.isCompleted }
        incompleteTasks.forEach { task ->
            val tomorrowTask = prefsManager.moveTaskToTomorrow(task)
            prefsManager.saveTask(tomorrowTask)
        }

        // ذخیره آمار و بازگشت به مرحله اول برای فردا
        saveStatistics()
        resetEveningStep()
        showThankYouMessage()
        clearEveningNotification()
        finish()
    }

    private fun saveStatistics() {
        // به‌روزرسانی آمار کلی
        val allRatings = prefsManager.getAllRatings()
        val totalDays = allRatings.size
        val averageRating = if (totalDays > 0) {
            allRatings.values.average()
        } else 0.0

        prefsManager.saveTotalRatingDays(totalDays)
        prefsManager.saveAverageRating(averageRating)

        // ذخیره تاریخ آخرین موفقیت
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        prefsManager.saveLastSuccessDate(today)

        // افزایش شمارنده موفقیت
        val currentCount = prefsManager.getSuccessCount()
        prefsManager.saveSuccessCount(currentCount + 1)
    }

    private fun resetEveningStep() {
        prefsManager.saveEveningStep(1)
    }

    private fun showThankYouMessage() {
        val message = when (dailyRating) {
            in 0..3 -> "متأسفم که روز خوبی نداشتید. فردا بهتر خواهد بود!"
            in 4..6 -> "یک روز معمولی! امیدوارم فردا پر از اتفاق خوب باشه."
            in 7..8 -> "عالیه! یه روز خوب رو پشت سر گذاشتی."
            in 9..10 -> "فوق‌العاده! بهت تبریک می‌گم!"
            else -> "ممنون از پاسخت!"
        }
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun showMotivationalMessage() {
        val messages = arrayOf(
            "امروز چطور بود؟ 🤔",
            "زمان ارزیابی روزتان! 📊",
            "بیایید روز امروز را بررسی کنیم! ✨",
            "چند دقیقه وقت دارید؟ 🕒"
        )
        val randomMessage = messages.random()
        Toast.makeText(this, randomMessage, Toast.LENGTH_SHORT).show()
    }

    private fun clearEveningNotification() {
        try {
            EveningReceiver.clearNotification(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // *** متد جدید برای متوقف کردن سرویس هنگام رفتن به مرحله 2 ***
    private fun stopEveningServiceOnStepChange() {
        if (!isEveningServiceStopped) {
            try {
                // استفاده از متد static EveningService برای متوقف کردن
                EveningService.stopEveningAlarmByStep()
                isEveningServiceStopped = true

                // نمایش پیام تایید
                Toast.makeText(this, "✅ صدا و ویبره متوقف شد", Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                e.printStackTrace()
                // در صورت خطا، سعی کن با روش قدیمی
                try {
                    val serviceIntent = Intent(this, EveningService::class.java)
                    stopService(serviceIntent)
                    isEveningServiceStopped = true
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        }
    }

    // === سیستم قدیمی (Legacy) ===

    private fun setupLegacySystem() {
        // کد قدیمی برای سیستم تک‌مرحله‌ای
        try {
            binding.ratingSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        dailyRating = progress
                        updateLegacyRatingText(progress)
                    }
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })

            binding.submitButton.setOnClickListener {
                saveRating(dailyRating)
                showThankYouMessage()
                clearEveningNotification()
                finish()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateLegacyRatingText(rating: Int) {
        try {
            // تلاش برای استفاده از المان‌های مختلف موجود در layout
            try {
                binding.dailyRatingValueTextView.text = "$rating"
            } catch (e: Exception) {
                // اگر dailyRatingValueTextView موجود نبود، فقط لاگ کن
                e.printStackTrace()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveRating(rating: Int) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        prefsManager.saveDailyRating(today, rating)

        // به‌روزرسانی آمار
        val allRatings = prefsManager.getAllRatings()
        val totalDays = allRatings.size
        val averageRating = if (totalDays > 0) {
            allRatings.values.average()
        } else 0.0

        prefsManager.saveTotalRatingDays(totalDays)
        prefsManager.saveAverageRating(averageRating)
        prefsManager.saveLastSuccessDate(today)

        val currentCount = prefsManager.getSuccessCount()
        prefsManager.saveSuccessCount(currentCount + 1)
    }

    // === بازگشت به عقب ===
    private fun setupBackPressedCallback() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when (currentStep) {
                    1 -> {
                        if (!isEveningServiceStopped) {
                            Toast.makeText(
                                this@EveningActivity,
                                "🚫 برای خروج، ابتدا مرحله اول را تکمیل کنید",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            clearEveningNotification()
                            resetEveningStep()
                            finish()
                        }
                    }
                    2 -> {
                        currentStep = 1
                        showCurrentStep()
                    }
                    3 -> {
                        currentStep = 2
                        showCurrentStep()
                    }
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }

    override fun onDestroy() {
        super.onDestroy()
        // اطمینان از متوقف شدن سرویس هنگام بستن Activity
        if (!isEveningServiceStopped) {
            try {
                EveningService.stopEveningAlarmByStep()
            } catch (e: Exception) {
                // fallback
                try {
                    val serviceIntent = Intent(this, EveningService::class.java)
                    stopService(serviceIntent)
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        }
    }
}