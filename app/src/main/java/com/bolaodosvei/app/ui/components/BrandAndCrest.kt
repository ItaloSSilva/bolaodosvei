package com.bolaodosvei.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.bolaodosvei.app.R
import com.bolaodosvei.app.ui.theme.GoldAccent
import com.bolaodosvei.app.ui.theme.PitchCard
import java.io.File
import kotlin.math.roundToInt

/**
 * Custom artwork emblem for Bolão dos Véi (replaces emojis with our dedicated design).
 */
@Composable
fun BolaoLogoBadge(
    size: Dp,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = CircleShape,
        color = Color(0xFF09090B),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.48f),
                    Color(0xFF27272A),
                    Color.White.copy(alpha = 0.16f)
                )
            )
        ),
        shadowElevation = if (size >= 64.dp) 12.dp else 4.dp,
        modifier = modifier.size(size)
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo_bolao_dos_vei),
            contentDescription = "Logo Bolão dos Véi",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
        )
    }
}

/**
 * Loads official club crest from URL via Coil, with a styled shield monogram fallback when offline.
 */
@Composable
fun TeamCrest(
    teamName: String,
    shortName: String,
    logoUrl: String,
    colorHex: Long,
    size: Dp = 36.dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val pxSize = with(LocalDensity.current) { size.toPx().roundToInt().coerceAtLeast(1) }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        if (logoUrl.isNotBlank()) {
            val request = remember(logoUrl, pxSize) {
                ImageRequest.Builder(context)
                    .data(logoUrl)
                    .size(pxSize)
                    .crossfade(false)
                    .build()
            }
            SubcomposeAsyncImage(
                model = request,
                contentDescription = "Escudo $teamName",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(1.dp),
                loading = {
                    FallbackCrestBadge(shortName = shortName, colorHex = colorHex, size = size)
                },
                error = {
                    FallbackCrestBadge(shortName = shortName, colorHex = colorHex, size = size)
                }
            )
        } else {
            FallbackCrestBadge(shortName = shortName, colorHex = colorHex, size = size)
        }
    }
}

@Composable
private fun FallbackCrestBadge(
    shortName: String,
    colorHex: Long,
    size: Dp
) {
    val base = Color(colorHex)
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(base, base.copy(alpha = 0.65f))
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = shortName.take(3).uppercase(),
            color = Color.White,
            fontSize = (size.value * 0.29f).sp,
            fontWeight = FontWeight.Black
        )
    }
}

/**
 * Sleek monogram or gallery photo avatar badge for real users (with optional shop frame).
 */
@Composable
fun UserAvatarBadge(
    name: String,
    colorHex: Long,
    size: Dp = 40.dp,
    photoPath: String? = null,
    frameAccentHex: Long? = null,
    modifier: Modifier = Modifier,
    frameId: String? = null
) {
    val initials = name
        .trim()
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifBlank { "BV" }

    val base = Color(colorHex)
    val hasFrame = frameId != null || frameAccentHex != null
    val innerPadding = if (hasFrame) (size * 0.08f).coerceAtLeast(3.dp) else 0.dp

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(base, Color(0xFF141824))
                    )
                )
                .then(
                    if (!hasFrame) {
                        Modifier.border(1.5.dp, base.copy(alpha = 0.85f), CircleShape)
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (!photoPath.isNullOrBlank()) {
                val context = LocalContext.current
                val pxSize = with(LocalDensity.current) { size.toPx().roundToInt().coerceAtLeast(1) }
                val request = remember(photoPath, pxSize) {
                    ImageRequest.Builder(context)
                        .data(File(photoPath))
                        .size(pxSize)
                        .crossfade(false)
                        .build()
                }
                SubcomposeAsyncImage(
                    model = request,
                    contentDescription = "Foto de $name",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    loading = {
                        Text(
                            text = initials,
                            color = Color.White,
                            fontSize = (size.value * 0.34f).sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    },
                    error = {
                        Text(
                            text = initials,
                            color = Color.White,
                            fontSize = (size.value * 0.34f).sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                )
            } else {
                Text(
                    text = initials,
                    color = Color.White,
                    fontSize = (size.value * 0.34f).sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        if (hasFrame) {
            HandcraftedAvatarFrameOverlay(
                frameId = frameId,
                fallbackAccentHex = frameAccentHex,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
