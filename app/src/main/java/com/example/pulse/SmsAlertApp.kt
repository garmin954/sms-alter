package com.example.pulse

import android.app.Application
import javax.inject.Inject
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SmsAlertApp : Application() {
    @Inject lateinit var keywordStore: KeywordStore

    override fun onCreate() {
        super.onCreate()
        // 强制初始化 KeywordStore，确保 SmsReceiver 可用
        keywordStore.getKeywords()
    }
}
