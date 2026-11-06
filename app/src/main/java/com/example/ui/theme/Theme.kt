package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = ArduinoTeal,
    onPrimary = Color.White,
    primaryContainer = ArduinoTealDark,
    onPrimaryContainer = ArduinoTealBright,
    secondary = GeminiCyan,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF0D3B42),
    onSecondaryContainer = GeminiCyan,
    tertiary = GeminiPurple,
    onTertiary = Color.White,
    background = IdeBackground,
    onBackground = IdeTextPrimary,
    surface = IdeSurface,
    onSurface = IdeTextPrimary,
    surfaceVariant = IdeSurfaceVariant,
    onSurfaceVariant = IdeTextSecondary,
    outline = IdeBorder,
    outlineVariant = Color(0xFF30363D),
    error = StatusError,
    onError = Color.White
)

@Composable
fun DroidDuinoTheme(
    content: @Composable () -> Unit
) {
    // For a code IDE experience, a consistent dark studio theme provides optimal syntax contrast
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
