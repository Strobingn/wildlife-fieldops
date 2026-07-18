package com.strobingn.wildlifefieldops.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class AppThemeMode { DARK, LIGHT }

object ThemeController {
    var mode by mutableStateOf(AppThemeMode.DARK)
        private set

    val isDark: Boolean get() = mode == AppThemeMode.DARK

    fun setDark(enabled: Boolean) {
        mode = if (enabled) AppThemeMode.DARK else AppThemeMode.LIGHT
    }
}