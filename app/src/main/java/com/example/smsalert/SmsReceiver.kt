package com.example.smsalert

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Telephony

class SmsReceiver : BroadcastReceiver() {

    companion object {
        private const val PREFS_NAME = "sms_alert_prefs"
        private const val KEY_LAST_BODY = "last_sms_body"
        private const val KEY_LAST_TIME = "last_sms_time"
        private const val KEY_LAST_ID = "last_sms_id"

        fun setEnabled(context: Context, enabled: Boolean) {
            val pm = context.packageManager
            val component = ComponentName(context, SmsReceiver::class.java)
            val state = if (enabled) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            pm.setComponentEnabledSetting(component, state, PackageManager.DONT_KILL_APP)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        LogStore.i("收到短信广播")
        try {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            LogStore.i("解析到 ${messages.size} 条短信")
            for (sms in messages) {
                val body = sms.displayMessageBody ?: sms.messageBody ?: ""
                LogStore.d("短信内容: ${body.take(40)}...")

                if (!KeywordStore.match(context, body)) {
                    LogStore.d("未匹配任何关键词，跳过")
                    continue
                }

                LogStore.i("匹配到关键词！触发报警...")

                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val lastBody = prefs.getString(KEY_LAST_BODY, "") ?: ""
                val lastTime = prefs.getLong(KEY_LAST_TIME, 0)
                val now = System.currentTimeMillis()
                if (SmsReceiverDedup.isDuplicate(body, now, lastBody, lastTime)) {
                    LogStore.w("3秒内重复短信，跳过")
                    return
                }
                prefs.edit()
                    .putString(KEY_LAST_BODY, body)
                    .putLong(KEY_LAST_TIME, now)
                    .apply()

                val serviceIntent = Intent(context, AlertService::class.java).apply {
                    putExtra("msg", body)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
                LogStore.i("已触发 AlertService")
            }
        } catch (e: Exception) {
            LogStore.e("SmsReceiver 异常: ${e.message}")
            e.printStackTrace()
        }
    }
}
