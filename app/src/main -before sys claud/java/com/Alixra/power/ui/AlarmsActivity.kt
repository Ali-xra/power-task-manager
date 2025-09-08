package com.Alixra.power.ui

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.Alixra.power.data.PreferencesManager
import com.Alixra.power.databinding.ActivityAlarmsBinding
import com.Alixra.power.utils.AlarmUtils

class AlarmsActivity : AppCompatActivity() {

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
                textView.setPadding(16, 12, 16, 12) // فاصله داخلی
                return view
            }
        }
        binding.quotesListView.adapter = quotesAdapter
    }

    private fun loadSavedData() {
        // بارگذاری جملات
        val savedQuotes = prefsManager.getQuotes()
        if (savedQuotes.isNotEmpty()) {
            quotes.clear()
            quotes.addAll(savedQuotes)
            quotesAdapter.notifyDataSetChanged()
        } else {
            // افزودن جملات پیش‌فرض در اولین اجرا
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
    }

    private fun addDefaultQuotes() {
        val defaultQuotes = listOf(
            "امروز اولین روز از بقیه عمر توست",
            "موفقیت پایان نیست، شکست کشنده نیست",
            "تنها راه انجام کارهای بزرگ، دوست داشتن کاری است که انجام می‌دهی"
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
                showToast("ابتدا حداقل یک جمله اضافه کنید!")
                return@setOnClickListener
            }

            val hour = binding.morningTimePicker.hour
            val minute = binding.morningTimePicker.minute
            val timeInMillis = AlarmUtils.getNextAlarmTime(hour, minute)

            AlarmUtils.setMorningAlarm(this, timeInMillis)
            prefsManager.saveMorningAlarmTime("$hour:$minute")
            showToast("زنگ صبح برای ساعت $hour:$minute تنظیم شد")
        }

        // دکمه تنظیم یادآور شب
        binding.setEveningAlarmBtn.setOnClickListener {
            val hour = binding.eveningTimePicker.hour
            val minute = binding.eveningTimePicker.minute
            val timeInMillis = AlarmUtils.getNextAlarmTime(hour, minute)

            AlarmUtils.setEveningAlarm(this, timeInMillis)
            prefsManager.saveEveningAlarmTime("$hour:$minute")
            showToast("یادآور شب برای ساعت $hour:$minute تنظیم شد")
        }

        // دکمه اضافه کردن جمله
        binding.addQuoteBtn.setOnClickListener {
            val newQuote = binding.quoteEditText.text.toString().trim()
            if (newQuote.isNotEmpty()) {
                quotes.add(newQuote)
                quotesAdapter.notifyDataSetChanged()
                prefsManager.saveQuotes(quotes)
                binding.quoteEditText.text.clear()
                showToast("جمله اضافه شد!")
            } else {
                showToast("لطفاً جمله‌ای وارد کنید!")
            }
        }

        // حذف جمله با کلیک طولانی
        binding.quotesListView.setOnItemLongClickListener { _, _, position, _ ->
            AlertDialog.Builder(this)
                .setTitle("حذف جمله")
                .setMessage("آیا مطمئن هستید که می‌خواهید این جمله را حذف کنید؟")
                .setPositiveButton("بله") { _, _ ->
                    quotes.removeAt(position)
                    quotesAdapter.notifyDataSetChanged()
                    prefsManager.saveQuotes(quotes)
                    showToast("جمله حذف شد!")
                }
                .setNegativeButton("خیر", null)
                .show()
            true
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}