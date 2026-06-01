package com.example.pulse

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock

object KeepAliveScheduler {

    private const val ALARM_REQUEST_CODE = 9001
    private const val INTERVAL_MS = 15 * 60 * 1000L // 15 minutes

    fun schedule(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, BootAndKeepAliveReceiver::class.java).apply {
            action = "com.example.pulse.ACTION_KEEP_ALIVE"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, ALARM_REQUEST_CODE, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + INTERVAL_MS,
            pendingIntent
        )
        LogStore.i("KeepAlive 已调度，${INTERVAL_MS / 60_000}分钟后唤醒")
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, BootAndKeepAliveReceiver::class.java).apply {
            action = "com.example.pulse.ACTION_KEEP_ALIVE"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, ALARM_REQUEST_CODE, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        alarmManager.cancel(pendingIntent)
        LogStore.i("KeepAlive 已取消")
    }
}
