package com.bolaodosvei.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val BallPatchDark = Color(0xFF111318)
private val BallSeamDark = Color(0xFF1F242D)

/**
 * Minimalist loading overlay: pure black background with only a spinning soccer ball centered on screen.
 */
@Composable
fun SplashIntroOverlay(
    isLoading: Boolean = false,
    onFinished: () -> Unit
) {
    val currentIsLoading by rememberUpdatedState(isLoading)

    LaunchedEffect(Unit) {
        delay(1200L)
        withTimeoutOrNull(2500L) {
            while (currentIsLoading) {
                delay(80L)
            }
        }
        onFinished()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "loading_ball_spin")
    val rotationDeg by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ball_rotation_deg"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(64.dp)) {
            drawSpinningSoccerBall(rotationDeg = rotationDeg)
        }
    }
}

private fun DrawScope.drawSpinningSoccerBall(rotationDeg: Float) {
    val radius = size.minDimension * 0.5f
    val center = Offset(size.width * 0.5f, size.height * 0.5f)

    val circleClip = Path().apply {
        addOval(Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius))
    }

    clipPath(circleClip) {
        // White leather base
        drawCircle(
            color = Color.White,
            radius = radius,
            center = center
        )

        rotate(degrees = rotationDeg, pivot = center) {
            val seamWidth = (radius * 0.055f).coerceAtLeast(2f)
            val centerPentagonR = radius * 0.34f
            val outerDist = radius * 0.90f
            val outerPentagonR = radius * 0.32f

            // Radial & perimeter seams forming the classic hexagons and pentagons
            val sideVerticesLeft = Array(5) { Offset.Zero }
            val sideVerticesRight = Array(5) { Offset.Zero }

            for (i in 0 until 5) {
                val baseDeg = -90.0 + i * 72.0
                val rad = baseDeg * PI / 180.0
                val innerVertex = Offset(
                    center.x + cos(rad).toFloat() * (centerPentagonR * 0.95f),
                    center.y + sin(rad).toFloat() * (centerPentagonR * 0.95f)
                )
                val outerCenter = Offset(
                    center.x + cos(rad).toFloat() * outerDist,
                    center.y + sin(rad).toFloat() * outerDist
                )
                val inwardRad = (baseDeg + 180.0) * PI / 180.0
                val outerInwardVertex = Offset(
                    outerCenter.x + cos(inwardRad).toFloat() * (outerPentagonR * 0.95f),
                    outerCenter.y + sin(inwardRad).toFloat() * (outerPentagonR * 0.95f)
                )

                drawLine(
                    color = BallSeamDark,
                    start = innerVertex,
                    end = outerInwardVertex,
                    strokeWidth = seamWidth,
                    cap = StrokeCap.Round
                )

                val leftRad = (baseDeg + 108.0) * PI / 180.0
                val rightRad = (baseDeg + 252.0) * PI / 180.0
                sideVerticesRight[i] = Offset(
                    outerCenter.x + cos(leftRad).toFloat() * outerPentagonR,
                    outerCenter.y + sin(leftRad).toFloat() * outerPentagonR
                )
                sideVerticesLeft[i] = Offset(
                    outerCenter.x + cos(rightRad).toFloat() * outerPentagonR,
                    outerCenter.y + sin(rightRad).toFloat() * outerPentagonR
                )
            }

            for (i in 0 until 5) {
                val next = (i + 1) % 5
                drawLine(
                    color = BallSeamDark,
                    start = sideVerticesRight[i],
                    end = sideVerticesLeft[next],
                    strokeWidth = seamWidth,
                    cap = StrokeCap.Round
                )
            }

            // Center black pentagon
            drawPentagon(
                center = center,
                radius = centerPentagonR,
                rotationOffsetDeg = 0f,
                color = BallPatchDark
            )

            // 5 surrounding black pentagons along the curved rim
            for (i in 0 until 5) {
                val rad = (-90.0 + i * 72.0) * PI / 180.0
                val outerCenter = Offset(
                    center.x + cos(rad).toFloat() * outerDist,
                    center.y + sin(rad).toFloat() * outerDist
                )
                drawPentagon(
                    center = outerCenter,
                    radius = outerPentagonR,
                    rotationOffsetDeg = 36f,
                    color = BallPatchDark
                )
            }
        }

        // Subtle spherical shading so the ball has clean depth on a pure black background
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.Transparent,
                    Color(0x33000000)
                ),
                center = Offset(center.x - radius * 0.22f, center.y - radius * 0.22f),
                radius = radius * 1.25f
            ),
            radius = radius,
            center = center
        )
    }

    // Crisp outer rim
    drawCircle(
        color = Color(0xFFD0D7E2),
        radius = radius - 0.75f,
        center = center,
        style = Stroke(width = 1.5f)
    )
}

private fun DrawScope.drawPentagon(
    center: Offset,
    radius: Float,
    rotationOffsetDeg: Float,
    color: Color
) {
    val path = Path()
    for (i in 0 until 5) {
        val rad = (-90.0 + rotationOffsetDeg + i * 72.0) * PI / 180.0
        val x = center.x + cos(rad).toFloat() * radius
        val y = center.y + sin(rad).toFloat() * radius
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path = path, color = color)
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = 1.5f, join = StrokeJoin.Round)
    )
}
