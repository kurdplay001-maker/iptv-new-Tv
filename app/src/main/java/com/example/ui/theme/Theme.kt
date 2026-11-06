package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = KurdPlayBluePrimary,
    onPrimary = Color.White,
    primaryContainer = KurdPlayBluePrimary.copy(alpha = 0.2f),
    onPrimaryContainer = KurdPlayBlueLight,
    secondary = KurdPlayRedAccent,
    onSecondary = Color.White,
    tertiary = KurdPlayAmberAccent,
    background = KurdPlayBg,
    surface = KurdPlaySurface,
    surfaceVariant = KurdPlaySurfaceVariant,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = KurdPlayBorder
)

@Composable
fun KurdPlayTheme(
    darkTheme: Boolean = true, // Default to sleek dark-mode IPTV layout
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) = KurdPlayTheme(darkTheme = darkTheme, content = content)
