package com.bolaodosvei.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bolaodosvei.app.model.Match
import com.bolaodosvei.app.model.MatchStatus
import com.bolaodosvei.app.ui.theme.*

@Composable
fun LiveTickerBar(
    matches: List<Match>,
    lastUpdated: String,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onMatchClick: (Match) -> Unit
) {
    val liveCount = matches.count { it.status == MatchStatus.LIVE || it.status == MatchStatus.HALFTIME }
    val pulseAlpha = if (liveCount > 0) {
        val infiniteTransition = rememberInfiniteTransition(label = "live_pulse")
        val animated by infiniteTransition.animateFloat(
            initialValue = 0.25f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(750),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_alpha"
        )
        animated
    } else {
        0.45f
    }

    val theme = LocalBolaoTheme.current
    val islandColor = if (theme.isLightTheme) theme.surfaceColor.copy(alpha = 0.96f) else Color(0xFF111114)
    val islandBorder = if (theme.isLightTheme) theme.borderColor else Color(0xFF232326)

    // Skiper UI Dynamic Island Live Match Pill
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = islandColor,
        border = BorderStroke(1.dp, islandBorder),
        shadowElevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 9.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .alpha(pulseAlpha)
                            .clip(CircleShape)
                            .background(LiveRed)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "JOGOS DE HOJE",
                        color = theme.textPrimaryColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = LiveRed.copy(alpha = 0.16f),
                        shape = CircleShape,
                        border = BorderStroke(0.8.dp, LiveRed.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = "$liveCount LIVE",
                            color = LiveRed,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (lastUpdated.isNotBlank()) {
                        Text(
                            text = lastUpdated,
                            color = theme.textSecondaryColor.copy(alpha = 0.8f),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Surface(
                        shape = CircleShape,
                        color = theme.cardColor,
                        border = BorderStroke(1.dp, islandBorder),
                        modifier = Modifier.size(26.dp)
                    ) {
                        IconButton(
                            onClick = onRefresh,
                            enabled = !isRefreshing,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Atualizar placares",
                                tint = if (isRefreshing) theme.accentColor else theme.textSecondaryColor,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(7.dp))

            if (matches.isEmpty()) {
                Text(
                    text = if (isRefreshing) "Sincronizando jogos de hoje..." else "Nenhum jogo oficial programado para hoje",
                    color = theme.textSecondaryColor,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(matches, key = { it.id }) { match ->
                        LiveTickerCard(
                            match = match,
                            pulseAlpha = pulseAlpha,
                            onClick = { onMatchClick(match) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveTickerCard(
    match: Match,
    pulseAlpha: Float,
    onClick: () -> Unit
) {
    val theme = LocalBolaoTheme.current
    val isLive = match.status == MatchStatus.LIVE || match.status == MatchStatus.HALFTIME
    val cardBorder = if (isLive) theme.accentColor.copy(alpha = 0.5f) else if (theme.isLightTheme) theme.borderColor else Color(0xFF232326)

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (theme.isLightTheme) theme.cardColor else Color(0xFF17171B),
        border = BorderStroke(1.dp, cardBorder),
        modifier = Modifier
            .width(172.dp)
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = match.league.shortName.uppercase(),
                    color = theme.accentColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1
                )
                when (match.status) {
                    MatchStatus.LIVE -> Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .alpha(pulseAlpha)
                                .clip(CircleShape)
                                .background(LiveRed)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${match.minute}'",
                            color = LiveRed,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    MatchStatus.HALFTIME -> Text(
                        text = "INT",
                        color = theme.accentColor,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    MatchStatus.FINISHED -> Text(
                        text = "FIM",
                        color = theme.textSecondaryColor,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    MatchStatus.SCHEDULED -> Text(
                        text = match.kickoffTime,
                        color = theme.accentColor,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TeamCrest(
                        teamName = match.homeTeam.name,
                        shortName = match.homeTeam.shortName,
                        logoUrl = match.homeTeam.logoUrl,
                        colorHex = match.homeTeam.colorHex,
                        size = 18.dp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = match.homeTeam.shortName,
                        color = theme.textPrimaryColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = if (match.status == MatchStatus.SCHEDULED) "x" else "${match.homeScore}-${match.awayScore}",
                    color = if (isLive) theme.accentColor else theme.textPrimaryColor,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = match.awayTeam.shortName,
                        color = theme.textPrimaryColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    TeamCrest(
                        teamName = match.awayTeam.name,
                        shortName = match.awayTeam.shortName,
                        logoUrl = match.awayTeam.logoUrl,
                        colorHex = match.awayTeam.colorHex,
                        size = 18.dp
                    )
                }
            }
        }
    }
}
