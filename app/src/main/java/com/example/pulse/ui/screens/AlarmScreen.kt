package com.example.pulse.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pulse.AlertService
import com.example.pulse.LogStore
import com.example.pulse.R
import com.example.pulse.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 用户确认前的 UI 倒计时秒数 */
private const val COUNTDOWN_SECONDS = 20

/** Pulse 在系统时钟中的闹钟唯一标签，用于先删后建确保只有一个 Pulse 闹钟，避免误删其他应用闹钟 */
private const val PULSE_ALARM_LABEL = "Pulse 紧急短信告警"

/**
 * 通过 PULSE_ALARM_LABEL 固定标签在系统时钟 App 创建可见闹钟。
 * skipUi=true 静默创建，不弹出时钟界面。
 * 系统闹钟 API 仅支持分钟级精度，闹钟设在下一整分触发（最长 60s 延迟）。
 * 调用前应先 tryDismissPulseAlarm() 清理旧闹钟，确保标签下只有一个。
 */
private fun setSystemAlarmViaIntent(context: Context) {
    val calendar = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.SECOND, 0)
        add(java.util.Calendar.MINUTE, 1)
    }
    val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
    val minute = calendar.get(java.util.Calendar.MINUTE)
    val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
        putExtra(AlarmClock.EXTRA_HOUR, hour)
        putExtra(AlarmClock.EXTRA_MINUTES, minute)
        putExtra(AlarmClock.EXTRA_MESSAGE, PULSE_ALARM_LABEL)
        putExtra(AlarmClock.EXTRA_VIBRATE, true)
        putExtra(AlarmClock.EXTRA_SKIP_UI, true)
        putExtra(AlarmClock.EXTRA_DAYS, ArrayList<Int>())  // 仅一次，不重复
    }
    try {
        context.startActivity(intent)
        LogStore.i("系统闹钟已创建：${"%02d".format(hour)}:${"%02d".format(minute)}，标签：$PULSE_ALARM_LABEL")
    } catch (e: Exception) {
        LogStore.w("ACTION_SET_ALARM 失败：${e.message}")
    }
}

/**
 * 按 PULSE_ALARM_LABEL 固定标签删除 Pulse 系统闹钟，不会误删其他应用的闹钟。
 */
private fun tryDismissPulseAlarm(context: Context) {
    try {
        val intent = Intent(AlarmClock.ACTION_DISMISS_ALARM).apply {
            putExtra(AlarmClock.EXTRA_ALARM_SEARCH_MODE, AlarmClock.ALARM_SEARCH_MODE_LABEL)
            putExtra(AlarmClock.EXTRA_MESSAGE, PULSE_ALARM_LABEL)
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
        }
        context.startActivity(intent)
        LogStore.i("已尝试撤销 Pulse 系统闹钟（标签：$PULSE_ALARM_LABEL）")
    } catch (e: Exception) {
        LogStore.w("撤销 Pulse 系统闹钟失败：${e.message}")
    }
}


@Composable
fun AlarmScreen(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    alarmFired: Boolean = false,
    onAlarmFired: () -> Unit = {},
) {
    val context = LocalContext.current

    val timeStr = remember {
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
    }

    var remainingSeconds by remember { mutableIntStateOf(COUNTDOWN_SECONDS) }
    var systemAlarmCreated by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (alarmFired) return@LaunchedEffect
        while (remainingSeconds > 0) {
            kotlinx.coroutines.delay(1000)
            remainingSeconds--
        }
        // 倒计时归零：先删旧的系统闹钟（防止残留），再建新的兜底
        tryDismissPulseAlarm(context)
        setSystemAlarmViaIntent(context)
        systemAlarmCreated = true
        LogStore.i("倒计时到期，系统闹钟已重新创建，停止 AlertService")
        context.stopService(Intent(context, AlertService::class.java))
        onAlarmFired()
    }

    // 用户主动确认：仅当系统闹钟已创建时才尝试撤销（避免20s内确认时无意义跳转系统时钟App）
    val dismissWithCancel: () -> Unit = {
        if (systemAlarmCreated) {
            tryDismissPulseAlarm(context)
        }
        context.stopService(Intent(context, AlertService::class.java))
        LogStore.i("用户已确认警报，AlertService 已停止")
        onDismiss()
    }

    // Warning icon flash (500ms, alpha 1.0 ↔ 0.15)
    val iconAlpha by rememberInfiniteTransition(label = "icon").animateFloat(
        initialValue = 1.0f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "iconAlpha",
    )

    // Full-screen breathing pulse (1500ms, alpha 0.0 ↔ 1.0)
    val pulseAlpha by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.0f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseAlpha",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AlarmBg),
    ) {
        // Full-screen breathing pulse overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(pulseAlpha)
                .background(AlarmPulseRed),
        )

        // Main scrollable content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp)
                .padding(top = 48.dp, bottom = 36.dp),
        ) {
            // Warning triangle icon — large and bright
            Text(
                text = "⚠",
                fontSize = 84.sp,
                color = AlarmIconAmber,
                modifier = Modifier.alpha(iconAlpha),
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Countdown timer
            if (remainingSeconds > 0) {
                Text(
                    text = "${remainingSeconds}s",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = AlarmCardText,
                    letterSpacing = 2.sp,
                )
                Spacer(modifier = Modifier.height(8.dp))
            } else {
                Text(
                    text = "等待系统闹钟唤醒...",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = AlarmSubtitleRed,
                    letterSpacing = 1.sp,
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Title
            Text(
                text = stringResource(R.string.alarm_title),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = AlarmTitleRed,
                letterSpacing = 1.sp,
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Subtitle
            Text(
                text = "EMERGENCY PULSE",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = AlarmSubtitleRed,
                letterSpacing = 2.sp,
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Divider
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(AlarmDividerRed),
            )

            Spacer(modifier = Modifier.height(24.dp))

            // SMS content card
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(AlarmCardBg)
                    .border(1.dp, AlarmDividerRed, RoundedCornerShape(16.dp))
                    .padding(20.dp),
            ) {
                Text(
                    text = stringResource(R.string.alarm_sms_content_label),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AlarmCardLabel,
                    letterSpacing = 0.8.sp,
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "[$timeStr]\n\n$message",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = AlarmCardText,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Confirm button
            Button(
                onClick = dismissWithCancel,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AlarmButtonRed,
                    contentColor = AlarmCardText,
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 1.dp,
                ),
            ) {
                Text(
                    text = stringResource(R.string.alarm_confirm_button),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = stringResource(R.string.alarm_dismiss_hint),
                fontSize = 12.sp,
                color = AlarmHintText,
            )
        }
    }
}
