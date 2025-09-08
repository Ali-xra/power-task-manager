package com.Alixra.power.ui.adapters

import android.graphics.Color
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.Alixra.power.R
import com.Alixra.power.data.PreferencesManager
import com.Alixra.power.data.Task
import com.Alixra.power.data.TaskPriority

class TasksAdapter(
    private val preferencesManager: PreferencesManager,
    private val onTaskChecked: (Task, Boolean) -> Unit
) : RecyclerView.Adapter<TasksAdapter.TaskViewHolder>() {

    private var tasks: List<Task> = emptyList()

    fun updateTasks(newTasks: List<Task>) {
        tasks = newTasks
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(tasks[position])
    }

    override fun getItemCount(): Int = tasks.size

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val taskCheckBox: CheckBox = itemView.findViewById(R.id.taskCheckBox)
        private val taskTitle: TextView = itemView.findViewById(R.id.taskTitle)
        private val taskDescription: TextView = itemView.findViewById(R.id.taskDescription)
        private val taskCategory: TextView = itemView.findViewById(R.id.taskCategory)
        private val priorityIndicator: View = itemView.findViewById(R.id.priorityIndicator)
        private val statusIcon: TextView = itemView.findViewById(R.id.statusIcon)

        fun bind(task: Task) {
            taskTitle.text = task.title
            taskDescription.text = task.description
            taskCheckBox.isChecked = task.isCompleted

            // تنظیم رنگ اولویت با پالت جدید
            val priorityColor = when (task.priority) {
                TaskPriority.NORMAL -> Color.parseColor("#3B82F6")  // priority_normal
                TaskPriority.HIGH -> Color.parseColor("#F59E0B")     // priority_high  
                TaskPriority.URGENT -> Color.parseColor("#EF4444")   // priority_urgent
            }
            priorityIndicator.setBackgroundColor(priorityColor)

            // نمایش ایموجی وضعیت
            statusIcon.text = task.getStatusEmoji()

            // استایل متن برای کارهای انجام شده
            if (task.isCompleted) {
                taskTitle.paintFlags = taskTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                taskTitle.alpha = 0.6f
                taskDescription.alpha = 0.6f
            } else {
                taskTitle.paintFlags = taskTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                taskTitle.alpha = 1.0f
                taskDescription.alpha = 1.0f
            }

            // نمایش/مخفی کردن توضیحات
            if (task.description.isNotEmpty()) {
                taskDescription.visibility = View.VISIBLE
                taskDescription.text = task.description
            } else {
                taskDescription.visibility = View.GONE
            }

            // نمایش اطلاعات هدف/بخش کار
            val category = preferencesManager.getTaskCategory(task.categoryId)
            if (category != null && task.categoryId.isNotEmpty()) {
                taskCategory.visibility = View.VISIBLE
                taskCategory.text = category.name
                try {
                    taskCategory.setBackgroundColor(Color.parseColor(category.color + "33")) // شفافیت 20%
                    taskCategory.setTextColor(Color.parseColor(category.color))
                } catch (e: Exception) {
                    // اگر رنگ نامعتبر بود، از رنگ پیش‌فرض استفاده کن
                    taskCategory.setBackgroundColor(Color.parseColor("#E3F2FD"))
                    taskCategory.setTextColor(Color.parseColor("#1976D2"))
                }
            } else {
                taskCategory.visibility = View.GONE
            }

            // کلیک روی چک‌باکس
            taskCheckBox.setOnCheckedChangeListener { _, isChecked ->
                onTaskChecked(task, isChecked)
            }
        }
    }
}