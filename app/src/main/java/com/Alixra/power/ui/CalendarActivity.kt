package com.Alixra.power.ui

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.Alixra.power.R
import com.Alixra.power.data.PreferencesManager
import com.Alixra.power.data.Task
import com.Alixra.power.data.TaskCategory
import com.Alixra.power.data.TaskPriority
import com.Alixra.power.data.TimePeriod
import com.Alixra.power.ui.adapters.CalendarAdapter
import java.text.SimpleDateFormat
import java.util.*

class CalendarActivity : BaseActivity() {

    // Views
    private lateinit var backButton: Button
    private lateinit var titleTextView: TextView
    private lateinit var calendarRecyclerView: RecyclerView
    private lateinit var emptyCalendarLayout: LinearLayout

    // Data
    private lateinit var prefsManager: PreferencesManager
    private lateinit var calendarAdapter: CalendarAdapter
    private var timePeriod: TimePeriod = TimePeriod.THIS_WEEK
    private var calendarDays: MutableList<CalendarDay> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar)

        // دریافت نوع بازه زمانی از Intent
        timePeriod = intent.getSerializableExtra("time_period") as? TimePeriod ?: TimePeriod.THIS_WEEK

        initViews()
        prefsManager = PreferencesManager(this)
        setupRecyclerView()
        setupClickListeners()
        generateCalendarDays()
        updateTitle()
    }

    private fun initViews() {
        backButton = findViewById(R.id.backButton)
        titleTextView = findViewById(R.id.titleTextView)
        calendarRecyclerView = findViewById(R.id.calendarRecyclerView)
        emptyCalendarLayout = findViewById(R.id.emptyCalendarLayout)
    }

    private fun setupRecyclerView() {
        calendarAdapter = CalendarAdapter(
            onDayClick = { calendarDay ->
                if (calendarDay.isClickable) {
                    showDayTasksDialog(calendarDay)
                }
            },
            onTaskClick = { task ->
                showTaskDetailsDialog(task)
            },
            getTasksForDate = { date ->
                getTasksForDate(date)
            }
        )

        calendarRecyclerView.layoutManager = GridLayoutManager(this, 7) // 7 روز در هفته
        calendarRecyclerView.adapter = calendarAdapter
    }

    private fun setupClickListeners() {
        backButton.setOnClickListener {
            onBackPressed()
        }
    }

    private fun updateTitle() {
        val title = when (timePeriod) {
            TimePeriod.THIS_WEEK -> "تقویم این هفته"
            TimePeriod.THIS_MONTH -> "تقویم این ماه"
            TimePeriod.THIS_SEASON -> "تقویم این فصل"
            TimePeriod.THIS_YEAR -> "تقویم امسال"
            else -> "تقویم"
        }
        titleTextView.text = title
    }

    private fun generateCalendarDays() {
        calendarDays.clear()

        when (timePeriod) {
            TimePeriod.THIS_WEEK -> generateWeekDays()
            TimePeriod.THIS_MONTH -> generateMonthDays()
            TimePeriod.THIS_SEASON -> generateSeasonDays()
            TimePeriod.THIS_YEAR -> generateYearDays()
            else -> generateWeekDays()
        }

        calendarAdapter.updateDays(calendarDays)
    }

    private fun generateWeekDays() {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)

        for (i in 0..6) {
            val dayCalendar = calendar.clone() as Calendar
            dayCalendar.add(Calendar.DAY_OF_YEAR, i)
            
            val day = CalendarDay(
                date = dayCalendar.timeInMillis,
                dayNumber = dayCalendar.get(Calendar.DAY_OF_MONTH),
                isToday = isToday(dayCalendar),
                isClickable = true,
                tasksCount = getTasksCountForDate(dayCalendar.timeInMillis)
            )
            calendarDays.add(day)
        }
    }

    private fun generateMonthDays() {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        // شروع از اول ماه
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        // اضافه کردن روزهای خالی برای شروع هفته
        val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val startOffset = (firstDayOfWeek - calendar.firstDayOfWeek + 7) % 7
        
        for (i in 0 until startOffset) {
            val emptyDay = CalendarDay(
                date = 0,
                dayNumber = 0,
                isToday = false,
                isClickable = false,
                tasksCount = 0
            )
            calendarDays.add(emptyDay)
        }

        // اضافه کردن روزهای ماه
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        for (day in 1..daysInMonth) {
            calendar.set(Calendar.DAY_OF_MONTH, day)
            
            val dayObj = CalendarDay(
                date = calendar.timeInMillis,
                dayNumber = day,
                isToday = isToday(calendar),
                isClickable = true,
                tasksCount = getTasksCountForDate(calendar.timeInMillis)
            )
            calendarDays.add(dayObj)
        }
    }

    private fun generateSeasonDays() {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        // تعیین ماه‌های فصل پیش رو
        val seasonMonths = when {
            currentMonth in 2..4 -> listOf(2, 3, 4) // بهار: مارس، آوریل، مه
            currentMonth in 5..7 -> listOf(5, 6, 7) // تابستان: ژوئن، جولای، آگوست
            currentMonth in 8..10 -> listOf(8, 9, 10) // پاییز: سپتامبر، اکتبر، نوامبر
            else -> listOf(11, 0, 1) // زمستان: دسامبر، ژانویه، فوریه
        }

        // اضافه کردن هدر ماه‌ها
        val monthNames = arrayOf("ژانویه", "فوریه", "مارس", "آوریل", "مه", "ژوئن", 
                                "جولای", "آگوست", "سپتامبر", "اکتبر", "نوامبر", "دسامبر")

        for (month in seasonMonths) {
            // اضافه کردن هدر ماه
            val monthHeader = CalendarDay(
                date = 0,
                dayNumber = 0,
                isToday = false,
                isClickable = false,
                tasksCount = 0
            )
            calendarDays.add(monthHeader)

            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            
            val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            for (day in 1..daysInMonth) {
                calendar.set(Calendar.DAY_OF_MONTH, day)
                
                val dayObj = CalendarDay(
                    date = calendar.timeInMillis,
                    dayNumber = day,
                    isToday = isToday(calendar),
                    isClickable = true,
                    tasksCount = getTasksCountForDate(calendar.timeInMillis)
                )
                calendarDays.add(dayObj)
            }
        }
    }

    private fun generateYearDays() {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)

        // نمایش ماه‌های سال
        for (month in 0..11) {
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            
            val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            for (day in 1..daysInMonth) {
                calendar.set(Calendar.DAY_OF_MONTH, day)
                
                val dayObj = CalendarDay(
                    date = calendar.timeInMillis,
                    dayNumber = day,
                    isToday = isToday(calendar),
                    isClickable = true,
                    tasksCount = getTasksCountForDate(calendar.timeInMillis)
                )
                calendarDays.add(dayObj)
            }
        }
    }

    private fun isToday(calendar: Calendar): Boolean {
        val today = Calendar.getInstance()
        return calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
    }

    private fun getTasksCountForDate(date: Long): Int {
        val allTasks = prefsManager.getAllTasks()
        return allTasks.count { task ->
            val taskDate = Calendar.getInstance().apply { timeInMillis = task.createdAt }
            val targetDate = Calendar.getInstance().apply { timeInMillis = date }
            
            taskDate.get(Calendar.YEAR) == targetDate.get(Calendar.YEAR) &&
            taskDate.get(Calendar.DAY_OF_YEAR) == targetDate.get(Calendar.DAY_OF_YEAR)
        }
    }

    private fun showDayTasksDialog(calendarDay: CalendarDay) {
        val date = Date(calendarDay.date)
        val formatter = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault())
        val dateString = formatter.format(date)

        val tasksForDay = getTasksForDate(calendarDay.date)
        
        if (tasksForDay.isEmpty()) {
            // نمایش دیالوگ افزودن کار جدید
            showAddTaskDialog(calendarDay.date)
        } else {
            // نمایش لیست کارهای روز
            showTasksListDialog(dateString, tasksForDay, calendarDay.date)
        }
    }

    private fun getTasksForDate(date: Long): List<Task> {
        val allTasks = prefsManager.getAllTasks()
        return allTasks.filter { task ->
            val taskDate = Calendar.getInstance().apply { timeInMillis = task.createdAt }
            val targetDate = Calendar.getInstance().apply { timeInMillis = date }
            
            taskDate.get(Calendar.YEAR) == targetDate.get(Calendar.YEAR) &&
            taskDate.get(Calendar.DAY_OF_YEAR) == targetDate.get(Calendar.DAY_OF_YEAR)
        }
    }

    private fun showAddTaskDialog(date: Long) {
        val editText = EditText(this)
        editText.hint = "عنوان کار"

        val dateFormatter = SimpleDateFormat("d MMMM", Locale.getDefault())
        val dateString = dateFormatter.format(Date(date))

        AlertDialog.Builder(this)
            .setTitle("افزودن کار برای $dateString")
            .setView(editText)
            .setPositiveButton("افزودن") { _, _ ->
                val title = editText.text.toString().trim()

                if (title.isNotEmpty()) {
                    showGoalSelectionDialog(title, date)
                } else {
                    showToast("لطفاً عنوان کار را وارد کنید!")
                }
            }
            .setNegativeButton("لغو", null)
            .show()
    }

    private fun showGoalSelectionDialog(title: String, date: Long) {
        val categories = prefsManager.getTaskCategories()
        val categoryNames = categories.map { it.name }.toTypedArray()
        
        // اضافه کردن گزینه "بدون هدف"
        val options = arrayOf("بدون هدف") + categoryNames

        AlertDialog.Builder(this)
            .setTitle("انتخاب هدف (اختیاری)")
            .setItems(options) { _, which ->
                val selectedCategory = if (which == 0) {
                    // بدون هدف - ایجاد بخش عمومی
                    val defaultCategory = TaskCategory("general", "📝 عمومی", "#607D8B")
                    if (categories.none { it.id == "general" }) {
                        prefsManager.saveTaskCategory(defaultCategory)
                    }
                    defaultCategory
                } else {
                    categories[which - 1]
                }

                val newTask = Task(
                    id = "task_${System.currentTimeMillis()}",
                    title = title,
                    description = "",
                    categoryId = selectedCategory.id,
                    timePeriod = timePeriod,
                    priority = TaskPriority.NORMAL,
                    createdAt = date
                )

                prefsManager.saveTask(newTask)
                generateCalendarDays()
                showToast("کار جدید اضافه شد!")
            }
            .setNegativeButton("لغو", null)
            .show()
    }

    private fun showTasksListDialog(dateString: String, tasks: List<Task>, date: Long) {
        val taskTitles = tasks.map { task ->
            val status = if (task.isCompleted) "✅" else "⏳"
            "$status ${task.title}"
        }

        AlertDialog.Builder(this)
            .setTitle("کارهای $dateString")
            .setItems(taskTitles.toTypedArray()) { _, which ->
                val selectedTask = tasks[which]
                showTaskDetailsDialog(selectedTask)
            }
            .setPositiveButton("افزودن کار جدید") { _, _ ->
                showAddTaskDialog(date)
            }
            .setNegativeButton("بستن", null)
            .show()
    }

    private fun showTaskDetailsDialog(task: Task) {
        val message = buildString {
            appendLine("📝 ${task.title}")
            if (task.description.isNotEmpty()) {
                appendLine("📋 ${task.description}")
            }
            appendLine("📂 بخش: ${getCategoryName(task.categoryId)}")
            appendLine("⏰ اولویت: ${getPriorityName(task.priority)}")
            appendLine("📅 تاریخ: ${formatDate(task.createdAt)}")
            if (task.isCompleted) {
                appendLine("✅ انجام شده در: ${formatDate(task.completionDate ?: 0)}")
            }
        }

        AlertDialog.Builder(this)
            .setTitle("جزئیات کار")
            .setMessage(message)
            .setPositiveButton("باشه", null)
            .show()
    }

    private fun getCategoryName(categoryId: String): String {
        val categories = prefsManager.getTaskCategories()
        return categories.find { it.id == categoryId }?.name ?: "نامشخص"
    }

    private fun getPriorityName(priority: TaskPriority): String {
        return when (priority) {
            TaskPriority.NORMAL -> "عادی"
            TaskPriority.HIGH -> "مهم"
            TaskPriority.URGENT -> "خیلی مهم"
        }
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // کلاس داده برای روزهای تقویم
    data class CalendarDay(
        val date: Long,
        val dayNumber: Int,
        val isToday: Boolean,
        val isClickable: Boolean,
        val tasksCount: Int
    )
}
