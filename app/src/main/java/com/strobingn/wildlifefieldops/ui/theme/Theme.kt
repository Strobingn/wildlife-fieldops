package com.strobingn.wildlifefieldops.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryGreen,
    onPrimary = Color(0xFF17191C),
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = Color(0xFF9DA5AE),
    onSecondary = Color(0xFF17191C),
    background = BackgroundDark,
    onBackground = TextPrimary,
    surface = BackgroundCard,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = BorderDark,
    outlineVariant = DividerDark,
    error = ErrorRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF555B63),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE1E4E8),
    onPrimaryContainer = Color(0xFF24272B),
    secondary = Color(0xFF6D747D),
    onSecondary = Color.White,
    background = Color(0xFFF4F5F6),
    onBackground = Color(0xFF202327),
    surface = Color.White,
    onSurface = Color(0xFF202327),
    surfaceVariant = Color(0xFFE8EAED),
    onSurfaceVariant = Color(0xFF555B63),
    outline = Color(0xFFB9BEC5),
    outlineVariant = Color(0xFFD5D9DE),
    error = Color(0xFFB3261E),
    onError = Color.White
)

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

@Composable
fun WildlifeFieldOpsTheme(
    darkTheme: Boolean = ThemeController.isDark,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val scheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context.findActivity() ?: return@SideEffect
            activity.window.statusBarColor = scheme.background.toArgb()
            activity.window.navigationBarColor = scheme.surface.toArgb()
            WindowCompat.getInsetsController(activity.window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(activity.window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }
    MaterialTheme(colorScheme = scheme, typography = AppTypography, shapes = AppShapes, content = content)
}
