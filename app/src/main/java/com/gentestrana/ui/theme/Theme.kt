package com.gentestrana.ui.theme

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

// **NEW: Define Neuro Light and Dark Color Schemes**
private val NeuroDarkColorScheme = darkColorScheme(
    primary = NeuroPrimary,       // Example: Use NeuroPrimary as primary
    secondary = NeuroSecondary,     // Example: Use NeuroSecondary as secondary
    tertiary = NeuroAccent,      // Example: Use NeuroAccent as tertiary
    background = NeuroBackground,   // Example: Use NeuroBackground as background
    surface = NeuroSurface,      // Example: Use NeuroSurface as surface
    onPrimary = Color.White,      // Example: Adjust 'on' colors for contrast
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = NeuroPrimary,   // Example: 'onBackground' should contrast with background
    onSurface = NeuroPrimary,      // Example: 'onSurface' should contrast with surface
    secondaryContainer = NeuroBeige // non ha senso metterlo anche qui
    )

private val NeuroLightColorScheme = lightColorScheme(
    primary = NeuroPrimary,       // Example: Use NeuroPrimary as primary
    secondary = NeuroSecondary,     // Example: Use NeuroSecondary as secondary
    tertiary = NeuroAccent,      // Example: Use NeuroAccent as tertiary
    background = NeuroBackground,   // Example: Use NeuroBackground as background
    surface = NeuroSurface,      // Example: Use NeuroSurface as surface
    onPrimary = Color.White,      // Example: Adjust 'on' colors for contrast
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = NeuroPrimary,   // Example: 'onBackground' should contrast with background
    onSurface = NeuroPrimary,      // Example: 'onSurface' should contrast with surface
    secondaryContainer = NeuroBeige
    )

@Composable
fun GenteStranaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> NeuroDarkColorScheme // **Use NeuroDarkColorScheme**
        else -> NeuroLightColorScheme // **Use NeuroLightColorScheme**
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}