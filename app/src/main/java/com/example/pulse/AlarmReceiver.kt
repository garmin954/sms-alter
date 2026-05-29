package com.example.pulse

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val msg = intent.getStringExtra("msg") ?: "系统闹钟：紧急短信警报"
        LogStore.i("AlarmReceiver: 系统闹钟触发，重新启动 AlertService，消息: ${msg.take(30)}")
        val serviceIntent = Intent(context, AlertService::class.java).apply {
            putExtra("msg", msg)
            putExtra("from_alarm_clock", true)  // 标记为兜底触发，避免 AlarmScreen 再次设闹钟
        }
        context.startForegroundService(serviceIntent)
    }
}
