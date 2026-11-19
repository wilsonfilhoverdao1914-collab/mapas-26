package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme =
  lightColorScheme(
    primary = ForestPrimary,
    onPrimary = Color.White,
    primaryContainer = ForestPrimaryContainer,
    onPrimaryContainer = OnForestPrimaryContainer,
    secondary = HarvestAmber,
    onSecondary = Color.White,
    secondaryContainer = HarvestAmberContainer,
    onSecondaryContainer = OnHarvestAmberContainer,
    tertiary = BarracaoAccent,
    onTertiary = Color.White,
    tertiaryContainer = BarracaoAccentContainer,
    onTertiaryContainer = BarracaoOnAccentContainer,
    background = CottonPaperBg,
    onBackground = CottonInk,
    surface = CottonSurface,
    onSurface = CottonInk,
    surfaceVariant = CottonSurfaceVariant,
    onSurfaceVariant = CottonMutedInk,
    outline = CottonSoftOutline,
    error = ErrorRed,
    onError = Color.White,
    errorContainer = ErrorContainerLight,
    onErrorContainer = ErrorRed,
  )

val MapaShapes =
  Shapes(
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp),
  )

@Composable
fun MapaDeBlocosTheme(
  content: @Composable () -> Unit,
) {
  // Tema claro apenas (papel #f4efe2)
  MaterialTheme(
    colorScheme = LightColorScheme,
    typography = MapaTypography,
    shapes = MapaShapes,
    content = content,
  )
}
