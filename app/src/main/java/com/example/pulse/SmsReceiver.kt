package com.example.pulse

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Telephony
import org.json.JSONArray
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking



private val KEY_DEDUP_BODY = stringPreferencesKey("last_sms_body")
private val KEY_DEDUP_TIME = longPreferencesKey("last_sms_time")

private val Context.dedupDataStore: DataStore<Preferences> by preferencesDataStore(name = "sms_dedup_prefs")

class SmsReceiver : BroadcastReceiver() {

    companion object {
        private const val PREFS_NAME = "sms_alert_prefs"
        private const val KEY_KEYWORDS = "keywords"
        private val DEFAULT_KEYWORDS = listOf("ALERT", "紧急", "交警", "服务器宕机")

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

        /** 从 SharedPreferences 直接读取关键词（Hilt 未就绪时的兜底方案） */
        internal fun loadKeywordsFallback(prefs: SharedPreferences): List<String> {
            val raw = prefs.getString(KEY_KEYWORDS, null) ?: return DEFAULT_KEYWORDS
            return try {
                val arr = JSONArray(raw)
                (0 until arr.length()).map { arr.getString(it) }
            } catch (_: Exception) {
                raw.split("##").filter { it.isNotEmpty() }.ifEmpty { DEFAULT_KEYWORDS }
            }
        }

        /** 同步关键词匹配（不依赖 KeywordStore） */
        internal fun matchKeywords(body: String, keywords: List<String>): Boolean =
            keywords.any { body.contains(it, ignoreCase = true) }

        /** 从 DataStore 同步读取去重数据（广播接收器需要同步调用） */
        internal fun loadDedupData(context: Context): Pair<String, Long> = runBlocking {
            val data = context.dedupDataStore.data.first()
            Pair(data[KEY_DEDUP_BODY] ?: "", data[KEY_DEDUP_TIME] ?: 0L)
        }
        
        /** 同步保存去重数据到 DataStore */
        internal fun saveDedupData(context: Context, body: String, time: Long) = runBlocking {
            context.dedupDataStore.edit {
                it[KEY_DEDUP_BODY] = body
                it[KEY_DEDUP_TIME] = time
            }
        }
    }



    override fun onReceive(context: Context, intent: Intent) {
        LogStore.i("══════════ SMS 广播接收 ══════════")
        LogStore.i("进程存活，开始处理短信广播")

        // 获取关键词：优先使用 KeywordStore（内存缓存），fallback 到 SharedPreferences
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val keywordStore = KeywordStore.getInstance()
        val keywords: List<String>
        val keywordSource: String

        if (keywordStore != null) {
            keywords = keywordStore.getKeywords()
            keywordSource = "KeywordStore(内存缓存)"
        } else {
            LogStore.e("【关键错误】KeywordStore 未初始化！Hilt 懒加载导致 getInstance() 返回 null")
            LogStore.e("→ 启用兜底方案：直接从 SharedPreferences 读取关键词")
            keywords = loadKeywordsFallback(prefs)
            keywordSource = "SharedPreferences(兜底)"
        }
        LogStore.i("关键词来源: $keywordSource, 当前关键词(${keywords.size}个): ${keywords.joinToString(", ")}")

        try {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            LogStore.i("解析到 ${messages.size} 条短信")
            for ((index, sms) in messages.withIndex()) {
                val body = sms.displayMessageBody ?: sms.messageBody ?: ""
                LogStore.d("短信[${index + 1}/${messages.size}] 内容: ${body.take(60)}")

                // 步骤 1：关键词匹配
                val matched = matchKeywords(body, keywords)
                if (!matched) {
                    LogStore.d("【步骤1】未匹配任何关键词，跳过")
                    continue
                }
                LogStore.i("【步骤1✓】关键词匹配成功")

                // 步骤 2：去重检查
                val (lastBody, lastTime) = loadDedupData(context)
                val now = System.currentTimeMillis()
                if (SmsReceiverDedup.isDuplicate(body, now, lastBody, lastTime)) {
                    LogStore.w("【步骤2失败】3秒内重复短信，去重拦截 — 内容: ${body.take(40)}")
                    return
                }
                LogStore.i("【步骤2✓】去重检查通过")
                saveDedupData(context, body, now)

                // 步骤 3：获取临时 WakeLock
                val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                val wakeLock = pm.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "pulse:SmsReceiverWakeLock"
                )
                wakeLock.acquire(5000)
                LogStore.i("【步骤3✓】临时 WakeLock 已获取（5秒超时）")

                // 步骤 4：启动 AlertService
                val serviceIntent = Intent(context, AlertService::class.java).apply {
                    putExtra("msg", body)
                }
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                    LogStore.i("【步骤4✓】AlertService 启动指令已发送")
                } catch (e: Exception) {
                    LogStore.e("【步骤4失败】启动 AlertService 异常: ${e.javaClass.simpleName}: ${e.message}")
                    e.printStackTrace()
                }
                LogStore.i("══════════ SMS 处理完成 ══════════")
            }
        } catch (e: Exception) {
            LogStore.e("SmsReceiver 异常: ${e.javaClass.simpleName}: ${e.message}")
            e.printStackTrace()
        }
    }
}
