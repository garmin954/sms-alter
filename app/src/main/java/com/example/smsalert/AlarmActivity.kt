package com.example.smsalert

import android.content.Intent
import android.os.Build
import android.os.Bundle
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
import com.example.smsalert.ui.screens.AlarmScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AlarmActivity : ComponentActivity() {

    private var alarmMessage by mutableStateOf("")
    private var triggerKey by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        val msg = intent.getStringExtra("msg") ?: getString(R.string.unknown_sms_content)
        alarmMessage = msg

        if (intent.getBooleanExtra("from_alarm_clock", false)) {
            LogStore.i("系统闹钟触发（onCreate），重新启动 AlertService")
            startForegroundService(Intent(this, AlertService::class.java).apply {
                putExtra("msg", msg)
            })
        }

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
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra("from_alarm_clock", false)) {
            LogStore.i("系统闹钟触发（onNewIntent），重新启动 AlertService")
            val msg = intent.getStringExtra("msg") ?: getString(R.string.unknown_sms_content)
            alarmMessage = msg
            startForegroundService(Intent(this, AlertService::class.java).apply {
                putExtra("msg", msg)
            })
            triggerKey++
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopService(Intent(this, AlertService::class.java))
    }
}
