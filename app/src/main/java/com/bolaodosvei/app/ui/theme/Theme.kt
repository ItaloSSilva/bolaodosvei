package com.bolaodosvei.app.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bolaodosvei.app.R

val PitchDark = Color(0xFF09090B)
val BlackGradientMid = Color(0xFF0D0D10)
val BlackGradientBottom = Color(0xFF111114)
val PitchSurface = Color(0xFF111114)
val PitchCard = Color(0xFF141418)
val PitchCardBorder = Color(0xFF222226)

val EmeraldAccent = Color(0xFF00E676)
val GoldAccent = Color(0xFFFFD600)
val LiveRed = Color(0xFFFF3D57)
val TextPrimary = Color(0xFFF5F7FA)
val TextSecondary = Color(0xFF98A0B3)

val BlackBackgroundBrush = Brush.verticalGradient(
    colors = listOf(PitchDark, BlackGradientMid, BlackGradientBottom)
)

val BolaoDisplayFontFamily = FontFamily(
    Font(R.font.bolao_display)
)

val LocalBolaoTheme = staticCompositionLocalOf { DefaultAppTheme }

@Composable
fun BolaoDosVeiTheme(
    themePreset: AppThemePreset = DefaultAppTheme,
    content: @Composable () -> Unit
) {
    val colorScheme = if (themePreset.isLightTheme) {
        lightColorScheme(
            primary = themePreset.accentColor,
            onPrimary = Color.White,
            secondary = themePreset.secondaryColor,
            onSecondary = Color.White,
            tertiary = LiveRed,
            background = Color(themePreset.gradientTopHex),
            onBackground = themePreset.textPrimaryColor,
            surface = themePreset.surfaceColor,
            onSurface = themePreset.textPrimaryColor,
            surfaceVariant = themePreset.cardColor,
            onSurfaceVariant = themePreset.textSecondaryColor,
            outline = themePreset.borderColor
        )
    } else {
        darkColorScheme(
            primary = themePreset.accentColor,
            onPrimary = Color(0xFF050507),
            secondary = themePreset.secondaryColor,
            onSecondary = Color(0xFF1A1500),
            tertiary = LiveRed,
            background = Color(themePreset.gradientTopHex),
            onBackground = themePreset.textPrimaryColor,
            surface = themePreset.surfaceColor,
            onSurface = themePreset.textPrimaryColor,
            surfaceVariant = themePreset.cardColor,
            onSurfaceVariant = themePreset.textSecondaryColor,
            outline = themePreset.borderColor
        )
    }

    CompositionLocalProvider(LocalBolaoTheme provides themePreset) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}

fun premiumGlassBorder(
    accent: Color,
    baseBorder: Color,
    width: Dp = 1.dp
): BorderStroke = BorderStroke(
    width = width,
    brush = Brush.linearGradient(
        colors = listOf(
            accent.copy(alpha = 0.45f),
            baseBorder,
            accent.copy(alpha = 0.22f)
        )
    )
)

fun premiumCardBrush(theme: AppThemePreset): Brush = Brush.verticalGradient(
    colors = listOf(
        theme.cardColor,
        theme.surfaceColor.copy(alpha = if (theme.isLightTheme) 0.94f else 0.96f)
    )
)

fun Color.contrastOnAccent(): Color = if (luminance() > 0.45f) PitchDark else Color.White
