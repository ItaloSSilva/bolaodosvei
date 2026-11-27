package com.bolaodosvei.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.bolaodosvei.app.ui.theme.AppThemePreset
import com.bolaodosvei.app.ui.theme.ClubBackgroundPattern
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Renders each club's unique visual identity:
 * - Distinct club gradient (dark or royal light)
 * - Unique club jersey/stadium pattern
 * - Authentic full-color illustrated Mascot artwork (prominent in theme picker cards, atmospheric watermark on screens)
 * - Subtle giant club crest watermark on full screens
 */
@Composable
fun ClubThemeBackdrop(
    theme: AppThemePreset,
    modifier: Modifier = Modifier,
    showLargeMascot: Boolean = true
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(theme.backgroundBrush)
    ) {
        // 1. Unique Club Pattern (cached via drawWithCache so Paths/Brushes allocate once per size)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithCache {
                    val w = size.width
                    val h = size.height
                    val alphaPrimary = if (theme.isLightTheme) 0.11f else 0.10f
                    val alphaSecondary = if (theme.isLightTheme) 0.08f else 0.07f
                    val primary = theme.accentColor.copy(alpha = alphaPrimary)
                    val secondary = theme.secondaryColor.copy(alpha = alphaSecondary)

                    when (theme.pattern) {
                        ClubBackgroundPattern.CLEAN_DARK -> {
                            val step = 28f
                            val centerX = w * 0.5f
                            val centerY = h * 0.16f
                            val maxRadius = w * 0.85f
                            val dots = ArrayList<Pair<Offset, Float>>()
                            var y = step * 0.5f
                            while (y < h * 0.72f) {
                                var x = step * 0.5f
                                while (x < w) {
                                    val dx = x - centerX
                                    val dy = y - centerY
                                    val dist = sqrt(dx * dx + dy * dy)
                                    val falloff = (1f - (dist / maxRadius)).coerceIn(0f, 1f)
                                    if (falloff > 0.02f) {
                                        dots.add(Offset(x, y) to (0.085f * falloff * falloff))
                                    }
                                    x += step
                                }
                                y += step
                            }
                            onDrawBehind {
                                for ((pos, alpha) in dots) {
                                    drawCircle(
                                        color = Color.White.copy(alpha = alpha),
                                        radius = 1.6f,
                                        center = pos
                                    )
                                }
                            }
                        }

                        ClubBackgroundPattern.HORIZONTAL_HOOPS -> {
                            val bandH = h / 10f
                            onDrawBehind {
                                for (i in 0 until 10 step 2) {
                                    drawRect(primary, Offset(0f, i * bandH), Size(w, bandH * 0.72f))
                                }
                            }
                        }

                        ClubBackgroundPattern.VERTICAL_STRIPES -> {
                            val stripeW = w / 9f
                            onDrawBehind {
                                for (i in 0 until 9 step 2) {
                                    drawRect(primary, Offset(i * stripeW, 0f), Size(stripeW * 0.68f, h))
                                }
                            }
                        }

                        ClubBackgroundPattern.DIAGONAL_SASH -> {
                            val sash = Path().apply {
                                moveTo(-w * 0.1f, h * 0.18f)
                                lineTo(w * 0.25f, h * 0.12f)
                                lineTo(w * 1.15f, h * 0.78f)
                                lineTo(w * 0.80f, h * 0.85f)
                                close()
                            }
                            val thinLine = Path().apply {
                                moveTo(-w * 0.1f, h * 0.29f)
                                lineTo(w * 0.80f, h * 0.91f)
                            }
                            val thinStroke = Stroke(width = 7f)
                            onDrawBehind {
                                drawPath(sash, color = primary)
                                drawPath(thinLine, color = secondary, style = thinStroke)
                            }
                        }

                        ClubBackgroundPattern.TRICOLOR_BANDS -> {
                            onDrawBehind {
                                drawRect(primary, Offset(0f, h * 0.26f), Size(w, h * 0.07f))
                                drawRect(secondary, Offset(0f, h * 0.34f), Size(w, h * 0.035f))
                                drawRect(primary, Offset(0f, h * 0.67f), Size(w, h * 0.06f))
                            }
                        }

                        ClubBackgroundPattern.ROYAL_CROWN_RAYS -> {
                            val center = Offset(w * 0.5f, h * 0.20f)
                            val ends = List(12) { i ->
                                val angle = (i * 30.0) * PI / 180.0
                                Offset(
                                    center.x + cos(angle).toFloat() * h * 0.9f,
                                    center.y + sin(angle).toFloat() * h * 0.9f
                                )
                            }
                            onDrawBehind {
                                ends.forEachIndexed { i, end ->
                                    drawLine(
                                        color = if (i % 2 == 0) primary else secondary,
                                        start = center,
                                        end = end,
                                        strokeWidth = 18f
                                    )
                                }
                            }
                        }

                        ClubBackgroundPattern.STAR_CONSTELLATION -> {
                            val starPositions = listOf(
                                Offset(w * 0.18f, h * 0.24f),
                                Offset(w * 0.78f, h * 0.28f),
                                Offset(w * 0.50f, h * 0.48f),
                                Offset(w * 0.26f, h * 0.72f),
                                Offset(w * 0.82f, h * 0.76f)
                            )
                            val starPaths = starPositions.mapIndexed { idx, pos ->
                                val r = if (idx == 2) 34f else 22f
                                buildFivePointStarPath(pos, r, r * 0.42f) to (if (idx % 2 == 0) primary else secondary)
                            }
                            onDrawBehind {
                                starPaths.forEach { (path, color) ->
                                    drawPath(path, color = color)
                                }
                            }
                        }

                        ClubBackgroundPattern.PALM_WAVES -> {
                            val wavePaths = List(5) { i ->
                                val baseY = h * (0.20f + i * 0.16f)
                                Path().apply {
                                    moveTo(0f, baseY)
                                    cubicTo(w * 0.33f, baseY - 60f, w * 0.66f, baseY + 60f, w, baseY)
                                }
                            }
                            val waveStroke = Stroke(width = 12f)
                            onDrawBehind {
                                wavePaths.forEachIndexed { i, wavePath ->
                                    drawPath(
                                        wavePath,
                                        color = if (i % 2 == 0) primary else secondary,
                                        style = waveStroke
                                    )
                                }
                            }
                        }

                        ClubBackgroundPattern.DIAMOND_CHECKER -> {
                            val step = 126f
                            val combined = Path()
                            var y = 0f
                            while (y < h + step) {
                                var x = 0f
                                while (x < w + step) {
                                    combined.moveTo(x, y - 34f)
                                    combined.lineTo(x + 34f, y)
                                    combined.lineTo(x, y + 34f)
                                    combined.lineTo(x - 34f, y)
                                    combined.close()
                                    x += step
                                }
                                y += step
                            }
                            val diamondStroke = Stroke(width = 3.5f)
                            onDrawBehind {
                                drawPath(combined, color = primary, style = diamondStroke)
                            }
                        }

                        ClubBackgroundPattern.STADIUM_LIGHTS -> {
                            val topCenter = Offset(w * 0.15f, h * 0.12f)
                            val topRadius = (w * 0.80f).coerceAtLeast(1f)
                            val topBrush = Brush.radialGradient(
                                colors = listOf(primary, Color.Transparent),
                                center = topCenter,
                                radius = topRadius
                            )
                            val bottomCenter = Offset(w * 0.88f, h * 0.85f)
                            val bottomRadius = (w * 0.72f).coerceAtLeast(1f)
                            val bottomBrush = Brush.radialGradient(
                                colors = listOf(secondary, Color.Transparent),
                                center = bottomCenter,
                                radius = bottomRadius
                            )
                            onDrawBehind {
                                drawCircle(brush = topBrush, center = topCenter, radius = topRadius)
                                drawCircle(brush = bottomBrush, center = bottomCenter, radius = bottomRadius)
                            }
                        }
                    }
                }
        )

        // 2. Authentic Full-Color Club Mascot Illustration
        val mascotRes = theme.mascotType.drawableRes
        if (mascotRes != null) {
            if (showLargeMascot) {
                Image(
                    painter = painterResource(id = mascotRes),
                    contentDescription = theme.mascotName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(216.dp)
                        .align(Alignment.BottomStart)
                        .padding(start = 8.dp, bottom = 84.dp)
                        .alpha(if (theme.isLightTheme) 0.19f else 0.22f)
                )
            } else {
                // Highlighted authentic mascot art inside each Theme Card
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .align(Alignment.BottomEnd)
                        .offset(x = 4.dp, y = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    theme.accentColor.copy(alpha = if (theme.isLightTheme) 0.20f else 0.28f),
                                    Color.Transparent
                                )
                            )
                        )
                    }
                    Image(
                        painter = painterResource(id = mascotRes),
                        contentDescription = theme.mascotName,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(80.dp)
                            .alpha(0.96f)
                    )
                }
            }
        }

        // 3. Giant Subtle Club Crest Watermark on the opposite corner (full screen only)
        if (theme.logoUrl.isNotBlank() && showLargeMascot) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 10.dp, bottom = 92.dp)
                    .alpha(if (theme.isLightTheme) 0.10f else 0.08f)
            ) {
                TeamCrest(
                    teamName = theme.name,
                    shortName = theme.name.take(3),
                    logoUrl = theme.logoUrl,
                    colorHex = theme.accentHex,
                    size = 180.dp
                )
            }
        }
    }
}

private fun buildFivePointStarPath(
    center: Offset,
    outerRadius: Float,
    innerRadius: Float
): Path {
    val path = Path()
    for (i in 0 until 10) {
        val r = if (i % 2 == 0) outerRadius else innerRadius
        val angle = (-90.0 + i * 36.0) * PI / 180.0
        val x = center.x + cos(angle).toFloat() * r
        val y = center.y + sin(angle).toFloat() * r
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}
