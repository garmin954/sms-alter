package com.example.smsalert.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.smsalert.ui.components.PermissionItem
import com.example.smsalert.ui.components.checkAllPermissions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val _permissions = MutableStateFlow(checkAllPermissions(application))
    val permissions: StateFlow<List<PermissionItem>> = _permissions.asStateFlow()

    fun refreshPermissions() {
        _permissions.value = checkAllPermissions(getApplication())
    }
}
