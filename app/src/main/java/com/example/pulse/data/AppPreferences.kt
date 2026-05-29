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
        private const val LEGACY_PREFS = "sms_alert_prefs"
        private const val LEGACY_KEY = "is_listening"
    }

    val isListening: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_IS_LISTENING] ?: context.getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE)
            .getBoolean(LEGACY_KEY, true)
    }

    val lastUpdateCheck: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[KEY_LAST_UPDATE_CHECK] ?: 0L
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
