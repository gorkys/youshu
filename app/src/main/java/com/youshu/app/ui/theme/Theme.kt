package com.youshu.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = OrangeStart,
    onPrimary = Color.White,
    primaryContainer = OrangeLight,
    secondary = OrangeEnd,
    background = BackgroundWarm,
    surface = CardWhite,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    surfaceVariant = OrangeTint,
    outline = DividerSoft
)

private val DarkColorScheme = darkColorScheme(
    primary = OrangeStart,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF4A2600),
    secondary = OrangeEnd,
    background = DarkBackground,
    surface = DarkCard,
    onBackground = Color.White,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF2C2C2C),
    outline = Color(0xFF3C3C3C)
)

@Composable
fun YouShuTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = YouShuTypography,
        shapes = YouShuShapes,
        content = content
    )
}
