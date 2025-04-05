package com.example.autoclicker

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

import android.content.Context
import android.graphics.PixelFormat

class CoordinateSelectorActivity : AppCompatActivity() {
    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: android.view.View
    private val OVERLAY_PERMISSION_REQUEST_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            // 请求 SYSTEM_ALERT_WINDOW 权限
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE)
        } else {
            // 权限已经授予，继续添加窗口的操作
            addWindow()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OVERLAY_PERMISSION_REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                // 权限已经授予，继续添加窗口的操作
                addWindow()
            } else {
                // 权限未授予，给出相应提示
                Toast.makeText(this, "需要授予悬浮窗权限才能使用此功能", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun addWindow() {
        // 在这里执行添加窗口的操作
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        overlayView = android.view.View(this).apply {
            setBackgroundColor(0x55000000) // 半透明背景
            setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    val x = event.rawX
                    val y = event.rawY
                    saveCoordinates(x, y)
                    Toast.makeText(
                        this@CoordinateSelectorActivity,
                        "坐标已保存: ($x, $y)",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
                true
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        windowManager.addView(overlayView, params)
    }

    /*   override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        overlayView = android.view.View(this).apply {
            setBackgroundColor(0x55000000) // 半透明背景
            setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    val x = event.rawX
                    val y = event.rawY
                    saveCoordinates(x, y)
                    Toast.makeText(this@CoordinateSelectorActivity, "坐标已保存: ($x, $y)", Toast.LENGTH_SHORT).show()
                    finish()
                }
                true
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        windowManager.addView(overlayView, params)
    }*/

    private fun saveCoordinates(x: Float, y: Float) {
        val prefs = getSharedPreferences("ClickSettings", MODE_PRIVATE)
        prefs.edit().putFloat("x", x).putFloat("y", y).apply()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::overlayView.isInitialized) {
            windowManager.removeView(overlayView)
        }
    }
}