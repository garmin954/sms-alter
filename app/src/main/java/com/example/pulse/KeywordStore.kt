package com.example.pulse

import android.content.Context
import org.json.JSONArray

object KeywordStore {

    private const val PREFS_NAME = "sms_alert_prefs"
    private const val KEY_KEYWORDS = "keywords"
    private val DEFAULT_KEYWORDS = listOf("ALERT", "紧急", "交警", "服务器宕机")
    private const val MAX_KEYWORDS = 50
    private const val MAX_KEYWORD_LENGTH = 50

    fun getKeywords(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedStr = prefs.getString(KEY_KEYWORDS, null) ?: return DEFAULT_KEYWORDS
        if (savedStr.isEmpty()) return emptyList()

        // Try JSON first (current format)
        try {
            val arr = JSONArray(savedStr)
            val result = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                result.add(arr.getString(i))
            }
            return result
        } catch (_: Exception) {
            // Migration: old ##-delimited format
            val migrated = savedStr.split("##").filter { it.isNotEmpty() }
            if (migrated.isNotEmpty()) {
                saveKeywordsInternal(prefs, migrated)
            }
            return migrated
        }
    }

    fun addKeyword(context: Context, keyword: String): Boolean {
        val trimmed = keyword.trim()
        if (trimmed.isEmpty() || trimmed.length > MAX_KEYWORD_LENGTH) return false
        val current = getKeywords(context).toMutableList()
        if (current.size >= MAX_KEYWORDS) return false
        if (current.contains(trimmed)) return false
        current.add(trimmed)
        saveKeywords(context, current)
        return true
    }

    fun removeKeyword(context: Context, keyword: String): Boolean {
        val current = getKeywords(context).toMutableList()
        if (!current.contains(keyword)) return false
        current.remove(keyword)
        saveKeywords(context, current)
        return true
    }

    private fun saveKeywords(context: Context, list: List<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        saveKeywordsInternal(prefs, list)
    }

    private fun saveKeywordsInternal(prefs: android.content.SharedPreferences, list: List<String>) {
        val json = JSONArray(list).toString()
        prefs.edit().putString(KEY_KEYWORDS, json).apply()
    }

    fun match(context: Context, text: String): Boolean {
        val list = getKeywords(context)
        return list.any {
            text.contains(it, ignoreCase = true)
        }
    }
}
