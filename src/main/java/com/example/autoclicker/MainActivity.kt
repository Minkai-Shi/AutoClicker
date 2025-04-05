package com.example.autoclicker

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityManager
import android.accessibilityservice.AccessibilityServiceInfo
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var prefs: SharedPreferences
    private lateinit var etHour: EditText
    private lateinit var etMinute: EditText
    private lateinit var etSecond: EditText
    private lateinit var etMilli: EditText
    private lateinit var etX: EditText
    private lateinit var etY: EditText
    private lateinit var btnSetTime: Button
    private lateinit var btnSetCoordinates: Button
    private lateinit var btnStart: Button

    private var floatingView: View? = null
    private var windowManager: WindowManager? = null
    private var tvTime: TextView? = null
    private val handler = Handler(Looper.getMainLooper())
    private val updateTimeRunnable = object : Runnable {
        override fun run() {
            updateTime()
            handler.postDelayed(this, 1000) // 每秒更新一次
        }
    }

    private val clickCompletedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "CLICK_COMPLETED") {
                removeFloatingWindow()
            }
        }
    }
    private var isReceiverRegistered = false // 定义 isReceiverRegistered 变量
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("ClickSettings", MODE_PRIVATE)
        etHour = findViewById(R.id.etHour)
        etMinute = findViewById(R.id.etMinute)
        etSecond = findViewById(R.id.etSecond)
        etMilli = findViewById(R.id.etMilli)
        etX = findViewById(R.id.etX)
        etY = findViewById(R.id.etY)
        btnSetTime = findViewById(R.id.btnSetTime)
        btnSetCoordinates = findViewById(R.id.btnSetCoordinates)
        btnStart = findViewById(R.id.btnStart)

        // 加载保存的设置
        loadSettings()

        btnSetTime.setOnClickListener {
            saveTimeSettings()
        }

        btnSetCoordinates.setOnClickListener {
            startActivity(Intent(this, CoordinateSelectorActivity::class.java))
        }

        btnStart.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&!Settings.canDrawOverlays(this)) {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
                return@setOnClickListener
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&!hasScheduleExactAlarmPermission()) {
                Toast.makeText(this, "没有设置精确闹钟的权限，请先授予权限", Toast.LENGTH_SHORT).show()
                requestScheduleExactAlarmPermission()
                return@setOnClickListener
            }

            if (!isAccessibilityServiceEnabled()) {
                Toast.makeText(this, "请先开启无障碍服务", Toast.LENGTH_SHORT).show()
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                return@setOnClickListener
            }

            // 移除已存在的悬浮窗口
            removeFloatingWindow()

            scheduleClick()
            showFloatingWindow()
            handler.post(updateTimeRunnable)
        }
    try {
        // 注册广播接收器
        registerReceiver(clickCompletedReceiver, IntentFilter("CLICK_COMPLETED"))
        isReceiverRegistered = true
    } catch (e: Exception) {
        Log.e("MainActivity", "Failed to register receiver", e)
    }
    }

    private fun loadSettings() {
        etHour.setText(prefs.getInt("hour", 8).toString())
        etMinute.setText(prefs.getInt("minute", 29).toString())
        etSecond.setText(prefs.getInt("second", 29).toString())
        etMilli.setText(prefs.getInt("milli", 998).toString())
        etX.setText(prefs.getFloat("x", 0f).toString())
        etY.setText(prefs.getFloat("y", 0f).toString())
    }

    private fun saveTimeSettings() {
        val hour = etHour.text.toString().toIntOrNull() ?: 0
        val minute = etMinute.text.toString().toIntOrNull() ?: 0
        val second = etSecond.text.toString().toIntOrNull() ?: 0
        val milli = etMilli.text.toString().toIntOrNull() ?: 0
        prefs.edit().putInt("hour", hour).putInt("minute", minute).putInt("second", second).putInt("milli", milli).apply()
        Toast.makeText(this, "时间设置已保存", Toast.LENGTH_SHORT).show()
    }

    private fun scheduleClick() {
        Log.d("MainActivity", "scheduleClick called")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&!hasScheduleExactAlarmPermission()) {
            Toast.makeText(this, "没有设置精确闹钟的权限，请先授予权限", Toast.LENGTH_SHORT).show()
            requestScheduleExactAlarmPermission()
            return
        }

        val hour = prefs.getInt("hour", 8)
        val minute = prefs.getInt("minute", 29)
        val second = prefs.getInt("second", 29)
        val milli = prefs.getInt("milli", 998)
        val x = prefs.getFloat("x", 0f)
        val y = prefs.getFloat("y", 0f)

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, second)
            set(Calendar.MILLISECOND, milli)
        }

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, ClickReceiver::class.java).apply {
            putExtra("x", x)
            putExtra("y", y)
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, flags)

        // 确保闹钟时间大于当前时间
        if (calendar.timeInMillis < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1) // 如果时间已过，设置为明天
            Toast.makeText(this, "设置为明天", Toast.LENGTH_SHORT).show()
        }

        alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        Toast.makeText(this, String.format("任务已设置 %d:%d:%d.%d", hour, minute, second, milli), Toast.LENGTH_SHORT).show()
        Log.d("scheduleClick", "Task setted")
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val accessibilityManager = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        val serviceName = "$packageName/.MyAccessibilityService"
        return enabledServices.any { it.id == serviceName }
    }

    private fun hasScheduleExactAlarmPermission(): Boolean {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    private fun requestScheduleExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            startActivityForResult(intent, 1)
        }
    }

    private fun showFloatingWindow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
            return
        }

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_window_layout, null)
        tvTime = floatingView?.findViewById(R.id.tv_time)

        val LAYOUT_FLAG = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            LAYOUT_FLAG,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 100
        }

        floatingView?.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_UP -> return true
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager?.updateViewLayout(floatingView, params)
                        return true
                    }
                }
                return false
            }
        })

        windowManager?.addView(floatingView, params)
    }

    private fun updateTime() {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val currentTime = sdf.format(Date())
        tvTime?.text = currentTime
    }

    private fun removeFloatingWindow() {
        floatingView?.let {
            windowManager?.removeView(it)
            floatingView = null
            tvTime = null
            handler.removeCallbacks(updateTimeRunnable)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        removeFloatingWindow()
        unregisterReceiver(clickCompletedReceiver)
    }

    override fun onStart() {
        super.onStart()
        Log.d("MainActivity", "onStart called")
    }

    override fun onResume() {
        super.onResume()
        Log.d("MainActivity", "onResume called")
        // 重新加载坐标数据
        loadSettings()
    }
}