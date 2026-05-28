package com.example.smsalert.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smsalert.ui.components.StatusCard
import com.example.smsalert.ui.components.checkAllPermissions
import com.example.smsalert.ui.theme.*

@Composable
fun SettingsScreen(
    onRequestPermissions: () -> Unit,
    onOpenSetting: (String) -> Unit = {},
    permissionsRefreshKey: Int,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Text(
            text = "设置",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = DarkBlue,
        )

        Spacer(modifier = Modifier.height(24.dp))

        val permissions = remember(permissionsRefreshKey) { checkAllPermissions(context) }
        StatusCard(
            permissions = permissions,
            onRequestAll = { onRequestPermissions() },
            onOpenSetting = { type -> onOpenSetting(type) },
        )

        Spacer(modifier = Modifier.height(100.dp))
    }
}
