package com.Alixra.power.ui

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.Alixra.power.R
import com.Alixra.power.data.PreferencesManager
import com.Alixra.power.databinding.ActivityAlarmsBinding
import com.Alixra.power.utils.AlarmUtils

class AlarmsActivity : BaseActivity() {

    private lateinit var binding: ActivityAlarmsBinding
    private lateinit var prefsManager: PreferencesManager
    private lateinit var quotesAdapter: ArrayAdapter<String>
    private val quotes = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlarmsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupPreferences()
        initViews()
        loadSavedData()
        setupClickListeners()
    }

    private fun setupPreferences() {
        prefsManager = PreferencesManager(this)
    }

    private fun initViews() {
        binding.morningTimePicker.setIs24HourView(true)
        binding.eveningTimePicker.setIs24HourView(true)

        // آداپتر سفارشی با متن تیره
        quotesAdapter = object : ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, quotes) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                val textView = view.findViewById<TextView>(android.R.id.text1)
                textView.setTextColor(Color.parseColor("#222222")) // متن تیره
                textView.textSize = 16f // سایز متن
                textView.setPadding(16, 16, 16, 16) // فاصله داخلی بیشتر
                textView.minHeight = 50 // حداقل ارتفاع برای هر آیتم
                textView.gravity = Gravity.CENTER_VERTICAL // متن در وسط عمودی
                return view
            }
        }
        binding.quotesListView.adapter = quotesAdapter
        
        // حل مشکل nested scrolling برای ListView درون ScrollView
        setupListViewScrolling()
        
        // تنظیم switch listeners
        setupSwitchListeners()
    }

    private fun loadSavedData() {
        // بارگذاری جملات
        val savedQuotes = prefsManager.getQuotes()
        if (prefsManager.areQuotesInitialized()) {
            // کاربر قبلاً با سیستم جملات کار کرده، حتی اگر الان خالی باشد
            quotes.clear()
            quotes.addAll(savedQuotes)
            quotesAdapter.notifyDataSetChanged()
        } else {
            // اولین بار است که اپ اجرا می‌شود، افزودن جملات پیش‌فرض
            addDefaultQuotes()
        }

        // بارگذاری زمان زنگ صبح
        val morningTime = prefsManager.getMorningAlarmTime()
        if (morningTime.isNotEmpty()) {
            val parts = morningTime.split(":")
            binding.morningTimePicker.hour = parts[0].toInt()
            binding.morningTimePicker.minute = parts[1].toInt()
        }

        // بارگذاری زمان یادآور شب
        val eveningTime = prefsManager.getEveningAlarmTime()
        if (eveningTime.isNotEmpty()) {
            val parts = eveningTime.split(":")
            binding.eveningTimePicker.hour = parts[0].toInt()
            binding.eveningTimePicker.minute = parts[1].toInt()
        }
        
        // بارگذاری روزهای انتخاب شده
        loadSelectedDays()
        loadEveningSelectedDays()
        
        // بارگذاری وضعیت آلارم‌ها و به‌روزرسانی نمایش
        updateAlarmStatusDisplay()
    }

    private fun addDefaultQuotes() {
        val defaultQuotes = listOf(
            getString(R.string.sample_quote_1),
            getString(R.string.sample_quote_2),
            getString(R.string.sample_quote_3)
        )
        quotes.addAll(defaultQuotes)
        quotesAdapter.notifyDataSetChanged()
        prefsManager.saveQuotes(quotes)
    }

    private fun setupClickListeners() {
        // دکمه بازگشت
        binding.backButton.setOnClickListener {
            finish()
        }

        // دکمه تنظیم زنگ صبح
        binding.setMorningAlarmBtn.setOnClickListener {
            if (quotes.isEmpty()) {
                showToast(getString(R.string.add_quote_first_message))
                return@setOnClickListener
            }

            val selectedDays = getSelectedDays()
            if (selectedDays.isEmpty()) {
                showToast(getString(R.string.select_at_least_one_day))
                return@setOnClickListener
            }

            val hour = binding.morningTimePicker.hour
            val minute = binding.morningTimePicker.minute
            
            // ذخیره روزهای انتخاب شده
            prefsManager.setSelectedDays(selectedDays)
            
            AlarmUtils.setMorningAlarmWithTime(this, hour, minute)
            updateAlarmStatusDisplay()
            
            val daysText = if (selectedDays.size == 7) getString(R.string.all_days_selected) else getString(R.string.days_selected_count, selectedDays.size)
            showToast(getString(R.string.morning_alarm_set_message, hour, minute) + " - $daysText")
        }

        // دکمه تنظیم یادآور شب
        binding.setEveningAlarmBtn.setOnClickListener {
            val selectedDays = getEveningSelectedDays()
            if (selectedDays.isEmpty()) {
                showToast(getString(R.string.select_at_least_one_day))
                return@setOnClickListener
            }

            val hour = binding.eveningTimePicker.hour
            val minute = binding.eveningTimePicker.minute
            
            // ذخیره روزهای انتخاب شده برای یادآور شب
            prefsManager.setEveningSelectedDays(selectedDays)
            
            AlarmUtils.setEveningAlarmWithTime(this, hour, minute)
            updateAlarmStatusDisplay()
            
            val daysText = if (selectedDays.size == 7) getString(R.string.all_days_selected) else getString(R.string.days_selected_count, selectedDays.size)
            showToast(getString(R.string.evening_reminder_set_message, hour, minute) + " - $daysText")
        }

        // دکمه اضافه کردن جمله
        binding.addQuoteBtn.setOnClickListener {
            val newQuote = binding.quoteEditText.text.toString().trim()
            if (newQuote.isNotEmpty()) {
                quotes.add(newQuote)
                quotesAdapter.notifyDataSetChanged()
                prefsManager.saveQuotes(quotes)
                binding.quoteEditText.text.clear()
                showToast(getString(R.string.quote_added_message))
            } else {
                showToast(getString(R.string.enter_quote_message))
            }
        }

        // حذف جمله با کلیک طولانی
        binding.quotesListView.setOnItemLongClickListener { _, _, position, _ ->
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.delete_quote_title))
                .setMessage(getString(R.string.delete_quote_message))
                .setPositiveButton(getString(R.string.yes_button)) { _, _ ->
                    quotes.removeAt(position)
                    quotesAdapter.notifyDataSetChanged()
                    prefsManager.saveQuotes(quotes)
                    showToast(getString(R.string.quote_deleted_message))
                }
                .setNegativeButton(getString(R.string.no_button), null)
                .show()
            true
        }
    }

    private fun setupListViewScrolling() {
        // حل مشکل nested scrolling: وقتی کاربر روی ListView تاچ می‌کند،
        // از ScrollView والدین می‌خواهیم که اجازه scroll کردن را به ListView بدهد
        binding.quotesListView.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    // درخواست از والدین برای عدم دخالت در touch events هنگام scroll
                    view.parent.requestDisallowInterceptTouchEvent(true)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    // اجازه دادن به والدین برای گرفتن دوباره کنترل
                    view.parent.requestDisallowInterceptTouchEvent(false)
                }
            }
            false // اجازه ادامه پردازش event به ListView
        }
    }
    
    private fun setupSwitchListeners() {
        // listener برای switch زنگ صبح
        binding.morningAlarmSwitch.setOnCheckedChangeListener { _, isChecked ->
            AlarmUtils.toggleMorningAlarm(this, isChecked)
            updateAlarmStatusDisplay()
            
            val message = if (isChecked) {
                val hour = binding.morningTimePicker.hour
                val minute = binding.morningTimePicker.minute
                getString(R.string.morning_alarm_set_message, hour, minute)
            } else {
                getString(R.string.morning_alarm_disabled_message)
            }
            showToast(message)
        }
        
        // listener برای switch یادآور شب
        binding.eveningAlarmSwitch.setOnCheckedChangeListener { _, isChecked ->
            AlarmUtils.toggleEveningAlarm(this, isChecked)
            updateAlarmStatusDisplay()
            
            val message = if (isChecked) {
                val hour = binding.eveningTimePicker.hour
                val minute = binding.eveningTimePicker.minute
                getString(R.string.evening_reminder_set_message, hour, minute)
            } else {
                getString(R.string.evening_alarm_disabled_message)
            }
            showToast(message)
        }
    }
    
    private fun updateAlarmStatusDisplay() {
        // به‌روزرسانی وضعیت زنگ صبح
        val (isMorningEnabled, morningTime) = AlarmUtils.getMorningAlarmStatus(this)
        binding.morningAlarmSwitch.isChecked = isMorningEnabled
        
        if (isMorningEnabled) {
            binding.morningAlarmStatus.text = "${getString(R.string.alarm_status_active)} - $morningTime"
            binding.morningAlarmStatus.setTextColor(Color.parseColor("#4CAF50")) // سبز
        } else {
            binding.morningAlarmStatus.text = getString(R.string.alarm_status_inactive)
            binding.morningAlarmStatus.setTextColor(Color.parseColor("#FF5722")) // قرمز
        }
        
        // به‌روزرسانی وضعیت یادآور شب
        val (isEveningEnabled, eveningTime) = AlarmUtils.getEveningAlarmStatus(this)
        binding.eveningAlarmSwitch.isChecked = isEveningEnabled
        
        if (isEveningEnabled) {
            binding.eveningAlarmStatus.text = "${getString(R.string.alarm_status_active)} - $eveningTime"
            binding.eveningAlarmStatus.setTextColor(Color.parseColor("#6A1B9A")) // بنفش
        } else {
            binding.eveningAlarmStatus.text = getString(R.string.alarm_status_inactive)
            binding.eveningAlarmStatus.setTextColor(Color.parseColor("#FF5722")) // قرمز
        }
    }

    private fun getSelectedDays(): List<Int> {
        val selectedDays = mutableListOf<Int>()
        
        // Sunday = 1, Monday = 2, ..., Saturday = 7 (Calendar constants)
        if (binding.morningCheckboxSunday.isChecked) selectedDays.add(java.util.Calendar.SUNDAY)
        if (binding.morningCheckboxMonday.isChecked) selectedDays.add(java.util.Calendar.MONDAY)
        if (binding.morningCheckboxTuesday.isChecked) selectedDays.add(java.util.Calendar.TUESDAY)
        if (binding.morningCheckboxWednesday.isChecked) selectedDays.add(java.util.Calendar.WEDNESDAY)
        if (binding.morningCheckboxThursday.isChecked) selectedDays.add(java.util.Calendar.THURSDAY)
        if (binding.morningCheckboxFriday.isChecked) selectedDays.add(java.util.Calendar.FRIDAY)
        if (binding.morningCheckboxSaturday.isChecked) selectedDays.add(java.util.Calendar.SATURDAY)
        
        return selectedDays
    }
    
    private fun loadSelectedDays() {
        val selectedDays = prefsManager.getSelectedDays()
        
        // اگر هیچ روزی ذخیره نشده، همه روزها را انتخاب کن (default behavior)
        if (selectedDays.isEmpty()) {
            binding.morningCheckboxSunday.isChecked = true
            binding.morningCheckboxMonday.isChecked = true
            binding.morningCheckboxTuesday.isChecked = true
            binding.morningCheckboxWednesday.isChecked = true
            binding.morningCheckboxThursday.isChecked = true
            binding.morningCheckboxFriday.isChecked = true
            binding.morningCheckboxSaturday.isChecked = true
        } else {
            // تنظیم checkboxها بر اساس روزهای ذخیره شده
            binding.morningCheckboxSunday.isChecked = selectedDays.contains(java.util.Calendar.SUNDAY)
            binding.morningCheckboxMonday.isChecked = selectedDays.contains(java.util.Calendar.MONDAY)
            binding.morningCheckboxTuesday.isChecked = selectedDays.contains(java.util.Calendar.TUESDAY)
            binding.morningCheckboxWednesday.isChecked = selectedDays.contains(java.util.Calendar.WEDNESDAY)
            binding.morningCheckboxThursday.isChecked = selectedDays.contains(java.util.Calendar.THURSDAY)
            binding.morningCheckboxFriday.isChecked = selectedDays.contains(java.util.Calendar.FRIDAY)
            binding.morningCheckboxSaturday.isChecked = selectedDays.contains(java.util.Calendar.SATURDAY)
        }
    }
    
    private fun getEveningSelectedDays(): List<Int> {
        val selectedDays = mutableListOf<Int>()
        
        // Sunday = 1, Monday = 2, ..., Saturday = 7 (Calendar constants)
        if (binding.eveningCheckboxSunday.isChecked) selectedDays.add(java.util.Calendar.SUNDAY)
        if (binding.eveningCheckboxMonday.isChecked) selectedDays.add(java.util.Calendar.MONDAY)
        if (binding.eveningCheckboxTuesday.isChecked) selectedDays.add(java.util.Calendar.TUESDAY)
        if (binding.eveningCheckboxWednesday.isChecked) selectedDays.add(java.util.Calendar.WEDNESDAY)
        if (binding.eveningCheckboxThursday.isChecked) selectedDays.add(java.util.Calendar.THURSDAY)
        if (binding.eveningCheckboxFriday.isChecked) selectedDays.add(java.util.Calendar.FRIDAY)
        if (binding.eveningCheckboxSaturday.isChecked) selectedDays.add(java.util.Calendar.SATURDAY)
        
        return selectedDays
    }
    
    private fun loadEveningSelectedDays() {
        val selectedDays = prefsManager.getEveningSelectedDays()
        
        // اگر هیچ روزی ذخیره نشده، همه روزها را انتخاب کن (default behavior)
        if (selectedDays.isEmpty()) {
            binding.eveningCheckboxSunday.isChecked = true
            binding.eveningCheckboxMonday.isChecked = true
            binding.eveningCheckboxTuesday.isChecked = true
            binding.eveningCheckboxWednesday.isChecked = true
            binding.eveningCheckboxThursday.isChecked = true
            binding.eveningCheckboxFriday.isChecked = true
            binding.eveningCheckboxSaturday.isChecked = true
        } else {
            // تنظیم checkboxها بر اساس روزهای ذخیره شده
            binding.eveningCheckboxSunday.isChecked = selectedDays.contains(java.util.Calendar.SUNDAY)
            binding.eveningCheckboxMonday.isChecked = selectedDays.contains(java.util.Calendar.MONDAY)
            binding.eveningCheckboxTuesday.isChecked = selectedDays.contains(java.util.Calendar.TUESDAY)
            binding.eveningCheckboxWednesday.isChecked = selectedDays.contains(java.util.Calendar.WEDNESDAY)
            binding.eveningCheckboxThursday.isChecked = selectedDays.contains(java.util.Calendar.THURSDAY)
            binding.eveningCheckboxFriday.isChecked = selectedDays.contains(java.util.Calendar.FRIDAY)
            binding.eveningCheckboxSaturday.isChecked = selectedDays.contains(java.util.Calendar.SATURDAY)
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}