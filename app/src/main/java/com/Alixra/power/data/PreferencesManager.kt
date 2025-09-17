package com.Alixra.power.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

class PreferencesManager(context: Context) {

    // ایجاد یک فایل private برای ذخیره سازی اطلاعات
    private val prefs = context.getSharedPreferences("SmartAlarmPrefs", Context.MODE_PRIVATE)
    private val editor = prefs.edit()
    private val gson = Gson()

    // یک کلید منحصر به فرد برای هر داده تعریف می کنیم
    companion object {
        // کلیدهای قدیمی (جملات و آلارم)
        private const val QUOTES_LIST_KEY = "QUOTES_LIST"
        private const val MORNING_ALARM_TIME_KEY = "MORNING_ALARM_TIME"
        private const val EVENING_ALARM_TIME_KEY = "EVENING_ALARM_TIME"
        private const val DAILY_RATINGS_KEY = "DAILY_RATINGS"
        private const val LAST_SUCCESS_DATE_KEY = "LAST_SUCCESS_DATE"
        private const val SUCCESS_COUNT_KEY = "SUCCESS_COUNT"
        private const val AVERAGE_RATING_KEY = "AVERAGE_RATING"
        private const val TOTAL_RATING_DAYS_KEY = "TOTAL_RATING_DAYS"

        // کلیدهای جدید (Task Management)
        private const val TASK_CATEGORIES_KEY = "TASK_CATEGORIES"
        private const val TASKS_KEY = "TASKS"
        private const val CATEGORY_RATINGS_KEY = "CATEGORY_RATINGS"
        private const val EVENING_STEP_KEY = "EVENING_STEP" // مرحله فعلی آلارم شبانه
        
        // کلیدهای جدید برای وضعیت فعال/غیرفعال آلارم‌ها
        private const val MORNING_ALARM_ENABLED_KEY = "MORNING_ALARM_ENABLED"
        private const val EVENING_ALARM_ENABLED_KEY = "EVENING_ALARM_ENABLED"
        
        // کلیدهای جدید برای backup/restore
        private const val ALARM_HOUR_KEY = "ALARM_HOUR"
        private const val ALARM_MINUTE_KEY = "ALARM_MINUTE"
        private const val SELECTED_DAYS_KEY = "SELECTED_DAYS"
        private const val EVENING_SELECTED_DAYS_KEY = "EVENING_SELECTED_DAYS"
        private const val VIBRATION_ENABLED_KEY = "VIBRATION_ENABLED"
        private const val ALARM_SOUND_URI_KEY = "ALARM_SOUND_URI"
        private const val MOTIVATIONAL_TEXTS_KEY = "MOTIVATIONAL_TEXTS"
        private const val EVENING_HOUR_KEY = "EVENING_HOUR"
        private const val EVENING_MINUTE_KEY = "EVENING_MINUTE"
        private const val DARK_MODE_KEY = "DARK_MODE"
        private const val LANGUAGE_KEY = "LANGUAGE"
        private const val NOTIFICATIONS_ENABLED_KEY = "NOTIFICATIONS_ENABLED"
        private const val AUTO_BACKUP_ENABLED_KEY = "AUTO_BACKUP_ENABLED"
        private const val QUOTES_INITIALIZED_KEY = "QUOTES_INITIALIZED"
        private const val CATEGORIES_INITIALIZED_KEY = "CATEGORIES_INITIALIZED"
        
        // کلیدهای مربوط به کاربر
        private const val USER_EMAIL_KEY = "USER_EMAIL"
        private const val USER_LOGGED_IN_KEY = "USER_LOGGED_IN"
    }

    // --- توابع مربوط به جملات انگیزشی (قدیمی) ---
    fun saveQuotes(quotes: List<String>) {
        val json = gson.toJson(quotes)
        editor.putString(QUOTES_LIST_KEY, json)
        editor.putBoolean(QUOTES_INITIALIZED_KEY, true)
        editor.apply()
    }

    fun getQuotes(): List<String> {
        val json = prefs.getString(QUOTES_LIST_KEY, null)
        return if (json != null) {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(json, type)
        } else {
            emptyList()
        }
    }

    fun areQuotesInitialized(): Boolean {
        return prefs.getBoolean(QUOTES_INITIALIZED_KEY, false)
    }

    fun areCategoriesInitialized(): Boolean {
        return prefs.getBoolean(CATEGORIES_INITIALIZED_KEY, false)
    }

    // --- توابع مربوط به زمان زنگ‌ها (قدیمی) ---
    fun saveMorningAlarmTime(time: String) {
        editor.putString(MORNING_ALARM_TIME_KEY, time)
        editor.apply()
    }

    fun getMorningAlarmTime(): String {
        return prefs.getString(MORNING_ALARM_TIME_KEY, "") ?: ""
    }

    fun saveEveningAlarmTime(time: String) {
        editor.putString(EVENING_ALARM_TIME_KEY, time)
        editor.apply()
    }

    fun getEveningAlarmTime(): String {
        return prefs.getString(EVENING_ALARM_TIME_KEY, "") ?: ""
    }

    // --- توابع مربوط به امتیازدهی شبانه (قدیمی) ---
    fun saveDailyRating(date: String, rating: Int) {
        val allRatings = getAllRatings().toMutableMap()
        allRatings[date] = rating
        val json = gson.toJson(allRatings)
        editor.putString(DAILY_RATINGS_KEY, json)
        editor.apply()
    }

    fun getAllRatings(): Map<String, Int> {
        val json = prefs.getString(DAILY_RATINGS_KEY, null)
        return if (json != null) {
            val type = object : TypeToken<Map<String, Int>>() {}.type
            gson.fromJson(json, type)
        } else {
            emptyMap()
        }
    }

    // --- توابع مربوط به آمار (قدیمی) ---
    fun saveLastSuccessDate(date: String) {
        editor.putString(LAST_SUCCESS_DATE_KEY, date)
        editor.apply()
    }

    fun getLastSuccessDate(): String {
        return prefs.getString(LAST_SUCCESS_DATE_KEY, "") ?: ""
    }

    fun saveSuccessCount(count: Int) {
        editor.putInt(SUCCESS_COUNT_KEY, count)
        editor.apply()
    }

    fun getSuccessCount(): Int {
        return prefs.getInt(SUCCESS_COUNT_KEY, 0)
    }

    fun saveAverageRating(average: Double) {
        editor.putFloat(AVERAGE_RATING_KEY, average.toFloat())
        editor.apply()
    }

    fun getAverageRating(): Double {
        return prefs.getFloat(AVERAGE_RATING_KEY, 0f).toDouble()
    }

    fun saveTotalRatingDays(count: Int) {
        editor.putInt(TOTAL_RATING_DAYS_KEY, count)
        editor.apply()
    }

    fun getTotalRatingDays(): Int {
        return prefs.getInt(TOTAL_RATING_DAYS_KEY, 0)
    }

    // --- توابع جدید مربوط به Task Categories ---
    fun saveTaskCategory(category: TaskCategory) {
        val categories = getTaskCategories().toMutableList()
        val existingIndex = categories.indexOfFirst { it.id == category.id }

        if (existingIndex != -1) {
            categories[existingIndex] = category
        } else {
            categories.add(category)
        }

        val json = gson.toJson(categories)
        editor.putString(TASK_CATEGORIES_KEY, json)
        editor.putBoolean(CATEGORIES_INITIALIZED_KEY, true)
        editor.apply()
    }

    fun getTaskCategories(): List<TaskCategory> {
        val json = prefs.getString(TASK_CATEGORIES_KEY, null)
        return if (json != null) {
            val type = object : TypeToken<List<TaskCategory>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } else {
            emptyList()
        }
    }

    fun deleteTaskCategory(categoryId: String) {
        val categories = getTaskCategories().toMutableList()
        categories.removeAll { it.id == categoryId }

        val json = gson.toJson(categories)
        editor.putString(TASK_CATEGORIES_KEY, json)
        editor.putBoolean(CATEGORIES_INITIALIZED_KEY, true)
        editor.apply()

        // حذف تمام کارهای مربوط به این بخش
        deleteTasksInCategory(categoryId)
    }

    fun getTaskCategory(categoryId: String): TaskCategory? {
        return getTaskCategories().find { it.id == categoryId }
    }

    // --- توابع جدید مربوط به Tasks ---
    fun saveTask(task: Task) {
        val tasks = getAllTasks().toMutableList()
        val existingIndex = tasks.indexOfFirst { it.id == task.id }

        if (existingIndex != -1) {
            tasks[existingIndex] = task
        } else {
            tasks.add(task)
        }

        val json = gson.toJson(tasks)
        editor.putString(TASKS_KEY, json)
        editor.apply()
    }

    fun getAllTasks(): List<Task> {
        val json = prefs.getString(TASKS_KEY, null)
        return if (json != null) {
            val type = object : TypeToken<List<Task>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } else {
            emptyList()
        }
    }

    fun getTask(taskId: String): Task? {
        return getAllTasks().find { it.id == taskId }
    }

    fun deleteTask(taskId: String) {
        val tasks = getAllTasks().toMutableList()
        tasks.removeAll { it.id == taskId }

        val json = gson.toJson(tasks)
        editor.putString(TASKS_KEY, json)
        editor.apply()
    }

    fun deleteTasksInCategory(categoryId: String) {
        val tasks = getAllTasks().toMutableList()
        tasks.removeAll { it.categoryId == categoryId }

        val json = gson.toJson(tasks)
        editor.putString(TASKS_KEY, json)
        editor.apply()
    }

    fun getTasksForPeriod(categoryId: String, timePeriod: TimePeriod): List<Task> {
        return getAllTasks().filter { task ->
            task.categoryId == categoryId &&
                    task.timePeriod == timePeriod &&
                    timePeriod.isDateInPeriod(task.createdAt)
        }
    }

    fun getTodayTasks(): List<Task> {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        return getAllTasks().filter { task ->
            task.timePeriod == TimePeriod.TODAY &&
                    task.isToday()
        }
    }

    fun getIncompleteTodayTasks(): List<Task> {
        return getTodayTasks().filter { !it.isCompleted }
    }

    fun moveTaskToTomorrow(task: Task): Task {
        val newTask = task.moveToTomorrow()
        saveTask(newTask)
        return newTask
    }

    // --- توابع جدید مربوط به امتیازدهی بخش‌ها ---
    fun saveCategoryRating(date: String, categoryId: String, rating: Int) {
        val allRatings = getCategoryRatings().toMutableMap()

        // اطمینان از وجود Map برای تاریخ مشخص
        val dayRatings = allRatings.getOrPut(date) { mutableMapOf() }.toMutableMap()
        dayRatings[categoryId] = rating
        allRatings[date] = dayRatings

        val json = gson.toJson(allRatings)
        editor.putString(CATEGORY_RATINGS_KEY, json)
        editor.apply()
    }

    fun getCategoryRatings(): Map<String, Map<String, Int>> {
        val json = prefs.getString(CATEGORY_RATINGS_KEY, null)
        return if (json != null) {
            val type = object : TypeToken<Map<String, Map<String, Int>>>() {}.type
            gson.fromJson(json, type) ?: emptyMap()
        } else {
            emptyMap()
        }
    }

    fun getCategoryRating(date: String, categoryId: String): Int {
        return getCategoryRatings()[date]?.get(categoryId) ?: 0
    }

    fun getTodayCategoryRatings(): Map<String, Int> {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        return getCategoryRatings()[today] ?: emptyMap()
    }

    // --- توابع جدید مربوط به مدیریت مراحل آلارم شبانه ---

    /**
     * ذخیره مرحله فعلی آلارم شبانه (1، 2، یا 3)
     * @param step مرحله فعلی (1=امتیاز کلی، 2=امتیاز بخش‌ها، 3=کارهای امروز)
     */
    fun saveEveningStep(step: Int) {
        editor.putInt(EVENING_STEP_KEY, step)
        editor.apply()
    }

    /**
     * دریافت مرحله فعلی آلارم شبانه
     * @return مرحله فعلی (پیش‌فرض: 1)
     */
    fun getEveningStep(): Int {
        return prefs.getInt(EVENING_STEP_KEY, 1)
    }

    /**
     * بازنشانی مرحله آلارم شبانه به حالت اول
     * معمولاً پس از تکمیل فرآیند ارزیابی شبانه استفاده می‌شود
     */
    fun resetEveningStep() {
        saveEveningStep(1)
    }

    // --- توابع کمکی برای آمار و گزارش ---

    /**
     * دریافت آمار کلی عملکرد کاربر
     * @return Map شامل اطلاعات آماری
     */
    fun getOverallStats(): Map<String, Any> {
        val allRatings = getAllRatings()
        val categoryRatings = getCategoryRatings()
        val allTasks = getAllTasks()

        val stats = mutableMapOf<String, Any>()

        // آمار امتیازات روزانه
        stats["total_rating_days"] = getTotalRatingDays()
        stats["average_rating"] = getAverageRating()
        stats["success_count"] = getSuccessCount()
        stats["last_success_date"] = getLastSuccessDate()

        // آمار کارها
        stats["total_tasks"] = allTasks.size
        stats["completed_tasks"] = allTasks.count { it.isCompleted }
        stats["pending_tasks"] = allTasks.count { !it.isCompleted }

        // آمار امروز
        val todayTasks = getTodayTasks()
        stats["today_total_tasks"] = todayTasks.size
        stats["today_completed_tasks"] = todayTasks.count { it.isCompleted }
        stats["today_pending_tasks"] = todayTasks.count { !it.isCompleted }

        return stats
    }

    /**
     * پاک کردن تمام داده‌ها (برای تست یا ریست کامل)
     * ⚠️ خطرناک: تمام اطلاعات کاربر حذف می‌شود
     */
    fun clearAllData() {
        editor.clear()
        editor.apply()
    }

    /**
     * پشتیبان‌گیری از داده‌ها به صورت JSON
     * @return JSON string شامل تمام داده‌ها
     */
    fun exportDataAsJson(): String {
        val allData = mutableMapOf<String, Any>()

        allData["quotes"] = getQuotes()
        allData["morning_alarm_time"] = getMorningAlarmTime()
        allData["evening_alarm_time"] = getEveningAlarmTime()
        allData["daily_ratings"] = getAllRatings()
        allData["task_categories"] = getTaskCategories()
        allData["tasks"] = getAllTasks()
        allData["category_ratings"] = getCategoryRatings()
        allData["evening_step"] = getEveningStep()
        allData["stats"] = getOverallStats()

        return gson.toJson(allData)
    }

    /**
     * بازگردانی داده‌ها از JSON
     * @param jsonData JSON string داده‌ها
     * @return true اگر موفق بود، false در غیر این صورت
     */
    fun importDataFromJson(jsonData: String): Boolean {
        return try {
            val type = object : TypeToken<Map<String, Any>>() {}.type
            val data: Map<String, Any> = gson.fromJson(jsonData, type)

            // بازگردانی داده‌ها...
            // این قسمت به تفصیل پیاده‌سازی می‌شود در صورت نیاز

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // --- توابع مورد نیاز StatsActivity ---

    /**
     * دریافت آمار کلی کارها برای نمایش در صفحه آمار
     * @return Map شامل آمار کارها
     */
    fun getTasksStats(): Map<String, Any> {
        val allTasks = getAllTasks()
        val todayTasks = getTodayTasks()
        val completedToday = todayTasks.count { it.isCompleted }
        val completedTotal = allTasks.count { it.isCompleted }

        val todayProgress = if (todayTasks.isNotEmpty()) {
            (completedToday * 100 / todayTasks.size)
        } else 0

        val totalProgress = if (allTasks.isNotEmpty()) {
            (completedTotal * 100 / allTasks.size)
        } else 0

        return mapOf(
            "totalTasks" to allTasks.size,
            "completedTotal" to completedTotal,
            "todayTasks" to todayTasks.size,
            "completedToday" to completedToday,
            "todayProgress" to todayProgress,
            "totalProgress" to totalProgress,
            "pendingTasks" to (allTasks.size - completedTotal),
            "todayPending" to (todayTasks.size - completedToday)
        )
    }

    /**
     * دریافت لیست بخش‌ها همراه با آمار هر بخش
     * @return List از Pair شامل TaskCategory و آمار مربوطه
     */
    fun getCategoriesWithStats(): List<Pair<TaskCategory, Map<String, Int>>> {
        val categories = getTaskCategories()
        return categories.map { category ->
            val categoryTasks = getAllTasks().filter { it.categoryId == category.id }
            val todayCategoryTasks = getTodayTasks().filter { it.categoryId == category.id }

            val totalTasks = categoryTasks.size
            val completedTasks = categoryTasks.count { it.isCompleted }
            val todayTotal = todayCategoryTasks.size
            val todayCompleted = todayCategoryTasks.count { it.isCompleted }

            val totalProgress = if (totalTasks > 0) {
                (completedTasks * 100 / totalTasks)
            } else 0

            val todayProgress = if (todayTotal > 0) {
                (todayCompleted * 100 / todayTotal)
            } else 0

            val stats = mapOf(
                "total" to totalTasks,
                "completed" to completedTasks,
                "todayTotal" to todayTotal,
                "todayCompleted" to todayCompleted,
                "progress" to totalProgress,
                "todayProgress" to todayProgress,
                "pending" to (totalTasks - completedTasks)
            )

            category to stats
        }
    }

    /**
     * پاک کردن تنها داده‌های مربوط به کارها و بخش‌ها
     * امتیازات روزانه و سایر داده‌ها حفظ می‌شوند
     */
    fun clearTaskData() {
        editor.remove(TASK_CATEGORIES_KEY)
        editor.remove(TASKS_KEY)
        editor.remove(CATEGORY_RATINGS_KEY)
        editor.apply()

        // بازنشانی مرحله آلارم شبانه
        resetEveningStep()
    }

    /**
     * بررسی سلامت داده‌ها و تعمیر خودکار
     * @return لیست مشکلات برطرف شده
     */
    fun validateAndRepairData(): List<String> {
        val issues = mutableListOf<String>()

        try {
            // بررسی سلامت کارها
            val tasks = getAllTasks()
            val validTasks = tasks.filter { task ->
                task.id.isNotEmpty() && task.title.isNotEmpty()
            }

            if (validTasks.size != tasks.size) {
                // حذف کارهای نامعتبر
                val json = gson.toJson(validTasks)
                editor.putString(TASKS_KEY, json)
                editor.apply()
                issues.add("${tasks.size - validTasks.size} کار نامعتبر حذف شد")
            }

            // بررسی سلامت دسته‌بندی‌ها
            val categories = getTaskCategories()
            val validCategories = categories.filter { category ->
                category.id.isNotEmpty() && category.name.isNotEmpty()
            }

            if (validCategories.size != categories.size) {
                // حذف دسته‌بندی‌های نامعتبر
                val json = gson.toJson(validCategories)
                editor.putString(TASK_CATEGORIES_KEY, json)
                editor.apply()
                issues.add("${categories.size - validCategories.size} دسته‌بندی نامعتبر حذف شد")
            }

            // بررسی مرحله آلارم شبانه
            val eveningStep = getEveningStep()
            if (eveningStep < 1 || eveningStep > 3) {
                resetEveningStep()
                issues.add("مرحله آلارم شبانه به حالت اول بازنشانی شد")
            }

            // بررسی یکپارچگی داده‌ها - حذف کارهای بدون بخش
            val validCategoryIds = validCategories.map { it.id }.toSet()
            val tasksWithValidCategories = validTasks.filter { task ->
                validCategoryIds.contains(task.categoryId) || task.categoryId.isEmpty()
            }

            if (tasksWithValidCategories.size != validTasks.size) {
                val json = gson.toJson(tasksWithValidCategories)
                editor.putString(TASKS_KEY, json)
                editor.apply()
                issues.add("${validTasks.size - tasksWithValidCategories.size} کار بدون بخش معتبر حذف شد")
            }

            // بررسی امتیازات بخش‌ها
            val categoryRatings = getCategoryRatings()
            val cleanedRatings = categoryRatings.mapValues { (date, ratings) ->
                ratings.filterKeys { categoryId ->
                    validCategoryIds.contains(categoryId)
                }
            }.filterValues { it.isNotEmpty() }

            if (cleanedRatings.size != categoryRatings.size) {
                val json = gson.toJson(cleanedRatings)
                editor.putString(CATEGORY_RATINGS_KEY, json)
                editor.apply()
                issues.add("امتیازات بخش‌های نامعتبر پاک شد")
            }

        } catch (e: Exception) {
            e.printStackTrace()
            issues.add("خطا در بررسی داده‌ها: ${e.message}")
        }

        return issues
    }

    /**
     * ایجاد بخش‌های پیش‌فرض در صورت عدم وجود
     * @return تعداد بخش‌های ایجاد شده
     */
    fun createDefaultCategoriesIfNeeded(): Int {
        val existingCategories = getTaskCategories()
        if (existingCategories.isNotEmpty()) {
            return 0
        }

        val defaultCategories = listOf(
            TaskCategory(
                id = "health_${System.currentTimeMillis()}",
                name = "سلامتی",
                color = "#4CAF50",
                icon = "🏃",
                isDefault = true,
                order = 1
            ),
            TaskCategory(
                id = "work_${System.currentTimeMillis() + 1}",
                name = "کار",
                color = "#2196F3",
                icon = "💼",
                isDefault = true,
                order = 2
            ),
            TaskCategory(
                id = "education_${System.currentTimeMillis() + 2}",
                name = "آموزش",
                color = "#FF9800",
                icon = "📚",
                isDefault = true,
                order = 3
            ),
            TaskCategory(
                id = "personal_${System.currentTimeMillis() + 3}",
                name = "شخصی",
                color = "#9C27B0",
                icon = "🏠",
                isDefault = true,
                order = 4
            )
        )

        defaultCategories.forEach { category ->
            saveTaskCategory(category)
        }

        return defaultCategories.size
    }
    
    // --- توابع جدید برای مدیریت وضعیت آلارم‌ها ---
    
    /**
     * ذخیره وضعیت فعال/غیرفعال آلارم صبح
     * @param enabled true برای فعال، false برای غیرفعال
     */
    fun setMorningAlarmEnabled(enabled: Boolean) {
        editor.putBoolean(MORNING_ALARM_ENABLED_KEY, enabled)
        editor.apply()
    }
    
    /**
     * دریافت وضعیت آلارم صبح
     * @return true اگر فعال باشد، false اگر غیرفعال باشد
     */
    fun isMorningAlarmEnabled(): Boolean {
        return prefs.getBoolean(MORNING_ALARM_ENABLED_KEY, false)
    }
    
    /**
     * ذخیره وضعیت فعال/غیرفعال یادآور شب
     * @param enabled true برای فعال، false برای غیرفعال
     */
    fun setEveningAlarmEnabled(enabled: Boolean) {
        editor.putBoolean(EVENING_ALARM_ENABLED_KEY, enabled)
        editor.apply()
    }
    
    /**
     * دریافت وضعیت یادآور شب
     * @return true اگر فعال باشد، false اگر غیرفعال باشد
     */
    fun isEveningAlarmEnabled(): Boolean {
        return prefs.getBoolean(EVENING_ALARM_ENABLED_KEY, false)
    }
    
    /**
     * تابع کمکی برای بررسی وضعیت هر دو آلارم
     * @return Pair<وضعیت صبح, وضعیت شب>
     */
    fun getAlarmsStatus(): Pair<Boolean, Boolean> {
        return Pair(isMorningAlarmEnabled(), isEveningAlarmEnabled())
    }
    
    /**
     * غیرفعال کردن هر دو آلارم (برای موارد اورژانسی)
     * زمان‌های تنظیم شده حفظ می‌شوند
     */
    fun disableAllAlarms() {
        setMorningAlarmEnabled(false)
        setEveningAlarmEnabled(false)
    }
    
    /**
     * فعال کردن هر دو آلارم (فقط اگر زمان‌شان تنظیم شده باشد)
     */
    fun enableAllAlarmsIfTimeSet() {
        if (getMorningAlarmTime().isNotEmpty()) {
            setMorningAlarmEnabled(true)
        }
        if (getEveningAlarmTime().isNotEmpty()) {
            setEveningAlarmEnabled(true)
        }
    }
    
    /**
     * بررسی اینکه آیا حداقل یکی از آلارم‌ها فعال است
     * @return true اگر آلارم صبح یا شب فعال باشد
     */
    fun hasActiveAlarms(): Boolean {
        return isMorningAlarmEnabled() || isEveningAlarmEnabled()
    }
    
    /**
     * تابع بررسی سازگاری نسخه‌های قدیمی
     * اگر زمان آلارم تنظیم شده ولی وضعیت آن مشخص نیست، فعال می‌کند
     */
    fun migrateOldAlarmSettings() {
        val morningTime = getMorningAlarmTime()
        val eveningTime = getEveningAlarmTime()
        
        // اگر زمان آلارم صبح تنظیم شده ولی وضعیت آن مشخص نیست
        if (morningTime.isNotEmpty() && !prefs.contains(MORNING_ALARM_ENABLED_KEY)) {
            setMorningAlarmEnabled(true)
        }
        
        // اگر زمان یادآور شب تنظیم شده ولی وضعیت آن مشخص نیست
        if (eveningTime.isNotEmpty() && !prefs.contains(EVENING_ALARM_ENABLED_KEY)) {
            setEveningAlarmEnabled(true)
        }
    }
    
    // === متدهای جدید برای Backup/Restore ===
    
    // --- تنظیمات آلارم صبح ---
    fun isAlarmEnabled(): Boolean = isMorningAlarmEnabled()
    
    fun getAlarmHour(): Int = prefs.getInt(ALARM_HOUR_KEY, 7)
    
    fun getAlarmMinute(): Int = prefs.getInt(ALARM_MINUTE_KEY, 0)
    
    fun setAlarmEnabled(enabled: Boolean) = setMorningAlarmEnabled(enabled)
    
    fun setAlarmTime(hour: Int, minute: Int) {
        editor.putInt(ALARM_HOUR_KEY, hour)
        editor.putInt(ALARM_MINUTE_KEY, minute)
        editor.apply()
    }
    
    // --- روزهای انتخاب شده ---
    fun getSelectedDays(): List<Int> {
        val json = prefs.getString(SELECTED_DAYS_KEY, null)
        return if (json != null) {
            val type = object : TypeToken<List<Int>>() {}.type
            gson.fromJson(json, type) ?: listOf(1, 2, 3, 4, 5, 6, 7)
        } else {
            listOf(1, 2, 3, 4, 5, 6, 7) // همه روزهای هفته
        }
    }
    
    fun setSelectedDays(days: List<Int>) {
        val json = gson.toJson(days)
        editor.putString(SELECTED_DAYS_KEY, json)
        editor.apply()
    }
    
    // --- روزهای انتخاب شده برای یادآور شب ---
    fun getEveningSelectedDays(): List<Int> {
        val json = prefs.getString(EVENING_SELECTED_DAYS_KEY, null)
        return if (json != null) {
            val type = object : TypeToken<List<Int>>() {}.type
            gson.fromJson(json, type) ?: listOf(1, 2, 3, 4, 5, 6, 7)
        } else {
            listOf(1, 2, 3, 4, 5, 6, 7) // همه روزهای هفته
        }
    }
    
    fun setEveningSelectedDays(days: List<Int>) {
        val json = gson.toJson(days)
        editor.putString(EVENING_SELECTED_DAYS_KEY, json)
        editor.apply()
    }
    
    // --- تنظیمات لرزش ---
    fun isVibrationEnabled(): Boolean = prefs.getBoolean(VIBRATION_ENABLED_KEY, true)
    
    fun setVibrationEnabled(enabled: Boolean) {
        editor.putBoolean(VIBRATION_ENABLED_KEY, enabled)
        editor.apply()
    }
    
    // --- صدای آلارم ---
    fun getAlarmSoundUri(): String? = prefs.getString(ALARM_SOUND_URI_KEY, null)
    
    fun setAlarmSoundUri(uri: String) {
        editor.putString(ALARM_SOUND_URI_KEY, uri)
        editor.apply()
    }
    
    // --- متن‌های انگیزشی ---
    fun getMotivationalTexts(): List<String> = getQuotes()
    
    fun setMotivationalTexts(texts: List<String>) = saveQuotes(texts)
    
    // --- تنظیمات آلارم شب ---
    fun isEveningEnabled(): Boolean = isEveningAlarmEnabled()
    
    fun getEveningHour(): Int = prefs.getInt(EVENING_HOUR_KEY, 21)
    
    fun getEveningMinute(): Int = prefs.getInt(EVENING_MINUTE_KEY, 0)
    
    fun setEveningEnabled(enabled: Boolean) = setEveningAlarmEnabled(enabled)
    
    fun setEveningTime(hour: Int, minute: Int) {
        editor.putInt(EVENING_HOUR_KEY, hour)
        editor.putInt(EVENING_MINUTE_KEY, minute)
        editor.apply()
    }
    
    // --- دسته‌بندی‌ها ---
    fun getAllTaskCategories(): List<TaskCategory> = getTaskCategories()
    
    fun getAllCategoryRatings(): Map<String, List<Int>> {
        val allRatings = getCategoryRatings()
        val result = mutableMapOf<String, List<Int>>()
        
        allRatings.forEach { (date, categoryRatings) ->
            categoryRatings.forEach { (categoryId, rating) ->
                val currentList = result[categoryId] ?: emptyList()
                result[categoryId] = currentList + rating
            }
        }
        
        return result
    }
    
    // --- تنظیمات اپلیکیشن ---
    fun isDarkMode(): Boolean = prefs.getBoolean(DARK_MODE_KEY, false)
    
    fun setDarkMode(enabled: Boolean) {
        editor.putBoolean(DARK_MODE_KEY, enabled)
        editor.apply()
    }
    
    fun getLanguage(): String = prefs.getString(LANGUAGE_KEY, "fa") ?: "fa"
    
    fun setLanguage(language: String) {
        editor.putString(LANGUAGE_KEY, language)
        editor.apply()
    }
    
    fun areNotificationsEnabled(): Boolean = prefs.getBoolean(NOTIFICATIONS_ENABLED_KEY, true)
    
    fun setNotificationsEnabled(enabled: Boolean) {
        editor.putBoolean(NOTIFICATIONS_ENABLED_KEY, enabled)
        editor.apply()
    }
    
    fun isAutoBackupEnabled(): Boolean = prefs.getBoolean(AUTO_BACKUP_ENABLED_KEY, false)
    
    fun setAutoBackupEnabled(enabled: Boolean) {
        editor.putBoolean(AUTO_BACKUP_ENABLED_KEY, enabled)
        editor.apply()
    }
    
    // === متدهای مربوط به کاربر ===
    
    /**
     * ذخیره ایمیل کاربر
     */
    fun saveUserEmail(email: String) {
        editor.putString(USER_EMAIL_KEY, email)
        editor.apply()
    }
    
    /**
     * دریافت ایمیل کاربر
     */
    fun getUserEmail(): String? {
        return prefs.getString(USER_EMAIL_KEY, null)
    }
    
    /**
     * تنظیم وضعیت ورود کاربر
     */
    fun setUserLoggedIn(isLoggedIn: Boolean) {
        editor.putBoolean(USER_LOGGED_IN_KEY, isLoggedIn)
        editor.apply()
    }
    
    /**
     * بررسی وضعیت ورود کاربر
     */
    fun isUserLoggedIn(): Boolean {
        return prefs.getBoolean(USER_LOGGED_IN_KEY, false)
    }
    
    /**
     * خروج کاربر از حساب
     */
    fun logoutUser() {
        editor.remove(USER_EMAIL_KEY)
        editor.putBoolean(USER_LOGGED_IN_KEY, false)
        editor.apply()
    }
    
    /**
     * دریافت نام کاربر از ایمیل
     */
    fun getUserDisplayName(): String {
        val email = getUserEmail()
        return if (email != null) {
            // استخراج نام از قسمت قبل از @ در ایمیل
            email.substringBefore("@").replaceFirstChar { 
                if (it.isLowerCase()) it.titlecase() else it.toString() 
            }
        } else {
            "کاربر"
        }
    }
}