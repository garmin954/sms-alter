package com.example.pulse

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.pulse.data.AppPreferences
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MonitorService : Service() {

    private lateinit var pendingIntent: PendingIntent
    private var updateNotificationJob: Job? = null

    @Inject lateinit var appPreferences: AppPreferences
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    companion object {
        const val CHANNEL_ID = "monitor_channel"
        const val NOTIFICATION_ID = 100
        const val EXTRA_RESET_TIME = "reset_time"

        @Volatile
        private var _isRunning = false
        @Volatile
        internal var _startTime = 0L

        fun isRunning(): Boolean = _isRunning
        fun getElapsedMs(): Long =
            if (_isRunning) SystemClock.elapsedRealtime() - _startTime else 0L

        fun start(context: Context, resetTime: Boolean = true) {
            val intent = Intent(context, MonitorService::class.java).apply {
                putExtra(EXTRA_RESET_TIME, resetTime)
            }
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

    private var shouldResetTime = false

    override fun onCreate() {
        super.onCreate()
        _isRunning = true
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        shouldResetTime = intent?.getBooleanExtra(EXTRA_RESET_TIME, false) == true

        if (shouldResetTime) {
            _startTime = SystemClock.elapsedRealtime()
            serviceScope.launch {
                appPreferences.saveMonitorStartTime(_startTime)
            }
            LogStore.i("MonitorService 重置计时: $_startTime")
        } else {
            serviceScope.launch {
                val restored = appPreferences.getMonitorStartTime()
                val valid = if (restored > 0L && restored <= SystemClock.elapsedRealtime()) restored else 0L
                _startTime = if (valid > 0L) valid else SystemClock.elapsedRealtime()
                if (valid > 0L) {
                    LogStore.i("MonitorService 恢复计时: $_startTime")
                } else {
                    LogStore.i("MonitorService 新建计时: $_startTime")
                }
                appPreferences.saveMonitorStartTime(_startTime)
            }
        }

        pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        try {
            startForeground(NOTIFICATION_ID, buildNotification(getElapsedMs()))
            LogStore.i("MonitorService 前台通知已显示")
        } catch (e: Exception) {
            LogStore.e("startForeground 失败: ${e.message}")
            e.printStackTrace()
        }

        LogStore.i("MonitorService 启动完成，关键词: ${KeywordStore.getInstance()?.getKeywords()?.joinToString() ?: ""}")

        // 启动通知更新协程
        updateNotificationJob = serviceScope.launch {
            while (isActive) {
                delay(1000)
                updateNotification()
            }
        }

        // 调度保活闹钟
        KeepAliveScheduler.schedule(this)

        return START_STICKY
    }

    override fun onDestroy() {
        updateNotificationJob?.cancel()
        _isRunning = false
        _startTime = 0L
        serviceScope.launch {
            appPreferences.clearMonitorStartTime()
        }
        serviceScope.cancel()
        KeepAliveScheduler.cancel(this)
        LogStore.i("MonitorService onDestroy")
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        LogStore.w("══════ MonitorService onTaskRemoved — 用户从最近任务划掉 App ══════")
        // stopWithTask="false" 应阻止服务被杀，但某些 ROM 仍会触发
    }

    private fun formatDuration(ms: Long): String {
        val totalSec = ms / 1000
        val sec = totalSec % 60
        val min = totalSec / 60 % 60
        val hour = totalSec / 3600 % 24
        val day = totalSec / 86400 % 30
        val month = totalSec / 2592000 % 12
        val year = totalSec / 31104000

        return buildString {
            if (year > 0) append("${year}年")
            if (month > 0) append("${month}月")
            if (day > 0) append("${day}天")
            if (hour > 0) append("${hour}时")
            if (min > 0) append("${min}分")
            if (sec > 0 || isEmpty()) append("${sec}秒")
        }
    }

    private fun buildNotification(elapsedMs: Long): android.app.Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.monitor_notification_title))
            .setContentText("运行时间: ${formatDuration(elapsedMs)}")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun updateNotification() {
        try {
            val manager = getSystemService(NotificationManager::class.java)
            manager.notify(NOTIFICATION_ID, buildNotification(getElapsedMs()))
        } catch (e: Exception) {
            LogStore.e("更新通知失败: ${e.message}")
        }
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
