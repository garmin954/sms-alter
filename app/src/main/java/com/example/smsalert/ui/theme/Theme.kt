package com.example.smsalert.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

private val LightColorScheme = lightColorScheme(
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

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlueDark,
    onPrimary = CardBackgroundDark,
    primaryContainer = PrimaryBlueDark,
    background = BackgroundDark,
    surface = CardBackgroundDark,
    onBackground = DarkBlueDark,
    onSurface = DarkBlueDark,
    outline = BorderGrayDark,
    error = AlertRedDark,
    onError = CardBackgroundDark,
)

data class AppColors(
    val background: androidx.compose.ui.graphics.Color,
    val cardBackground: androidx.compose.ui.graphics.Color,
    val inputBackground: androidx.compose.ui.graphics.Color,
    val chipBackground: androidx.compose.ui.graphics.Color,
    val borderGray: androidx.compose.ui.graphics.Color,
    val textGray: androidx.compose.ui.graphics.Color,
    val primaryBlue: androidx.compose.ui.graphics.Color,
    val darkBlue: androidx.compose.ui.graphics.Color,
    val dangerRed: androidx.compose.ui.graphics.Color,
    val alertRed: androidx.compose.ui.graphics.Color,
    val bottomBarBackground: androidx.compose.ui.graphics.Color,
    val pausedGray: androidx.compose.ui.graphics.Color,
    val bottomNavActiveBg: androidx.compose.ui.graphics.Color,
)

val LightAppColors = AppColors(
    background = Background,
    cardBackground = CardBackground,
    inputBackground = InputBackground,
    chipBackground = ChipBackground,
    borderGray = BorderGray,
    textGray = TextGray,
    primaryBlue = PrimaryBlue,
    darkBlue = DarkBlue,
    dangerRed = DangerRed,
    alertRed = AlertRed,
    bottomBarBackground = BottomBarBackground,
    pausedGray = PausedGray,
    bottomNavActiveBg = BottomNavActiveBg,
)

val DarkAppColors = AppColors(
    background = BackgroundDark,
    cardBackground = CardBackgroundDark,
    inputBackground = InputBackgroundDark,
    chipBackground = ChipBackgroundDark,
    borderGray = BorderGrayDark,
    textGray = TextGrayDark,
    primaryBlue = PrimaryBlueDark,
    darkBlue = DarkBlueDark,
    dangerRed = DangerRedDark,
    alertRed = AlertRedDark,
    bottomBarBackground = BottomBarBackgroundDark,
    pausedGray = PausedGrayDark,
    bottomNavActiveBg = BottomNavActiveBgDark,
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }

@Composable
fun SmsAlertTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val appColors = if (darkTheme) DarkAppColors else LightAppColors

    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content,
        )
    }
}
