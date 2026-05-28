package com.example.smsalert.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.smsalert.R
import com.example.smsalert.ui.components.StatusCard
import com.example.smsalert.ui.theme.*
import com.example.smsalert.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    onRequestPermissions: () -> Unit,
    onOpenSetting: (String) -> Unit = {},
    permissionsRefreshKey: Int = 0,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    LaunchedEffect(permissionsRefreshKey) {
        viewModel.refreshPermissions()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = DarkBlue,
        )

        Spacer(modifier = Modifier.height(24.dp))

        val permissions by viewModel.permissions.collectAsState()
        StatusCard(
            permissions = permissions,
            onRequestAll = { onRequestPermissions() },
            onOpenSetting = { type -> onOpenSetting(type) },
        )

        Spacer(modifier = Modifier.height(100.dp))
    }
}
