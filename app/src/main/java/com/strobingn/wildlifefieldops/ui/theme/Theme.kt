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
    primary = PrimaryGreen,
    onPrimary = Color(0xFF00391C),
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = AccentCyan,
    onSecondary = Color(0xFF003731),
    secondaryContainer = Color(0xFF0E3D38),
    onSecondaryContainer = Color(0xFF9FF2E4),
    tertiary = AccentAmber,
    onTertiary = Color(0xFF3D2A00),
    tertiaryContainer = Color(0xFF3D2E0A),
    onTertiaryContainer = Color(0xFFFFE08A),
    background = BackgroundDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = TextSecondary,
    surfaceBright = SurfaceBright,
    surfaceContainerLowest = BackgroundDark,
    surfaceContainerLow = BackgroundCard,
    surfaceContainer = BackgroundElevated,
    surfaceContainerHigh = SurfaceVariant,
    surfaceContainerHighest = SurfaceBright,
    error = ErrorRed,
    onError = Color(0xFF3B0000),
    errorContainer = Color(0xFF5C1010),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = BorderDark,
    outlineVariant = DividerDark,
    scrim = ScrimDark,
    inverseSurface = Color(0xFFE4EAE6),
    inverseOnSurface = Color(0xFF1A211D),
    inversePrimary = PrimaryGreenDark
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryGreenDark,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC8F5D9),
    onPrimaryContainer = Color(0xFF002112),
    secondary = Color(0xFF0D7377),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFC4F5F0),
    onSecondaryContainer = Color(0xFF00201E),
    tertiary = Color(0xFFB45309),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFE8C2),
    onTertiaryContainer = Color(0xFF2A1700),
    background = Color(0xFFF6F9F7),
    onBackground = Color(0xFF121816),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF121816),
    surfaceVariant = Color(0xFFE6EDE9),
    onSurfaceVariant = Color(0xFF424B46),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF0F5F2),
    surfaceContainer = Color(0xFFEAEFEA),
    surfaceContainerHigh = Color(0xFFE4EAE6),
    surfaceContainerHighest = Color(0xFFDEE5E0),
    error = ErrorRedDark,
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFFC5D0C9),
    outlineVariant = Color(0xFFDCE5DF),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF272E2A),
    inverseOnSurface = Color(0xFFEFF4F0),
    inversePrimary = PrimaryGreen
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
    darkTheme: Boolean = true, // Field ops prefers dark; avoid light flash on launch
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
