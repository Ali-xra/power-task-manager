package com.Alixra.power.ui

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.Alixra.power.R
import com.Alixra.power.data.PreferencesManager
import com.Alixra.power.data.Task
import com.Alixra.power.data.TaskCategory
import com.Alixra.power.data.TimePeriod
import com.Alixra.power.data.TaskPriority
import com.Alixra.power.ui.adapters.TasksAdapter
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.*

class TasksActivity : BaseActivity() {

    companion object {
        const val EXTRA_TARGET_PERIOD = "target_period"
        const val EXTRA_FROM_MAIN_PAGE = "from_main_page"
        const val EXTRA_SHOW_SHORTCUTS = "show_shortcuts"
    }

    // Views
    private lateinit var backButton: Button
    private lateinit var timePeriodsLayout: LinearLayout
    private lateinit var tasksLayout: LinearLayout
    private lateinit var addTaskButton: Button
    private lateinit var tasksRecyclerView: RecyclerView
    private lateinit var emptyTasksLayout: LinearLayout
    private lateinit var currentPeriodTitle: TextView

    // Period Cards
    private lateinit var todayCard: MaterialCardView
    private lateinit var thisWeekCard: MaterialCardView
    private lateinit var thisMonthCard: MaterialCardView
    private lateinit var thisSeasonCard: MaterialCardView
    private lateinit var thisYearCard: MaterialCardView

    // Count TextViews
    private lateinit var todayTasksCount: TextView
    private lateinit var thisWeekTasksCount: TextView
    private lateinit var thisMonthTasksCount: TextView
    private lateinit var thisSeasonTasksCount: TextView
    private lateinit var thisYearTasksCount: TextView

    private lateinit var todayCompletedCount: TextView
    private lateinit var thisWeekCompletedCount: TextView
    private lateinit var thisMonthCompletedCount: TextView
    private lateinit var thisSeasonCompletedCount: TextView
    private lateinit var thisYearCompletedCount: TextView

    private lateinit var prefsManager: PreferencesManager
    private lateinit var tasksAdapter: TasksAdapter

    private var currentPeriod: TimePeriod = TimePeriod.TODAY
    private var currentTasks: MutableList<Task> = mutableListOf()

    // Navigation Stack
    private enum class ViewState { TIME_PERIODS, TASKS }
    private var currentViewState = ViewState.TIME_PERIODS

    // Track navigation source to handle back button correctly
    private var cameFromMainPage = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tasks)

        initViews()
        prefsManager = PreferencesManager(this)
        setupRecyclerView()
        setupClickListeners()

        // Check if we came from main page with specific period
        val targetPeriod = intent.getSerializableExtra(EXTRA_TARGET_PERIOD) as? TimePeriod
        cameFromMainPage = intent.getBooleanExtra(EXTRA_FROM_MAIN_PAGE, false)
        val showShortcuts = intent.getBooleanExtra(EXTRA_SHOW_SHORTCUTS, false)

        when {
            targetPeriod != null && cameFromMainPage -> {
                // Direct navigation to specific period from main page
                currentPeriod = targetPeriod
                showTasksView()
            }
            showShortcuts -> {
                // Show main page shortcuts (Today and This Year only)
                showTimePeriodsView(shortcutsOnly = true)
            }
            else -> {
                // Default: show all time periods selection
                showTimePeriodsView()
            }
        }
    }

    private fun initViews() {
        backButton = findViewById(R.id.backButton)
        timePeriodsLayout = findViewById(R.id.timePeriodsLayout)
        tasksLayout = findViewById(R.id.tasksLayout)
        addTaskButton = findViewById(R.id.addTaskButton)
        tasksRecyclerView = findViewById(R.id.tasksRecyclerView)
        emptyTasksLayout = findViewById(R.id.emptyTasksLayout)
        currentPeriodTitle = findViewById(R.id.currentPeriodTitle)

        // Period Cards
        todayCard = findViewById(R.id.todayCard)
        thisWeekCard = findViewById(R.id.thisWeekCard)
        thisMonthCard = findViewById(R.id.thisMonthCard)
        thisSeasonCard = findViewById(R.id.thisSeasonCard)
        thisYearCard = findViewById(R.id.thisYearCard)

        // Count TextViews
        todayTasksCount = findViewById(R.id.todayTasksCount)
        thisWeekTasksCount = findViewById(R.id.thisWeekTasksCount)
        thisMonthTasksCount = findViewById(R.id.thisMonthTasksCount)
        thisSeasonTasksCount = findViewById(R.id.thisSeasonTasksCount)
        thisYearTasksCount = findViewById(R.id.thisYearTasksCount)

        todayCompletedCount = findViewById(R.id.todayCompletedCount)
        thisWeekCompletedCount = findViewById(R.id.thisWeekCompletedCount)
        thisMonthCompletedCount = findViewById(R.id.thisMonthCompletedCount)
        thisSeasonCompletedCount = findViewById(R.id.thisSeasonCompletedCount)
        thisYearCompletedCount = findViewById(R.id.thisYearCompletedCount)
    }

    private fun setupRecyclerView() {
        tasksAdapter = TasksAdapter(prefsManager) { task, isCompleted ->
            toggleTaskCompletion(task, isCompleted)
        }

        tasksRecyclerView.layoutManager = LinearLayoutManager(this)
        tasksRecyclerView.adapter = tasksAdapter
    }

    private fun setupClickListeners() {
        // دکمه بازگشت/ناوبری
        backButton.setOnClickListener {
            if (currentViewState == ViewState.TASKS) {
                if (cameFromMainPage) {
                    // If we came directly from main page, go back to main page
                    finish()
                } else {
                    // Otherwise, go back to time periods selection
                    showTimePeriodsView()
                }
            } else {
                finish()
            }
        }

        // کارت‌های بازه زمانی
        todayCard.setOnClickListener {
            currentPeriod = TimePeriod.TODAY
            // If we're showing shortcuts only, mark as coming from main page
            if (thisWeekCard.visibility == View.GONE) {
                cameFromMainPage = true
            }
            showTasksView()
        }

        thisWeekCard.setOnClickListener {
            openCalendar(TimePeriod.THIS_WEEK)
        }

        thisMonthCard.setOnClickListener {
            openCalendar(TimePeriod.THIS_MONTH)
        }

        thisSeasonCard.setOnClickListener {
            openCalendar(TimePeriod.THIS_SEASON)
        }

        thisYearCard.setOnClickListener {
            currentPeriod = TimePeriod.THIS_YEAR
            // If we're showing shortcuts only, mark as coming from main page
            if (thisWeekCard.visibility == View.GONE) {
                cameFromMainPage = true
            }
            showTasksView()
        }

        // دکمه اضافه کردن کار
        addTaskButton.setOnClickListener {
            showAddTaskDialog()
        }
    }

    private fun showTimePeriodsView(shortcutsOnly: Boolean = false) {
        currentViewState = ViewState.TIME_PERIODS
        timePeriodsLayout.visibility = View.VISIBLE
        tasksLayout.visibility = View.GONE

        if (shortcutsOnly) {
            // Show only Today and This Year cards from main page
            todayCard.visibility = View.VISIBLE
            thisYearCard.visibility = View.VISIBLE
            thisWeekCard.visibility = View.GONE
            thisMonthCard.visibility = View.GONE
            thisSeasonCard.visibility = View.GONE
        } else {
            // Show all period cards
            todayCard.visibility = View.VISIBLE
            thisYearCard.visibility = View.VISIBLE
            thisWeekCard.visibility = View.VISIBLE
            thisMonthCard.visibility = View.VISIBLE
            thisSeasonCard.visibility = View.VISIBLE
        }

        updateTaskCounts()
    }

    private fun showTasksView() {
        currentViewState = ViewState.TASKS
        timePeriodsLayout.visibility = View.GONE
        tasksLayout.visibility = View.VISIBLE

        val periodName = when (currentPeriod) {
            TimePeriod.TODAY -> getString(R.string.today_tasks_title)
            TimePeriod.THIS_WEEK -> getString(R.string.this_week_tasks_title)
            TimePeriod.THIS_MONTH -> getString(R.string.this_month_tasks_title)
            TimePeriod.THIS_SEASON -> getString(R.string.this_season_tasks_title)
            TimePeriod.THIS_YEAR -> getString(R.string.this_year_tasks_title)
        }

        currentPeriodTitle.text = getString(R.string.tasks_for_period_title, periodName)
        loadTasksList()
    }

    private fun updateTaskCounts() {
        // به‌روزرسانی تعداد کارها برای هر بازه زمانی
        val todayTasks = getTasksForTimePeriod(TimePeriod.TODAY)
        val weekTasks = getTasksForTimePeriod(TimePeriod.THIS_WEEK)
        val monthTasks = getTasksForTimePeriod(TimePeriod.THIS_MONTH)
        val seasonTasks = getTasksForTimePeriod(TimePeriod.THIS_SEASON)
        val yearTasks = getTasksForTimePeriod(TimePeriod.THIS_YEAR)

        todayTasksCount.text = getString(R.string.task_count_dynamic, todayTasks.size)
        thisWeekTasksCount.text = getString(R.string.task_count_dynamic, weekTasks.size)
        thisMonthTasksCount.text = getString(R.string.task_count_dynamic, monthTasks.size)
        thisSeasonTasksCount.text = getString(R.string.task_count_dynamic, seasonTasks.size)
        thisYearTasksCount.text = getString(R.string.task_count_dynamic, yearTasks.size)

        // نمایش تعداد کارهای انجام شده
        val todayCompleted = todayTasks.count { it.isCompleted }
        val weekCompleted = weekTasks.count { it.isCompleted }
        val monthCompleted = monthTasks.count { it.isCompleted }
        val seasonCompleted = seasonTasks.count { it.isCompleted }
        val yearCompleted = yearTasks.count { it.isCompleted }

        todayCompletedCount.text = getString(R.string.completed_format_dynamic, todayCompleted)
        thisWeekCompletedCount.text = getString(R.string.completed_format_dynamic, weekCompleted)
        thisMonthCompletedCount.text = getString(R.string.completed_format_dynamic, monthCompleted)
        thisSeasonCompletedCount.text = getString(R.string.completed_format_dynamic, seasonCompleted)
        thisYearCompletedCount.text = getString(R.string.completed_format_dynamic, yearCompleted)
    }

    private fun loadTasksList() {
        currentTasks = getTasksForTimePeriod(currentPeriod).toMutableList()
        tasksAdapter.updateTasks(currentTasks)

        // نمایش پیام خالی بودن لیست
        if (currentTasks.isEmpty()) {
            emptyTasksLayout.visibility = View.VISIBLE
            tasksRecyclerView.visibility = View.GONE
        } else {
            emptyTasksLayout.visibility = View.GONE
            tasksRecyclerView.visibility = View.VISIBLE
        }
    }

    private fun getTasksForTimePeriod(timePeriod: TimePeriod): List<Task> {
        return prefsManager.getAllTasks().filter { task ->
            when (timePeriod) {
                TimePeriod.TODAY -> {
                    // برای امروز، کارهایی که در تاریخ امروز ایجاد شده‌اند
                    val today = Calendar.getInstance()
                    val taskDate = Calendar.getInstance().apply { timeInMillis = task.createdAt }
                    today.get(Calendar.YEAR) == taskDate.get(Calendar.YEAR) &&
                    today.get(Calendar.DAY_OF_YEAR) == taskDate.get(Calendar.DAY_OF_YEAR)
                }
                else -> {
                    // برای سایر بازه‌ها، کارهایی که در آن بازه ایجاد شده‌اند
                    timePeriod.isDateInPeriod(task.createdAt)
                }
            }
        }
    }

    private fun showAddTaskDialog() {
        val editText = EditText(this)
        editText.hint = getString(R.string.task_title_hint)

        val dialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.add_new_task_title))
            .setView(editText)
            .setPositiveButton(getString(R.string.add_action), null) // null to override later
            .setNegativeButton("لغو", null)
            .create()

        dialog.show()

        // Override positive button to prevent automatic closing
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val title = editText.text.toString().trim()

            when {
                title.isEmpty() -> {
                    showToast(getString(R.string.enter_task_title_message))
                }
                title.length > 100 -> {
                    showToast("عنوان کار نباید بیش از 100 کاراکتر باشد")
                }
                title.contains("\n") || title.contains("\r") -> {
                    showToast("عنوان کار نمی‌تواند شامل خط جدید باشد")
                }
                else -> {
                    showGoalSelectionDialog(title, editText, dialog)
                }
            }
        }
    }

    private fun showGoalSelectionDialog(title: String, editText: EditText, parentDialog: AlertDialog) {
        val categories = prefsManager.getTaskCategories()
        val categoryNames = categories.map { it.name }.toTypedArray()

        // اضافه کردن گزینه "بدون هدف"
        val options = arrayOf(getString(R.string.no_goal_option)) + categoryNames

        val goalDialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.select_goal_optional_title))
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

                val newTask = Task.Builder()
                    .id("task_${System.currentTimeMillis()}")
                    .title(title)
                    .categoryId(selectedCategory.id)
                    .timePeriod(currentPeriod)
                    .priority(TaskPriority.NORMAL)
                    .build()

                prefsManager.saveTask(newTask)
                loadTasksList()
                updateTaskCounts()
                showToast(getString(R.string.new_task_added_message))

                // Clear the input field and keep the parent dialog open
                editText.text.clear()
                editText.requestFocus()
            }
            .setNegativeButton("لغو", null)
            .create()

        goalDialog.show()
    }

    private fun toggleTaskCompletion(task: Task, isCompleted: Boolean) {
        try {
            // جلوگیری از تغییر تکراری وضعیت
            if (task.isCompleted == isCompleted) {
                return // هیچ تغییری لازم نیست
            }

            val updatedTask = if (isCompleted) {
                task.copy(isCompleted = true, completionDate = System.currentTimeMillis())
            } else {
                task.copy(isCompleted = false, completionDate = null)
            }

            // ذخیره کار به‌روزرسانی شده
            prefsManager.saveTask(updatedTask)

            // به‌روزرسانی لیست کارها
            val updatedPosition = currentTasks.indexOfFirst { it.id == task.id }
            if (updatedPosition != -1) {
                currentTasks[updatedPosition] = updatedTask
                tasksAdapter.updateTasks(currentTasks)
            } else {
                // اگر کار پیدا نشد، کل لیست را بازیابی کن
                loadTasksList()
            }

            // به‌روزرسانی شمارنده‌ها
            updateTaskCounts()

            val message = if (isCompleted) getString(R.string.task_completed_message) else getString(R.string.task_uncompleted_message)
            showToast(message)

        } catch (e: Exception) {
            e.printStackTrace()
            showToast("خطا در به‌روزرسانی وضعیت کار")
            // بازیابی کل لیست در صورت خطا
            loadTasksList()
        }
    }

    private fun getPriorityName(priority: TaskPriority): String {
        return when (priority) {
            TaskPriority.NORMAL -> getString(R.string.priority_normal)
            TaskPriority.HIGH -> getString(R.string.priority_high)
            TaskPriority.URGENT -> getString(R.string.priority_urgent)
        }
    }

    private fun formatDate(timestamp: Long): String {
        return try {
            val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
            sdf.format(Date(timestamp))
        } catch (e: Exception) {
            "تاریخ نامعلوم"
        }
    }

    private fun openCalendar(timePeriod: TimePeriod) {
        val intent = Intent(this, CalendarActivity::class.java)
        intent.putExtra("time_period", timePeriod)
        startActivity(intent)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}