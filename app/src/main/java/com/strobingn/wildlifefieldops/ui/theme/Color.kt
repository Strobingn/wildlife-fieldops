package com.strobingn.wildlifefieldops.ui.theme

import androidx.compose.ui.graphics.Color

// ── Brand (cool field blue) ────────────────────────────────────────────────
// Names are retained for call-site compatibility.
val PrimaryGreen = Color(0xFF3B7AE8)
val PrimaryGreenDark = Color(0xFF245FC4)
val PrimaryGreenLight = Color(0xFF93C5FD)
val PrimaryContainer = Color(0xFFD6E6FF)
val OnPrimaryContainer = Color(0xFF001833)

// ── Light-first compatibility surfaces ─────────────────────────────────────
// Most screens still reference these legacy names directly. Keeping them light
// makes every existing screen consistent until all call sites migrate fully to
// MaterialTheme.colorScheme.
val BackgroundDark = Color(0xFFF5F6F8)
val BackgroundCard = Color(0xFFFFFFFF)
val BackgroundElevated = Color(0xFFF0F3F7)
val SurfaceDark = Color(0xFFFFFFFF)
val SurfaceVariant = Color(0xFFE7EBF0)
val SurfaceBright = Color(0xFFFFFFFF)

// ── Text ────────────────────────────────────────────────────────────────────
val TextPrimary = Color(0xFF171A1F)
val TextSecondary = Color(0xFF515866)
val TextTertiary = Color(0xFF747D8C)

// ── Status ──────────────────────────────────────────────────────────────────
val StatusPending = Color(0xFFF59E0B)
val StatusInProgress = Color(0xFF3B82F6)
val StatusCompleted = Color(0xFF2563EB)
val StatusCancelled = Color(0xFFDC2626)
val StatusUrgent = Color(0xFFDC2626)

// ── Accents ─────────────────────────────────────────────────────────────────
val AccentBlue = Color(0xFF3B82F6)
val AccentPurple = Color(0xFF8B5CF6)
val AccentOrange = Color(0xFFF97316)
val AccentCyan = Color(0xFF0284C7)
val AccentPink = Color(0xFFDB2777)
val AccentAmber = Color(0xFFD97706)

// ── Borders / chrome ────────────────────────────────────────────────────────
val BorderDark = Color(0xFFC9D0DA)
val DividerDark = Color(0xFFE0E5EC)
val ScrimDark = Color(0x66000000)

// ── Semantic ────────────────────────────────────────────────────────────────
val ErrorRed = Color(0xFFDC2626)
val ErrorRedDark = Color(0xFFB91C1C)
val SuccessGreen = Color(0xFF2563EB)
val WarningYellow = Color(0xFFF59E0B)
val InfoBlue = Color(0xFF3B82F6)

// ── Gradients ───────────────────────────────────────────────────────────────
val GradientStart = Color(0xFFE8F0FF)
val GradientMid = Color(0xFFCFE0FF)
val GradientEnd = Color(0xFF6EA8FF)
