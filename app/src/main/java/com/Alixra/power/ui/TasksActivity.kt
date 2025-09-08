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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tasks)

        initViews()
        prefsManager = PreferencesManager(this)
        setupRecyclerView()
        setupClickListeners()
        showTimePeriodsView()
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
                showTimePeriodsView()
            } else {
                finish()
            }
        }

        // کارت‌های بازه زمانی
        todayCard.setOnClickListener {
            currentPeriod = TimePeriod.TODAY
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
            showTasksView()
        }

        // دکمه اضافه کردن کار
        addTaskButton.setOnClickListener {
            showAddTaskDialog()
        }
    }

    private fun showTimePeriodsView() {
        currentViewState = ViewState.TIME_PERIODS
        timePeriodsLayout.visibility = View.VISIBLE
        tasksLayout.visibility = View.GONE

        updateTaskCounts()
    }

    private fun showTasksView() {
        currentViewState = ViewState.TASKS
        timePeriodsLayout.visibility = View.GONE
        tasksLayout.visibility = View.VISIBLE

        val periodName = when (currentPeriod) {
            TimePeriod.TODAY -> "امروز"
            TimePeriod.THIS_WEEK -> "این هفته"
            TimePeriod.THIS_MONTH -> "این ماه"
            TimePeriod.THIS_SEASON -> "این فصل"
            TimePeriod.THIS_YEAR -> "امسال"
        }

        currentPeriodTitle.text = "کارهای $periodName"
        loadTasksList()
    }

    private fun updateTaskCounts() {
        // به‌روزرسانی تعداد کارها برای هر بازه زمانی
        val todayTasks = getTasksForTimePeriod(TimePeriod.TODAY)
        val weekTasks = getTasksForTimePeriod(TimePeriod.THIS_WEEK)
        val monthTasks = getTasksForTimePeriod(TimePeriod.THIS_MONTH)
        val seasonTasks = getTasksForTimePeriod(TimePeriod.THIS_SEASON)
        val yearTasks = getTasksForTimePeriod(TimePeriod.THIS_YEAR)

        todayTasksCount.text = "${todayTasks.size} کار"
        thisWeekTasksCount.text = "${weekTasks.size} کار"
        thisMonthTasksCount.text = "${monthTasks.size} کار"
        thisSeasonTasksCount.text = "${seasonTasks.size} کار"
        thisYearTasksCount.text = "${yearTasks.size} کار"

        // نمایش تعداد کارهای انجام شده
        val todayCompleted = todayTasks.count { it.isCompleted }
        val weekCompleted = weekTasks.count { it.isCompleted }
        val monthCompleted = monthTasks.count { it.isCompleted }
        val seasonCompleted = seasonTasks.count { it.isCompleted }
        val yearCompleted = yearTasks.count { it.isCompleted }

        todayCompletedCount.text = "($todayCompleted انجام شده)"
        thisWeekCompletedCount.text = "($weekCompleted انجام شده)"
        thisMonthCompletedCount.text = "($monthCompleted انجام شده)"
        thisSeasonCompletedCount.text = "($seasonCompleted انجام شده)"
        thisYearCompletedCount.text = "($yearCompleted انجام شده)"
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
        editText.hint = "عنوان کار"

        AlertDialog.Builder(this)
            .setTitle("افزودن کار جدید")
            .setView(editText)
            .setPositiveButton("افزودن") { _, _ ->
                val title = editText.text.toString().trim()

                if (title.isNotEmpty()) {
                    showGoalSelectionDialog(title)
                } else {
                    showToast("لطفاً عنوان کار را وارد کنید!")
                }
            }
            .setNegativeButton("لغو", null)
            .show()
    }

    private fun showGoalSelectionDialog(title: String) {
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
                    timePeriod = currentPeriod,
                    priority = TaskPriority.NORMAL
                )

                prefsManager.saveTask(newTask)
                loadTasksList()
                updateTaskCounts()
                showToast("کار جدید اضافه شد!")
            }
            .setNegativeButton("لغو", null)
            .show()
    }

    private fun toggleTaskCompletion(task: Task, isCompleted: Boolean) {
        val updatedTask = if (isCompleted) {
            task.copy(isCompleted = true, completionDate = System.currentTimeMillis())
        } else {
            task.copy(isCompleted = false, completionDate = null)
        }

        prefsManager.saveTask(updatedTask)
        loadTasksList()
        updateTaskCounts()

        val message = if (isCompleted) "کار انجام شد! 🎉" else "کار به حالت در انتظار برگشت"
        showToast(message)
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

    private fun openCalendar(timePeriod: TimePeriod) {
        val intent = Intent(this, CalendarActivity::class.java)
        intent.putExtra("time_period", timePeriod)
        startActivity(intent)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}