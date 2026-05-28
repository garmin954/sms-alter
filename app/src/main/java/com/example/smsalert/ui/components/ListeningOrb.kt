package com.example.smsalert.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smsalert.ui.theme.DarkBlue
import com.example.smsalert.ui.theme.PausedGray
import com.example.smsalert.ui.theme.PrimaryBlue
import com.example.smsalert.ui.theme.TextGray

@Composable
fun ListeningOrb(
    isListening: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Color transition
    val orbColor by animateColorAsState(
        targetValue = if (isListening) PrimaryBlue else PausedGray,
        animationSpec = tween(300),
        label = "orbColor",
    )

    // Breathing animation
    val breathScale by rememberInfiniteTransition(label = "breath").animateFloat(
        initialValue = 1.0f,
        targetValue = if (isListening) 1.04f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breathScale",
    )

    // Ripple layer 1
    val ripple1Alpha by rememberInfiniteTransition(label = "ripple1").animateFloat(
        initialValue = 0.18f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ripple1Alpha",
    )
    val ripple1Scale by rememberInfiniteTransition(label = "ripple1Scale").animateFloat(
        initialValue = 1.0f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ripple1Scale",
    )

    // Ripple layer 2
    val ripple2Alpha by rememberInfiniteTransition(label = "ripple2").animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ripple2Alpha",
    )
    val ripple2Scale by rememberInfiniteTransition(label = "ripple2Scale").animateFloat(
        initialValue = 1.0f,
        targetValue = 1.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ripple2Scale",
    )

    // Ripple layer 3
    val ripple3Alpha by rememberInfiniteTransition(label = "ripple3").animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ripple3Alpha",
    )
    val ripple3Scale by rememberInfiniteTransition(label = "ripple3Scale").animateFloat(
        initialValue = 1.0f,
        targetValue = 1.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ripple3Scale",
    )

    // Better ripple phases using delay
    val rippleAnimProgress by rememberInfiniteTransition(label = "rippleProgress").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "rippleProgress",
    )

    val r1Progress = rippleAnimProgress
    val r2Progress = ((rippleAnimProgress + 0.33f) % 1.0f)
    val r3Progress = ((rippleAnimProgress + 0.66f) % 1.0f)

    val r1a = if (isListening) (0.18f * (1f - r1Progress)) else 0f
    val r1s = 1.0f + (r1Progress * 0.45f)
    val r2a = if (isListening) (0.12f * (1f - r2Progress)) else 0f
    val r2s = 1.0f + (r2Progress * 0.65f)
    val r3a = if (isListening) (0.08f * (1f - r3Progress)) else 0f
    val r3s = 1.0f + (r3Progress * 0.9f)

    val orbAlpha by animateFloatAsState(
        targetValue = if (isListening) 1.0f else 0.7f,
        animationSpec = tween(300),
        label = "orbAlpha",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        // Orb area
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(240.dp),
        ) {
            // Ripple layer 3
            if (isListening && r3a > 0.001f) {
                Canvas(
                    modifier = Modifier
                        .size(160.dp)
                        .scale(r3s)
                        .alpha(r3a),
                ) {
                    drawCircle(
                        color = PrimaryBlue,
                        radius = size.minDimension / 2f,
                    )
                }
            }
            // Ripple layer 2
            if (isListening && r2a > 0.001f) {
                Canvas(
                    modifier = Modifier
                        .size(160.dp)
                        .scale(r2s)
                        .alpha(r2a),
                ) {
                    drawCircle(
                        color = PrimaryBlue,
                        radius = size.minDimension / 2f,
                    )
                }
            }
            // Ripple layer 1
            if (isListening && r1a > 0.001f) {
                Canvas(
                    modifier = Modifier
                        .size(160.dp)
                        .scale(r1s)
                        .alpha(r1a),
                ) {
                    drawCircle(
                        color = PrimaryBlue,
                        radius = size.minDimension / 2f,
                    )
                }
            }

            // Main circle with white stroke
            Canvas(
                modifier = Modifier
                    .size(160.dp)
                    .scale(breathScale)
                    .alpha(orbAlpha)
                    .clickable { onClick() },
            ) {
                // Shadow simulation with darker edge
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            orbColor,
                            orbColor.copy(alpha = 0.85f),
                        ),
                    ),
                    radius = size.minDimension / 2f,
                )
                // White stroke
                drawCircle(
                    color = Color.White,
                    radius = size.minDimension / 2f,
                    style = Stroke(width = 4.dp.toPx()),
                )
            }

            // Inner content
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.size(160.dp).clickable { onClick() },
            ) {
                Spacer(modifier = Modifier.height(52.dp))
                Text(
                    text = if (isListening) "AI" else "OFF",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isListening) "MONITORING" else "PAUSED",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.85f),
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Status text
        Text(
            text = if (isListening) "点击图标切换监听状态" else "监听已暂停",
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            color = TextGray,
        )
    }
}
