package com.example.smsalert.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    // Warning icon flash (500ms, alpha 1.0 ↔ 0.15, reverse infinite)
    val warningAlpha by rememberInfiniteTransition(label = "warning").animateFloat(
        initialValue = 1.0f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "warningAlpha",
    )

    // Background pulse starts after 1.2s delay
    var pulseStarted by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1200)
        pulseStarted = true
    }

    val pulseAlpha by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseAlpha",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AlarmBg),
    ) {
        // Pulse background layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AlarmPulseRed)
                .alpha(if (pulseStarted) pulseAlpha else 0.3f),
        )

        // Main scrollable content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp)
                .padding(top = 80.dp, bottom = 32.dp),
        ) {
            // Warning icon
            Text(
                text = "⚠",
                fontSize = 72.sp,
                modifier = Modifier.alpha(warningAlpha),
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "紧急短信警报",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = AlarmTitleRed,
                letterSpacing = 0.08.sp,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "EMERGENCY SMS ALERT",
                fontSize = 14.sp,
                color = AlarmSubtitleRed,
                letterSpacing = 0.12.sp,
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Divider
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(3.dp)
                    .background(AlarmDividerRed),
            )

            Spacer(modifier = Modifier.height(28.dp))

            // SMS content card
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = AlarmCardBg,
                        shape = RoundedCornerShape(0.dp),
                    )
                    .padding(20.dp),
            ) {
                Text(
                    text = "短信内容",
                    fontSize = 11.sp,
                    color = AlarmCardLabel,
                    letterSpacing = 0.06.sp,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "[$timeStr]\n\n$message",
                    fontSize = 16.sp,
                    color = AlarmCardText,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp,
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .width(220.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AlarmButtonRed,
                    contentColor = AlarmCardText,
                ),
            ) {
                Text(
                    text = "我已确认",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.06.sp,
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "点击确认后将关闭警报",
                fontSize = 11.sp,
                color = AlarmHintText,
            )
        }
    }
}
