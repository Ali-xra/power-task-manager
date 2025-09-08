package com.Alixra.power.ui

import android.app.AlertDialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.Alixra.power.R
import com.Alixra.power.data.PreferencesManager
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
        categoriesAdapter = CategoriesAdapter { category ->
            showCategoryStatsDialog(category)
        }

        categoriesRecyclerView.layoutManager = LinearLayoutManager(this)
        categoriesRecyclerView.adapter = categoriesAdapter
    }

    private fun setupClickListeners() {
        // دکمه بازگشت
        backButton.setOnClickListener {
            onBackPressed()
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
            // بخش‌های پیش‌فرض
            val defaultCategories = listOf(
                TaskCategory("health", "🏃 سلامتی", "#4CAF50"),
                TaskCategory("work", "💼 کار", "#2196F3"),
                TaskCategory("education", "📚 آموزش", "#FF9800"),
                TaskCategory("ideas", "💡 ایده", "#9C27B0"),
                TaskCategory("family", "👨‍👩‍👧‍👦 خانواده", "#E91E63"),
                TaskCategory("finance", "💰 مالی", "#607D8B")
            )
            defaultCategories.forEach { category ->
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
        editText.setText(category.name)
        editText.hint = "نام هدف"

        AlertDialog.Builder(this)
            .setTitle("ویرایش هدف")
            .setView(editText)
            .setPositiveButton("ذخیره") { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty()) {
                    val updatedCategory = category.copy(name = newName)
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
            appendLine("📅 امروز: $todayCompleted از ${todayTasks.size} کار")
            appendLine("🗓️ این هفته: $weekCompleted از ${weekTasks.size} کار")
            appendLine("📆 این ماه: $monthCompleted از ${monthTasks.size} کار")
            appendLine("📅 امسال: $yearCompleted از ${yearTasks.size} کار")
            appendLine()
            if (yearTasks.isNotEmpty()) {
                val yearProgress = (yearCompleted * 100 / yearTasks.size)
                appendLine("📈 پیشرفت کلی: %$yearProgress")
            }
        }

        AlertDialog.Builder(this)
            .setTitle("آمار هدف")
            .setMessage(message)
            .setPositiveButton("باشه", null)
            .show()
    }

    private fun getRandomColor(): String {
        val colors = arrayOf(
            "#4CAF50", "#2196F3", "#FF9800", "#9C27B0",
            "#E91E63", "#607D8B", "#795548", "#009688",
            "#FF5722", "#3F51B5", "#CDDC39", "#FFC107"
        )
        return colors.random()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}