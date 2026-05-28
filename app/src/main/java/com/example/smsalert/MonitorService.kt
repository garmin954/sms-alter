package com.example.smsalert

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.widget.Toast
import androidx.core.app.NotificationCompat

class MonitorService : Service() {

    companion object {
        const val CHANNEL_ID = "monitor_channel"
        const val NOTIFICATION_ID = 100

        @Volatile
        private var _isRunning = false

        fun isRunning(): Boolean = _isRunning

        fun start(context: Context) {
            val intent = Intent(context, MonitorService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                LogStore.e("启动监控服务失败: ${e.message}")
                Toast.makeText(context, context.getString(R.string.monitor_start_failed) + ": ${e.message}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, MonitorService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        _isRunning = true
        LogStore.i("MonitorService onCreate")
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.monitor_notification_title))
            .setContentText(getString(R.string.monitor_notification_text))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .build()

        try {
            startForeground(NOTIFICATION_ID, notification)
            LogStore.i("MonitorService 前台通知已显示")
        } catch (e: Exception) {
            LogStore.e("startForeground 失败: ${e.message}")
            e.printStackTrace()
        }

        LogStore.i("MonitorService 启动完成，关键词: ${KeywordStore.getKeywords(this).joinToString()}")

        return START_STICKY
    }

    override fun onDestroy() {
        _isRunning = false
        LogStore.i("MonitorService onDestroy")
        super.onDestroy()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.monitor_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.monitor_channel_description)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
