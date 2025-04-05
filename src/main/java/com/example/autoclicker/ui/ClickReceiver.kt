package com.example.autoclicker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class ClickReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("ClickReceiver", "onReceive called")
        val x = intent.getFloatExtra("x", 0f)
        val y = intent.getFloatExtra("y", 0f)

        // 启动无障碍服务进行点击操作
        val serviceIntent = Intent(context, MyAccessibilityService::class.java)
        serviceIntent.putExtra("x", x)
        serviceIntent.putExtra("y", y)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }

        // 发送点击完成广播
        val clickCompletedIntent = Intent("CLICK_COMPLETED")
        context.sendBroadcast(clickCompletedIntent)
    }
}