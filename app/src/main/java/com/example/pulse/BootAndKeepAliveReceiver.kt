package com.example.pulse

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.pulse.data.AppPreferences
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@AndroidEntryPoint
class BootAndKeepAliveReceiver : BroadcastReceiver() {

    @Inject lateinit var appPreferences: AppPreferences

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: ""
        LogStore.i("BootAndKeepAlive 收到: $action")

        val isListening = runCatching {
            kotlinx.coroutines.runBlocking { appPreferences.isListening.first() }
        }.getOrDefault(true)

        if (!isListening) {
            LogStore.i("监听未开启，跳过保活")
            return
        }

        if (!MonitorService.isRunning()) {
            LogStore.w("MonitorService 未运行，尝试重启...")
            MonitorService.start(context)
            // onStartCommand() 中会自动调度下一次保活，此处不再重复调度
        } else {
            LogStore.i("MonitorService 运行中，无需重启")
            // MonitorService 已在运行，不会走 onStartCommand，必须在此调度
            KeepAliveScheduler.schedule(context)
        }
    }
}
