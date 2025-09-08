package com.Alixra.power.ui.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.Alixra.power.R
import com.Alixra.power.data.TaskCategory

class CategoryRatingAdapter(
    private val onRatingChanged: (String, Int) -> Unit
) : RecyclerView.Adapter<CategoryRatingAdapter.CategoryRatingViewHolder>() {

    private val categories = mutableListOf<TaskCategory>()
    private val categoryRatings = mutableMapOf<String, Int>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryRatingViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_category_rating, parent, false)
        return CategoryRatingViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryRatingViewHolder, position: Int) {
        holder.bind(categories[position])
    }

    override fun getItemCount(): Int = categories.size

    fun updateCategories(newCategories: List<TaskCategory>) {
        categories.clear()
        categories.addAll(newCategories)
        // ایجاد امتیاز پیش‌فرض 5 برای همه بخش‌ها
        newCategories.forEach { category ->
            if (!categoryRatings.containsKey(category.id)) {
                categoryRatings[category.id] = 5
            }
        }
        notifyDataSetChanged()
    }

    fun getAllRatings(): Map<String, Int> {
        return categoryRatings.toMap()
    }

    fun setRating(categoryId: String, rating: Int) {
        categoryRatings[categoryId] = rating
        val position = categories.indexOfFirst { it.id == categoryId }
        if (position != -1) {
            notifyItemChanged(position)
        }
    }

    inner class CategoryRatingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val categoryIcon: TextView = itemView.findViewById(R.id.categoryIcon)
        private val categoryName: TextView = itemView.findViewById(R.id.categoryName)
        private val currentRating: TextView = itemView.findViewById(R.id.currentRating)
        private val ratingSeekBar: SeekBar = itemView.findViewById(R.id.ratingSeekBar)
        private val ratingDisplay: TextView = itemView.findViewById(R.id.ratingDisplay)
        private val progressBar: ProgressBar = itemView.findViewById(R.id.progressBar)

        fun bind(category: TaskCategory) {
            // تنظیم اطلاعات بخش
            categoryIcon.text = category.icon
            categoryName.text = category.name

            // دریافت امتیاز فعلی
            val rating = categoryRatings[category.id] ?: 5

            // تنظیم امتیاز فعلی
            currentRating.text = "$rating/10"
            updateRatingColor(currentRating, rating)

            // تنظیم SeekBar
            ratingSeekBar.setOnSeekBarChangeListener(null) // حذف listener موقت
            ratingSeekBar.progress = rating

            // تنظیم listener جدید
            ratingSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        updateRatingUI(progress)
                        categoryRatings[category.id] = progress
                        onRatingChanged(category.id, progress)
                    }
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })

            // به‌روزرسانی UI
            updateRatingUI(rating)
        }

        private fun updateRatingUI(rating: Int) {
            // به‌روزرسانی امتیاز فعلی
            currentRating.text = "$rating/10"
            updateRatingColor(currentRating, rating)

            // به‌روزرسانی نمایش عددی
            ratingDisplay.text = rating.toString()
            updateRatingColor(ratingDisplay, rating)

            // به‌روزرسانی نوار پیشرفت
            progressBar.progress = rating
            progressBar.progressTintList = android.content.res.ColorStateList.valueOf(getRatingColor(rating))

            // به‌روزرسانی SeekBar
            ratingSeekBar.progressTintList = android.content.res.ColorStateList.valueOf(getRatingColor(rating))
            ratingSeekBar.thumbTintList = android.content.res.ColorStateList.valueOf(getRatingColor(rating))
        }

        private fun updateRatingColor(textView: TextView, rating: Int) {
            textView.setTextColor(getRatingColor(rating))
        }

        private fun getRatingColor(rating: Int): Int {
            return when (rating) {
                0 -> Color.parseColor("#757575")      // خاکستری - بدون امتیاز
                in 1..2 -> Color.parseColor("#F44336") // قرمز - ضعیف
                in 3..4 -> Color.parseColor("#FF9800") // نارنجی - متوسط ضعیف
                in 5..6 -> Color.parseColor("#FFC107") // زرد - متوسط
                in 7..8 -> Color.parseColor("#8BC34A") // سبز روشن - خوب
                in 9..10 -> Color.parseColor("#4CAF50") // سبز پررنگ - عالی
                else -> Color.parseColor("#757575")
            }
        }
    }
}