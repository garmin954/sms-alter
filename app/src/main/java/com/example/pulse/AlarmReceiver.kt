package com.example.pulse

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val msg = intent.getStringExtra("msg") ?: "系统闹钟：紧急短信警报"
        LogStore.i("══════════ AlarmReceiver 触发 ══════════")
        LogStore.i("系统闹钟兜底触发，消息: ${msg.take(50)}")
        val serviceIntent = Intent(context, AlertService::class.java).apply {
            putExtra("msg", msg)
            putExtra("from_alarm_clock", true)
        }
        try {
            context.startForegroundService(serviceIntent)
            LogStore.i("AlarmReceiver 已启动 AlertService（from_alarm_clock=true）")
        } catch (e: Exception) {
            LogStore.e("AlarmReceiver 启动 AlertService 失败: ${e.javaClass.simpleName}: ${e.message}")
            e.printStackTrace()
        }
    }
}
