package com.example.smsalert

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        LogStore.i("AlarmReceiver: 系统闹钟触发，重新启动 AlertService")
        val serviceIntent = Intent(context, AlertService::class.java).apply {
            putExtra("msg", "系统闹钟：紧急短信警报")
        }
        context.startForegroundService(serviceIntent)
    }
}
