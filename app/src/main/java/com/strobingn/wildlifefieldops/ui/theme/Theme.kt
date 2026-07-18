package com.strobingn.wildlifefieldops.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFD1D5DB),
    onPrimary = Color(0xFF1B1D20),
    primaryContainer = Color(0xFF454A51),
    onPrimaryContainer = Color(0xFFF3F4F6),
    secondary = Color(0xFFB9BDC4),
    onSecondary = Color(0xFF1B1D20),
    secondaryContainer = Color(0xFF3B4046),
    onSecondaryContainer = Color(0xFFE8EAED),
    tertiary = Color(0xFFFBBF24),
    onTertiary = Color(0xFF3D2A00),
    tertiaryContainer = Color(0xFF3D2E0A),
    onTertiaryContainer = Color(0xFFFFE08A),
    background = Color(0xFF1B1D20),
    onBackground = Color(0xFFE8EAED),
    surface = Color(0xFF2B2E33),
    onSurface = Color(0xFFE8EAED),
    surfaceVariant = Color(0xFF3B4046),
    onSurfaceVariant = Color(0xFFB9BDC4),
    surfaceBright = Color(0xFF454A51),
    surfaceContainerLowest = Color(0xFF16181B),
    surfaceContainerLow = Color(0xFF222528),
    surfaceContainer = Color(0xFF2B2E33),
    surfaceContainerHigh = Color(0xFF35393F),
    surfaceContainerHighest = Color(0xFF3B4046),
    error = Color(0xFFFF6B6B),
    onError = Color(0xFF3B0000),
    errorContainer = Color(0xFF5C1010),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF4A5058),
    outlineVariant = Color(0xFF383D43),
    scrim = Color(0xCC000000),
    inverseSurface = Color(0xFFE4E8F0),
    inverseOnSurface = Color(0xFF1A1E26),
    inversePrimary = Color(0xFF9CA3AF)
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryGreen,
    onPrimary = BackgroundDark,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = AccentCyan,
    onSecondary = BackgroundDark,
    secondaryContainer = Color(0xFFE5E7EB),
    onSecondaryContainer = Color(0xFF2B2E33),
    tertiary = AccentAmber,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE8C2),
    onTertiaryContainer = Color(0xFF2A1700),
    background = BackgroundDark,
    onBackground = TextPrimary,
    surface = BackgroundCard,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = TextSecondary,
    surfaceBright = SurfaceBright,
    surfaceContainerLowest = BackgroundCard,
    surfaceContainerLow = BackgroundDark,
    surfaceContainer = BackgroundElevated,
    surfaceContainerHigh = SurfaceVariant,
    surfaceContainerHighest = Color(0xFFDDE3EB),
    error = ErrorRed,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = BorderDark,
    outlineVariant = DividerDark,
    scrim = Color.Black,
    inverseSurface = Color(0xFF272C34),
    inverseOnSurface = Color(0xFFEFF2F6),
    inversePrimary = PrimaryGreenLight
)

private fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

@Composable
fun WildlifeFieldOpsTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context.findActivity() ?: return@SideEffect
            val window = activity.window
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.background.toArgb()
            @Suppress("DEPRECATION")
            window.navigationBarColor = colorScheme.surfaceContainerLow.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
