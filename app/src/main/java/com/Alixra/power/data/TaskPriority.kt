package com.Alixra.power.data

import com.google.gson.annotations.SerializedName

/**
 * انواع اولویت کارها
 *
 * برای دسته‌بندی و اولویت‌بندی کارها استفاده می‌شود
 */
enum class TaskPriority {
    /** اولویت عادی - برای کارهای روتین */
    @SerializedName("normal")
    NORMAL,

    /** اولویت بالا - برای کارهای مهم */
    @SerializedName("high")
    HIGH,

    /** اولویت فوری - برای کارهای ضروری */
    @SerializedName("urgent")
    URGENT;

    /**
     * بازگشت نام فارسی اولویت
     */
    fun getDisplayName(): String {
        return when (this) {
            NORMAL -> "عادی"
            HIGH -> "مهم"
            URGENT -> "خیلی مهم"
        }
    }

    /**
     * بازگشت رنگ مناسب برای هر اولویت
     */
    fun getColor(): String = when (this) {
        NORMAL -> "#2196F3"
        HIGH -> "#FF9800"
        URGENT -> "#F44336"
    }

    /**
     * بازگشت ایموجی مناسب برای هر اولویت
     */
    fun getEmoji(): String {
        return when (this) {
            NORMAL -> "📋"
            HIGH -> "⚠️"
            URGENT -> "🚨"
        }
    }
}