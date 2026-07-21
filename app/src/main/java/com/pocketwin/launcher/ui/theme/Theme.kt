package com.pocketwin.launcher.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = Win11Blue,
    onPrimary = Color.White,
    background = Win11LightBackground,
    surface = Win11LightSurface,
    surfaceVariant = Win11LightSurfaceVariant,
    onSurface = Win11LightOnSurface,
)

private val DarkColors = darkColorScheme(
    primary = Win11BlueDark,
    onPrimary = Win11DarkBackground,
    background = Win11DarkBackground,
    surface = Win11DarkSurface,
    surfaceVariant = Win11DarkSurfaceVariant,
    onSurface = Win11DarkOnSurface,
)

// Windows 11's Fluent shapes round almost everything at 8dp (cards, buttons, dialogs).
private val Win11Shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(8.dp),
    extraLarge = RoundedCornerShape(12.dp),
)

private val Win11Typography = Typography(
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 15.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 13.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp),
)

@Composable
fun PocketWinTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        shapes = Win11Shapes,
        typography = Win11Typography,
        content = content,
    )
}
