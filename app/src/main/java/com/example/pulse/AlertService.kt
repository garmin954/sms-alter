package com.example.pulse

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.*
import android.provider.AlarmClock
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.pulse.data.dao.AlertDao
import com.example.pulse.data.entity.AlertRecord
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AlertService : Service() {

    companion object {
        const val CHANNEL_ID = "alert_channel"
        const val ACTION_DISMISS = "com.example.pulse.ACTION_DISMISS_ALARM"
        const val ACTION_FINISH_ACTIVITY = "com.example.pulse.ACTION_FINISH_ALARM_ACTIVITY"
        /** Pulse 在系统时钟中的闹钟唯一标签，与 AlarmScreen 中保持一致 */
        private const val PULSE_ALARM_LABEL = "Pulse 紧急短信告警"
    }

    @Inject lateinit var alertDao: AlertDao

    private var wakeLock: PowerManager.WakeLock? = null
    private var ringtone: android.media.Ringtone? = null
    private var isActive = false
    private var currentMessage: String = ""
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        LogStore.i("══════════ AlertService 创建 ══════════")
        createChannel()
        LogStore.i("AlertService onCreate 完成")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val fromAlarmClock = intent?.getBooleanExtra("from_alarm_clock", false) ?: false
        LogStore.i("══════════ AlertService onStartCommand ══════════")
        LogStore.i("from_alarm_clock=$fromAlarmClock, isActive=$isActive")

        // Handle dismiss action from notification button
        if (ACTION_DISMISS == intent?.action) {
            LogStore.i("通知栏确认按钮：关闭警报")
            tryDismissSystemAlarm()
            sendBroadcast(Intent(ACTION_FINISH_ACTIVITY).apply {
                setPackage(packageName)
            })
            stopSelf()
            return START_NOT_STICKY
        }

        val msg = intent?.getStringExtra("msg") ?: ""
        currentMessage = msg
        LogStore.i("报警消息: ${msg.take(50)}")

        // 步骤 5：保存记录到 Room
        serviceScope.launch {
            try {
                val source = if (msg.contains("模拟报警测试")) "test" else "sms"
                alertDao.insert(AlertRecord(message = msg, source = source))
                LogStore.d("【步骤5✓】警报记录已保存到数据库")
            } catch (e: Exception) {
                LogStore.e("【步骤5失败】保存警报记录失败: ${e.javaClass.simpleName}: ${e.message}")
                Log.e("AlertService", "保存警报记录失败", e)
            }
        }

        // 步骤 6：获取 WakeLock
        val wakeLockType = acquireWakeLock()
        LogStore.i("【步骤6✓】WakeLock 已获取，类型: $wakeLockType")

        val alarmIntent = Intent(this, AlarmActivity::class.java).apply {
            putExtra("msg", msg)
            putExtra("from_alarm_clock", fromAlarmClock)
            this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, alarmIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val dismissIntent = Intent(this, AlertService::class.java).apply {
            action = ACTION_DISMISS
        }
        val dismissPendingIntent = PendingIntent.getService(
            this, 1, dismissIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // 步骤 7：发布前台通知（含 FullScreenIntent）
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.alert_notification_title))
            .setContentText(msg)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(pendingIntent, true)
            .setOngoing(true)
            .setStyle(NotificationCompat.BigTextStyle().bigText(msg))
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.alarm_confirm_button),
                dismissPendingIntent
            )
            .build()

        try {
            startForeground(1, notification)
            LogStore.i("【步骤7✓】前台通知已发布（含 FullScreenIntent）")
        } catch (e: Exception) {
            LogStore.e("【步骤7失败】startForeground 异常: ${e.javaClass.simpleName}: ${e.message}")
            e.printStackTrace()
        }

        if (!isActive) {
            isActive = true
            LogStore.i("触发声音+振动报警（循环）")
            triggerAlarm()

            // 步骤 8：尝试显式启动 AlarmActivity
            LogStore.i("【步骤8】尝试显式启动 AlarmActivity...")
            try {
                startActivity(alarmIntent)
                LogStore.i("【步骤8✓】AlarmActivity 显式启动成功")
            } catch (e: SecurityException) {
                LogStore.e("【步骤8失败】SecurityException — 后台启动 Activity 被系统拦截")
                LogStore.e("  异常详情: ${e.javaClass.name}: ${e.message}")
                LogStore.e("  这通常是因为 Android 10+ 后台启动限制或缺少 SYSTEM_ALERT_WINDOW 权限")
                LogStore.e("  后续依赖通知栏 FullScreenIntent 拉起界面（需系统支持）")
                e.printStackTrace()
            } catch (e: Exception) {
                LogStore.e("【步骤8失败】${e.javaClass.simpleName}: ${e.message}")
                LogStore.e("  后续依赖通知栏 FullScreenIntent 拉起界面")
                e.printStackTrace()
            }
        } else {
            LogStore.d("AlertService 已激活，更新通知内容，拉回 AlarmActivity")
            // 新短信仅更新通知，系统闹钟的删旧+建新由 AlarmScreen 倒计时归零时统一处理
            try {
                startActivity(alarmIntent)
                LogStore.i("【步骤8✓】AlarmActivity 已拉回前台（新短信）")
            } catch (e: Exception) {
                LogStore.e("【步骤8失败】拉回 AlarmActivity 失败: ${e.javaClass.simpleName}: ${e.message}")
            }
        }

        LogStore.i("══════════ AlertService 启动完成 ══════════")
        return START_STICKY
    }

    override fun onDestroy() {
        LogStore.i("AlertService onDestroy — 停止报警")
        isActive = false
        ringtone?.stop()
        ringtone = null
        serviceScope.cancel()
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                isLooping = true
            }
            play()
        }
        LogStore.d("铃声已播放（循环模式）")
    }

    private fun acquireWakeLock(): String {
        if (wakeLock?.isHeld == true) return "已持有(复用)"
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        val flags = PowerManager.PARTIAL_WAKE_LOCK or
            PowerManager.ACQUIRE_CAUSES_WAKEUP or
            PowerManager.ON_AFTER_RELEASE
        val flagNames = mutableListOf<String>()
        if (flags and PowerManager.PARTIAL_WAKE_LOCK != 0) flagNames.add("PARTIAL")
        if (flags and PowerManager.ACQUIRE_CAUSES_WAKEUP != 0) flagNames.add("CAUSES_WAKEUP")
        if (flags and PowerManager.ON_AFTER_RELEASE != 0) flagNames.add("ON_AFTER_RELEASE")
        wakeLock = pm.newWakeLock(flags, "smsalert:alert").apply {
            setReferenceCounted(false)
            acquire(60_000)
        }
        LogStore.d("WakeLock 已获取 — 标志: ${flagNames.joinToString("|")}（60秒超时）")
        return flagNames.joinToString("|")
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

    private fun tryDismissSystemAlarm() {
        try {
            val intent = Intent(AlarmClock.ACTION_DISMISS_ALARM).apply {
                putExtra(AlarmClock.EXTRA_ALARM_SEARCH_MODE, AlarmClock.ALARM_SEARCH_MODE_LABEL)
                putExtra(AlarmClock.EXTRA_MESSAGE, PULSE_ALARM_LABEL)
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            }
            startActivity(intent)
            LogStore.i("已尝试撤销 Pulse 系统闹钟（通知栏确认，标签：$PULSE_ALARM_LABEL）")
        } catch (e: Exception) {
            LogStore.w("撤销 Pulse 系统闹钟失败：${e.message}")
        }
    }
}
