package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = MbgGreenPrimary,
    onPrimary = Color.White,
    primaryContainer = MbgGreenContainer,
    onPrimaryContainer = MbgOnGreenContainer,
    secondary = MbgBlueAccent,
    onSecondary = Color.White,
    secondaryContainer = MbgBlueContainer,
    onSecondaryContainer = MbgOnBlueContainer,
    tertiary = MbgYellowAccent,
    onTertiary = Color.White,
    tertiaryContainer = MbgYellowContainer,
    onTertiaryContainer = MbgOnYellowContainer,
    error = MbgRedAlert,
    onError = Color.White,
    errorContainer = MbgRedContainer,
    onErrorContainer = MbgOnRedContainer,
    background = MbgSurfaceLight,
    onBackground = MbgTextPrimary,
    surface = MbgCardWhite,
    onSurface = MbgTextPrimary,
    surfaceVariant = MbgSurfaceVariantLight,
    onSurfaceVariant = MbgTextSecondary,
    outline = MbgOutlineLight
)

private val DarkColorScheme = darkColorScheme(
    primary = MbgDarkPrimary,
    onPrimary = MbgDarkOnPrimary,
    primaryContainer = MbgDarkPrimaryContainer,
    onPrimaryContainer = MbgDarkOnPrimaryContainer,
    secondary = Color(0xFF38BDF8),
    onSecondary = Color(0xFF082F49),
    secondaryContainer = Color(0xFF0C4A6E),
    onSecondaryContainer = Color(0xFFE0F2FE),
    tertiary = MbgYellowBright,
    onTertiary = Color(0xFF451A03),
    tertiaryContainer = Color(0xFF78350F),
    onTertiaryContainer = Color(0xFFFEF3C7),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2),
    background = MbgDarkBackground,
    onBackground = MbgDarkTextPrimary,
    surface = MbgDarkSurface,
    onSurface = MbgDarkTextPrimary,
    surfaceVariant = MbgDarkSurfaceVariant,
    onSurfaceVariant = MbgDarkTextSecondary,
    outline = MbgDarkOutline
)

val MbgShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp)
)

@Composable
fun MbgStockTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = MbgTypography,
        shapes = MbgShapes,
        content = content
    )
}
