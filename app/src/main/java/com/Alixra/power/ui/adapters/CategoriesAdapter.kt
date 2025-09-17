package com.Alixra.power.ui.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.Alixra.power.R
import com.Alixra.power.data.TaskCategory
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class CategoriesAdapter(
    private val onCategoryClick: (TaskCategory) -> Unit,
    private val onEditClick: (TaskCategory) -> Unit,
    private val onDeleteClick: (TaskCategory) -> Unit,
    private val getTasksCount: (String) -> Int
) : RecyclerView.Adapter<CategoriesAdapter.CategoryViewHolder>() {

    private var categories: List<TaskCategory> = emptyList()

    fun updateCategories(newCategories: List<TaskCategory>) {
        categories = newCategories
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(categories[position])
    }

    override fun getItemCount(): Int = categories.size

    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView.findViewById(R.id.categoryCard)
        private val nameTextView: TextView = itemView.findViewById(R.id.categoryName)
        private val tasksCountTextView: TextView = itemView.findViewById(R.id.tasksCount)
        private val editButton: MaterialButton = itemView.findViewById(R.id.editButton)
        private val deleteButton: MaterialButton = itemView.findViewById(R.id.deleteButton)

        fun bind(category: TaskCategory) {
            nameTextView.text = category.name
            
            // نمایش تعداد کارهای واقعی
            val tasksCount = getTasksCount(category.id)
            tasksCountTextView.text = itemView.context.getString(R.string.task_count_dynamic, tasksCount)

            // تنظیم رنگ
            try {
                cardView.setCardBackgroundColor(Color.parseColor(category.color))
            } catch (e: Exception) {
                cardView.setCardBackgroundColor(Color.parseColor("#2196F3"))
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
        }
    }
}