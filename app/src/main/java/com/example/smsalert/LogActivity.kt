package com.example.smsalert

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.method.ScrollingMovementMethod
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class LogActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log)

        val tvLogContent = findViewById<TextView>(R.id.tvLogContent)
        val tvLogCount = findViewById<TextView>(R.id.tvLogCount)
        val svLog = findViewById<ScrollView>(R.id.svLog)
        val btnClear = findViewById<Button>(R.id.btnClearLog)

        tvLogContent.movementMethod = ScrollingMovementMethod()

        val updateUi = {
            val list = LogStore.entries
            tvLogContent.text = if (list.isEmpty()) "暂无日志" else list.joinToString("\n")
            tvLogCount.text = "${list.size} 条"
            // 自动滚到底部
            svLog.post {
                svLog.fullScroll(ScrollView.FOCUS_DOWN)
            }
        }

        updateUi()

        LogStore.onNewEntry = {
            handler.post { updateUi() }
        }

        btnClear.setOnClickListener {
            LogStore.clear()
            updateUi()
        }
    }
}
