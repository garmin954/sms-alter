package com.example.smsalert.viewmodel

import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smsalert.AlertService
import com.example.smsalert.KeywordStore
import com.example.smsalert.MonitorService
import com.example.smsalert.R
import com.example.smsalert.SmsReceiver
import com.example.smsalert.data.AppPreferences
import com.example.smsalert.ui.components.checkEssentialPermissions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    application: Application,
    private val appPreferences: AppPreferences,
) : AndroidViewModel(application) {

    val isListening: StateFlow<Boolean> = appPreferences.isListening
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MonitorService.isRunning())

    val elapsedTime: StateFlow<String> = flow {
        while (true) {
            if (MonitorService.isRunning()) {
                val totalSec = MonitorService.getElapsedMs() / 1000
                val h = totalSec / 3600
                val m = (totalSec % 3600) / 60
                val s = totalSec % 60
                emit(String.format("%02d:%02d:%02d", h, m, s))
            } else {
                emit("")
            }
            delay(1000)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    private val _keywords = MutableStateFlow(KeywordStore.getKeywords(application))
    val keywords: StateFlow<List<String>> = _keywords.asStateFlow()

    private val _showPermissionDialog = MutableStateFlow(false)
    val showPermissionDialog: StateFlow<Boolean> = _showPermissionDialog.asStateFlow()

    fun toggleListening() {
        val nextState = !isListening.value
        val context = getApplication<Application>()

        if (nextState && !checkEssentialPermissions(context)) {
            _showPermissionDialog.value = true
            return
        }

        viewModelScope.launch {
            appPreferences.setIsListening(nextState)
        }

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
