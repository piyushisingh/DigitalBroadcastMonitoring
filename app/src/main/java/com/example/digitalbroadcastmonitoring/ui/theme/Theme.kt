package com.example.digitalbroadcastmonitoring.ui.theme

import android.app.Activity
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
    primary = PrimaryBlue,
    onPrimary = TextWhite,
    primaryContainer = PrimaryBlueContainer,
    onPrimaryContainer = TextWhite,
    secondary = PrimaryBlueVariant,
    onSecondary = TextWhite,
    secondaryContainer = DarkSecondarySurface,
    onSecondaryContainer = TextWhite,
    tertiary = WarningAmber,
    onTertiary = DarkBackground,
    tertiaryContainer = WarningAmberContainer,
    onTertiaryContainer = WarningAmber,
    background = DarkBackground,
    onBackground = TextWhite,
    surface = DarkSurface,
    onSurface = TextWhite,
    surfaceVariant = DarkSecondarySurface,
    onSurfaceVariant = TextMuted,
    outline = BorderStroke,
    outlineVariant = BorderStroke.copy(alpha = 0.5f),
    error = AnomalyRed,
    onError = TextWhite,
    errorContainer = AnomalyRedContainer,
    onErrorContainer = AnomalyRed
)

@Composable
fun DigitalBroadcastMonitoringTheme(
    darkTheme: Boolean = true, // Default to dark theme as required
    dynamicColor: Boolean = false, // Disabled so device wallpaper doesn't alter theme
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> DarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}