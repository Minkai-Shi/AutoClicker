package com.example.autoclicker // Implement an automatic timed clicking feature

import android.app.Application
import android.os.Process
import android.util.Log

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Thread.setDefaultUncaughtExceptionHandler { thread, ex ->
            Log.e("GlobalExceptionHandler", "Uncaught exception: ${ex.message}", ex)
            // �������������һЩ�������ϴ���־��
            android.os.Process.killProcess(android.os.Process.myPid())
        }
    }
}