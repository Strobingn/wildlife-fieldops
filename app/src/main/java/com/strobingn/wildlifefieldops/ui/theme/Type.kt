package com.strobingn.wildlifefieldops.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val UiFont = FontFamily.SansSerif

val AppTypography = Typography(
    displayLarge = TextStyle(UiFont, FontWeight.Bold, 38.sp, 44.sp, (-0.8).sp),
    displayMedium = TextStyle(UiFont, FontWeight.Bold, 32.sp, 38.sp, (-0.6).sp),
    displaySmall = TextStyle(UiFont, FontWeight.Bold, 28.sp, 34.sp, (-0.35).sp),
    headlineLarge = TextStyle(UiFont, FontWeight.Bold, 26.sp, 32.sp, (-0.25).sp),
    headlineMedium = TextStyle(UiFont, FontWeight.Bold, 22.sp, 28.sp, (-0.15).sp),
    headlineSmall = TextStyle(UiFont, FontWeight.SemiBold, 19.sp, 24.sp, 0.sp),
    titleLarge = TextStyle(UiFont, FontWeight.Bold, 20.sp, 25.sp, (-0.1).sp),
    titleMedium = TextStyle(UiFont, FontWeight.SemiBold, 16.sp, 21.sp, 0.sp),
    titleSmall = TextStyle(UiFont, FontWeight.SemiBold, 14.sp, 19.sp, 0.05.sp),
    bodyLarge = TextStyle(UiFont, FontWeight.Normal, 16.sp, 23.sp, 0.sp),
    bodyMedium = TextStyle(UiFont, FontWeight.Normal, 14.sp, 20.sp, 0.05.sp),
    bodySmall = TextStyle(UiFont, FontWeight.Normal, 12.sp, 17.sp, 0.1.sp),
    labelLarge = TextStyle(UiFont, FontWeight.SemiBold, 14.sp, 19.sp, 0.05.sp),
    labelMedium = TextStyle(UiFont, FontWeight.Medium, 12.sp, 16.sp, 0.2.sp),
    labelSmall = TextStyle(UiFont, FontWeight.Medium, 11.sp, 14.sp, 0.25.sp)
)
