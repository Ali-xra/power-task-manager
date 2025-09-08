package com.Alixra.power.ui.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.Alixra.power.R
import com.Alixra.power.data.Task
import com.Alixra.power.ui.CalendarActivity
import com.google.android.material.card.MaterialCardView

class CalendarAdapter(
    private val onDayClick: (CalendarActivity.CalendarDay) -> Unit,
    private val onTaskClick: (Task) -> Unit,
    private val getTasksForDate: (Long) -> List<Task>
) : RecyclerView.Adapter<CalendarAdapter.CalendarDayViewHolder>() {

    private var calendarDays: List<CalendarActivity.CalendarDay> = emptyList()

    fun updateDays(newDays: List<CalendarActivity.CalendarDay>) {
        calendarDays = newDays
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarDayViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_calendar_day, parent, false)
        return CalendarDayViewHolder(view)
    }

    override fun onBindViewHolder(holder: CalendarDayViewHolder, position: Int) {
        holder.bind(calendarDays[position])
    }

    override fun getItemCount(): Int = calendarDays.size

    inner class CalendarDayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView.findViewById(R.id.calendarDayCard)
        private val dayNumberTextView: TextView = itemView.findViewById(R.id.dayNumberTextView)
        private val tasksCountTextView: TextView = itemView.findViewById(R.id.tasksCountTextView)
        private val todayIndicator: View = itemView.findViewById(R.id.todayIndicator)
        private val tasksRecyclerView: RecyclerView = itemView.findViewById(R.id.tasksRecyclerView)
        
        private lateinit var miniTasksAdapter: MiniTasksAdapter

        fun bind(calendarDay: CalendarActivity.CalendarDay) {
            if (calendarDay.dayNumber == 0) {
                // روز خالی یا هدر ماه
                dayNumberTextView.text = ""
                tasksCountTextView.text = ""
                todayIndicator.visibility = View.GONE
                tasksRecyclerView.visibility = View.GONE
                cardView.setCardBackgroundColor(Color.TRANSPARENT)
                cardView.isClickable = false
                cardView.isFocusable = false
                return
            }

            // نمایش شماره روز
            dayNumberTextView.text = calendarDay.dayNumber.toString()

            // نشانگر امروز
            if (calendarDay.isToday) {
                todayIndicator.visibility = View.VISIBLE
                cardView.setCardBackgroundColor(Color.parseColor("#E3F2FD"))
                dayNumberTextView.setTextColor(Color.parseColor("#1976D2"))
                dayNumberTextView.textSize = 18f
            } else {
                todayIndicator.visibility = View.GONE
                cardView.setCardBackgroundColor(Color.WHITE)
                dayNumberTextView.setTextColor(Color.parseColor("#333333"))
                dayNumberTextView.textSize = 16f
            }

            // تنظیم RecyclerView برای کارها
            if (!::miniTasksAdapter.isInitialized) {
                miniTasksAdapter = MiniTasksAdapter { task ->
                    onTaskClick(task)
                }
                tasksRecyclerView.layoutManager = LinearLayoutManager(itemView.context)
                tasksRecyclerView.adapter = miniTasksAdapter
            }

            // دریافت و نمایش کارهای روز
            val tasksForDay = getTasksForDate(calendarDay.date)
            miniTasksAdapter.updateTasks(tasksForDay)

            // نمایش تعداد کارها
            if (tasksForDay.isNotEmpty()) {
                tasksCountTextView.text = "${tasksForDay.size} کار"
                tasksCountTextView.visibility = View.VISIBLE
                
                val color = when {
                    tasksForDay.size >= 5 -> "#F44336" // قرمز برای کارهای زیاد
                    tasksForDay.size >= 3 -> "#FF9800" // نارنجی برای کارهای متوسط
                    else -> "#4CAF50" // سبز برای کارهای کم
                }
                tasksCountTextView.setTextColor(Color.parseColor(color))
            } else {
                tasksCountTextView.visibility = View.GONE
            }

            // کلیک روی روز
            if (calendarDay.isClickable) {
                cardView.setOnClickListener {
                    onDayClick(calendarDay)
                }
                cardView.isClickable = true
                cardView.isFocusable = true
            } else {
                cardView.setOnClickListener(null)
                cardView.isClickable = false
                cardView.isFocusable = false
            }
        }
    }
}
