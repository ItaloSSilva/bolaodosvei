package com.bolaodosvei.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bolaodosvei.app.model.GoalEvent
import com.bolaodosvei.app.ui.theme.EmeraldAccent
import com.bolaodosvei.app.ui.theme.GoldAccent
import com.bolaodosvei.app.ui.theme.PitchCard
import com.bolaodosvei.app.ui.theme.TextPrimary
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

@Composable
fun GoalAnimationOverlay(
    event: GoalEvent,
    onDismiss: () -> Unit
) {
    val progress = remember(event.id) { Animatable(0f) }

    LaunchedEffect(event.id) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2800, easing = LinearEasing)
        )
        delay(900L)
        onDismiss()
    }

    val p = progress.value
    // Phase 1: Ball approaches goal (0.0 -> 0.45)
    // Phase 2: Impact & net ripple + confetti (0.45 -> 1.0)
    val hitTime = 0.45f
    val postHit = ((p - hitTime) / (1f - hitTime)).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xD9040D08))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        // Stadium flash + Goal + Net + Ball + Confetti Canvas
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(380.dp)
                .align(Alignment.TopCenter)
                .padding(top = 56.dp)
        ) {
            val w = size.width
            val h = size.height

            // Goal dimensions
            val goalLeft = w * 0.12f
            val goalRight = w * 0.88f
            val goalTop = h * 0.14f
            val goalBottom = h * 0.62f
            val goalWidth = goalRight - goalLeft
            val goalHeight = goalBottom - goalTop

            // Impact point inside upper-right or upper-left corner of net
            val impactX = if (event.isHomeTeam) goalLeft + goalWidth * 0.72f else goalLeft + goalWidth * 0.28f
            val impactY = goalTop + goalHeight * 0.42f

            // Stadium glow flash on impact
            if (postHit > 0f) {
                val flashAlpha = (1f - postHit).coerceIn(0f, 1f) * 0.55f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(GoldAccent.copy(alpha = flashAlpha), EmeraldAccent.copy(alpha = flashAlpha * 0.5f), Color.Transparent),
                        center = Offset(impactX, impactY),
                        radius = w * (0.25f + postHit * 0.7f)
                    ),
                    center = Offset(impactX, impactY),
                    radius = w * (0.25f + postHit * 0.7f)
                )
            }

            // Grass pitch strip under the goal
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF0E4D2B), Color(0xFF072916))
                ),
                topLeft = Offset(0f, goalBottom - 8f),
                size = androidx.compose.ui.geometry.Size(w, h - goalBottom + 8f)
            )

            // Back net depth corners
            val backLeft = goalLeft + goalWidth * 0.08f
            val backRight = goalRight - goalWidth * 0.08f
            val backTop = goalTop + goalHeight * 0.14f
            val backBottom = goalBottom - goalHeight * 0.08f

            // Compute net ripple displacement
            fun netDisplace(x: Float, y: Float): Offset {
                if (postHit <= 0f) return Offset(x, y)
                val dx = x - impactX
                val dy = y - impactY
                val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                val falloff = exp(-dist / (goalWidth * 0.28f))
                val wave = sin(postHit * 22f - dist * 0.03f) * exp(-postHit * 3.2f)
                val bulge = 28f * falloff * (wave + 0.45f * exp(-postHit * 2.5f))
                return Offset(x + (dx * 0.04f * falloff), y - bulge * 0.35f + bulge)
            }

            // Draw dynamic net mesh (vertical & horizontal lines)
            val cols = 14
            val rows = 8
            val netColor = Color.White.copy(alpha = 0.55f)

            for (c in 0..cols) {
                val frac = c / cols.toFloat()
                val topX = backLeft + (backRight - backLeft) * frac
                val path = Path()
                for (r in 0..rows) {
                    val rFrac = r / rows.toFloat()
                    val y = backTop + (backBottom - backTop) * rFrac
                    val pt = netDisplace(topX, y)
                    if (r == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
                }
                drawPath(path, color = netColor, style = Stroke(width = 2.2f))
            }

            for (r in 0..rows) {
                val rFrac = r / rows.toFloat()
                val y = backTop + (backBottom - backTop) * rFrac
                val path = Path()
                for (c in 0..cols) {
                    val frac = c / cols.toFloat()
                    val x = backLeft + (backRight - backLeft) * frac
                    val pt = netDisplace(x, y)
                    if (c == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
                }
                drawPath(path, color = netColor, style = Stroke(width = 2.2f))
            }

            // Side net perspective lines
            drawLine(netColor, Offset(goalLeft, goalTop), Offset(backLeft, backTop), strokeWidth = 2.5f)
            drawLine(netColor, Offset(goalRight, goalTop), Offset(backRight, backTop), strokeWidth = 2.5f)
            drawLine(netColor, Offset(goalLeft, goalBottom), Offset(backLeft, backBottom), strokeWidth = 2.5f)
            drawLine(netColor, Offset(goalRight, goalBottom), Offset(backRight, backBottom), strokeWidth = 2.5f)

            // Soccer ball trajectory (Bezier curve into the net, then drops slightly inside net)
            val startX = if (event.isHomeTeam) w * 0.18f else w * 0.82f
            val startY = h * 0.96f
            val ctrlX = w * 0.50f
            val ctrlY = h * 0.15f

            val ballPos: Offset
            val ballRadius: Float
            val ballRotation: Float

            if (p <= hitTime) {
                val t = FastOutSlowInEasing.transform((p / hitTime).coerceIn(0f, 1f))
                val inv = 1f - t
                val bx = inv * inv * startX + 2f * inv * t * ctrlX + t * t * impactX
                val by = inv * inv * startY + 2f * inv * t * ctrlY + t * t * impactY
                ballPos = Offset(bx, by)
                ballRadius = 38f * (1f - 0.45f * t)
                ballRotation = t * 540f
            } else {
                // Ball settles inside the net after impact
                val drop = FastOutSlowInEasing.transform(postHit.coerceAtMost(1f))
                val settleX = impactX + (if (event.isHomeTeam) -10f else 10f) * drop
                val settleY = impactY + (backBottom - impactY - 14f) * drop * 0.85f
                ballPos = Offset(settleX, settleY)
                ballRadius = 21f
                ballRotation = 540f + drop * 180f
            }

            // Draw soccer ball with pentagon pattern
            rotate(degrees = ballRotation, pivot = ballPos) {
                drawCircle(color = Color.White, radius = ballRadius, center = ballPos)
                drawCircle(
                    color = Color(0xFF111827),
                    radius = ballRadius,
                    center = ballPos,
                    style = Stroke(width = 2.5f)
                )
                // Center pentagon
                drawCircle(color = Color(0xFF111827), radius = ballRadius * 0.34f, center = ballPos)
                // Surrounding patches
                for (i in 0 until 5) {
                    val angle = (i * 72.0) * PI / 180.0
                    val px = ballPos.x + cos(angle).toFloat() * ballRadius * 0.72f
                    val py = ballPos.y + sin(angle).toFloat() * ballRadius * 0.72f
                    drawCircle(color = Color(0xFF111827), radius = ballRadius * 0.22f, center = Offset(px, py))
                }
            }

            // Goal posts & crossbar drawn over the ball once it's inside the net for true depth
            val postColor = Color(0xFFF8FAFC)
            drawLine(postColor, Offset(goalLeft, goalBottom), Offset(goalLeft, goalTop), strokeWidth = 10f, cap = StrokeCap.Round)
            drawLine(postColor, Offset(goalRight, goalBottom), Offset(goalRight, goalTop), strokeWidth = 10f, cap = StrokeCap.Round)
            drawLine(postColor, Offset(goalLeft, goalTop), Offset(goalRight, goalTop), strokeWidth = 10f, cap = StrokeCap.Round)

            // Confetti burst on impact
            if (postHit > 0f) {
                val confettiColors = listOf(GoldAccent, EmeraldAccent, Color.White, Color(0xFFFF5252), Color(0xFF38BDF8))
                for (i in 0 until 28) {
                    val angle = (i * 137.5) * PI / 180.0
                    val speed = 80f + (i % 6) * 45f
                    val cx = impactX + cos(angle).toFloat() * speed * postHit
                    val cy = impactY + sin(angle).toFloat() * speed * postHit + 90f * postHit * postHit
                    val alpha = (1f - postHit).coerceIn(0f, 1f)
                    drawCircle(
                        color = confettiColors[i % confettiColors.size].copy(alpha = alpha),
                        radius = 5.5f + (i % 3) * 2f,
                        center = Offset(cx, cy)
                    )
                }
            }
        }

        // Celebratory Banner
        if (postHit > 0.05f) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 24.dp, vertical = 68.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = PitchCard,
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "GOOOOOL!",
                        color = GoldAccent,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${event.scoringTeamName} • ${event.scorerName} (${event.minute}')",
                        color = EmeraldAccent,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "${event.match.homeTeam.shortName}  ${event.match.homeScore} x ${event.match.awayScore}  ${event.match.awayTeam.shortName}",
                        color = TextPrimary,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${event.match.league.displayName} • Toque para fechar",
                        color = Color(0xFF98A0B3),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
