package com.example.pulse.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        val KEY_IS_LISTENING = booleanPreferencesKey("is_listening")
        val KEY_LAST_UPDATE_CHECK = longPreferencesKey("last_update_check")
        val KEY_MONITOR_START_TIME = longPreferencesKey("monitor_start_time")
        private const val LEGACY_PREFS = "sms_alert_prefs"
        private const val LEGACY_KEY = "is_listening"
        private const val MONITOR_LEGACY_PREFS = "monitor_prefs"
        private const val MONITOR_LEGACY_KEY = "start_time_elapsed"
    }

    val isListening: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_IS_LISTENING] ?: context.getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE)
            .getBoolean(LEGACY_KEY, true)
    }

    val lastUpdateCheck: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[KEY_LAST_UPDATE_CHECK] ?: 0L
    }

    /** 一次性读取 monitor 启动时间（恢复进程后使用） */
    suspend fun getMonitorStartTime(): Long {
        return context.dataStore.data.first()[KEY_MONITOR_START_TIME]
            ?: context.getSharedPreferences(MONITOR_LEGACY_PREFS, Context.MODE_PRIVATE)
                .getLong(MONITOR_LEGACY_KEY, 0L)
    }

    suspend fun saveMonitorStartTime(value: Long) {
        context.dataStore.edit { prefs ->
            prefs[KEY_MONITOR_START_TIME] = value
        }
    }

    suspend fun clearMonitorStartTime() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_MONITOR_START_TIME)
        }
    }

    suspend fun setIsListening(value: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_IS_LISTENING] = value
        }
    }

    suspend fun setLastUpdateCheck(value: Long) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LAST_UPDATE_CHECK] = value
        }
    }
}


