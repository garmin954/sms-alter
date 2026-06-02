package com.example.pulse.viewmodel

import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pulse.AlertService
import com.example.pulse.KeywordStore
import com.example.pulse.MonitorService
import com.example.pulse.R
import com.example.pulse.SmsReceiver
import com.example.pulse.data.AppPreferences
import com.example.pulse.ui.components.checkEssentialPermissions
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
    private val keywordStore: KeywordStore,
) : AndroidViewModel(application) {

    val isListening: StateFlow<Boolean> = appPreferences.isListening
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MonitorService.isRunning())

    private fun formatDuration(ms: Long): String {
        val totalSec = ms / 1000
        val sec = totalSec % 60
        val min = totalSec / 60 % 60
        val hour = totalSec / 3600 % 24
        val day = totalSec / 86400 % 30
        val month = totalSec / 2592000 % 12
        val year = totalSec / 31104000

        return buildString {
            if (year > 0) append("${year}年")
            if (month > 0) append("${month}月")
            if (day > 0) append("${day}天")
            if (hour > 0) append("${hour}时")
            if (min > 0) append("${min}分")
            if (sec > 0 || isEmpty()) append("${sec}秒")
        }
    }

    val elapsedTime: StateFlow<String> = flow {
        while (true) {
            if (MonitorService.isRunning()) {
                emit(formatDuration(MonitorService.getElapsedMs()))
            } else {
                emit("")
            }
            delay(1000)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    private val _keywords = MutableStateFlow(keywordStore.getKeywords())
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
        viewModelScope.launch {
            keywordStore.addKeyword(keyword)
            _keywords.value = keywordStore.getKeywords()
        }
    }

    fun removeKeyword(keyword: String) {
        viewModelScope.launch {
            keywordStore.removeKeyword(keyword)
            _keywords.value = keywordStore.getKeywords()
        }
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

