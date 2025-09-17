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
    private val onTaskChecked: (Task, Boolean) -> Unit,
    private val onTaskLongClick: (Task) -> Unit = {},
    private val onTaskClick: (Task) -> Unit = {},
    private val onSelectionChanged: (List<Task>) -> Unit = {}
) : RecyclerView.Adapter<TasksAdapter.TaskViewHolder>() {

    private var tasks: List<Task> = emptyList()
    private var isSelectionMode = false
    private val selectedTasks = mutableSetOf<String>() // Task IDs

    fun updateTasks(newTasks: List<Task>) {
        val oldTasks = tasks
        tasks = newTasks

        // استفاده از notifyItemChanged برای بهینه‌سازی عملکرد
        if (oldTasks.size == newTasks.size) {
            // بررسی تغییرات فردی
            for (i in newTasks.indices) {
                val oldTask = oldTasks.getOrNull(i)
                val newTask = newTasks[i]
                if (oldTask?.id == newTask.id && oldTask.isCompleted != newTask.isCompleted) {
                    // فقط وضعیت تکمیل تغییر کرده
                    notifyItemChanged(i)
                    return
                }
            }
        }

        // اگر تغییرات پیچیده‌تر بود، از notifyDataSetChanged استفاده کن
        notifyDataSetChanged()
    }

    fun enterSelectionMode(task: Task) {
        isSelectionMode = true
        selectedTasks.clear()
        selectedTasks.add(task.id)
        notifyDataSetChanged()
        onSelectionChanged(getSelectedTasks())
    }

    fun exitSelectionMode() {
        isSelectionMode = false
        selectedTasks.clear()
        notifyDataSetChanged()
        onSelectionChanged(emptyList())
    }

    fun toggleTaskSelection(task: Task) {
        if (selectedTasks.contains(task.id)) {
            selectedTasks.remove(task.id)
        } else {
            selectedTasks.add(task.id)
        }
        notifyItemChanged(tasks.indexOfFirst { it.id == task.id })
        onSelectionChanged(getSelectedTasks())

        // Exit selection mode if no tasks selected
        if (selectedTasks.isEmpty()) {
            exitSelectionMode()
        }
    }

    fun getSelectedTasks(): List<Task> {
        return tasks.filter { selectedTasks.contains(it.id) }
    }

    fun isInSelectionMode(): Boolean = isSelectionMode

    fun getSelectedCount(): Int = selectedTasks.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(tasks[position])
    }

    override fun getItemCount(): Int = tasks.size

    override fun getItemId(position: Int): Long {
        return if (position < tasks.size) {
            tasks[position].id.hashCode().toLong()
        } else {
            super.getItemId(position)
        }
    }

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val taskCheckBox: CheckBox = itemView.findViewById(R.id.taskCheckBox)
        private val taskTitle: TextView = itemView.findViewById(R.id.taskTitle)
        private val taskDescription: TextView = itemView.findViewById(R.id.taskDescription)
        private val taskCategory: TextView = itemView.findViewById(R.id.taskCategory)
        private val priorityIndicator: View = itemView.findViewById(R.id.priorityIndicator)
        private val statusIcon: TextView = itemView.findViewById(R.id.statusIcon)

        fun bind(task: Task) {
            // ابتدا listener را حذف کن تا از تداخل جلوگیری شود
            taskCheckBox.setOnCheckedChangeListener(null)

            taskTitle.text = task.title
            taskDescription.text = task.description

            // در حالت انتخاب، چک‌باکس برای انتخاب است، در غیر این صورت برای تکمیل
            if (isSelectionMode) {
                taskCheckBox.isChecked = selectedTasks.contains(task.id)
                // تغییر ظاهر آیتم در حالت انتخاب
                itemView.setBackgroundColor(
                    if (selectedTasks.contains(task.id))
                        Color.parseColor("#E3F2FD")
                    else
                        Color.TRANSPARENT
                )
            } else {
                taskCheckBox.isChecked = task.isCompleted
                itemView.setBackgroundColor(Color.TRANSPARENT)
            }

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

            // حالا listener را دوباره تنظیم کن
            taskCheckBox.setOnCheckedChangeListener { _, isChecked ->
                if (isSelectionMode) {
                    // در حالت انتخاب، تغییر وضعیت انتخاب
                    toggleTaskSelection(task)
                } else {
                    // در حالت عادی، تغییر وضعیت تکمیل
                    if (isChecked != task.isCompleted) {
                        onTaskChecked(task, isChecked)
                    }
                }
            }

            // Long click برای ورود به حالت انتخاب
            itemView.setOnLongClickListener {
                if (!isSelectionMode) {
                    enterSelectionMode(task)
                    onTaskLongClick(task)
                    true
                } else {
                    false
                }
            }

            // Click عادی
            itemView.setOnClickListener {
                if (isSelectionMode) {
                    toggleTaskSelection(task)
                } else {
                    onTaskClick(task)
                }
            }
        }
    }
}