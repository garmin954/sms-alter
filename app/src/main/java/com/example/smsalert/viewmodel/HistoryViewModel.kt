package com.example.smsalert.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smsalert.LogStore
import com.example.smsalert.data.dao.AlertDao
import com.example.smsalert.data.entity.AlertRecord
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    application: Application,
    private val alertDao: AlertDao,
) : AndroidViewModel(application) {

    private val _logs = MutableStateFlow(LogStore.entries)
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    private val _logCount = MutableStateFlow(LogStore.entries.size)
    val logCount: StateFlow<Int> = _logCount.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val alertRecords: StateFlow<List<AlertRecord>> = combine(
        alertDao.getAll(),
        _searchQuery,
    ) { alerts, query ->
        if (query.isBlank()) alerts
        else alerts.filter { it.message.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _alertCountToday = MutableStateFlow(0)
    val alertCountToday: StateFlow<Int> = _alertCountToday.asStateFlow()

    init {
        viewModelScope.launch {
            LogStore.events.collect {
                _logs.value = LogStore.entries
                _logCount.value = LogStore.entries.size
            }
        }
        refreshAlertCount()
    }

    private fun refreshAlertCount() {
        viewModelScope.launch {
            val todayStart = System.currentTimeMillis() - (System.currentTimeMillis() % 86400000L)
            _alertCountToday.value = alertDao.countSince(todayStart)
        }
    }

    fun updateSearch(query: String) {
        _searchQuery.value = query
    }

    fun clearLogs() {
        LogStore.clear()
        _logs.value = emptyList()
        _logCount.value = 0
    }

    fun clearAlerts() {
        viewModelScope.launch {
            alertDao.clearAll()
            refreshAlertCount()
        }
    }

    fun deleteAlert(id: Long) {
        viewModelScope.launch {
            alertDao.deleteById(id)
            refreshAlertCount()
        }
    }
}
