package com.example.pulse.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pulse.data.AppPreferences
import com.example.pulse.data.UpdateChecker
import com.example.pulse.data.UpdateResult
import com.example.pulse.ui.components.PermissionItem
import com.example.pulse.ui.components.checkAllPermissions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class UpdateUiState {
    data object Idle : UpdateUiState()
    data object Checking : UpdateUiState()
    data object UpToDate : UpdateUiState()
    data class UpdateAvailable(val versionName: String, val htmlUrl: String, val changelog: String) : UpdateUiState()
    data class Error(val message: String) : UpdateUiState()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application,
    private val appPreferences: AppPreferences,
) : AndroidViewModel(application) {

    private val _permissions = MutableStateFlow(checkAllPermissions(application))
    val permissions: StateFlow<List<PermissionItem>> = _permissions.asStateFlow()

    private val _updateState = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val updateState: StateFlow<UpdateUiState> = _updateState.asStateFlow()

    val themeMode = appPreferences.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun setThemeMode(mode: Int) {
        viewModelScope.launch { appPreferences.setThemeMode(mode) }
    }

    val currentVersion: String = try {
        val info = application.packageManager.getPackageInfo(application.packageName, 0)
        info.versionName ?: "unknown"
    } catch (e: Exception) {
        "unknown"
    }

    fun refreshPermissions() {
        _permissions.value = checkAllPermissions(getApplication())
    }

    fun resetUpdateState() {
        _updateState.value = UpdateUiState.Idle
    }

    fun checkForUpdates() {
        if (_updateState.value is UpdateUiState.Checking) return
        _updateState.value = UpdateUiState.Checking
        viewModelScope.launch {
            when (val result = UpdateChecker.check(currentVersion)) {
                is UpdateResult.UpdateAvailable -> {
                    _updateState.value = UpdateUiState.UpdateAvailable(
                        versionName = result.versionName,
                        htmlUrl = result.htmlUrl,
                        changelog = result.changelog,
                    )
                }
                is UpdateResult.UpToDate -> {
                    _updateState.value = UpdateUiState.UpToDate
                }
                is UpdateResult.Error -> {
                    _updateState.value = UpdateUiState.Error(result.message)
                }
            }
        }
    }
}
