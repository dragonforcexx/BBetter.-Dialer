package com.bbetterdialer.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.bbetterdialer.data.AppPreferences
import com.bbetterdialer.data.ThemeMode

/*
 * ===========================================================================
 *  TÉMA – sötét / világos / rendszer követése
 * ===========================================================================
 *  A `ThemeState.dark` értékét itt állítjuk be; a paletta getterjei ezt
 *  olvassák, ezért a teljes felület automatikusan átvált.
 * ===========================================================================
 */

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF00E676),
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF1C1C1E),
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Color(0xFF9A9AA0),
    onSecondary = Color(0xFF000000),
    tertiary = Color(0xFF9A9AA0),
    background = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF0D0D0D),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF1C1C1E),
    onSurfaceVariant = Color(0xFF9A9AA0),
    outline = Color(0xFF2A2A2C),
    outlineVariant = Color(0xFF2A2A2C),
    error = Color(0xFFFF3B30),
    onError = Color(0xFF000000)
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF00A554),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEDEFF2),
    onPrimaryContainer = Color(0xFF101216),
    secondary = Color(0xFF5A616B),
    onSecondary = Color(0xFFFFFFFF),
    tertiary = Color(0xFF5A616B),
    background = Color(0xFFF5F6F8),
    onBackground = Color(0xFF101216),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF101216),
    surfaceVariant = Color(0xFFEDEFF2),
    onSurfaceVariant = Color(0xFF5A616B),
    outline = Color(0xFFE2E5E9),
    outlineVariant = Color(0xFFE2E5E9),
    error = Color(0xFFFF3B30),
    onError = Color(0xFF000000)
)

@Composable
fun DialerTheme(content: @Composable () -> Unit) {
    val dark = when (AppPreferences.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    ThemeState.dark = dark

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = view.context.findActivity()?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
    }

    MaterialTheme(
        colorScheme = if (dark) DarkScheme else LightScheme,
        content = content
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
