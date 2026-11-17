package com.gonzalowebdev.mix.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gonzalowebdev.mix.R

object MixColors {
    val Black = Color(0xFF0B0B0D)
    val Graphite = Color(0xFF1A1A1F)
    val Pink = Color(0xFFFF4F8B)
    val PinkSoft = Color(0xFFFFB3CF)
    val Bone = Color(0xFFF4EFF1)
}

private val MixDarkScheme = darkColorScheme(
    primary = MixColors.Pink,
    onPrimary = MixColors.Black,
    primaryContainer = MixColors.Pink,
    onPrimaryContainer = MixColors.Black,
    inversePrimary = Color(0xFFB0285A),
    secondary = MixColors.PinkSoft,
    onSecondary = MixColors.Black,
    secondaryContainer = Color(0xFF3A1626),
    onSecondaryContainer = MixColors.PinkSoft,
    tertiary = MixColors.PinkSoft,
    onTertiary = MixColors.Black,
    tertiaryContainer = Color(0xFF3A1626),
    onTertiaryContainer = MixColors.PinkSoft,
    background = MixColors.Black,
    onBackground = MixColors.Bone,
    surface = MixColors.Black,
    onSurface = MixColors.Bone,
    surfaceVariant = MixColors.Graphite,
    onSurfaceVariant = Color(0xFFB4AEB2),
    surfaceTint = Color.Transparent,
    inverseSurface = MixColors.Bone,
    inverseOnSurface = MixColors.Black,
    error = Color(0xFFFF6B7A),
    onError = MixColors.Black,
    outline = Color(0xFF4A4A52),
    outlineVariant = Color(0xFF2A2A31),
    scrim = Color.Black,
    surfaceContainerLowest = Color(0xFF070708),
    surfaceContainerLow = Color(0xFF101013),
    surfaceContainer = Color(0xFF141417),
    surfaceContainerHigh = MixColors.Graphite,
    surfaceContainerHighest = Color(0xFF222228)
)

@OptIn(ExperimentalTextApi::class)
private fun soraFamily(): FontFamily = FontFamily(
    Font(R.font.sora, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.sora, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.sora, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600)))
)

private fun mixTypography(): Typography {
    val family = soraFamily()
    val t = Typography()
    fun TextStyle.sora() = copy(fontFamily = family)
    return Typography(
        displayLarge = t.displayLarge.sora(),
        displayMedium = t.displayMedium.sora(),
        displaySmall = t.displaySmall.sora(),
        headlineLarge = t.headlineLarge.sora(),
        headlineMedium = t.headlineMedium.sora(),
        headlineSmall = t.headlineSmall.sora().copy(fontSize = 22.sp, fontWeight = FontWeight.Medium, letterSpacing = (-0.3).sp),
        titleLarge = t.titleLarge.sora().copy(fontSize = 20.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp),
        titleMedium = t.titleMedium.sora().copy(fontSize = 16.sp, fontWeight = FontWeight.Medium),
        titleSmall = t.titleSmall.sora().copy(fontSize = 14.sp, fontWeight = FontWeight.Medium),
        bodyLarge = t.bodyLarge.sora().copy(fontSize = 15.sp, lineHeight = 22.sp),
        bodyMedium = t.bodyMedium.sora().copy(fontSize = 13.5.sp, lineHeight = 20.sp),
        bodySmall = t.bodySmall.sora().copy(fontSize = 12.sp),
        labelLarge = t.labelLarge.sora().copy(fontSize = 13.sp, fontWeight = FontWeight.Medium),
        labelMedium = t.labelMedium.sora().copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
        labelSmall = t.labelSmall.sora().copy(fontSize = 11.sp, fontWeight = FontWeight.Medium)
    )
}

private val MixShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

/** Tema propio de Mix: siempre oscuro, rosa como único acento. */
@Composable
fun MixTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MixDarkScheme,
        typography = mixTypography(),
        shapes = MixShapes,
        content = content
    )
}
