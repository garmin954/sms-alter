package com.example.pulse

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.KeyguardManager
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.pulse.ui.screens.AlarmScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AlarmActivity : ComponentActivity() {

    private var alarmMessage by mutableStateOf("")
    private var triggerKey by mutableIntStateOf(0)

    private val dismissReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            LogStore.i("收到通知栏关闭广播，关闭 AlarmActivity")
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 诊断：屏幕和锁屏状态
        val km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        val isDeviceLocked = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            km.isDeviceLocked
        } else {
            @Suppress("DEPRECATION")
            km.isKeyguardLocked
        }
        val isKeyguardSecure = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            km.isKeyguardSecure
        } else {
            @Suppress("DEPRECATION")
            km.isKeyguardSecure
        }
        LogStore.i("设备状态: isDeviceLocked=$isDeviceLocked, isKeyguardSecure=$isKeyguardSecure")

        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        val isScreenOn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            pm.isInteractive
        } else {
            @Suppress("DEPRECATION")
            pm.isScreenOn
        }
        LogStore.i("屏幕状态: isInteractive=$isScreenOn")

        // 诊断：检查关键权限
        val hasOverlay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.provider.Settings.canDrawOverlays(this)
        } else true
        LogStore.i("权限状态: SYSTEM_ALERT_WINDOW=$hasOverlay")

        registerReceiver(
            dismissReceiver,
            IntentFilter(AlertService.ACTION_FINISH_ACTIVITY),
            Context.RECEIVER_NOT_EXPORTED
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            LogStore.i("已设置 showWhenLocked=true, turnScreenOn=true")
        }

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        LogStore.i("Window flags: KEEP_SCREEN_ON | ALLOW_LOCK_WHILE_SCREEN_ON | FULLSCREEN")

        val msg = intent.getStringExtra("msg") ?: getString(R.string.unknown_sms_content)
        alarmMessage = msg
        LogStore.i("报警消息: ${msg.take(50)}")

        setContent {
            key(triggerKey) {
                AlarmScreen(
                    message = alarmMessage,
                    onDismiss = {
                        LogStore.i("用户点击确认，关闭警报")
                        finish()
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        LogStore.i("界面已渲染")
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val msg = intent.getStringExtra("msg") ?: getString(R.string.unknown_sms_content)
        alarmMessage = msg
        LogStore.i("新短信到达（onNewIntent），更新消息并重置倒计时")
        triggerKey++
    }

    override fun onStart() {
        super.onStart()
        LogStore.i("报警界面可见 start")
    }

    override fun onStop() {
        super.onStop()
        LogStore.i("报警界面不可见 stop")
    }

    override fun onDestroy() {
        super.onDestroy()
        LogStore.i("报警界面关闭")
        try {
            unregisterReceiver(dismissReceiver)
        } catch (e: IllegalArgumentException) {
            // already unregistered
        }
        stopService(Intent(this, AlertService::class.java))
    }
}

