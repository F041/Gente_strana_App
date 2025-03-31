package com.gentestrana.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.gentestrana.screens.AppTheme
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration

private val NeuroDarkColorScheme = darkColorScheme(
    primary = NeuroAccent,
    secondary = NeuroSecondary,
    tertiary = NeuroAccent,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onPrimary = Color.White,        // MODIFICA: da Color.Black a Color.White
    onSecondary = Color.White,
    onTertiary = Color.White,        // MODIFICA: da Color.Black a Color.White
    onBackground = Color.White,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF333333),
    secondaryContainer = NeuroSecondary.copy(alpha = 0.6f),
    outlineVariant =  Color(0xFFCBD5E0),
    error = Color(0xFFCF6679)
)

private val NeuroLightColorScheme = lightColorScheme(
    primary = NeuroPrimary,
    secondary = NeuroSecondary,
    tertiary = NeuroAccent,
    background = NeuroSurfaceLightVariant,  // Sfondo principale leggermente grigiastro
    surface = NeuroSurfaceLight,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = NeuroPrimary,
    onSurface = Color.Black.copy(alpha = 0.87f),
    onSurfaceVariant = Color.Black.copy(alpha = 0.80f),
    secondaryContainer = NeuroBeige,
    surfaceVariant = Color(0xFFF0F0F0),
    outlineVariant = Color(0xFFE2E8F0)
)

val LocalAppTheme = staticCompositionLocalOf<AppTheme> {
    error("No AppTheme provided") // Valore di default (in caso di errore)
    // Nota: potremmo anche usare AppTheme.SYSTEM come default "sicuro"
}

@Composable
fun GenteStranaTheme(
    appTheme: AppTheme, // Manteniamo il parametro appTheme (per ora)
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    // Fornisci il tema corrente a LocalAppTheme
    CompositionLocalProvider(
        LocalAppTheme provides appTheme
    ) {
        // Calcola il colorScheme in base a LocalAppTheme.current
        val colorScheme = when (LocalAppTheme.current) {
            AppTheme.SYSTEM -> {
                if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val context = LocalContext.current
                    if (isSystemInDarkTheme()) dynamicDarkColorScheme(context)
                    else dynamicLightColorScheme(context)
                } else {
                    if (isSystemInDarkTheme()) NeuroDarkColorScheme else NeuroLightColorScheme
                }
            }
            AppTheme.LIGHT -> NeuroLightColorScheme
            AppTheme.DARK -> NeuroDarkColorScheme
            // AppTheme.SPECIAL -> TODO: Define NeuroSpecialColorScheme
        }

        // Recupera la configurazione del dispositivo
        val configuration = LocalConfiguration.current
        // Se lo schermo ha una larghezza maggiore o uguale a 600dp, consideriamo il dispositivo come tablet
        val dimensions = if (configuration.screenWidthDp >= 600) TabletDimensions else MobileDimensions

        // Fornisci anche le dimensioni dinamiche tramite LocalDimensions
        CompositionLocalProvider(
            LocalDimensions provides dimensions
        ) {
            MaterialTheme(
                colorScheme = colorScheme,
                typography = Typography,
                content = content
            )
        }
    }
}