package com.example.pulse.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

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
        "https://api.github.com/repos/garmin954/sms-alter/releases/latest"

    suspend fun check(currentVersionName: String): UpdateResult = withContext(Dispatchers.IO) {
        try {
            val connection = URL(GITHUB_API_URL).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext UpdateResult.Error("HTTP $responseCode")
            }

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            val tagName = json.getString("tag_name")
            val htmlUrl = json.getString("html_url")
            val changelog = json.optString("body", "")

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
        } catch (e: IOException) {
            UpdateResult.Error(e.message ?: "Network error")
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
