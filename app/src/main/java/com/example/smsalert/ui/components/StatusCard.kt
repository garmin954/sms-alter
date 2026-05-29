package com.example.smsalert.ui.components

import android.app.AlarmManager
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.smsalert.R
import com.example.smsalert.ui.theme.*

data class PermissionItem(
    val title: String,
    val status: String,
    val granted: Boolean,
    val settingType: String = "",
)

fun checkAllPermissions(context: Context): List<PermissionItem> {
    val hasReceiveSms = ContextCompat.checkSelfPermission(
        context, Manifest.permission.RECEIVE_SMS
    ) == PackageManager.PERMISSION_GRANTED
    val hasReadSms = ContextCompat.checkSelfPermission(
        context, Manifest.permission.READ_SMS
    ) == PackageManager.PERMISSION_GRANTED
    val smsGranted = hasReceiveSms && hasReadSms

    val hasNotification = if (Build.VERSION.SDK_INT >= 33) {
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else true

    val hasOverlay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        Settings.canDrawOverlays(context)
    } else true

    val isIgnoringBattery = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        pm.isIgnoringBatteryOptimizations(context.packageName)
    } else true

    // 厂商权限无法通过标准 API 准确检查，始终需用户手动确认

    val hasExactAlarm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.canScheduleExactAlarms()
    } else true

    val grantedStr = context.getString(R.string.perm_granted)
    val notGrantedStr = context.getString(R.string.perm_not_granted)
    val manualStr = context.getString(R.string.perm_manual_setup)

    return listOf(
        PermissionItem(
            title = context.getString(R.string.perm_sms),
            status = if (smsGranted) grantedStr else notGrantedStr,
            granted = smsGranted,
            settingType = "sms",
        ),
        PermissionItem(
            title = context.getString(R.string.perm_notification),
            status = if (hasNotification) grantedStr else notGrantedStr,
            granted = hasNotification,
            settingType = "notification",
        ),
        PermissionItem(
            title = context.getString(R.string.perm_battery),
            status = if (isIgnoringBattery) grantedStr else notGrantedStr,
            granted = isIgnoringBattery,
            settingType = "battery",
        ),
        PermissionItem(
            title = context.getString(R.string.perm_overlay),
            status = if (hasOverlay) grantedStr else notGrantedStr,
            granted = hasOverlay,
            settingType = "overlay",
        ),
        PermissionItem(
            title = context.getString(R.string.perm_autostart),
            status = manualStr,
            granted = false,
            settingType = "autostart",
        ),
        PermissionItem(
            title = context.getString(R.string.perm_lockscreen),
            status = manualStr,
            granted = false,
            settingType = "lockscreen",
        ),
        PermissionItem(
            title = context.getString(R.string.perm_bg_popup),
            status = manualStr,
            granted = false,
            settingType = "bgpopup",
        ),
        PermissionItem(
            title = context.getString(R.string.perm_alarm),
            status = if (hasExactAlarm) grantedStr else notGrantedStr,
            granted = hasExactAlarm,
            settingType = "alarm",
        ),
    )
}

fun checkEssentialPermissions(context: Context): Boolean {
    val smsGranted = ContextCompat.checkSelfPermission(
        context, Manifest.permission.RECEIVE_SMS
    ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
        context, Manifest.permission.READ_SMS
    ) == PackageManager.PERMISSION_GRANTED

    val hasNotification = if (Build.VERSION.SDK_INT >= 33) {
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else true

    val hasOverlay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        Settings.canDrawOverlays(context)
    } else true

    val isIgnoringBattery = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        pm.isIgnoringBatteryOptimizations(context.packageName)
    } else true

    return smsGranted && hasNotification && hasOverlay && isIgnoringBattery
}

@Composable
fun StatusCard(
    permissions: List<PermissionItem>,
    onRequestAll: () -> Unit,
    onOpenSetting: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.cardBackground)
            .padding(24.dp),
    ) {
        Text(
            text = stringResource(R.string.permission_status_title),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = colors.darkBlue,
        )

        Spacer(modifier = Modifier.height(16.dp))

        permissions.forEach { item ->
            StatusRow(
                title = item.title,
                status = item.status,
                granted = item.granted,
                onClick = if (item.settingType.isNotEmpty()) {
                    { onOpenSetting(item.settingType) }
                } else null,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onRequestAll,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colors.primaryBlue),
        ) {
            Text(
                text = stringResource(R.string.request_all_permissions),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = androidx.compose.ui.graphics.Color.White,
            )
        }
    }
}

@Composable
private fun StatusRow(
    title: String,
    status: String,
    granted: Boolean,
    onClick: (() -> Unit)? = null,
) {
    val colors = LocalAppColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .then(
                if (onClick != null) Modifier.clickable { onClick() }
                else Modifier
            ),
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = if (granted) colors.primaryBlue else colors.textGray,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = colors.darkBlue,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = status,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = if (granted) colors.primaryBlue else colors.textGray,
        )
        if (onClick != null) {
            Spacer(modifier = Modifier.width(2.dp))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = stringResource(R.string.open_settings_description),
                tint = colors.textGray.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
