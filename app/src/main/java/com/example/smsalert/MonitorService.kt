package com.example.smsalert

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

class MonitorService : Service() {

    companion object {
        const val CHANNEL_ID = "monitor_channel"
        const val NOTIFICATION_ID = 100
        private const val POLL_INTERVAL_MS = 8_000L

        @Volatile
        var lastProcessedSmsId: Long = -1

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
                Toast.makeText(context, "启动监控服务失败: ${e.message}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, MonitorService::class.java))
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private val smsObserver = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            super.onChange(selfChange, uri)
            LogStore.d("ContentObserver 检测到变化: $uri")
            checkNewSms()
        }
    }
    private var pollingActive = false

    override fun onCreate() {
        super.onCreate()
        _isRunning = true
        LogStore.i("MonitorService onCreate")
        createChannel()

        try {
            contentResolver.registerContentObserver(
                Uri.parse("content://sms/"),
                true,
                smsObserver
            )
            LogStore.i("ContentObserver 已注册")
        } catch (e: Exception) {
            LogStore.e("注册 ContentObserver 失败: ${e.message}")
            e.printStackTrace()
        }

        startPolling()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("短信报警")
            .setContentText("正在监控短信...")
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
        checkNewSms()

        return START_STICKY
    }

    override fun onDestroy() {
        _isRunning = false
        LogStore.i("MonitorService onDestroy")
        stopPolling()
        try {
            contentResolver.unregisterContentObserver(smsObserver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        super.onDestroy()
    }

    private fun startPolling() {
        pollingActive = true
        LogStore.i("定时轮询已启动 (间隔 ${POLL_INTERVAL_MS / 1000}s)")
        val runnable = object : Runnable {
            override fun run() {
                if (!pollingActive) return
                checkNewSms()
                handler.postDelayed(this, POLL_INTERVAL_MS)
            }
        }
        handler.postDelayed(runnable, POLL_INTERVAL_MS)
    }

    private fun stopPolling() {
        pollingActive = false
        handler.removeCallbacksAndMessages(null)
        LogStore.i("定时轮询已停止")
    }

    private fun checkNewSms() {
        val hasReadSms = ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasReadSms) {
            LogStore.w("checkNewSms 跳过: READ_SMS 权限未授予")
            return
        }

        try {
            val cursor = contentResolver.query(
                Uri.parse("content://sms/inbox"),
                arrayOf("_id", "body", "date"),
                null, null, "date DESC"
            )
            if (cursor != null && cursor.moveToFirst()) {
                val idIndex = cursor.getColumnIndex("_id")
                val bodyIndex = cursor.getColumnIndex("body")
                val dateIndex = cursor.getColumnIndex("date")

                if (idIndex != -1 && bodyIndex != -1 && dateIndex != -1) {
                    val id = cursor.getLong(idIndex)
                    val body = cursor.getString(bodyIndex) ?: ""
                    val rawDate = cursor.getLong(dateIndex)

                    val date = if (rawDate < 10_000_000_000L) rawDate * 1000L else rawDate

                    val now = System.currentTimeMillis()
                    if (id != lastProcessedSmsId && (now - date) < 120_000) {
                        lastProcessedSmsId = id
                        LogStore.d("检查短信 #$id: ${body.take(30)}...")
                        if (KeywordStore.match(this, body)) {
                            LogStore.i("匹配到关键词！触发报警...")
                            triggerAlert(body)
                        }
                    }
                }
                cursor.close()
            }
        } catch (e: Exception) {
            LogStore.e("checkNewSms 异常: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun triggerAlert(msg: String) {
        try {
            val serviceIntent = Intent(this, AlertService::class.java).apply {
                putExtra("msg", msg)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            LogStore.i("已触发 AlertService")
        } catch (e: Exception) {
            LogStore.e("触发 AlertService 失败: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "监控服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "短信监控服务运行中"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
