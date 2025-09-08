package com.Alixra.power.ui

import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.Alixra.power.data.PreferencesManager
import com.Alixra.power.data.TimePeriod
import com.Alixra.power.databinding.ActivityStatsBinding
import java.text.SimpleDateFormat
import java.util.*

class StatsActivity : BaseActivity() {

    private lateinit var binding: ActivityStatsBinding
    private lateinit var prefsManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefsManager = PreferencesManager(this)

        loadAndDisplayStats()
        setupClickListeners()
    }

    private fun loadAndDisplayStats() {
        // آمار کلی روزانه (قدیمی)
        displayDailyRatingsStats()

        // آمار آلارم صبحگاهی
        displayAlarmStats()

        // آمار جدید Task Management
        displayTaskStats()

        // آمار بخش‌ها
        displayCategoryStats()

        // تحلیل روند
        analyzeAndDisplayTrend()
    }

    private fun displayDailyRatingsStats() {
        val allRatings = prefsManager.getAllRatings()

        if (allRatings.isEmpty()) {
            binding.averageRatingText.text = getString(R.string.no_rating_recorded)
            binding.totalDaysText.text = "0 روز"
            binding.lastWeekAverageText.text = getString(R.string.no_data_available)
        } else {
            // محاسبه میانگین کل
            val averageRating = allRatings.values.average()
            binding.averageRatingText.text = String.format("%.1f از 10", averageRating)

            // تعداد روزهای ثبت شده
            binding.totalDaysText.text = "${allRatings.size} روز"

            // میانگین هفته اخیر
            val lastWeekAverage = calculateLastWeekAverage(allRatings)
            binding.lastWeekAverageText.text = if (lastWeekAverage > 0) {
                String.format("%.1f از 10", lastWeekAverage)
            } else {
                getString(R.string.insufficient_data)
            }
        }
    }

    private fun displayAlarmStats() {
        // تعداد دفعات موفقیت در آلارم صبح
        val successCount = prefsManager.getSuccessCount()
        binding.successCountText.text = "$successCount بار"
    }

    private fun displayTaskStats() {
        val taskStats = prefsManager.getTasksStats()

        // اضافه کردن آمار کارها به صفحه
        // این نیاز به تغییر در layout دارد، فعلاً در کامنت گذاشتم
        /*
        binding.totalTasksText.text = "${taskStats["totalTasks"]} کار کل"
        binding.todayTasksText.text = "${taskStats["todayTasks"]} کار امروز"
        binding.completedTodayText.text = "${taskStats["completedToday"]} انجام شده"
        binding.todayProgressText.text = "${taskStats["todayProgress"]}% پیشرفت"
        */
    }

    private fun displayCategoryStats() {
        val categoriesWithStats = prefsManager.getCategoriesWithStats()

        if (categoriesWithStats.isNotEmpty()) {
            val categoryStatsText = categoriesWithStats.joinToString("\n") { (category, stats) ->
                "${category.name}: ${stats["completed"]}/${stats["total"]} (${stats["progress"]}%)"
            }

            // نمایش در بخش تحلیل روند (موقتی)
            // در آینده باید بخش جداگانه‌ای برای آمار بخش‌ها اضافه شود
        }
    }

    private fun calculateLastWeekAverage(allRatings: Map<String, Int>): Double {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        val weekAgo = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

        val lastWeekRatings = allRatings.filter { it.key >= weekAgo }

        return if (lastWeekRatings.isNotEmpty()) {
            lastWeekRatings.values.average()
        } else {
            0.0
        }
    }

    private fun displayRecentRatings() {
        val allRatings = prefsManager.getAllRatings()

        // آخرین 30 امتیاز را نمایش می‌دهیم
        val sortedRatings = allRatings.toList()
            .sortedByDescending { it.first }
            .take(30)

        val displayList = sortedRatings.map { (date, rating) ->
            val formattedDate = formatDate(date)
            "$formattedDate: $rating از 10 ${getEmoji(rating)}"
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, displayList)
        binding.ratingsListView.adapter = adapter
    }

    private fun formatDate(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("MM/dd", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            outputFormat.format(date!!)
        } catch (e: Exception) {
            dateString
        }
    }

    private fun getEmoji(rating: Int): String {
        return when (rating) {
            in 1..3 -> "😞"
            in 4..5 -> "😐"
            in 6..7 -> "🙂"
            in 8..9 -> "😊"
            10 -> "😄"
            else -> ""
        }
    }

    private fun analyzeAndDisplayTrend() {
        val allRatings = prefsManager.getAllRatings()

        if (allRatings.size < 3) {
            binding.trendText.text = getString(R.string.insufficient_trend_data)
            return
        }

        val sortedRatings = allRatings.toList().sortedBy { it.first }
        val recentRatings = sortedRatings.takeLast(7)
        val olderRatings = sortedRatings.dropLast(7).takeLast(7)

        if (olderRatings.isEmpty()) {
            // اضافه کردن تحلیل Task Progress
            val taskStats = prefsManager.getTasksStats()
            val todayProgress = taskStats["todayProgress"] as Int

            val taskTrend = when {
                todayProgress >= 80 -> "📈 عملکرد عالی در کارهای امروز!"
                todayProgress >= 60 -> "👍 پیشرفت خوب در کارهای امروز"
                todayProgress >= 40 -> "⚠️ نیاز به تلاش بیشتر برای کارهای امروز"
                else -> "🔴 بیشتر کارهای امروز انجام نشده"
            }

            binding.trendText.text = taskTrend
            return
        }

        val recentAverage = recentRatings.map { it.second }.average()
        val olderAverage = olderRatings.map { it.second }.average()

        val trend = when {
            recentAverage > olderAverage + 0.5 -> "📈 روند رو به بهبود! ادامه بده!"
            recentAverage < olderAverage - 0.5 -> "📉 روند نزولی. شاید نیاز به تغییر روش داری"
            else -> "➡️ روند ثابت. سعی کن چالش‌های جدیدی امتحان کنی"
        }

        // اضافه کردن آمار کارها به تحلیل
        val taskStats = prefsManager.getTasksStats()
        val todayProgress = taskStats["todayProgress"] as Int
        val progressText = "\n📊 پیشرفت امروز: $todayProgress%"

        binding.trendText.text = trend + progressText
    }

    private fun setupClickListeners() {
        binding.backButton.setOnClickListener {
            finish()
        }

        binding.clearDataButton.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.clear_data_title))
                .setMessage(getString(R.string.clear_data_message))
                .setPositiveButton(getString(R.string.all_data_option)) { _, _ ->
                    clearAllData()
                }
                .setNeutralButton(getString(R.string.tasks_only_option)) { _, _ ->
                    clearTaskData()
                }
                .setNegativeButton(getString(R.string.cancel_option), null)
                .show()
        }
    }

    private fun clearAllData() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.final_confirmation_title))
            .setMessage(getString(R.string.final_confirmation_message))
            .setPositiveButton(getString(R.string.yes_delete_button)) { _, _ ->
                prefsManager.clearAllData()
                loadAndDisplayStats()
                displayRecentRatings()
            }
            .setNegativeButton(getString(R.string.no_button), null)
            .show()
    }

    private fun clearTaskData() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.clear_tasks_title))
            .setMessage(getString(R.string.clear_tasks_message))
            .setPositiveButton(getString(R.string.yes_delete_button)) { _, _ ->
                prefsManager.clearTaskData()
                loadAndDisplayStats()
            }
            .setNegativeButton(getString(R.string.no_button), null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        // به‌روزرسانی آمار هنگام بازگشت به صفحه
        loadAndDisplayStats()
        displayRecentRatings()
    }
}