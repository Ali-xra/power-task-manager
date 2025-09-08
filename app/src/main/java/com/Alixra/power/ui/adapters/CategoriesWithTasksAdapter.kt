package com.Alixra.power.ui.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.Alixra.power.R
import com.Alixra.power.data.PreferencesManager
import com.Alixra.power.data.Task
import com.Alixra.power.data.TaskCategory
import com.Alixra.power.data.TimePeriod
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class CategoriesWithTasksAdapter(
    private val preferencesManager: PreferencesManager,
    private val onCategoryClick: (TaskCategory) -> Unit,
    private val onEditClick: (TaskCategory) -> Unit,
    private val onDeleteClick: (TaskCategory) -> Unit,
    private val onTaskClick: (Task) -> Unit,
    private val getTasksForCategory: (String, TimePeriod) -> List<Task>
) : RecyclerView.Adapter<CategoriesWithTasksAdapter.CategoryViewHolder>() {

    private var categories: List<TaskCategory> = emptyList()
    private var currentTimePeriod: TimePeriod = TimePeriod.TODAY

    fun updateCategories(newCategories: List<TaskCategory>, timePeriod: TimePeriod) {
        categories = newCategories
        currentTimePeriod = timePeriod
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_with_tasks, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(categories[position])
    }

    override fun getItemCount(): Int = categories.size

    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView.findViewById(R.id.categoryCard)
        private val nameTextView: TextView = itemView.findViewById(R.id.categoryName)
        private val totalTasksCount: TextView = itemView.findViewById(R.id.totalTasksCount)
        private val completedTasksCount: TextView = itemView.findViewById(R.id.completedTasksCount)
        private val progressPercentage: TextView = itemView.findViewById(R.id.progressPercentage)
        private val tasksRecyclerView: RecyclerView = itemView.findViewById(R.id.tasksRecyclerView)
        private val toggleTasksButton: Button = itemView.findViewById(R.id.toggleTasksButton)
        private val editButton: MaterialButton = itemView.findViewById(R.id.editButton)
        private val deleteButton: MaterialButton = itemView.findViewById(R.id.deleteButton)

        private lateinit var tasksAdapter: TasksAdapter
        private var isTasksVisible = false

        fun bind(category: TaskCategory) {
            nameTextView.text = category.name

            // تنظیم رنگ
            try {
                cardView.setCardBackgroundColor(Color.parseColor(category.color))
            } catch (e: Exception) {
                cardView.setCardBackgroundColor(Color.parseColor("#2196F3"))
            }

            // دریافت کارهای این بخش برای بازه زمانی فعلی
            val tasks = getTasksForCategory(category.id, currentTimePeriod)
            val completedTasks = tasks.count { it.isCompleted }
            val progress = if (tasks.isNotEmpty()) (completedTasks * 100 / tasks.size) else 0

            // نمایش آمار
            totalTasksCount.text = "${tasks.size} کار"
            completedTasksCount.text = "$completedTasks انجام شده"
            progressPercentage.text = "$progress%"

            // تنظیم RecyclerView برای کارها
            if (!::tasksAdapter.isInitialized) {
                tasksAdapter = TasksAdapter(preferencesManager) { task, _ ->
                    onTaskClick(task)
                }
                tasksRecyclerView.layoutManager = LinearLayoutManager(itemView.context)
                tasksRecyclerView.adapter = tasksAdapter
            }

            tasksAdapter.updateTasks(tasks)

            // نمایش/مخفی کردن دکمه و لیست کارها
            if (tasks.isNotEmpty()) {
                toggleTasksButton.visibility = View.VISIBLE
                toggleTasksButton.text = if (isTasksVisible) "مخفی کردن کارها" else "نمایش کارها"
                tasksRecyclerView.visibility = if (isTasksVisible) View.VISIBLE else View.GONE
            } else {
                toggleTasksButton.visibility = View.GONE
                tasksRecyclerView.visibility = View.GONE
            }

            // کلیک روی کارت برای نمایش آمار
            cardView.setOnClickListener {
                onCategoryClick(category)
            }

            // دکمه ویرایش
            editButton.setOnClickListener {
                onEditClick(category)
            }

            // دکمه حذف
            deleteButton.setOnClickListener {
                onDeleteClick(category)
            }

            // دکمه نمایش/مخفی کردن کارها
            toggleTasksButton.setOnClickListener {
                isTasksVisible = !isTasksVisible
                tasksRecyclerView.visibility = if (isTasksVisible) View.VISIBLE else View.GONE
                toggleTasksButton.text = if (isTasksVisible) "مخفی کردن کارها" else "نمایش کارها"
            }
        }
    }
}
