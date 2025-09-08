package com.Alixra.power.data

import com.google.gson.annotations.SerializedName

/**
 * مدل داده برای بخش‌های کاری (مثل سلامتی، کار، آموزش و ...)
 */
data class TaskCategory(
    @SerializedName("id")
    val id: String,

    @SerializedName("name")
    var name: String,

    @SerializedName("color")
    var color: String,

    @SerializedName("icon")
    var icon: String = "📌",

    @SerializedName("is_default")
    val isDefault: Boolean = false,

    @SerializedName("created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @SerializedName("order")
    var order: Int = 0
) {

    /**
     * سازنده خالی برای Gson
     */
    constructor() : this("", "", "", "📌", false, System.currentTimeMillis(), 0)

    /**
     * بازگشت تعداد کارهای مرتبط با این بخش
     */
    fun getTasksCountForPeriod(period: TimePeriod, prefsManager: PreferencesManager): Int {
        return prefsManager.getTasksForPeriod(id, period).size
    }

    /**
     * بازگشت تعداد کارهای انجام شده در این بخش
     */
    fun getCompletedTasksCountForPeriod(period: TimePeriod, prefsManager: PreferencesManager): Int {
        return prefsManager.getTasksForPeriod(id, period).count { it.isCompleted }
    }

    /**
     * محاسبه درصد پیشرفت برای یک بازه زمانی
     */
    fun getProgressPercentage(period: TimePeriod, prefsManager: PreferencesManager): Int {
        val totalTasks = getTasksCountForPeriod(period, prefsManager)
        if (totalTasks == 0) return 0

        val completedTasks = getCompletedTasksCountForPeriod(period, prefsManager)
        return ((completedTasks.toDouble() / totalTasks.toDouble()) * 100).toInt()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as TaskCategory
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "TaskCategory(id='$id', name='$name', color='$color')"
    }
}