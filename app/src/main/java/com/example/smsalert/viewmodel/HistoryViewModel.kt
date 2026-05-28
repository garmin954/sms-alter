package com.example.smsalert.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smsalert.LogStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val _logs = MutableStateFlow(LogStore.entries)
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    private val _logCount = MutableStateFlow(LogStore.entries.size)
    val logCount: StateFlow<Int> = _logCount.asStateFlow()

    init {
        viewModelScope.launch {
            LogStore.events.collect {
                _logs.value = LogStore.entries
                _logCount.value = LogStore.entries.size
            }
        }
    }

    fun clearLogs() {
        LogStore.clear()
        _logs.value = emptyList()
        _logCount.value = 0
    }
}
