package com.bolaodosvei.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Hand-crafted Canvas illustrations for every item in ShopCatalog.
 * Zero generic Material icons are used for shop item emblems.
 */
@Composable
fun ShopItemIcon(
    itemId: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(44.dp)) {
        val w = size.width
        val h = size.height
        val c = Offset(w / 2f, h / 2f)
        val minDim = minOf(w, h)

        when (itemId) {
            "frame_gold" -> drawFrameGoldIcon(c, minDim)
            "frame_champions" -> drawFrameChampionsIcon(c, minDim)
            "frame_fire" -> drawFrameFireIcon(c, minDim)
            "frame_legend" -> drawFrameLegendIcon(c, minDim)
            "title_profeta" -> drawTitleProfetaIcon(c, minDim)
            "title_artilheiro" -> drawTitleArtilheiroIcon(c, minDim)
            "title_capitao" -> drawTitleCapitaoIcon(c, minDim)
            "title_rei_cravada" -> drawTitleReiCravadaIcon(c, minDim)
            "flair_golaco" -> drawFlairGolacoIcon(c, minDim)
            "flair_vip" -> drawFlairVipIcon(c, minDim)
            else -> {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(accentColor, accentColor.copy(alpha = 0.25f)),
                        center = c,
                        radius = minDim * 0.45f
                    ),
                    radius = minDim * 0.36f,
                    center = c
                )
                drawPath(
                    path = createStarPath(c, minDim * 0.22f, minDim * 0.1f, 5),
                    color = Color.White
                )
            }
        }
    }
}

/**
 * Bespoke ornate ring overlay rendered around UserAvatarBadge for equipped or previewed frames.
 */
@Composable
fun HandcraftedAvatarFrameOverlay(
    frameId: String?,
    fallbackAccentHex: Long?,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val c = Offset(w / 2f, h / 2f)
        val minDim = minOf(w, h)
        val ringRadius = minDim * 0.47f
        val strokeW = (minDim * 0.055f).coerceIn(2.2f, 6.5f)

        when (frameId) {
            "frame_gold" -> {
                val goldBrush = Brush.sweepGradient(
                    colors = listOf(
                        Color(0xFFFFF59D),
                        Color(0xFFFFD600),
                        Color(0xFFB45309),
                        Color(0xFFFFEA00),
                        Color(0xFFFFF59D)
                    ),
                    center = c
                )
                drawCircle(
                    brush = goldBrush,
                    radius = ringRadius,
                    center = c,
                    style = Stroke(width = strokeW)
                )
                drawCircle(
                    color = Color(0xFFFFF8E1).copy(alpha = 0.55f),
                    radius = ringRadius - strokeW * 0.58f,
                    center = c,
                    style = Stroke(width = strokeW * 0.25f)
                )
                // Laurel ticks on left and right arcs
                val tickAngles = listOf(145f, 165f, 185f, 205f, 35f, 15f, -5f, -25f)
                tickAngles.forEach { deg ->
                    val rad = deg * (PI / 180.0)
                    val p1 = Offset(
                        c.x + (ringRadius - strokeW * 0.2f) * cos(rad).toFloat(),
                        c.y + (ringRadius - strokeW * 0.2f) * sin(rad).toFloat()
                    )
                    val p2 = Offset(
                        c.x + (ringRadius + strokeW * 0.85f) * cos(rad - 0.08).toFloat(),
                        c.y + (ringRadius + strokeW * 0.85f) * sin(rad - 0.08).toFloat()
                    )
                    drawLine(
                        color = Color(0xFFFFE57F),
                        start = p1,
                        end = p2,
                        strokeWidth = strokeW * 0.48f,
                        cap = StrokeCap.Round
                    )
                }
                // Top crown gem
                val gemCenter = Offset(c.x, c.y - ringRadius)
                val gemPath = Path().apply {
                    moveTo(gemCenter.x, gemCenter.y - strokeW * 1.15f)
                    lineTo(gemCenter.x + strokeW * 0.9f, gemCenter.y)
                    lineTo(gemCenter.x, gemCenter.y + strokeW * 0.9f)
                    lineTo(gemCenter.x - strokeW * 0.9f, gemCenter.y)
                    close()
                }
                drawPath(gemPath, color = Color(0xFFFFD600))
                drawPath(gemPath, color = Color.White.copy(alpha = 0.7f), style = Stroke(width = strokeW * 0.25f))
            }

            "frame_champions" -> {
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color(0xFF0F172A),
                            Color(0xFF38BDF8),
                            Color(0xFF1D4ED8),
                            Color(0xFF67E8F9),
                            Color(0xFF0F172A)
                        ),
                        center = c
                    ),
                    radius = ringRadius,
                    center = c,
                    style = Stroke(width = strokeW)
                )
                drawCircle(
                    color = Color(0xFF38BDF8).copy(alpha = 0.45f),
                    radius = ringRadius + strokeW * 0.55f,
                    center = c,
                    style = Stroke(width = strokeW * 0.28f)
                )
                // 5 orbiting 5-point stars
                val starAngles = listOf(-90f, -18f, 54f, 126f, 198f)
                starAngles.forEach { deg ->
                    val rad = deg * (PI / 180.0)
                    val sc = Offset(
                        c.x + ringRadius * cos(rad).toFloat(),
                        c.y + ringRadius * sin(rad).toFloat()
                    )
                    drawPath(
                        path = createStarPath(sc, strokeW * 0.95f, strokeW * 0.42f, 5),
                        color = Color(0xFFE0F2FE)
                    )
                }
            }

            "frame_fire" -> {
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color(0xFFFF3D00),
                            Color(0xFFFF9100),
                            Color(0xFFFFEA00),
                            Color(0xFFD50000),
                            Color(0xFFFF3D00)
                        ),
                        center = c
                    ),
                    radius = ringRadius,
                    center = c,
                    style = Stroke(width = strokeW * 1.05f)
                )
                // Top stylized flame crest
                val topY = c.y - ringRadius
                val flamePath = Path().apply {
                    moveTo(c.x - strokeW * 1.2f, topY + strokeW * 0.3f)
                    quadraticTo(c.x - strokeW * 0.9f, topY - strokeW * 1.1f, c.x, topY - strokeW * 1.6f)
                    quadraticTo(c.x + strokeW * 0.9f, topY - strokeW * 1.1f, c.x + strokeW * 1.2f, topY + strokeW * 0.3f)
                    close()
                }
                drawPath(flamePath, color = Color(0xFFFF6D00))
                // Glowing ember dots around the ring
                val emberAngles = listOf(-135f, -45f, 35f, 145f, 90f)
                emberAngles.forEachIndexed { idx, deg ->
                    val rad = deg * (PI / 180.0)
                    val r = ringRadius + if (idx % 2 == 0) strokeW * 0.65f else -strokeW * 0.1f
                    val ec = Offset(c.x + r * cos(rad).toFloat(), c.y + r * sin(rad).toFloat())
                    drawCircle(
                        color = if (idx % 2 == 0) Color(0xFFFFEA00) else Color(0xFFFF3D00),
                        radius = strokeW * 0.38f,
                        center = ec
                    )
                }
            }

            "frame_legend" -> {
                // Faceted emerald-cyan & royal purple legend aura
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color(0xFF00E676),
                            Color(0xFF22D3EE),
                            Color(0xFFA855F7),
                            Color(0xFF34D399),
                            Color(0xFF00E676)
                        ),
                        center = c
                    ),
                    radius = ringRadius,
                    center = c,
                    style = Stroke(width = strokeW * 1.05f)
                )
                // Octagonal faceted outer aura
                val octPath = Path()
                for (i in 0 until 8) {
                    val angle = (i * 45f - 22.5f) * (PI / 180.0)
                    val px = c.x + (ringRadius + strokeW * 0.45f) * cos(angle).toFloat()
                    val py = c.y + (ringRadius + strokeW * 0.45f) * sin(angle).toFloat()
                    if (i == 0) octPath.moveTo(px, py) else octPath.lineTo(px, py)
                }
                octPath.close()
                drawPath(
                    path = octPath,
                    color = Color(0xFF67E8F9).copy(alpha = 0.7f),
                    style = Stroke(width = strokeW * 0.35f, join = StrokeJoin.Miter)
                )
                // Specular sparkles at top & bottom
                drawSparkle(Offset(c.x, c.y - ringRadius - strokeW * 0.3f), strokeW * 0.9f, Color(0xFF67E8F9))
                drawSparkle(Offset(c.x, c.y + ringRadius + strokeW * 0.3f), strokeW * 0.75f, Color(0xFF00E676))
            }

            else -> {
                val base = fallbackAccentHex?.let { Color(it) } ?: Color(0xFFFFD600)
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(base, base.copy(alpha = 0.55f), Color.White.copy(alpha = 0.8f), base),
                        center = c
                    ),
                    radius = ringRadius,
                    center = c,
                    style = Stroke(width = strokeW)
                )
            }
        }
    }
}

private fun DrawScope.drawFrameGoldIcon(c: Offset, minDim: Float) {
    // Laurel wreath branches
    val wreathRadius = minDim * 0.38f
    drawArc(
        brush = Brush.linearGradient(listOf(Color(0xFFFFF59D), Color(0xFFD97706))),
        startAngle = 120f,
        sweepAngle = 110f,
        useCenter = false,
        topLeft = Offset(c.x - wreathRadius, c.y - wreathRadius),
        size = Size(wreathRadius * 2f, wreathRadius * 2f),
        style = Stroke(width = minDim * 0.065f, cap = StrokeCap.Round)
    )
    drawArc(
        brush = Brush.linearGradient(listOf(Color(0xFFFFF59D), Color(0xFFD97706))),
        startAngle = -50f,
        sweepAngle = 110f,
        useCenter = false,
        topLeft = Offset(c.x - wreathRadius, c.y - wreathRadius),
        size = Size(wreathRadius * 2f, wreathRadius * 2f),
        style = Stroke(width = minDim * 0.065f, cap = StrokeCap.Round)
    )
    // Laurel leaves
    val leafAngles = listOf(140f, 165f, 190f, 215f, 40f, 15f, -10f, -35f)
    leafAngles.forEach { deg ->
        val rad = deg * (PI / 180.0)
        val p1 = Offset(c.x + wreathRadius * cos(rad).toFloat(), c.y + wreathRadius * sin(rad).toFloat())
        val p2 = Offset(
            c.x + (wreathRadius + minDim * 0.09f) * cos(rad - 0.12).toFloat(),
            c.y + (wreathRadius + minDim * 0.09f) * sin(rad - 0.12).toFloat()
        )
        drawLine(Color(0xFFFFEA00), p1, p2, strokeWidth = minDim * 0.045f, cap = StrokeCap.Round)
    }
    // Golden soccer ball in center
    drawSoccerBall(
        center = c,
        radius = minDim * 0.25f,
        baseBrush = Brush.radialGradient(
            colors = listOf(Color(0xFFFFF9C4), Color(0xFFFFD600), Color(0xFFB45309)),
            center = Offset(c.x - minDim * 0.06f, c.y - minDim * 0.06f),
            radius = minDim * 0.3f
        ),
        seamColor = Color(0xFF78350F)
    )
    drawSparkle(Offset(c.x + minDim * 0.28f, c.y - minDim * 0.28f), minDim * 0.08f, Color(0xFFFFFDE7))
}

private fun DrawScope.drawFrameChampionsIcon(c: Offset, minDim: Float) {
    // Cyan outer glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF38BDF8).copy(alpha = 0.45f), Color.Transparent),
            center = c,
            radius = minDim * 0.48f
        ),
        radius = minDim * 0.46f,
        center = c
    )
    // Midnight-blue emblem ring
    val r = minDim * 0.34f
    drawCircle(
        color = Color(0xFF0B193C),
        radius = r,
        center = c
    )
    drawCircle(
        brush = Brush.sweepGradient(listOf(Color(0xFF38BDF8), Color(0xFF1D4ED8), Color(0xFF67E8F9), Color(0xFF38BDF8))),
        radius = r,
        center = c,
        style = Stroke(width = minDim * 0.07f)
    )
    // 5 orbiting 5-point stars
    val orbitR = minDim * 0.22f
    for (i in 0 until 5) {
        val angle = (i * 72f - 90f) * (PI / 180.0)
        val sc = Offset(c.x + orbitR * cos(angle).toFloat(), c.y + orbitR * sin(angle).toFloat())
        drawPath(
            path = createStarPath(sc, minDim * 0.08f, minDim * 0.036f, 5),
            color = Color(0xFFE0F2FE)
        )
    }
    drawCircle(color = Color(0xFF38BDF8), radius = minDim * 0.045f, center = c)
}

private fun DrawScope.drawFrameFireIcon(c: Offset, minDim: Float) {
    // Outer crimson/orange flame
    val outerFlame = Path().apply {
        moveTo(c.x, c.y - minDim * 0.42f)
        cubicTo(
            c.x + minDim * 0.24f, c.y - minDim * 0.16f,
            c.x + minDim * 0.36f, c.y + minDim * 0.14f,
            c.x + minDim * 0.18f, c.y + minDim * 0.36f
        )
        quadraticTo(c.x, c.y + minDim * 0.44f, c.x - minDim * 0.22f, c.y + minDim * 0.34f)
        cubicTo(
            c.x - minDim * 0.36f, c.y + minDim * 0.12f,
            c.x - minDim * 0.22f, c.y - minDim * 0.14f,
            c.x, c.y - minDim * 0.42f
        )
        close()
    }
    drawPath(
        path = outerFlame,
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFFFFD600), Color(0xFFFF5722), Color(0xFFB71C1C))
        )
    )
    // Inner golden flame
    val innerFlame = Path().apply {
        moveTo(c.x, c.y - minDim * 0.18f)
        cubicTo(
            c.x + minDim * 0.16f, c.y + minDim * 0.02f,
            c.x + minDim * 0.18f, c.y + minDim * 0.26f,
            c.x, c.y + minDim * 0.34f
        )
        cubicTo(
            c.x - minDim * 0.18f, c.y + minDim * 0.26f,
            c.x - minDim * 0.14f, c.y + minDim * 0.02f,
            c.x, c.y - minDim * 0.18f
        )
        close()
    }
    drawPath(
        path = innerFlame,
        brush = Brush.verticalGradient(colors = listOf(Color(0xFFFFF59D), Color(0xFFFF9100)))
    )
    // Ember core + floating sparks
    drawCircle(color = Color(0xFFFFFDE7), radius = minDim * 0.065f, center = Offset(c.x, c.y + minDim * 0.18f))
    drawCircle(color = Color(0xFFFFEA00), radius = minDim * 0.032f, center = Offset(c.x - minDim * 0.28f, c.y - minDim * 0.18f))
    drawCircle(color = Color(0xFFFF6D00), radius = minDim * 0.028f, center = Offset(c.x + minDim * 0.27f, c.y - minDim * 0.24f))
}

private fun DrawScope.drawFrameLegendIcon(c: Offset, minDim: Float) {
    // Faceted emerald-and-cyan diamond crest with specular reflections
    val top = Offset(c.x, c.y - minDim * 0.38f)
    val left = Offset(c.x - minDim * 0.34f, c.y)
    val right = Offset(c.x + minDim * 0.34f, c.y)
    val bottom = Offset(c.x, c.y + minDim * 0.38f)

    val topLeftFacet = Path().apply { moveTo(top.x, top.y); lineTo(c.x, c.y); lineTo(left.x, left.y); close() }
    val topRightFacet = Path().apply { moveTo(top.x, top.y); lineTo(right.x, right.y); lineTo(c.x, c.y); close() }
    val bottomLeftFacet = Path().apply { moveTo(left.x, left.y); lineTo(c.x, c.y); lineTo(bottom.x, bottom.y); close() }
    val bottomRightFacet = Path().apply { moveTo(right.x, right.y); lineTo(bottom.x, bottom.y); lineTo(c.x, c.y); close() }

    drawPath(topLeftFacet, color = Color(0xFF67E8F9))
    drawPath(topRightFacet, color = Color(0xFF10B981))
    drawPath(bottomLeftFacet, color = Color(0xFF059669))
    drawPath(bottomRightFacet, color = Color(0xFF06B6D4))

    // Inner diamond core
    val innerDiamond = Path().apply {
        moveTo(c.x, c.y - minDim * 0.18f)
        lineTo(c.x + minDim * 0.16f, c.y)
        lineTo(c.x, c.y + minDim * 0.18f)
        lineTo(c.x - minDim * 0.16f, c.y)
        close()
    }
    drawPath(
        innerDiamond,
        brush = Brush.linearGradient(listOf(Color(0xFFE0F2FE), Color(0xFF34D399)))
    )
    drawSparkle(Offset(c.x - minDim * 0.22f, c.y - minDim * 0.24f), minDim * 0.08f, Color.White)
}

private fun DrawScope.drawTitleProfetaIcon(c: Offset, minDim: Float) {
    // Orbital arc around crystal orb
    rotate(degrees = -22f, pivot = c) {
        drawOval(
            brush = Brush.linearGradient(listOf(Color(0xFFFFD600), Color(0xFFA855F7))),
            topLeft = Offset(c.x - minDim * 0.42f, c.y - minDim * 0.14f),
            size = Size(minDim * 0.84f, minDim * 0.28f),
            style = Stroke(width = minDim * 0.048f)
        )
    }
    // Mystic violet/gold crystal orb
    val orbR = minDim * 0.25f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFE9D5FF), Color(0xFF7C3AED), Color(0xFF3B0764)),
            center = Offset(c.x - orbR * 0.3f, c.y - orbR * 0.3f),
            radius = orbR * 1.4f
        ),
        radius = orbR,
        center = c
    )
    drawCircle(
        color = Color(0xFFFFD600),
        radius = orbR,
        center = c,
        style = Stroke(width = minDim * 0.035f)
    )
    // Prophecy star inside orb
    drawPath(
        path = createStarPath(c, minDim * 0.11f, minDim * 0.045f, 4),
        color = Color(0xFFFFF59D)
    )
}

private fun DrawScope.drawTitleArtilheiroIcon(c: Offset, minDim: Float) {
    // Speed streaks
    val streakColor = Color(0xFFFFD600)
    drawLine(streakColor.copy(alpha = 0.8f), Offset(c.x - minDim * 0.4f, c.y - minDim * 0.16f), Offset(c.x - minDim * 0.2f, c.y - minDim * 0.16f), minDim * 0.05f, StrokeCap.Round)
    drawLine(streakColor.copy(alpha = 0.6f), Offset(c.x - minDim * 0.44f, c.y), Offset(c.x - minDim * 0.24f, c.y), minDim * 0.05f, StrokeCap.Round)
    drawLine(streakColor.copy(alpha = 0.8f), Offset(c.x - minDim * 0.38f, c.y + minDim * 0.16f), Offset(c.x - minDim * 0.18f, c.y + minDim * 0.16f), minDim * 0.05f, StrokeCap.Round)

    // Target crosshair
    val targetCenter = Offset(c.x + minDim * 0.08f, c.y)
    val rOuter = minDim * 0.26f
    drawCircle(color = Color(0xFFEF4444), radius = rOuter, center = targetCenter, style = Stroke(width = minDim * 0.055f))
    drawCircle(color = Color(0xFFFFD600), radius = rOuter * 0.52f, center = targetCenter, style = Stroke(width = minDim * 0.045f))
    drawCircle(color = Color(0xFFFFEA00), radius = minDim * 0.055f, center = targetCenter)

    // Crosshair ticks
    val tick = minDim * 0.09f
    drawLine(Color.White, Offset(targetCenter.x, targetCenter.y - rOuter - tick * 0.4f), Offset(targetCenter.x, targetCenter.y - rOuter + tick * 0.5f), minDim * 0.04f, StrokeCap.Round)
    drawLine(Color.White, Offset(targetCenter.x, targetCenter.y + rOuter - tick * 0.5f), Offset(targetCenter.x, targetCenter.y + rOuter + tick * 0.4f), minDim * 0.04f, StrokeCap.Round)
    drawLine(Color.White, Offset(targetCenter.x + rOuter - tick * 0.5f, targetCenter.y), Offset(targetCenter.x + rOuter + tick * 0.4f, targetCenter.y), minDim * 0.04f, StrokeCap.Round)
}

private fun DrawScope.drawTitleCapitaoIcon(c: Offset, minDim: Float) {
    // Metallic captain's armband
    val bandW = minDim * 0.72f
    val bandH = minDim * 0.42f
    val topLeft = Offset(c.x - bandW / 2f, c.y - bandH / 2f)
    drawRoundRect(
        brush = Brush.linearGradient(listOf(Color(0xFF1D4ED8), Color(0xFF3B82F6), Color(0xFF1E3A8A))),
        topLeft = topLeft,
        size = Size(bandW, bandH),
        cornerRadius = CornerRadius(minDim * 0.08f, minDim * 0.08f)
    )
    // Metallic horizontal stripes
    drawRect(
        color = Color(0xFFE2E8F0),
        topLeft = Offset(topLeft.x, topLeft.y + bandH * 0.16f),
        size = Size(bandW, bandH * 0.14f)
    )
    drawRect(
        color = Color(0xFFE2E8F0),
        topLeft = Offset(topLeft.x, topLeft.y + bandH * 0.70f),
        size = Size(bandW, bandH * 0.14f)
    )
    drawRoundRect(
        color = Color(0xFF93C5FD),
        topLeft = topLeft,
        size = Size(bandW, bandH),
        cornerRadius = CornerRadius(minDim * 0.08f, minDim * 0.08f),
        style = Stroke(width = minDim * 0.035f)
    )
    // Bold 5-point star in center
    drawPath(
        path = createStarPath(c, minDim * 0.14f, minDim * 0.06f, 5),
        color = Color(0xFFFFD600)
    )
}

private fun DrawScope.drawTitleReiCravadaIcon(c: Offset, minDim: Float) {
    val crownW = minDim * 0.68f
    val crownH = minDim * 0.44f
    val baseY = c.y + crownH * 0.45f
    val topY = c.y - crownH * 0.55f

    val crownPath = Path().apply {
        moveTo(c.x - crownW * 0.46f, baseY)
        lineTo(c.x - crownW * 0.5f, topY + crownH * 0.25f)
        lineTo(c.x - crownW * 0.22f, c.y + crownH * 0.05f)
        lineTo(c.x, topY)
        lineTo(c.x + crownW * 0.22f, c.y + crownH * 0.05f)
        lineTo(c.x + crownW * 0.5f, topY + crownH * 0.25f)
        lineTo(c.x + crownW * 0.46f, baseY)
        close()
    }
    drawPath(
        path = crownPath,
        brush = Brush.verticalGradient(listOf(Color(0xFFFFF59D), Color(0xFFFFD600), Color(0xFFD97706)))
    )
    drawPath(
        path = crownPath,
        color = Color(0xFFB45309),
        style = Stroke(width = minDim * 0.035f, join = StrokeJoin.Round)
    )
    // Crown band & jewels (ruby + emeralds)
    drawCircle(color = Color(0xFFEF4444), radius = minDim * 0.05f, center = Offset(c.x, c.y + crownH * 0.2f))
    drawCircle(color = Color(0xFF10B981), radius = minDim * 0.04f, center = Offset(c.x - crownW * 0.26f, c.y + crownH * 0.22f))
    drawCircle(color = Color(0xFF10B981), radius = minDim * 0.04f, center = Offset(c.x + crownW * 0.26f, c.y + crownH * 0.22f))
    // Peak sparkle
    drawSparkle(Offset(c.x, topY - minDim * 0.04f), minDim * 0.08f, Color(0xFFFFFDE7))
}

private fun DrawScope.drawFlairGolacoIcon(c: Offset, minDim: Float) {
    // Dynamic golden/emerald shockwave burst
    for (i in 0 until 8) {
        val angle = (i * 45f) * (PI / 180.0)
        val p1 = Offset(c.x + minDim * 0.25f * cos(angle).toFloat(), c.y + minDim * 0.25f * sin(angle).toFloat())
        val p2 = Offset(c.x + minDim * 0.42f * cos(angle).toFloat(), c.y + minDim * 0.42f * sin(angle).toFloat())
        drawLine(
            color = if (i % 2 == 0) Color(0xFF00E676) else Color(0xFFFFD600),
            start = p1,
            end = p2,
            strokeWidth = minDim * 0.055f,
            cap = StrokeCap.Round
        )
    }
    drawCircle(
        color = Color(0xFF00E676).copy(alpha = 0.28f),
        radius = minDim * 0.32f,
        center = c,
        style = Stroke(width = minDim * 0.04f)
    )
    // Flying soccer ball
    drawSoccerBall(
        center = c,
        radius = minDim * 0.21f,
        baseBrush = Brush.radialGradient(
            colors = listOf(Color.White, Color(0xFFD1FAE5), Color(0xFF059669)),
            center = Offset(c.x - minDim * 0.05f, c.y - minDim * 0.05f),
            radius = minDim * 0.25f
        ),
        seamColor = Color(0xFF064E3B)
    )
}

private fun DrawScope.drawFlairVipIcon(c: Offset, minDim: Float) {
    // Luxury black-and-gold velvet VIP pass
    val cardW = minDim * 0.72f
    val cardH = minDim * 0.5f
    val tl = Offset(c.x - cardW / 2f, c.y - cardH / 2f)
    drawRoundRect(
        brush = Brush.linearGradient(listOf(Color(0xFF1E1B18), Color(0xFF09090B), Color(0xFF292212))),
        topLeft = tl,
        size = Size(cardW, cardH),
        cornerRadius = CornerRadius(minDim * 0.1f, minDim * 0.1f)
    )
    drawRoundRect(
        brush = Brush.linearGradient(listOf(Color(0xFFFFF59D), Color(0xFFFFD600), Color(0xFFB45309))),
        topLeft = tl,
        size = Size(cardW, cardH),
        cornerRadius = CornerRadius(minDim * 0.1f, minDim * 0.1f),
        style = Stroke(width = minDim * 0.045f)
    )
    // Crown emblem inside pass
    val cw = cardW * 0.45f
    val ch = cardH * 0.42f
    val crown = Path().apply {
        moveTo(c.x - cw * 0.45f, c.y + ch * 0.45f)
        lineTo(c.x - cw * 0.5f, c.y - ch * 0.2f)
        lineTo(c.x - cw * 0.2f, c.y + ch * 0.05f)
        lineTo(c.x, c.y - ch * 0.45f)
        lineTo(c.x + cw * 0.2f, c.y + ch * 0.05f)
        lineTo(c.x + cw * 0.5f, c.y - ch * 0.2f)
        lineTo(c.x + cw * 0.45f, c.y + ch * 0.45f)
        close()
    }
    drawPath(crown, color = Color(0xFFFFD600))
    // Corner sparkles
    drawSparkle(Offset(tl.x + minDim * 0.04f, tl.y + minDim * 0.04f), minDim * 0.07f, Color(0xFFFFF59D))
    drawSparkle(Offset(tl.x + cardW - minDim * 0.04f, tl.y + cardH - minDim * 0.04f), minDim * 0.06f, Color(0xFFFFD600))
}

private fun DrawScope.drawSoccerBall(
    center: Offset,
    radius: Float,
    baseBrush: Brush,
    seamColor: Color
) {
    drawCircle(brush = baseBrush, radius = radius, center = center)
    drawCircle(color = seamColor, radius = radius, center = center, style = Stroke(width = radius * 0.1f))
    // Center pentagon
    val pent = createPolygonPath(center, radius * 0.36f, 5, -90f)
    drawPath(pent, color = seamColor)
    // Radiating seams
    for (i in 0 until 5) {
        val angle = (i * 72f - 90f) * (PI / 180.0)
        val p1 = Offset(center.x + radius * 0.34f * cos(angle).toFloat(), center.y + radius * 0.34f * sin(angle).toFloat())
        val p2 = Offset(center.x + radius * 0.92f * cos(angle).toFloat(), center.y + radius * 0.92f * sin(angle).toFloat())
        drawLine(color = seamColor, start = p1, end = p2, strokeWidth = radius * 0.09f, cap = StrokeCap.Round)
    }
}

private fun DrawScope.drawSparkle(center: Offset, radius: Float, color: Color) {
    val path = createStarPath(center, radius, radius * 0.28f, 4, 0f)
    drawPath(path = path, color = color)
}

private fun createStarPath(
    center: Offset,
    outerRadius: Float,
    innerRadius: Float,
    points: Int = 5,
    rotationDeg: Float = -90f
): Path {
    val path = Path()
    val totalPoints = points * 2
    val step = 360f / totalPoints
    for (i in 0 until totalPoints) {
        val r = if (i % 2 == 0) outerRadius else innerRadius
        val angleRad = (rotationDeg + i * step) * (PI / 180.0)
        val x = center.x + r * cos(angleRad).toFloat()
        val y = center.y + r * sin(angleRad).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

private fun createPolygonPath(
    center: Offset,
    radius: Float,
    sides: Int,
    rotationDeg: Float = -90f
): Path {
    val path = Path()
    val step = 360f / sides
    for (i in 0 until sides) {
        val angleRad = (rotationDeg + i * step) * (PI / 180.0)
        val x = center.x + radius * cos(angleRad).toFloat()
        val y = center.y + radius * sin(angleRad).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}
