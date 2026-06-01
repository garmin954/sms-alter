package com.example.pulse.data

import com.example.pulse.LogStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

sealed class UpdateResult {
    data class UpdateAvailable(
        val versionName: String,
        val htmlUrl: String,
        val changelog: String,
    ) : UpdateResult()

    data object UpToDate : UpdateResult()
    data class Error(val message: String) : UpdateResult()
}

object UpdateChecker {

    private const val GITHUB_API_URL =
        "https://api.github.com/repos/garmin954/sms-alter/releases"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun check(currentVersionName: String): UpdateResult = withContext(Dispatchers.IO) {
        try {
            LogStore.d("检查更新: GET $GITHUB_API_URL")
            val request = Request.Builder()
                .url(GITHUB_API_URL)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "Pulse-Android-App")
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = when {
                    response.code == 403 && body.contains("rate limit", ignoreCase = true) ->
                        "GitHub API 限流，请稍后再试"
                    response.code == 404 -> "仓库不可访问"
                    else -> "检查失败（HTTP ${response.code}）"
                }
                return@withContext UpdateResult.Error(errorMsg)
            }

            val releases = JSONArray(body)
            var latestRelease: org.json.JSONObject? = null
            for (i in 0 until releases.length()) {
                val release = releases.getJSONObject(i)
                if (!release.optBoolean("draft", false)) {
                    latestRelease = release
                    break
                }
            }
            if (latestRelease == null) {
                return@withContext UpdateResult.UpToDate
            }

            val tagName = latestRelease.getString("tag_name")
            val htmlUrl = latestRelease.getString("html_url")
            val changelog = latestRelease.optString("body", "暂无更新说明").take(1000)

            val latestVersion = tagName.removePrefix("v")

            if (isVersionNewer(latestVersion, currentVersionName)) {
                UpdateResult.UpdateAvailable(
                    versionName = latestVersion,
                    htmlUrl = htmlUrl,
                    changelog = changelog,
                )
            } else {
                UpdateResult.UpToDate
            }
        } catch (e: SocketTimeoutException) {
            UpdateResult.Error("连接超时，请检查网络")
        } catch (e: IOException) {
            LogStore.e("检查更新异常: ${e.message}")
            UpdateResult.Error(e.message?.take(100) ?: "网络错误")
        }
    }

    private fun isVersionNewer(latest: String, current: String): Boolean {
        val latestParts = latest.split(".").map { it.toIntOrNull() ?: 0 }
        val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }
        val maxLen = maxOf(latestParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val l = latestParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (l != c) return l > c
        }
        return false
    }
}
