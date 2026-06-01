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
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MonitorService : Service() {

    private lateinit var pendingIntent: PendingIntent

    @Inject lateinit var appPreferences: AppPreferences
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    companion object {
        const val CHANNEL_ID = "monitor_channel"
        const val NOTIFICATION_ID = 100

        @Volatile
        private var _isRunning = false
        @Volatile
        internal var _startTime = 0L

        fun isRunning(): Boolean = _isRunning
        fun getElapsedMs(): Long =
            if (_isRunning) SystemClock.elapsedRealtime() - _startTime else 0L

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
        // 从 DataStore 恢复计时（兼容旧版 SharedPreferences 遗留数据）
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

        LogStore.i("MonitorService 启动完成，关键词: ${KeywordStore.getInstance()?.getKeywords()?.joinToString() ?: ""}")

        // 调度保活闹钟
        KeepAliveScheduler.schedule(this)

        return START_STICKY
    }

    override fun onDestroy() {
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

    private fun buildNotification(): android.app.Notification {
        val whenTime = System.currentTimeMillis() - getElapsedMs()
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.monitor_notification_title))
            .setContentText(getString(R.string.monitor_notification_text))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setUsesChronometer(true)
            .setWhen(whenTime)
            .build()
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
