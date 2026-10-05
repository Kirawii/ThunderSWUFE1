package com.kirawii.thunderswufe.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF70D9C0),
    onPrimary = Color(0xFF00382C),
    primaryContainer = Color(0xFF124F41),
    onPrimaryContainer = Color(0xFFB0F2DE),
    secondary = Color(0xFFA8C9C0),
    tertiary = Color(0xFFEDC17B),
    background = Color(0xFF101917),
    surface = Color(0xFF182420),
    surfaceVariant = Color(0xFF273B34)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF006C53),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1F2E6),
    onPrimaryContainer = Color(0xFF123E32),
    secondary = Color(0xFF526A60),
    tertiary = Color(0xFF93652C),
    background = Color(0xFFF3F7F5),
    surface = Color.White,
    surfaceVariant = Color(0xFFE3EDE7),
    onSurface = Color(0xFF192D25),
    onSurfaceVariant = Color(0xFF52655B)

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
fun ThunderSWUFETheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor -> {
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
