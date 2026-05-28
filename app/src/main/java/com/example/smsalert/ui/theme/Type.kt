package com.example.smsalert.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val AppTypography = Typography(
    titleLarge = TextStyle(
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        color = DarkBlue,
    ),
    headlineMedium = TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        color = DarkBlue,
    ),
    bodyLarge = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = DarkBlue,
    ),
    bodyMedium = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        color = TextGray,
    ),
    labelMedium = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = PrimaryBlue,
    ),
    labelSmall = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
    ),
)
