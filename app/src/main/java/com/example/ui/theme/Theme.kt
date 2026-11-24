package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val NexoraDarkColorScheme = darkColorScheme(
    primary = NexoraBlueBright,
    onPrimary = Color.White,
    primaryContainer = NexoraBlue.copy(alpha = 0.22f),
    onPrimaryContainer = NexoraBlueLight,
    secondary = NexoraIndigo,
    onSecondary = Color.White,
    secondaryContainer = NexoraCardElevated,
    onSecondaryContainer = NexoraText,
    tertiary = NexoraGreen,
    onTertiary = Color.White,
    tertiaryContainer = NexoraGreen.copy(alpha = 0.18f),
    onTertiaryContainer = NexoraGreenLight,
    background = NexoraBg,
    onBackground = NexoraText,
    surface = NexoraCard,
    onSurface = NexoraText,
    surfaceVariant = NexoraCardElevated,
    onSurfaceVariant = NexoraMuted,
    outline = NexoraLine,
    outlineVariant = NexoraLineBright,
    error = NexoraRed,
    onError = Color.White,
    errorContainer = NexoraRed.copy(alpha = 0.18f),
    onErrorContainer = NexoraRedLight
)

val NexoraShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun NexoraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NexoraDarkColorScheme,
        typography = NexoraTypography,
        shapes = NexoraShapes,
        content = content
    )
}
