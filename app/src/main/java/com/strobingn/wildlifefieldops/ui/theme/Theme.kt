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
    primary = Color(0xFF5B9DFF),
    onPrimary = Color(0xFF001833),
    primaryContainer = Color(0xFF1A2F4A),
    onPrimaryContainer = Color(0xFFD6E6FF),
    secondary = Color(0xFF38BDF8),
    onSecondary = Color(0xFF00263A),
    secondaryContainer = Color(0xFF0C2A40),
    onSecondaryContainer = Color(0xFFCFEFFF),
    tertiary = Color(0xFFFBBF24),
    onTertiary = Color(0xFF3D2A00),
    tertiaryContainer = Color(0xFF3D2E0A),
    onTertiaryContainer = Color(0xFFFFE08A),
    background = Color(0xFF0B0D12),
    onBackground = Color(0xFFF2F4F8),
    surface = Color(0xFF141820),
    onSurface = Color(0xFFF2F4F8),
    surfaceVariant = Color(0xFF222833),
    onSurfaceVariant = Color(0xFFA8B0BD),
    surfaceBright = Color(0xFF2A3140),
    surfaceContainerLowest = Color(0xFF0B0D12),
    surfaceContainerLow = Color(0xFF141820),
    surfaceContainer = Color(0xFF1B212B),
    surfaceContainerHigh = Color(0xFF222833),
    surfaceContainerHighest = Color(0xFF2A3140),
    error = Color(0xFFFF6B6B),
    onError = Color(0xFF3B0000),
    errorContainer = Color(0xFF5C1010),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF2E3644),
    outlineVariant = Color(0xFF1F2530),
    scrim = Color(0xCC000000),
    inverseSurface = Color(0xFFE4E8F0),
    inverseOnSurface = Color(0xFF1A1E26),
    inversePrimary = Color(0xFF3B7AE8)
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryGreen,
    onPrimary = Color.White,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = AccentCyan,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF0C4A6E),
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
    darkTheme: Boolean = false,
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
