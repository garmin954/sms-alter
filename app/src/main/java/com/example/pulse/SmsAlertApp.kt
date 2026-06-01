package com.example.pulse

import android.app.Application
import javax.inject.Inject
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SmsAlertApp : Application() {
    @Inject lateinit var keywordStore: KeywordStore

    override fun onCreate() {
        super.onCreate()
        // 持久化日志必须最先初始化，确保后续所有日志都能写入文件
        LogStore.init(this)
        LogStore.i("══════════ App 进程启动 ══════════")
        // 强制初始化 KeywordStore，确保 SmsReceiver 可用
        keywordStore.getKeywords()
        LogStore.i("KeywordStore 已初始化，当前关键词: ${keywordStore.getKeywords().joinToString(", ")}")
    }
}
