package com.entertainmentbrowser.presentation.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = RedPrimary,
    onPrimary = TextPrimary,
    primaryContainer = RedPrimaryVariant,
    onPrimaryContainer = TextPrimary,
    
    secondary = AccentPurple,
    onSecondary = TextPrimary,
    secondaryContainer = AccentPurple,
    onSecondaryContainer = TextPrimary,
    
    tertiary = AccentOrange,
    onTertiary = TextPrimary,
    
    background = DarkBackground,
    onBackground = TextPrimary,
    
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    
    error = ErrorRed,
    onError = TextPrimary,
    
    outline = TextTertiary,
    outlineVariant = DarkSurfaceVariant
)

private val LightColorScheme = lightColorScheme(
    primary = RedPrimary,
    onPrimary = TextPrimary,
    primaryContainer = RedPrimaryVariant,
    onPrimaryContainer = TextPrimary,
    
    secondary = AccentPurple,
    onSecondary = TextPrimary,
    
    tertiary = AccentOrange,
    onTertiary = TextPrimary,
    
    background = TextPrimary,
    onBackground = DarkBackground,
    
    surface = TextPrimary,
    onSurface = DarkBackground,
    
    error = ErrorRed,
    onError = TextPrimary
)

@Composable
fun EntertainmentBrowserTheme(
    darkTheme: Boolean = true, // Always use dark theme by default
    dynamicColor: Boolean = false, // Disable dynamic colors to prevent red flash from Material You
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
