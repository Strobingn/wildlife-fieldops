package com.strobingn.wildlifefieldops.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryGreen,
    onPrimary = Color(0xFF000000),
    primaryContainer = PrimaryGreenDark,
    onPrimaryContainer = Color(0xFFffffff),
    secondary = AccentBlue,
    onSecondary = Color(0xFFffffff),
    secondaryContainer = Color(0xFF1e3a5f),
    onSecondaryContainer = Color(0xFFbfdbfe),
    tertiary = AccentPurple,
    onTertiary = Color(0xFFffffff),
    tertiaryContainer = Color(0xFF2d1b69),
    onTertiaryContainer = Color(0xFFddd6fe),
    background = BackgroundDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = TextSecondary,
    error = ErrorRed,
    onError = Color(0xFFffffff),
    errorContainer = Color(0xFF450a0a),
    onErrorContainer = Color(0xFFfecaca),
    outline = BorderDark,
    outlineVariant = DividerDark,
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFe4e4e7),
    inverseOnSurface = Color(0xFF1a1a1a),
    inversePrimary = PrimaryGreenLight
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryGreenDark,
    onPrimary = Color(0xFFffffff),
    primaryContainer = PrimaryGreenLight,
    onPrimaryContainer = Color(0xFF052e16),
    secondary = AccentBlue,
    onSecondary = Color(0xFFffffff),
    secondaryContainer = Color(0xFFdbeafe),
    onSecondaryContainer = Color(0xFF1e3a5f),
    tertiary = AccentPurple,
    onTertiary = Color(0xFFffffff),
    tertiaryContainer = Color(0xFFede9fe),
    onTertiaryContainer = Color(0xFF2d1b69),
    background = Color(0xFFfafafa),
    onBackground = Color(0xFF18181b),
    surface = Color(0xFFffffff),
    onSurface = Color(0xFF18181b),
    surfaceVariant = Color(0xFFf4f4f5),
    onSurfaceVariant = Color(0xFF52525b),
    error = ErrorRed,
    onError = Color(0xFFffffff),
    errorContainer = Color(0xFFfee2e2),
    onErrorContainer = Color(0xFF450a0a),
    outline = Color(0xFFd4d4d8),
    outlineVariant = Color(0xFFe4e4e7),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF27272a),
    inverseOnSurface = Color(0xFFf4f4f5),
    inversePrimary = PrimaryGreen
)

@Composable
fun WildlifeFieldOpsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
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
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
