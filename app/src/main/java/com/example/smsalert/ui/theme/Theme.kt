package com.example.smsalert.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val AppColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = CardBackground,
    primaryContainer = PrimaryBlue,
    background = Background,
    surface = CardBackground,
    onBackground = DarkBlue,
    onSurface = DarkBlue,
    outline = BorderGray,
    error = AlertRed,
    onError = CardBackground,
)

@Composable
fun SmsAlertTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = AppTypography,
        content = content,
    )
}
