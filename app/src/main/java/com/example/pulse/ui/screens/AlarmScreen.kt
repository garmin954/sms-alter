package com.example.pulse.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import android.app.AlarmManager
import android.app.PendingIntent
import android.os.Build
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
import com.example.pulse.AlertService
import com.example.pulse.AlarmReceiver
import com.example.pulse.LogStore
import com.example.pulse.R
import com.example.pulse.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 用户确认前的 UI 倒计时秒数 */
private const val COUNTDOWN_SECONDS = 20

/** 倒计时归零后，系统闹钟延迟秒数（从归零时刻起算）*/
private const val FALLBACK_DELAY_SECONDS = 15L

/** AlarmReceiver 兜底闹钟的 PendingIntent request code */
private const val FALLBACK_ALARM_REQUEST_CODE = 9002

/**
 * 通过 ACTION_SET_ALARM 在系统时钟 App 创建可见闹钟（用户可在时钟 App 中看到）。
 * skipUi=true 静默创建，不弹出时钟界面。
 *
 * 系统闹钟 API 仅支持分钟级精度，实际触发时间为「当前时间 + FALLBACK_DELAY_SECONDS」
 * 向上取整到下一整分，偏差范围 0 ~ 59 秒。
 */
private fun setSystemAlarmViaIntent(context: Context, message: String) {
    val calendar = java.util.Calendar.getInstance().apply {
        // 当前时间 + 15s，再向上取整到下一整分
        add(java.util.Calendar.SECOND, FALLBACK_DELAY_SECONDS.toInt())
        if (get(java.util.Calendar.SECOND) > 0) {
            set(java.util.Calendar.SECOND, 0)
            add(java.util.Calendar.MINUTE, 1)
        }
    }
    val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
    val minute = calendar.get(java.util.Calendar.MINUTE)
    val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
        putExtra(AlarmClock.EXTRA_HOUR, hour)
        putExtra(AlarmClock.EXTRA_MINUTES, minute)
        putExtra(AlarmClock.EXTRA_MESSAGE, message.take(40))
        putExtra(AlarmClock.EXTRA_VIBRATE, true)
        putExtra(AlarmClock.EXTRA_SKIP_UI, true)
        putExtra(AlarmClock.EXTRA_DAYS, ArrayList<Int>())  // 仅一次，不重复
    }
    try {
        context.startActivity(intent)
        LogStore.i("系统闹钟已创建：${hour}:${String.format("%02d", minute)}")
    } catch (e: Exception) {
        LogStore.w("ACTION_SET_ALARM 失败：${e.message}")
    }
}

/**
 * 尝试通过 ACTION_DISMISS_ALARM 按标签撤销系统闹钟（尽力而为，不保证成功）。
 */
private fun tryDismissSystemAlarm(context: Context, message: String) {
    try {
        val intent = Intent(AlarmClock.ACTION_DISMISS_ALARM).apply {
            putExtra(AlarmClock.EXTRA_ALARM_SEARCH_MODE, AlarmClock.ALARM_SEARCH_MODE_LABEL)
            putExtra(AlarmClock.EXTRA_MESSAGE, message.take(40))
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
        }
        context.startActivity(intent)
        LogStore.i("已尝试撤销系统闹钟（ACTION_DISMISS_ALARM）")
    } catch (e: Exception) {
        LogStore.w("撤销系统闹钟失败：${e.message}")
    }
}

/**
 * 通过 AlarmManager.setExact(RTC_WAKEUP) 设置精确兜底闹钟。
 * 15s 后触发 AlarmReceiver → 重启 AlertService 重新拉起报警界面。
 * 相比 ACTION_SET_ALARM 仅分钟级精度，setExact 提供毫秒级精确唤醒。
 */
private fun setExactFallbackAlarm(context: Context, message: String) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
    val intent = Intent(context, AlarmReceiver::class.java).apply {
        putExtra("msg", message)
    }
    val pendingIntent = PendingIntent.getBroadcast(
        context, FALLBACK_ALARM_REQUEST_CODE, intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
    val triggerTime = System.currentTimeMillis() + FALLBACK_DELAY_SECONDS * 1000L
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
        alarmManager.setWindow(AlarmManager.RTC_WAKEUP, triggerTime, 30_000L, pendingIntent)
        LogStore.w("兜底闹钟已设置（setWindow，无精确闹钟权限）")
    } else {
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        LogStore.i("兜底闹钟已设置（setExact，${FALLBACK_DELAY_SECONDS}秒后触发）")
    }
}

/**
 * 取消 AlarmManager 兜底闹钟。
 * 用户确认报警后调用，防止 15s 后 AlarmReceiver 误触发。
 */
private fun cancelExactFallbackAlarm(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
    val intent = Intent(context, AlarmReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        context, FALLBACK_ALARM_REQUEST_CODE, intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
    alarmManager.cancel(pendingIntent)
    pendingIntent.cancel()
    LogStore.i("兜底闹钟已取消")
}


@Composable
fun AlarmScreen(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val timeStr = remember {
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
    }

    var remainingSeconds by remember { mutableIntStateOf(COUNTDOWN_SECONDS) }

    LaunchedEffect(Unit) {
        while (remainingSeconds > 0) {
            kotlinx.coroutines.delay(1000)
            remainingSeconds--
        }
        // 倒计时归零：创建系统时钟可见闹钟，停止 AlertService
        setExactFallbackAlarm(context, message)
        setSystemAlarmViaIntent(context, message)
        LogStore.i("倒计时到期，停止 AlertService，系统闹钟已设置")
        context.stopService(Intent(context, AlertService::class.java))
    }

    // 用户主动确认：尝试撤销系统时钟闹钟，停止 AlertService
    val dismissWithCancel: () -> Unit = {
        cancelExactFallbackAlarm(context)
        tryDismissSystemAlarm(context, message)
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
