package com.example.smsalert.viewmodel

import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import com.example.smsalert.AlertService
import com.example.smsalert.KeywordStore
import com.example.smsalert.MonitorService
import com.example.smsalert.R
import com.example.smsalert.SmsReceiver
import com.example.smsalert.ui.components.checkEssentialPermissions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    application: Application,
) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("sms_alert_prefs", Application.MODE_PRIVATE)

    private val _isListening = MutableStateFlow(
        prefs.getBoolean("is_listening", MonitorService.isRunning())
    )
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _keywords = MutableStateFlow(KeywordStore.getKeywords(application))
    val keywords: StateFlow<List<String>> = _keywords.asStateFlow()

    private val _showPermissionDialog = MutableStateFlow(false)
    val showPermissionDialog: StateFlow<Boolean> = _showPermissionDialog.asStateFlow()

    fun toggleListening() {
        val nextState = !_isListening.value
        val context = getApplication<Application>()

        if (nextState && !checkEssentialPermissions(context)) {
            _showPermissionDialog.value = true
            return
        }

        _isListening.value = nextState
        prefs.edit().putBoolean("is_listening", nextState).apply()

        if (nextState) {
            MonitorService.start(context)
        } else {
            MonitorService.stop(context)
        }
        SmsReceiver.setEnabled(context, nextState)
    }

    fun dismissPermissionDialog() {
        _showPermissionDialog.value = false
    }

    fun addKeyword(keyword: String) {
        val context = getApplication<Application>()
        KeywordStore.addKeyword(context, keyword)
        _keywords.value = KeywordStore.getKeywords(context)
    }

    fun removeKeyword(keyword: String) {
        val context = getApplication<Application>()
        KeywordStore.removeKeyword(context, keyword)
        _keywords.value = KeywordStore.getKeywords(context)
    }

    fun testAlarm() {
        val context = getApplication<Application>()
        val intent = Intent(context, AlertService::class.java).apply {
            putExtra("msg", context.getString(R.string.test_alarm_message))
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
}
