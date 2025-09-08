package com.Alixra.power.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.Alixra.power.R
import com.Alixra.power.data.Task

class MiniTasksAdapter(
    private val onTaskClick: (Task) -> Unit
) : RecyclerView.Adapter<MiniTasksAdapter.MiniTaskViewHolder>() {

    private var tasks: List<Task> = emptyList()

    fun updateTasks(newTasks: List<Task>) {
        tasks = newTasks
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MiniTaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_mini_task, parent, false)
        return MiniTaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: MiniTaskViewHolder, position: Int) {
        holder.bind(tasks[position])
    }

    override fun getItemCount(): Int = tasks.size

    inner class MiniTaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val statusTextView: TextView = itemView.findViewById(R.id.taskStatusTextView)
        private val titleTextView: TextView = itemView.findViewById(R.id.taskTitleTextView)

        fun bind(task: Task) {
            // نمایش وضعیت کار
            statusTextView.text = if (task.isCompleted) "✅" else "⏳"
            
            // نمایش عنوان کار
            titleTextView.text = task.title

            // کلیک روی کار
            itemView.setOnClickListener {
                onTaskClick(task)
            }
        }
    }
}
