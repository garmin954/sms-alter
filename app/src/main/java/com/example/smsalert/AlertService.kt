package com.example.smsalert

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.*
import androidx.core.app.NotificationCompat

class AlertService : Service() {

    companion object {
        const val CHANNEL_ID = "alert_channel"
    }

    private var wakeLock: PowerManager.WakeLock? = null
    private var ringtone: android.media.Ringtone? = null
    private var isActive = false

    override fun onCreate() {
        super.onCreate()
        LogStore.i("AlertService onCreate")
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val msg = intent?.getStringExtra("msg") ?: ""
        LogStore.i("AlertService onStartCommand: ${msg.take(30)}...")

        acquireWakeLock()

        val alarmIntent = Intent(this, AlarmActivity::class.java).apply {
            putExtra("msg", msg)
            this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, alarmIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("紧急短信")
            .setContentText(msg)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(pendingIntent, true)
            .setOngoing(true)
            .build()

        startForeground(1, notification)

        if (!isActive) {
            isActive = true
            LogStore.i("触发声音+振动报警（循环）")
            triggerAlarm()
            try {
                startActivity(alarmIntent)
                LogStore.i("AlarmActivity 已启动")
            } catch (e: Exception) {
                LogStore.e("启动 AlarmActivity 失败: ${e.message}")
                e.printStackTrace()
            }
        } else {
            LogStore.d("AlertService 已激活，更新通知内容")
        }

        return START_STICKY
    }

    override fun onDestroy() {
        LogStore.i("AlertService onDestroy — 停止报警")
        isActive = false
        ringtone?.stop()
        ringtone = null
        try {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.cancel()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        releaseWakeLock()
        super.onDestroy()
    }

    private fun triggerAlarm() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        // 长振动模式：反复循环直到用户关闭
        val pattern = longArrayOf(0, 600, 300, 600, 300, 600, 300, 600, 300, 600, 300)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, 0)
        }
        LogStore.d("振动已触发（循环模式）")

        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        ringtone = RingtoneManager.getRingtone(this, uri).apply {
            isLooping = true
            play()
        }
        LogStore.d("铃声已播放（循环模式）")
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
            PowerManager.ACQUIRE_CAUSES_WAKEUP or
            PowerManager.ON_AFTER_RELEASE,
            "smsalert:alert"
        ).apply {
            setReferenceCounted(false)
            acquire(60_000)
        }
        LogStore.d("WakeLock 已获取（60秒超时）")
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                LogStore.d("WakeLock 已释放")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        wakeLock = null
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Alert Channel",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            enableVibration(true)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
