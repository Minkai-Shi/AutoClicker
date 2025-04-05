package com.example.autoclicker

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.Path
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.accessibilityservice.GestureDescription

class MyAccessibilityService : AccessibilityService() {
    private var clickX: Float = 0f
    private var clickY: Float = 0f

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        Log.d("MyAccessibilityService", "onAccessibilityEvent called")
        try {
            // 处理无障碍事件的逻辑
            // 这里可以根据事件类型进行不同的处理
            event?.let {
                when (it.eventType) {
                    AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                        Log.d("MyAccessibilityService", "View clicked event received")
                    }
                    // 可以添加更多的事件类型处理
                }
            }
        } catch (e: Exception) {
            Log.e("MyAccessibilityService", "onAccessibilityEvent error: ${e.message}", e)
        }
    }

    override fun onInterrupt() {
        Log.d("MyAccessibilityService", "onInterrupt called")
        try {
            // 服务中断时的逻辑
        } catch (e: Exception) {
            Log.e("MyAccessibilityService", "onInterrupt error: ${e.message}", e)
        }
    }

    override fun onServiceConnected() {
        Log.d("MyAccessibilityService", "onServiceConnected called")
        try {
            super.onServiceConnected()
            // 服务连接成功时的逻辑
        } catch (e: Exception) {
            Log.e("MyAccessibilityService", "onServiceConnected error: ${e.message}", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            clickX = intent.getFloatExtra("x", 0f)
            clickY = intent.getFloatExtra("y", 0f)
            Log.d("MyAccessibilityService", "Received x: $clickX, y: $clickY")
            performClick(clickX, clickY)
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun performClick(x: Float, y: Float) {
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            try {
                val path = Path()
                path.moveTo(x, y)
                val gestureDescription = GestureDescription.Builder()
                   .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
                   .build()
                dispatchGesture(gestureDescription, object : AccessibilityService.GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription) {
                        super.onCompleted(gestureDescription)
                        Log.d("MyAccessibilityService", "Click gesture completed")
                    }

                    override fun onCancelled(gestureDescription: GestureDescription) {
                        super.onCancelled(gestureDescription)
                        Log.e("MyAccessibilityService", "Click gesture cancelled")
                    }
                }, null)
            } catch (e: Exception) {
                Log.e("MyAccessibilityService", "performClick error: ${e.message}", e)
            }
        }
    }
}