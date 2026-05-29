package com.example.smsalert

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.widget.Toast
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MonitorService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var pendingIntent: PendingIntent

    private val updateRunnable = object : Runnable {
        override fun run() {
            updateNotification()
            handler.postDelayed(this, 1000)
        }
    }

    companion object {
        const val CHANNEL_ID = "monitor_channel"
        const val NOTIFICATION_ID = 100
        private const val PREFS_NAME = "monitor_prefs"
        private const val KEY_START_TIME = "start_time_elapsed"

        @Volatile
        private var _isRunning = false
        @Volatile
        private var _startTime = 0L

        fun isRunning(): Boolean = _isRunning
        fun getElapsedMs(): Long =
            if (_isRunning) SystemClock.elapsedRealtime() - _startTime else 0L

        private fun restoreStartTime(context: Context): Long {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val saved = prefs.getLong(KEY_START_TIME, 0L)
            // 如果保存的值比当前 elapsedRealtime 还大，说明设备重启过，抛弃旧值
            return if (saved > 0L && saved <= SystemClock.elapsedRealtime()) saved else 0L
        }

        private fun persistStartTime(context: Context, time: Long) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putLong(KEY_START_TIME, time).apply()
        }

        private fun clearPersistedStartTime(context: Context) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().remove(KEY_START_TIME).apply()
        }

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
            clearPersistedStartTime(context)
            context.stopService(Intent(context, MonitorService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        _isRunning = true
        val restored = restoreStartTime(this)
        _startTime = if (restored > 0L) restored else SystemClock.elapsedRealtime()
        persistStartTime(this, _startTime)
        LogStore.i("MonitorService onCreate, _startTime=$_startTime (restored=${restored > 0L})")
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        try {
            startForeground(NOTIFICATION_ID, buildNotification())
            LogStore.i("MonitorService 前台通知已显示")
        } catch (e: Exception) {
            LogStore.e("startForeground 失败: ${e.message}")
            e.printStackTrace()
        }

        handler.post(updateRunnable)

        LogStore.i("MonitorService 启动完成，关键词: ${KeywordStore.getKeywords(this).joinToString()}")

        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(updateRunnable)
        _isRunning = false
        _startTime = 0L
        LogStore.i("MonitorService onDestroy")
        super.onDestroy()
    }

    private fun buildNotification(): android.app.Notification {
        val elapsed = getElapsedMs() / 1000
        val timeStr = if (elapsed > 0) {
            val h = elapsed / 3600
            val m = (elapsed % 3600) / 60
            val s = elapsed % 60
            String.format("%02d:%02d:%02d", h, m, s)
        } else ""

        val contentText = if (timeStr.isNotEmpty())
            "${getString(R.string.monitor_notification_text)} $timeStr"
        else
            getString(R.string.monitor_notification_text)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.monitor_notification_title))
            .setContentText(contentText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun updateNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification())
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
