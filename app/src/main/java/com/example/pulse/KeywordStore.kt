package com.example.pulse

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton

private val Context.keywordDataStore: DataStore<Preferences> by preferencesDataStore(name = "keyword_prefs")

@Singleton
class KeywordStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        @Volatile
        private var instance: KeywordStore? = null

        private const val MAX_KEYWORDS = 50
        private const val MAX_KEYWORD_LENGTH = 50
        private val DEFAULT_KEYWORDS = listOf<String>()
        private const val LEGACY_PREFS_NAME = "sms_alert_prefs"
        private const val LEGACY_KEY_KEYWORDS = "keywords"
        private val KEY_KEYWORDS_JSON = stringPreferencesKey("keywords_json")

        /** SmsReceiver 等非 Hilt 组件通过此方法获取单例 */
        fun getInstance(): KeywordStore? = instance
        internal fun setInstance(store: KeywordStore) { instance = store }
    }

    @Volatile
    private var cachedKeywords: List<String> = DEFAULT_KEYWORDS

    init {
        setInstance(this)
        cachedKeywords = runBlocking { loadInitial() }
    }

    private suspend fun loadInitial(): List<String> {
        // 优先从 DataStore 读取（包含用户自定义关键词）
        val json = context.keywordDataStore.data.first()[KEY_KEYWORDS_JSON]
        if (json != null) {
            return parseJsonKeywords(json) ?: DEFAULT_KEYWORDS
        }
        // 首次迁移：从旧 SharedPreferences → DataStore
        val legacy = context.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
            .getString(LEGACY_KEY_KEYWORDS, null)
        if (legacy != null) {
            val keywords = parseKeywords(legacy)
            context.keywordDataStore.edit { prefs ->
                prefs[KEY_KEYWORDS_JSON] = JSONArray(keywords).toString()
            }
            return keywords
        }
        return DEFAULT_KEYWORDS
    }

    /** 同步读取（供 SmsReceiver 使用） */
    fun getKeywords(): List<String> = cachedKeywords

    /** 同步匹配（基于内存缓存） */
    fun match(text: String): Boolean =
        cachedKeywords.any { text.contains(it, ignoreCase = true) }

    /** 异步添加 */
    suspend fun addKeyword(keyword: String): Boolean {
        val trimmed = keyword.trim()
        if (trimmed.isEmpty() || trimmed.length > MAX_KEYWORD_LENGTH) return false
        val current = cachedKeywords.toMutableList()
        if (current.size >= MAX_KEYWORDS || current.contains(trimmed)) return false
        current.add(trimmed)
        persistAndCache(current)
        return true
    }

    /** 异步移除 */
    suspend fun removeKeyword(keyword: String): Boolean {
        val current = cachedKeywords.toMutableList()
        if (!current.contains(keyword)) return false
        current.remove(keyword)
        persistAndCache(current)
        return true
    }

    private suspend fun persistAndCache(list: List<String>) {
        cachedKeywords = list.toList()
        context.keywordDataStore.edit { prefs ->
            prefs[KEY_KEYWORDS_JSON] = JSONArray(list).toString()
        }
    }

    private fun parseKeywords(input: String): List<String> {
        try {
            val arr = JSONArray(input)
            val result = mutableListOf<String>()
            for (i in 0 until arr.length()) result.add(arr.getString(i))
            return result
        } catch (_: Exception) {
            return input.split("##").filter { it.isNotEmpty() }
        }
    }

    private fun parseJsonKeywords(json: String): List<String>? = try {
        val arr = JSONArray(json)
        (0 until arr.length()).map { arr.getString(it) }
    } catch (_: Exception) { null }
}
