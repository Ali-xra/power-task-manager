package com.Alixra.power.data

import com.google.gson.annotations.SerializedName
import java.text.SimpleDateFormat
import java.util.*

/**
 * انواع بازه‌های زمانی برای دسته‌بندی کارها
 */
enum class TimePeriod {
    @SerializedName("today")
    TODAY,

    @SerializedName("this_week")
    THIS_WEEK,

    @SerializedName("this_month")
    THIS_MONTH,

    @SerializedName("this_season")
    THIS_SEASON,

    @SerializedName("this_year")
    THIS_YEAR;

    /**
     * بازگشت نام فارسی بازه زمانی
     */
    fun getDisplayName(): String {
        return when (this) {
            TODAY -> "امروز"
            THIS_WEEK -> "این هفته"
            THIS_MONTH -> "این ماه"
            THIS_SEASON -> "این فصل"
            THIS_YEAR -> "امسال"
        }
    }

    /**
     * بازگشت ایموجی مناسب برای هر بازه
     */
    fun getEmoji(): String {
        return when (this) {
            TODAY -> "📅"
            THIS_WEEK -> "🗓️"
            THIS_MONTH -> "📆"
            THIS_SEASON -> "🍂"
            THIS_YEAR -> "📅"
        }
    }

    /**
     * بازگشت رنگ مناسب برای هر بازه
     */
    fun getColor(): String {
        return when (this) {
            TODAY -> "#1976D2"
            THIS_WEEK -> "#388E3C"
            THIS_MONTH -> "#F57C00"
            THIS_SEASON -> "#7B1FA2"
            THIS_YEAR -> "#D32F2F"
        }
    }

    /**
     * بررسی اینکه آیا تاریخ داده شده در این بازه زمانی قرار دارد یا نه
     */
    fun isDateInPeriod(timestamp: Long): Boolean {
        val calendar = Calendar.getInstance()
        val targetDate = Calendar.getInstance().apply { timeInMillis = timestamp }

        return when (this) {
            TODAY -> {
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val targetDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timestamp))
                today == targetDateStr
            }

            THIS_WEEK -> {
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                val weekStart = calendar.timeInMillis

                calendar.add(Calendar.WEEK_OF_YEAR, 1)
                calendar.add(Calendar.MILLISECOND, -1)
                val weekEnd = calendar.timeInMillis

                timestamp in weekStart..weekEnd
            }

            THIS_MONTH -> {
                calendar.get(Calendar.YEAR) == targetDate.get(Calendar.YEAR) &&
                        calendar.get(Calendar.MONTH) == targetDate.get(Calendar.MONTH)
            }

            THIS_SEASON -> {
                val currentSeason = getCurrentSeason()
                val targetSeason = getSeason(targetDate.get(Calendar.MONTH))

                calendar.get(Calendar.YEAR) == targetDate.get(Calendar.YEAR) &&
                        currentSeason == targetSeason
            }

            THIS_YEAR -> {
                calendar.get(Calendar.YEAR) == targetDate.get(Calendar.YEAR)
            }
        }
    }

    /**
     * بازگشت تاریخ شروع بازه
     */
    fun getStartDate(): Long {
        val calendar = Calendar.getInstance()

        return when (this) {
            TODAY -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.timeInMillis
            }

            THIS_WEEK -> {
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.timeInMillis
            }

            THIS_MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.timeInMillis
            }

            THIS_SEASON -> {
                val currentMonth = calendar.get(Calendar.MONTH)
                val seasonStartMonth = when (getCurrentSeason()) {
                    0 -> Calendar.MARCH      // بهار
                    1 -> Calendar.JUNE       // تابستان
                    2 -> Calendar.SEPTEMBER  // پاییز
                    else -> Calendar.DECEMBER // زمستان
                }
                calendar.set(Calendar.MONTH, seasonStartMonth)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.timeInMillis
            }

            THIS_YEAR -> {
                calendar.set(Calendar.MONTH, Calendar.JANUARY)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.timeInMillis
            }
        }
    }

    /**
     * بازگشت تاریخ پایان بازه
     */
    fun getEndDate(): Long {
        val calendar = Calendar.getInstance()

        return when (this) {
            TODAY -> {
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                calendar.timeInMillis
            }

            THIS_WEEK -> {
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.add(Calendar.WEEK_OF_YEAR, 1)
                calendar.add(Calendar.MILLISECOND, -1)
                calendar.timeInMillis
            }

            THIS_MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                calendar.timeInMillis
            }

            THIS_SEASON -> {
                val currentMonth = calendar.get(Calendar.MONTH)
                val seasonEndMonth = when (getCurrentSeason()) {
                    0 -> Calendar.MAY        // بهار
                    1 -> Calendar.AUGUST     // تابستان
                    2 -> Calendar.NOVEMBER   // پاییز
                    else -> Calendar.FEBRUARY // زمستان
                }
                calendar.set(Calendar.MONTH, seasonEndMonth)
                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                calendar.timeInMillis
            }

            THIS_YEAR -> {
                calendar.set(Calendar.MONTH, Calendar.DECEMBER)
                calendar.set(Calendar.DAY_OF_MONTH, 31)
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                calendar.timeInMillis
            }
        }
    }

    companion object {
        /**
         * بازگشت فصل فعلی (0: بهار، 1: تابستان، 2: پاییز، 3: زمستان)
         */
        private fun getCurrentSeason(): Int {
            val month = Calendar.getInstance().get(Calendar.MONTH)
            return getSeason(month)
        }

        /**
         * بازگشت فصل بر اساس ماه
         */
        private fun getSeason(month: Int): Int {
            return when (month) {
                Calendar.MARCH, Calendar.APRIL, Calendar.MAY -> 0        // بهار
                Calendar.JUNE, Calendar.JULY, Calendar.AUGUST -> 1       // تابستان
                Calendar.SEPTEMBER, Calendar.OCTOBER, Calendar.NOVEMBER -> 2  // پاییز
                else -> 3  // زمستان
            }
        }
    }
}