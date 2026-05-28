package com.example.smsalert

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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.smsalert.ui.components.BottomNavBar
import com.example.smsalert.ui.screens.DashboardScreen
import com.example.smsalert.ui.screens.HistoryScreen
import com.example.smsalert.ui.screens.SettingsScreen
import com.example.smsalert.ui.theme.Background
import com.example.smsalert.ui.theme.SmsAlertTheme

class MainActivity : ComponentActivity() {

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

        val prefs = getSharedPreferences("sms_alert_prefs", MODE_PRIVATE)
        val shouldListen = prefs.getBoolean("is_listening", true)
        SmsReceiver.setEnabled(this, shouldListen)
        if (shouldListen) {
            MonitorService.start(this)
        }

        setContent {
            SmsAlertTheme {
                var selectedRoute by remember { mutableStateOf("home") }

                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Background),
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .statusBarsPadding(),
                        ) {
                            when (selectedRoute) {
                                "home" -> DashboardScreen()
                                "logs" -> HistoryScreen()
                                "settings" -> SettingsScreen(
                                    onRequestPermissions = {
                                        requestRuntimePermissions()
                                    },
                                    onOpenSetting = { type ->
                                        openPermissionSetting(type)
                                    },
                                    permissionsRefreshKey = permissionRefreshKey,
                                )
                            }
                        }

                        BottomNavBar(
                            selectedRoute = selectedRoute,
                            onItemClick = { item ->
                                selectedRoute = item.route
                            },
                        )
                    }
                }
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
                    "请开启「显示在其他应用上层」权限以保障锁屏报警弹窗正常工作",
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
                Toast.makeText(this, "请在列表中开启本应用的「自启动」权限", Toast.LENGTH_LONG).show()
                return
            } catch (e: Exception) {
                // try next intent
            }
        }

        openAppSettings()
        Toast.makeText(this, "请手动在设置中开启「自启动」权限", Toast.LENGTH_LONG).show()
    }

    private fun openLockScreenSettings() {
        try {
            val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                putExtra(Settings.EXTRA_CHANNEL_ID, "sms_alert_channel")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
            Toast.makeText(this, "请确保「在锁定屏幕上」设置为显示通知内容", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            try {
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
                Toast.makeText(this, "请在锁定屏幕上显示本应用通知", Toast.LENGTH_LONG).show()
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
            Toast.makeText(this, "请手动前往系统设置配置权限", Toast.LENGTH_SHORT).show()
        }
    }
}
