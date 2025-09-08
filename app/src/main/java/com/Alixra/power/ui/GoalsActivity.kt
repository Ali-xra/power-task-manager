package com.Alixra.power.ui

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.Alixra.power.R
import com.Alixra.power.data.PreferencesManager
import com.Alixra.power.data.Task
import com.Alixra.power.data.TaskCategory
import com.Alixra.power.data.TimePeriod
import com.Alixra.power.ui.adapters.CategoriesAdapter
import com.google.android.material.floatingactionbutton.FloatingActionButton

class GoalsActivity : AppCompatActivity() {

    private lateinit var backButton: Button
    private lateinit var addGoalButton: Button
    private lateinit var fabAddGoal: FloatingActionButton
    private lateinit var categoriesRecyclerView: RecyclerView

    private lateinit var prefsManager: PreferencesManager
    private lateinit var categoriesAdapter: CategoriesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_goals)

        initViews()
        prefsManager = PreferencesManager(this)
        setupRecyclerView()
        setupClickListeners()
        loadDefaultCategories()
        updateCategoriesList()
    }

    private fun initViews() {
        backButton = findViewById(R.id.backButton)
        addGoalButton = findViewById(R.id.addGoalButton)
        fabAddGoal = findViewById(R.id.fabAddGoal)
        categoriesRecyclerView = findViewById(R.id.categoriesRecyclerView)
    }

    private fun setupRecyclerView() {
        categoriesAdapter = CategoriesAdapter(
            onCategoryClick = { category ->
                showCategoryStatsDialog(category)
            },
            onEditClick = { category ->
                showEditCategoryDialog(category)
            },
            onDeleteClick = { category ->
                showDeleteCategoryDialog(category)
            },
            getTasksCount = { categoryId ->
                prefsManager.getAllTasks().count { it.categoryId == categoryId }
            }
        )

        categoriesRecyclerView.layoutManager = LinearLayoutManager(this)
        categoriesRecyclerView.adapter = categoriesAdapter
    }

    private fun setupClickListeners() {
        // دکمه بازگشت
        backButton.setOnClickListener {
            finish()
        }

        // دکمه افزودن هدف جدید
        addGoalButton.setOnClickListener {
            showAddCategoryDialog()
        }

        // دکمه شناور افزودن هدف
        fabAddGoal.setOnClickListener {
            showAddCategoryDialog()
        }
    }

    private fun loadDefaultCategories() {
        val savedCategories = prefsManager.getTaskCategories()
        if (savedCategories.isEmpty()) {
            // بخش‌های پیش‌فرض با رنگ‌های مدرن
            val defaultCategories = listOf(
                TaskCategory("health", "🏃 سلامتی", "#10B981"),     // Green
                TaskCategory("work", "💼 کار", "#3B82F6"),         // Blue
                TaskCategory("education", "📚 آموزش", "#F59E0B"),  // Orange
                TaskCategory("ideas", "💡 ایده", "#8B5CF6"),       // Purple
                TaskCategory("family", "👨‍👩‍👧‍👦 خانواده", "#06B6D4"), // Cyan
                TaskCategory("finance", "💰 مالی", "#475569")      // Gray
            )
            for (category in defaultCategories) {
                prefsManager.saveTaskCategory(category)
            }
        }
        updateCategoriesList()
    }

    private fun updateCategoriesList() {
        val categories = prefsManager.getTaskCategories()
        categoriesAdapter.updateCategories(categories)
    }

    private fun showAddCategoryDialog() {
        val editText = EditText(this)
        editText.hint = "نام هدف جدید"

        AlertDialog.Builder(this)
            .setTitle("افزودن هدف جدید")
            .setView(editText)
            .setPositiveButton("ایجاد") { _, _ ->
                val categoryName = editText.text.toString().trim()
                if (categoryName.isNotEmpty()) {
                    val newCategory = TaskCategory(
                        id = "goal_${System.currentTimeMillis()}",
                        name = "🎯 $categoryName",
                        color = getRandomColor()
                    )
                    prefsManager.saveTaskCategory(newCategory)
                    updateCategoriesList()
                    showToast("هدف جدید اضافه شد!")
                } else {
                    showToast("لطفاً نام هدف را وارد کنید!")
                }
            }
            .setNegativeButton("لغو", null)
            .show()
    }

    private fun showEditCategoryDialog(category: TaskCategory) {
        val editText = EditText(this)
        // حذف ایموجی از نام فعلی برای ویرایش
        val currentName = category.name.replaceFirst("^[🎯🏃💼📚💡👨‍👩‍👧‍👦💰📝]\\s*".toRegex(), "")
        editText.setText(currentName)
        editText.hint = "نام هدف"

        AlertDialog.Builder(this)
            .setTitle("ویرایش هدف")
            .setView(editText)
            .setPositiveButton("ذخیره") { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty()) {
                    // اضافه کردن ایموجی مناسب بر اساس نام
                    val emoji = getEmojiForCategory(newName)
                    val updatedCategory = category.copy(name = "$emoji $newName")
                    prefsManager.saveTaskCategory(updatedCategory)
                    updateCategoriesList()
                    showToast("هدف ویرایش شد!")
                } else {
                    showToast("لطفاً نام هدف را وارد کنید!")
                }
            }
            .setNegativeButton("لغو", null)
            .show()
    }

    private fun showDeleteCategoryDialog(category: TaskCategory) {
        AlertDialog.Builder(this)
            .setTitle("حذف هدف")
            .setMessage("آیا مطمئن هستید که می‌خواهید این هدف و تمام کارهای مرتبط با آن را حذف کنید؟")
            .setPositiveButton("حذف") { _, _ ->
                prefsManager.deleteTaskCategory(category.id)
                updateCategoriesList()
                showToast("هدف حذف شد!")
            }
            .setNegativeButton("لغو", null)
            .show()
    }

    private fun showCategoryStatsDialog(category: TaskCategory) {
        val todayTasks = prefsManager.getTasksForPeriod(category.id, TimePeriod.TODAY)
        val weekTasks = prefsManager.getTasksForPeriod(category.id, TimePeriod.THIS_WEEK)
        val monthTasks = prefsManager.getTasksForPeriod(category.id, TimePeriod.THIS_MONTH)
        val yearTasks = prefsManager.getTasksForPeriod(category.id, TimePeriod.THIS_YEAR)

        val todayCompleted = todayTasks.count { it.isCompleted }
        val weekCompleted = weekTasks.count { it.isCompleted }
        val monthCompleted = monthTasks.count { it.isCompleted }
        val yearCompleted = yearTasks.count { it.isCompleted }

        val message = buildString {
            appendLine("📊 آمار ${category.name}")
            appendLine()
            
            // آمار امروز + لیست کارها
            appendLine("📅 امروز: $todayCompleted از ${todayTasks.size} کار")
            if (todayTasks.isNotEmpty()) {
                for (task in todayTasks) {
                    val status = if (task.isCompleted) "✅" else "⏳"
                    appendLine("   $status ${task.title}")
                }
                appendLine()
            }
            
            // آمار این هفته + لیست کارها
            appendLine("🗓️ این هفته: $weekCompleted از ${weekTasks.size} کار")
            if (weekTasks.isNotEmpty()) {
                for (task in weekTasks) {
                    val status = if (task.isCompleted) "✅" else "⏳"
                    appendLine("   $status ${task.title}")
                }
                appendLine()
            }
            
            // آمار این ماه + لیست کارها (محدود به 10 کار اول)
            appendLine("📆 این ماه: $monthCompleted از ${monthTasks.size} کار")
            if (monthTasks.isNotEmpty()) {
                val displayTasks = monthTasks.take(10)
                for (task in displayTasks) {
                    val status = if (task.isCompleted) "✅" else "⏳"
                    appendLine("   $status ${task.title}")
                }
                if (monthTasks.size > 10) {
                    appendLine("   ... و ${monthTasks.size - 10} کار دیگر")
                }
                appendLine()
            }
            
            // آمار امسال (فقط تعداد کل)
            appendLine("📅 امسال: $yearCompleted از ${yearTasks.size} کار")
            if (yearTasks.isNotEmpty()) {
                val yearProgress = (yearCompleted * 100 / yearTasks.size)
                appendLine("📈 پیشرفت کلی: %$yearProgress")
            }
        }

        AlertDialog.Builder(this)
            .setTitle("آمار هدف")
            .setMessage(message)
            .setPositiveButton("باشه", null)
            .setNeutralButton("مشاهده همه") { _, _ ->
                showDetailedTasksDialog(category, yearTasks)
            }
            .show()
    }

    private fun showDetailedTasksDialog(category: TaskCategory, allTasks: List<Task>) {
        val message = buildString {
            appendLine("📋 همه کارهای ${category.name}")
            appendLine()
            
            if (allTasks.isEmpty()) {
                appendLine("هیچ کاری برای این هدف ثبت نشده است.")
            } else {
                // گروه‌بندی بر اساس وضعیت
                val completedTasks = allTasks.filter { it.isCompleted }
                val pendingTasks = allTasks.filter { !it.isCompleted }
                
                // کارهای انجام شده
                if (completedTasks.isNotEmpty()) {
                    appendLine("✅ کارهای انجام شده (${completedTasks.size}):")
                    for (task in completedTasks) {
                        appendLine("   • ${task.title}")
                        task.getFormattedCompletionDate()?.let { date ->
                            appendLine("     تکمیل شده: $date")
                        }
                    }
                    appendLine()
                }
                
                // کارهای در انتظار
                if (pendingTasks.isNotEmpty()) {
                    appendLine("⏳ کارهای در انتظار (${pendingTasks.size}):")
                    for (task in pendingTasks) {
                        appendLine("   • ${task.title}")
                        appendLine("     ایجاد شده: ${task.getFormattedCreatedDate()}")
                    }
                    appendLine()
                }
                
                // آمار کلی
                val completionRate = if (allTasks.isNotEmpty()) {
                    (completedTasks.size * 100) / allTasks.size
                } else 0
                
                appendLine("📊 نرخ تکمیل: %$completionRate")
                appendLine("📈 کل کارها: ${allTasks.size}")
            }
        }

        AlertDialog.Builder(this)
            .setTitle("جزئیات کامل")
            .setMessage(message)
            .setPositiveButton("باشه", null)
            .setNegativeButton("علامت‌گذاری") { _, _ ->
                showTaskMarkingDialog(category, allTasks.filter { !it.isCompleted })
            }
            .show()
    }

    private fun showTaskMarkingDialog(category: TaskCategory, pendingTasks: List<Task>) {
        if (pendingTasks.isEmpty()) {
            showToast("همه کارهای این هدف انجام شده است! 🎉")
            return
        }

        val taskTitles = pendingTasks.map { it.title }.toTypedArray()
        val checkedItems = BooleanArray(pendingTasks.size) { false }

        AlertDialog.Builder(this)
            .setTitle("انتخاب کارهای انجام شده")
            .setMultiChoiceItems(taskTitles, checkedItems,
                DialogInterface.OnMultiChoiceClickListener { _, which, isChecked ->
                    checkedItems[which] = isChecked
                })
            .setPositiveButton("علامت‌گذاری") { _, _ ->
                var markedCount = 0
                for (index in checkedItems.indices) {
                    if (checkedItems[index]) {
                        val task = pendingTasks[index]
                        val completedTask = task.markAsCompleted()
                        prefsManager.saveTask(completedTask)
                        markedCount++
                    }
                }
                
                if (markedCount > 0) {
                    showToast("$markedCount کار به عنوان انجام شده علامت‌گذاری شد! ✅")
                    updateCategoriesList() // به‌روزرسانی لیست
                }
            }
            .setNegativeButton("لغو", null)
            .show()
    }

    private fun getRandomColor(): String {
        val modernColors = arrayOf(
            "#3B82F6", // Blue
            "#10B981", // Green  
            "#F59E0B", // Orange
            "#EF4444", // Red
            "#8B5CF6", // Purple
            "#06B6D4", // Cyan
            "#84CC16", // Lime
            "#F97316"  // Orange-500
        )
        return modernColors.random()
    }

    private fun getEmojiForCategory(categoryName: String): String {
        return when {
            categoryName.contains("سلامت", ignoreCase = true) || 
            categoryName.contains("ورزش", ignoreCase = true) || 
            categoryName.contains("تندرستی", ignoreCase = true) -> "🏃"
            
            categoryName.contains("کار", ignoreCase = true) || 
            categoryName.contains("شغل", ignoreCase = true) || 
            categoryName.contains("حرفه", ignoreCase = true) -> "💼"
            
            categoryName.contains("آموزش", ignoreCase = true) || 
            categoryName.contains("تحصیل", ignoreCase = true) || 
            categoryName.contains("مطالعه", ignoreCase = true) -> "📚"
            
            categoryName.contains("ایده", ignoreCase = true) || 
            categoryName.contains("خلاقیت", ignoreCase = true) || 
            categoryName.contains("نوآوری", ignoreCase = true) -> "💡"
            
            categoryName.contains("خانواده", ignoreCase = true) || 
            categoryName.contains("فرزند", ignoreCase = true) || 
            categoryName.contains("همسر", ignoreCase = true) -> "👨‍👩‍👧‍👦"
            
            categoryName.contains("مالی", ignoreCase = true) || 
            categoryName.contains("پول", ignoreCase = true) || 
            categoryName.contains("سرمایه", ignoreCase = true) -> "💰"
            
            else -> "🎯" // پیش‌فرض
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}