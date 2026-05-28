package com.example.smsalert

import android.content.Context

object KeywordStore {

    private const val PREFS_NAME = "sms_alert_prefs"
    private const val KEY_KEYWORDS = "keywords"
    private val DEFAULT_KEYWORDS = listOf("ALERT", "紧急", "验证码", "服务器宕机")

    /**
     * 获取当前所有关键词
     */
    fun getKeywords(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedStr = prefs.getString(KEY_KEYWORDS, null) ?: return DEFAULT_KEYWORDS
        if (savedStr.isEmpty()) return emptyList()
        return savedStr.split("##")
    }

    /**
     * 添加新关键词
     */
    fun addKeyword(context: Context, keyword: String): Boolean {
        val trimmed = keyword.trim()
        if (trimmed.isEmpty()) return false
        val current = getKeywords(context).toMutableList()
        if (current.contains(trimmed)) return false
        current.add(trimmed)
        saveKeywords(context, current)
        return true
    }

    /**
     * 移除关键词
     */
    fun removeKeyword(context: Context, keyword: String): Boolean {
        val current = getKeywords(context).toMutableList()
        if (!current.contains(keyword)) return false
        current.remove(keyword)
        saveKeywords(context, current)
        return true
    }

    /**
     * 保存关键词列表到 SharedPreferences
     */
    private fun saveKeywords(context: Context, list: List<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val joined = list.joinToString("##")
        prefs.edit().putString(KEY_KEYWORDS, joined).apply()
    }

    /**
     * 匹配短信文本中是否包含任何关键词
     */
    fun match(context: Context, text: String): Boolean {
        val list = getKeywords(context)
        return list.any {
            text.contains(it, ignoreCase = true)
        }
    }
}
