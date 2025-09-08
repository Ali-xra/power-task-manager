package com.Alixra.power.data

import com.google.gson.annotations.SerializedName
import java.text.SimpleDateFormat
import java.util.*

/**
 * مدل داده برای کارها
 */
data class Task(
    @SerializedName("id")
    val id: String,

    @SerializedName("title")
    var title: String,

    @SerializedName("description")
    var description: String = "",

    @SerializedName("category_id")
    val categoryId: String,

    @SerializedName("time_period")
    val timePeriod: TimePeriod,

    @SerializedName("is_completed")
    var isCompleted: Boolean = false,

    @SerializedName("completion_date")
    var completionDate: Long? = null,

    @SerializedName("created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @SerializedName("due_date")
    var dueDate: Long? = null,

    @SerializedName("priority")
    var priority: TaskPriority = TaskPriority.NORMAL,

    @SerializedName("rating")
    var rating: Int = 0, // امتیاز 0-10 برای کارهای انجام شده

    @SerializedName("notes")
    var notes: String = "",

    @SerializedName("moved_from_date")
    var movedFromDate: String? = null
) {

    /**
     * سازنده خالی برای Gson
     */
    constructor() : this("", "", "", "", TimePeriod.TODAY, false, null, System.currentTimeMillis(), null, TaskPriority.NORMAL, 0, "", null)

    /**
     * بررسی اینکه آیا این کار مربوط به امروز است یا نه
     */
    fun isToday(): Boolean {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val taskDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(createdAt))
        return today == taskDate
    }

    /**
     * انتقال کار به فردا
     */
    fun moveToTomorrow(): Task {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, 1)

        return this.copy(
            id = "task_${System.currentTimeMillis()}", // ID جدید
            createdAt = calendar.timeInMillis,
            movedFromDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
            isCompleted = false,
            completionDate = null
        )
    }

    /**
     * علامت‌گذاری کار به عنوان انجام شده
     */
    fun markAsCompleted(): Task {
        return this.copy(
            isCompleted = true,
            completionDate = System.currentTimeMillis()
        )
    }

    /**
     * علامت‌گذاری کار به عنوان ناتمام
     */
    fun markAsIncomplete(): Task {
        return this.copy(
            isCompleted = false,
            completionDate = null
        )
    }

    /**
     * بازگشت رنگ مناسب بر اساس اولویت
     */
    fun getPriorityColor(): String {
        return priority.getColor()
    }

    /**
     * بازگشت ایموجی مناسب بر اساس وضعیت انجام
     */
    fun getStatusEmoji(): String {
        return if (isCompleted) {
            "✅"
        } else {
            when (priority) {
                TaskPriority.URGENT -> "🚨"
                TaskPriority.HIGH -> "⚠️"
                TaskPriority.NORMAL -> "📋"
            }
        }
    }

    /**
     * بازگشت رشته فرمت شده تاریخ ایجاد (نام دیگر)
     */
    fun getCreatedDateText(): String {
        return getFormattedCreatedDate()
    }

    /**
     * بازگشت رشته فرمت شده تاریخ ایجاد
     */
    fun getFormattedCreatedDate(): String {
        val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return formatter.format(Date(createdAt))
    }

    /**
     * بازگشت رشته فرمت شده تاریخ تکمیل (اگر وجود داشته باشد)
     */
    fun getFormattedCompletionDate(): String? {
        return completionDate?.let { date ->
            val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            formatter.format(Date(date))
        }
    }

    /**
     * بررسی اینکه آیا کار معوقه است یا نه
     */
    fun isOverdue(): Boolean {
        if (isCompleted || dueDate == null) return false
        return System.currentTimeMillis() > dueDate!!
    }

    /**
     * محاسبه تعداد روزهای باقی‌مانده تا سررسید
     */
    fun getDaysUntilDue(): Int? {
        if (dueDate == null) return null

        val currentTime = System.currentTimeMillis()
        val timeDiff = dueDate!! - currentTime

        return (timeDiff / (1000 * 60 * 60 * 24)).toInt()
    }

    /**
     * تغییر اولویت کار
     */
    fun changePriority(newPriority: TaskPriority): Task {
        return this.copy(priority = newPriority)
    }

    /**
     * اضافه کردن یا ویرایش یادداشت
     */
    fun updateNotes(newNotes: String): Task {
        return this.copy(notes = newNotes)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Task
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "Task(id='$id', title='$title', isCompleted=$isCompleted, timePeriod=$timePeriod)"
    }
}