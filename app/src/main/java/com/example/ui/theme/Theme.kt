package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = AccentBlue,
    onPrimary = Color(0xFF003258),
    primaryContainer = CardAccentDark,
    onPrimaryContainer = AccentBlue,
    secondary = AccentGreen,
    onSecondary = Color(0xFF00390A),
    background = BgDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceDark,
    onSurfaceVariant = TextMutedDark,
    outline = BorderDark,
    error = ErrorRed,
    onError = DarkErrorBg
)

private val LightColorScheme = DarkColorScheme // Standard immersive dark dashboard design for modern status app

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Keep dynamicColor false to preserve custom slate grey & green branding
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = DarkColorScheme
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
