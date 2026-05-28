package com.example.smsalert.ui.screens

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
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
import com.example.smsalert.AlertService
import com.example.smsalert.AlarmReceiver
import com.example.smsalert.LogStore
import com.example.smsalert.R
import com.example.smsalert.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val COUNTDOWN_SECONDS = 30
private const val ALARM_DELAY_MS = 2_000L
private const val ALARM_REQUEST_CODE = 1001

@Composable
fun AlarmScreen(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val alarmManager = remember { context.getSystemService(Context.ALARM_SERVICE) as AlarmManager }

    val timeStr = remember {
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
    }

    var remainingSeconds by remember { mutableIntStateOf(COUNTDOWN_SECONDS) }

    // 30s countdown: update UI every second
    LaunchedEffect(Unit) {
        while (remainingSeconds > 0) {
            kotlinx.coroutines.delay(1000)
            remainingSeconds--
        }
    }

    // When countdown reaches 0, stop AlertService and set system alarm
    LaunchedEffect(remainingSeconds) {
        if (remainingSeconds == 0) {
            LogStore.i("30s 倒计时到期，停止 AlertService 并设置系统闹钟")
            context.stopService(Intent(context, AlertService::class.java))

            val triggerTime = System.currentTimeMillis() + ALARM_DELAY_MS
            val alarmIntent = Intent(context, AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context, ALARM_REQUEST_CODE, alarmIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

            val canSchedule = alarmManager.canScheduleExactAlarms()
            if (canSchedule) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent,
                )
                LogStore.i("系统闹钟已设置（setExactAndAllowWhileIdle），${ALARM_DELAY_MS / 1000}s 后触发")
            } else {
                // Fallback: set inexact alarm + coroutine backup
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                LogStore.i("精确闹钟权限未授予，使用 set() + 协程兜底")
                kotlinx.coroutines.delay(ALARM_DELAY_MS + 500)
                LogStore.i("协程兜底触发，重新启动 AlertService")
                context.startForegroundService(Intent(context, AlertService::class.java).apply {
                    putExtra("msg", "系统闹钟兜底：紧急短信警报")
                })
            }
        }
    }

    // Dismiss with alarm cancellation
    val dismissWithCancel: () -> Unit = {
        val alarmIntent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, ALARM_REQUEST_CODE, alarmIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE,
        )
        pendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
            LogStore.i("系统闹钟已取消")
        }
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
                    text = "系统闹钟已触发",
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
                text = "EMERGENCY SMS ALERT",
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
