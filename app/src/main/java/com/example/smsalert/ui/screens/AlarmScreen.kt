package com.example.smsalert.ui.screens

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smsalert.R
import com.example.smsalert.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AlarmScreen(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val timeStr = remember {
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
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
        // Full-screen breathing pulse overlay (always visible)
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
                .padding(top = 56.dp, bottom = 36.dp),
        ) {
            // Warning triangle icon — large and bright
            Text(
                text = "⚠",
                fontSize = 84.sp,
                color = AlarmIconAmber,
                modifier = Modifier.alpha(iconAlpha),
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Title
            Text(
                text = stringResource(R.string.alarm_title),
                fontSize = 26.sp,
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

            Spacer(modifier = Modifier.height(28.dp))

            // Divider
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(AlarmDividerRed),
            )

            Spacer(modifier = Modifier.height(28.dp))

            // SMS content card
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(AlarmCardBg)
                    .border(1.dp, AlarmDividerRed, RoundedCornerShape(16.dp))
                    .padding(24.dp),
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

            Spacer(modifier = Modifier.height(44.dp))

            // Confirm button
            Button(
                onClick = onDismiss,
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
