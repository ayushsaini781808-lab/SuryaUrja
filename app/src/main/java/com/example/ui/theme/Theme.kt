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
    primary = SolarAmber,
    onPrimary = Color.Black,
    primaryContainer = SolarAmberDark,
    onPrimaryContainer = SolarAmberLight,
    secondary = SolarCyan,
    onSecondary = Color.Black,
    secondaryContainer = SolarCyanDark,
    onSecondaryContainer = SolarCyanLight,
    tertiary = SolarEmerald,
    onTertiary = Color.Black,
    background = SolarDarkBackground,
    onBackground = TextPrimaryDark,
    surface = SolarDarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = SolarDarkSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    outline = SolarDarkBorder,
    error = SolarRose,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = SolarAmberDark,
    onPrimary = Color.White,
    primaryContainer = SolarAmberLight,
    onPrimaryContainer = Color.Black,
    secondary = SolarCyanDark,
    onSecondary = Color.White,
    secondaryContainer = SolarCyanLight,
    onSecondaryContainer = Color.Black,
    tertiary = SolarEmerald,
    onTertiary = Color.White,
    background = SolarLightBackground,
    onBackground = TextPrimaryLight,
    surface = SolarLightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = SolarLightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    outline = SolarLightBorder,
    error = SolarRose,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to dark aesthetic for solar telemetry dashboard
    dynamicColor: Boolean = false, // Keep branded high-contrast theme
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
