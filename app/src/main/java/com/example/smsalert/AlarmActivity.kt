package com.example.smsalert

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AlarmActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private var warningAnimator: ObjectAnimator? = null
    private var pulseAnimator: ObjectAnimator? = null

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

        setContentView(R.layout.activity_alarm)

        val msg = intent.getStringExtra("msg") ?: "未知短信内容"
        val tvMessage = findViewById<TextView>(R.id.tvAlarmMessage)
        val tvWarning = findViewById<TextView>(R.id.tvWarningIcon)
        val vPulse = findViewById<View>(R.id.vPulseBg)
        val btnDismiss = findViewById<Button>(R.id.btnDismiss)

        // 短信内容 + 接收时间
        val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        tvMessage.text = "[$timeStr]\n\n$msg"

        // 警告图标闪烁动画
        warningAnimator = ObjectAnimator.ofFloat(tvWarning, "alpha", 1.0f, 0.15f).apply {
            duration = 500
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            start()
        }

        // 背景脉冲动画
        pulseAnimator = ObjectAnimator.ofFloat(vPulse, "alpha", 0.3f, 0.8f).apply {
            duration = 800
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
        }

        // 1.2 秒后启动脉冲
        handler.postDelayed({ pulseAnimator?.start() }, 1200)

        // 确认关闭按钮
        btnDismiss.setOnClickListener {
            LogStore.i("用户点击确认，关闭警报")
            finish()
        }
    }

    override fun onDestroy() {
        warningAnimator?.cancel()
        pulseAnimator?.cancel()
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
        stopService(Intent(this, AlertService::class.java))
    }
}
