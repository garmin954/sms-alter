package com.example.pulse

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.pulse.ui.components.BottomNavBar
import com.example.pulse.ui.screens.DashboardScreen
import com.example.pulse.ui.screens.HistoryScreen
import com.example.pulse.ui.screens.SettingsScreen
import com.example.pulse.data.AppPreferences
import com.example.pulse.ui.theme.LocalAppColors
import com.example.pulse.ui.theme.SmsAlertTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var appPreferences: AppPreferences

    private var permissionRefreshKey by mutableIntStateOf(0)

    private val launcher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) {
            permissionRefreshKey++
        }

    override fun onResume() {
        super.onResume()
        permissionRefreshKey++
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val shouldListen = runBlocking { appPreferences.isListening.first() }
        SmsReceiver.setEnabled(this, shouldListen)
        if (shouldListen) {
            MonitorService.start(this)
        }

        setContent {
            SmsAlertTheme {
                MainNavGraph(
                    onRequestPermissions = { requestRuntimePermissions() },
                    onOpenSetting = { type -> openPermissionSetting(type) },
                    permissionsRefreshKey = permissionRefreshKey,
                )
            }
        }
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    @Composable
    private fun MainNavGraph(
        onRequestPermissions: () -> Unit,
        onOpenSetting: (String) -> Unit,
        permissionsRefreshKey: Int,
    ) {
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route ?: "home"
        val colors = LocalAppColors.current
        val activity = androidx.compose.ui.platform.LocalContext.current as android.app.Activity
        val windowSizeClass = calculateWindowSizeClass(activity)
        val contentMaxWidth = if (windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded) 600.dp else null

        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.background),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .then(contentMaxWidth?.let { Modifier.widthIn(max = it) } ?: Modifier)
                        .fillMaxWidth()
                        .statusBarsPadding(),
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = "home",
                    ) {
                        composable("home") { DashboardScreen() }
                        composable("logs") { HistoryScreen() }
                        composable("settings") {
                            SettingsScreen(
                                onRequestPermissions = onRequestPermissions,
                                onOpenSetting = onOpenSetting,
                                permissionsRefreshKey = permissionsRefreshKey,
                            )
                        }
                    }
                }

                BottomNavBar(
                    selectedRoute = currentRoute,
                    onItemClick = { item ->
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        }
    }

    private fun requestRuntimePermissions() {
        val list = mutableListOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.VIBRATE,
        )
        if (Build.VERSION.SDK_INT >= 33) {
            list.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        launcher.launch(list.toTypedArray())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                try {
                    startActivity(
                        Intent(
                            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                            Uri.parse("package:$packageName")
                        )
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun openPermissionSetting(type: String) {
        when (type) {
            "sms" -> requestRuntimePermissions()
            "notification" -> {
                if (Build.VERSION.SDK_INT >= 33) {
                    requestRuntimePermissions()
                } else {
                    openAppSettings()
                }
            }
            "battery" -> openBatterySettings()
            "overlay" -> openOverlaySettings()
            "autostart" -> openAppSettings()
            "lockscreen" -> openAppSettings()
            "bgpopup" -> openAppSettings()
            "alarm" -> openAlarmSettings()
        }
    }

    private fun openBatterySettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                startActivity(
                    Intent(
                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:$packageName")
                    )
                )
            } catch (e: Exception) {
                openAppSettings()
            }
        }
    }

    private fun openOverlaySettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                )
                Toast.makeText(
                    this,
                    getString(R.string.overlay_permission_hint),
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                openAppSettings()
            }
        }
    }

    private fun openAutoStartSettings() {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val intents = mutableListOf<Intent>()

        when {
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") -> {
                intents.add(Intent().setComponent(
                    ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
                ))
                intents.add(Intent().setComponent(
                    ComponentName("com.miui.securitycenter", "com.miui.appmanager.ApplicationsManagerActivity")
                ))
            }
            manufacturer.contains("oppo") -> {
                intents.add(Intent().setComponent(
                    ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")
                ))
                intents.add(Intent().setComponent(
                    ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startupapp.StartupAppListActivity")
                ))
                intents.add(Intent().setComponent(
                    ComponentName("com.color.safecenter", "com.color.safecenter.permission.floatwindow.FloatWindowListActivity")
                ))
            }
            manufacturer.contains("vivo") -> {
                intents.add(Intent().setComponent(
                    ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")
                ))
                intents.add(Intent().setComponent(
                    ComponentName("com.iqoo.secure", "com.iqoo.secure.MainActivity")
                ))
            }
            manufacturer.contains("huawei") || manufacturer.contains("honor") -> {
                intents.add(Intent().setComponent(
                    ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity")
                ))
                intents.add(Intent().setComponent(
                    ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")
                ))
            }
            manufacturer.contains("samsung") -> {
                intents.add(Intent().setComponent(
                    ComponentName("com.samsung.android.sm_cn", "com.samsung.android.sm_cn.ui.ramguard.RamGuardActivity")
                ))
            }
        }

        for (intent in intents) {
            try {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                Toast.makeText(this, getString(R.string.autostart_permission_hint), Toast.LENGTH_LONG).show()
                return
            } catch (e: Exception) {
                // try next intent
            }
        }

        openAppSettings()
        Toast.makeText(this, getString(R.string.autostart_manual_hint), Toast.LENGTH_LONG).show()
    }

    private fun openAlarmSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:$packageName")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            } catch (e: Exception) {
                openAppSettings()
            }
        } else {
            openAppSettings()
        }
    }

    private fun openLockScreenSettings() {
        try {
            val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                putExtra(Settings.EXTRA_CHANNEL_ID, "sms_alert_channel")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
            Toast.makeText(this, getString(R.string.lockscreen_permission_hint), Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            try {
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
                Toast.makeText(this, getString(R.string.lockscreen_notification_hint), Toast.LENGTH_LONG).show()
            } catch (e2: Exception) {
                openAppSettings()
            }
        }
    }

    private fun openAppSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.manual_settings_hint), Toast.LENGTH_SHORT).show()
        }
    }
}
