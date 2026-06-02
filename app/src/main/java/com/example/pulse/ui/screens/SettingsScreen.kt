package com.example.pulse.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.pulse.R
import com.example.pulse.ui.components.StatusCard
import com.example.pulse.ui.theme.*
import com.example.pulse.viewmodel.SettingsViewModel
import com.example.pulse.viewmodel.UpdateUiState

@OptIn(ExperimentalMaterial3Api::class)
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

    val colors = LocalAppColors.current
    val context = LocalContext.current
    val updateState by viewModel.updateState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle one-shot UI events from update state
    LaunchedEffect(updateState) {
        when (val state = updateState) {
            is UpdateUiState.UpToDate -> {
                snackbarHostState.showSnackbar(
                    message = context.getString(R.string.update_up_to_date),
                    duration = SnackbarDuration.Short,
                )
                viewModel.resetUpdateState()
            }
            is UpdateUiState.Error -> {
                snackbarHostState.showSnackbar(
                    message = state.message,
                    duration = SnackbarDuration.Short,
                )
                viewModel.resetUpdateState()
            }
            else -> {}
        }
    }

    // Update available dialog
    val showUpdateDialog = remember { mutableStateOf<UpdateUiState.UpdateAvailable?>(null) }
    LaunchedEffect(updateState) {
        if (updateState is UpdateUiState.UpdateAvailable) {
            showUpdateDialog.value = updateState as UpdateUiState.UpdateAvailable
        }
    }

    showUpdateDialog.value?.let { update ->
        AlertDialog(
            onDismissRequest = {
                showUpdateDialog.value = null
                viewModel.resetUpdateState()
            },
            title = {
                Text(
                    text = stringResource(R.string.update_available_title),
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Column {
                    Text("v${update.versionName}")
                    if (update.changelog.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = update.changelog,
                            fontSize = 13.sp,
                            color = colors.textGray,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(update.htmlUrl))
                    context.startActivity(intent)
                    showUpdateDialog.value = null
                    viewModel.resetUpdateState()
                }) {
                    Text(stringResource(R.string.update_download_button))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showUpdateDialog.value = null
                    viewModel.resetUpdateState()
                }) {
                    Text(stringResource(R.string.dismiss_button))
                }
            },
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = colors.background,
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(colors.background)
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 24.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = colors.darkBlue,
            )

            Spacer(modifier = Modifier.height(24.dp))

            val permissions by viewModel.permissions.collectAsState()
            StatusCard(
                permissions = permissions,
                onRequestAll = { onRequestPermissions() },
                onOpenSetting = { type -> onOpenSetting(type) },
            )

            Spacer(modifier = Modifier.height(32.dp))

            // About & Updates section
            SectionHeader(title = stringResource(R.string.settings_about_title))

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.settings_version_label),
                                fontSize = 14.sp,
                                color = colors.textGray,
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = viewModel.currentVersion,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.darkBlue,
                            )
                        }

                        val isChecking = updateState is UpdateUiState.Checking
                        Button(
                            onClick = { viewModel.checkForUpdates() },
                            enabled = !isChecking,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.primaryBlue,
                                disabledContainerColor = colors.darkBlue.copy(alpha = 0.6f),
                            ),
                        ) {
                            if (isChecking) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = stringResource(
                                    if (isChecking) R.string.settings_checking_update
                                    else R.string.settings_check_update_button
                                ),
                                color = Color.White,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── 外观设置 ──
            SectionHeader(title = stringResource(R.string.settings_appearance_title))

            Spacer(modifier = Modifier.height(12.dp))

            val currentTheme by viewModel.themeMode.collectAsState()
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth(),
            ) {
                SegmentedButton(
                    selected = currentTheme == 0,
                    onClick = { viewModel.setThemeMode(0) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = colors.primaryBlue,
                    ),
                ) {
                    Text(
                        stringResource(R.string.theme_follow_system),
                        color = if (currentTheme == 0) Color.White else colors.textGray
                    )
                }
                SegmentedButton(
                    selected = currentTheme == 1,
                    onClick = { viewModel.setThemeMode(1) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = colors.primaryBlue,
                    ),
                ) {
                    Text(
                        stringResource(R.string.theme_light),
                        color = if (currentTheme == 1) Color.White else colors.textGray
                    )
                }
                SegmentedButton(
                    selected = currentTheme == 2,
                    onClick = { viewModel.setThemeMode(2) },
                    shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = colors.primaryBlue,
                    ),
                ) {
                    Text(
                        stringResource(R.string.theme_dark),
                        color = if (currentTheme == 2) Color.White else colors.textGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}


@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = LocalAppColors.current.textGray,
        letterSpacing = 1.sp,
    )
}
