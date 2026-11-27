package com.bolaodosvei.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bolaodosvei.app.model.*
import com.bolaodosvei.app.ui.components.TeamCrest
import com.bolaodosvei.app.ui.components.UserAvatarBadge
import com.bolaodosvei.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchesAndPredictionsScreen(
    matches: List<Match>,
    userPredictions: Map<String, Prediction>,
    participants: List<FriendBolao>,
    selectedLeagueFilter: League?,
    onSelectLeagueFilter: (League?) -> Unit,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onSavePrediction: (String, Int, Int) -> Unit,
    upcomingMatches: List<Match> = emptyList(),
    onToggleJoker: (String) -> Unit = {}
) {
    val activeTheme = LocalBolaoTheme.current
    val ctaBg = if (activeTheme.isLightTheme) Color(0xFF09090B) else Color.White
    val ctaFg = if (activeTheme.isLightTheme) Color.White else Color(0xFF09090B)
    var selectedDayFilter by remember { mutableStateOf(MatchDayFilter.TODAY) }

    val dayMatches = remember(matches, upcomingMatches, selectedDayFilter) {
        when (selectedDayFilter) {
            MatchDayFilter.TODAY -> matches
            MatchDayFilter.TOMORROW -> upcomingMatches.filter { it.dayOffset == 1 }
            MatchDayFilter.UPCOMING -> upcomingMatches.filter { it.dayOffset >= 2 }
        }
    }

    val filteredMatches = remember(dayMatches, selectedLeagueFilter) {
        if (selectedLeagueFilter == null) dayMatches else dayMatches.filter { it.league == selectedLeagueFilter }
    }

    val countsByLeague = remember(dayMatches) {
        dayMatches.groupingBy { it.league }.eachCount()
    }

    val missingPredictionsCount = remember(dayMatches, userPredictions) {
        dayMatches.count { m ->
            m.status == MatchStatus.SCHEDULED && !userPredictions.containsKey(m.id)
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Day Filter Segmented Row + Missing Predictions Badge
            item(key = "day_filter", contentType = "day_filter") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = activeTheme.cardColor,
                        border = BorderStroke(1.dp, activeTheme.borderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            MatchDayFilter.entries.forEach { dayFilter ->
                                val selected = dayFilter == selectedDayFilter
                                Surface(
                                    shape = CircleShape,
                                    color = if (selected) ctaBg else Color.Transparent,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(CircleShape)
                                        .clickable { selectedDayFilter = dayFilter }
                                ) {
                                    Text(
                                        text = dayFilter.label,
                                        color = if (selected) ctaFg else activeTheme.textSecondaryColor,
                                        fontSize = 12.sp,
                                        fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (missingPredictionsCount > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = GoldAccent.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "Faltam $missingPredictionsCount palpite${if (missingPredictionsCount == 1) "" else "s"}",
                                    color = GoldAccent,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Text(
                                text = "Arraste para atualizar",
                                color = activeTheme.textSecondaryColor,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            // OriginKit Count-Badge Filter Pills
            item(key = "league_filter", contentType = "league_filter") {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        LeagueCountPill(
                            label = "Todas",
                            count = dayMatches.size,
                            selected = selectedLeagueFilter == null,
                            accentColor = activeTheme.accentColor,
                            onClick = { onSelectLeagueFilter(null) }
                        )
                    }
                    items(League.entries, key = { it.id }) { league ->
                        LeagueCountPill(
                            label = league.shortName,
                            count = countsByLeague[league] ?: 0,
                            selected = selectedLeagueFilter == league,
                            accentColor = Color(league.accentColorHex),
                            onClick = { onSelectLeagueFilter(league) }
                        )
                    }
                }
            }

            if (filteredMatches.isEmpty()) {
                item(key = "empty_state", contentType = "empty_state") {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = activeTheme.cardColor,
                        border = BorderStroke(1.dp, activeTheme.borderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.padding(10.dp)) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = activeTheme.surfaceColor,
                                border = BorderStroke(1.dp, activeTheme.borderColor.copy(alpha = 0.7f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 28.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    if (isRefreshing) {
                                        CircularProgressIndicator(
                                            color = activeTheme.accentColor,
                                            strokeWidth = 2.5.dp,
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Spacer(modifier = Modifier.height(14.dp))
                                        Text(
                                            text = "Carregando jogos...",
                                            color = activeTheme.textPrimaryColor,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Buscando placares e horários oficiais da rodada.",
                                            color = activeTheme.textSecondaryColor,
                                            fontSize = 12.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.SportsSoccer,
                                            contentDescription = null,
                                            tint = activeTheme.textSecondaryColor,
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = if (selectedLeagueFilter == null) {
                                                "Nenhum jogo programado para ${selectedDayFilter.label.lowercase()}"
                                            } else {
                                                "Sem jogos na ${selectedLeagueFilter.displayName}"
                                            },
                                            color = activeTheme.textPrimaryColor,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = if (selectedLeagueFilter == null) {
                                                "Os jogos oficiais aparecerão automaticamente aqui assim que estiverem disponíveis."
                                            } else {
                                                "Toque em \"Todas\" para ver os jogos de outras ligas."
                                            },
                                            color = activeTheme.textSecondaryColor,
                                            fontSize = 12.sp,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Button(
                                            onClick = onRefresh,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = ctaBg,
                                                contentColor = ctaFg
                                            ),
                                            shape = CircleShape,
                                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                                        ) {
                                            Text("Atualizar jogos", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                items(
                    items = filteredMatches,
                    key = { it.id },
                    contentType = { "match_card" }
                ) { match ->
                    MatchPredictionCard(
                        match = match,
                        savedPrediction = userPredictions[match.id],
                        otherFriends = participants.filter { !it.isCurrentUser },
                        onSavePrediction = { h, a -> onSavePrediction(match.id, h, a) },
                        onToggleJoker = { onToggleJoker(match.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun LeagueCountPill(
    label: String,
    count: Int,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    val activeTheme = LocalBolaoTheme.current
    val pillBg = when {
        selected && activeTheme.isLightTheme -> Color(0xFF09090B)
        selected -> Color.White
        else -> activeTheme.cardColor
    }
    val pillFg = when {
        selected && activeTheme.isLightTheme -> Color.White
        selected -> Color(0xFF09090B)
        else -> activeTheme.textSecondaryColor
    }
    val badgeBg = when {
        selected -> accentColor.copy(alpha = 0.22f)
        else -> activeTheme.surfaceColor
    }
    val badgeFg = when {
        selected -> pillFg
        else -> activeTheme.textPrimaryColor
    }

    Surface(
        shape = CircleShape,
        color = pillBg,
        border = BorderStroke(1.dp, if (selected) pillBg else activeTheme.borderColor),
        modifier = Modifier
            .clip(CircleShape)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 8.dp, top = 7.dp, bottom = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = label,
                color = pillFg,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                maxLines = 1
            )
            Surface(
                shape = CircleShape,
                color = badgeBg
            ) {
                Text(
                    text = "$count",
                    color = badgeFg,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun MatchPredictionCard(
    match: Match,
    savedPrediction: Prediction?,
    otherFriends: List<FriendBolao>,
    onSavePrediction: (Int, Int) -> Unit,
    onToggleJoker: () -> Unit
) {
    val activeTheme = LocalBolaoTheme.current
    val haptic = LocalHapticFeedback.current
    val ctaBg = if (activeTheme.isLightTheme) Color(0xFF09090B) else Color.White
    val ctaFg = if (activeTheme.isLightTheme) Color.White else Color(0xFF09090B)

    var draftHome by remember(match.id, savedPrediction) {
        mutableIntStateOf(savedPrediction?.homeScore ?: 1)
    }
    var draftAway by remember(match.id, savedPrediction) {
        mutableIntStateOf(savedPrediction?.awayScore ?: 1)
    }
    var showFriends by remember(match.id) { mutableStateOf(false) }

    val isLive = match.status == MatchStatus.LIVE || match.status == MatchStatus.HALFTIME
    val isLocked = match.status != MatchStatus.SCHEDULED
    val isJoker = savedPrediction?.isJoker == true
    val earnedPoints = calculatePredictionPoints(match, savedPrediction)
    val friendsWithPrediction = otherFriends.filter { it.predictions.containsKey(match.id) }
    val isAlreadySavedExact = savedPrediction != null &&
        draftHome == savedPrediction.homeScore &&
        draftAway == savedPrediction.awayScore

    // Outer Double-Nested Bento Shell
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = activeTheme.cardColor,
        border = BorderStroke(
            width = 1.dp,
            color = when {
                isLive -> LiveRed.copy(alpha = 0.55f)
                isJoker -> GoldAccent.copy(alpha = 0.65f)
                else -> activeTheme.borderColor
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Top Telemetry Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Color(match.league.accentColorHex))
                    )
                    Text(
                        text = match.league.displayName.uppercase(),
                        color = activeTheme.textSecondaryColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp
                    )
                }
                when (match.status) {
                    MatchStatus.LIVE -> Surface(
                        color = LiveRed.copy(alpha = 0.16f),
                        shape = CircleShape,
                        border = BorderStroke(1.dp, LiveRed.copy(alpha = 0.45f))
                    ) {
                        Text(
                            text = "LIVE ${match.minute}'",
                            color = LiveRed,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    MatchStatus.HALFTIME -> Surface(
                        color = GoldAccent.copy(alpha = 0.14f),
                        shape = CircleShape
                    ) {
                        Text(
                            text = "INTERVALO",
                            color = GoldAccent,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    MatchStatus.FINISHED -> Text(
                        text = "ENCERRADO",
                        color = activeTheme.textSecondaryColor,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    MatchStatus.SCHEDULED -> Text(
                        text = if (match.dayOffset > 0) "${match.dateLabel} · ${match.kickoffTime}" else match.kickoffTime,
                        color = EmeraldAccent,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Inset Scoreboard Stage (Double-Nested Inner Surface)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = activeTheme.surfaceColor,
                border = BorderStroke(1.dp, activeTheme.borderColor.copy(alpha = 0.75f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TeamColumn(team = match.homeTeam, modifier = Modifier.weight(1f))
                        Spacer(modifier = Modifier.width(64.dp))
                        TeamColumn(team = match.awayTeam, modifier = Modifier.weight(1f))
                    }

                    // Centered Overlapping Circular VS / Live Score Connector Badge
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = activeTheme.cardColor,
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isLive) GoldAccent.copy(alpha = 0.65f) else activeTheme.borderColor
                            )
                        ) {
                            Text(
                                text = if (match.status == MatchStatus.SCHEDULED) "VS" else "${match.homeScore}:${match.awayScore}",
                                color = if (isLive) GoldAccent else activeTheme.textPrimaryColor,
                                fontSize = if (match.status == MatchStatus.SCHEDULED) 12.sp else 16.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(
                                    horizontal = if (match.status == MatchStatus.SCHEDULED) 12.dp else 14.dp,
                                    vertical = 8.dp
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (match.lastScorer != null && isLive) "Gol: ${match.lastScorer}" else match.stadium,
                            color = if (match.lastScorer != null && isLive) EmeraldAccent else activeTheme.textSecondaryColor,
                            fontSize = 9.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 96.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Skiper UI Kinetic Rolling Score Stepper Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                KineticScoreStepper(
                    label = match.homeTeam.shortName,
                    value = draftHome,
                    enabled = !isLocked,
                    onDecrement = {
                        if (draftHome > 0) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            draftHome--
                        }
                    },
                    onIncrement = {
                        if (draftHome < 15) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            draftHome++
                        }
                    }
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "×",
                        color = activeTheme.textSecondaryColor,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    if (!isLocked) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = CircleShape,
                            color = if (isJoker) GoldAccent.copy(alpha = 0.2f) else activeTheme.surfaceColor,
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isJoker) GoldAccent else activeTheme.borderColor
                            ),
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onToggleJoker()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Coringa 2x",
                                    tint = if (isJoker) GoldAccent else activeTheme.textSecondaryColor,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = "Coringa 2x",
                                    color = if (isJoker) GoldAccent else activeTheme.textSecondaryColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }

                KineticScoreStepper(
                    label = match.awayTeam.shortName,
                    value = draftAway,
                    enabled = !isLocked,
                    onDecrement = {
                        if (draftAway > 0) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            draftAway--
                        }
                    },
                    onIncrement = {
                        if (draftAway < 15) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            draftAway++
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(thickness = 1.dp, color = activeTheme.borderColor)
            Spacer(modifier = Modifier.height(10.dp))

            // Templens Telemetry Footer + High-Contrast Achromatic CTA
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val statusText: String
                val statusColor: Color
                val statusBg: Color
                when {
                    savedPrediction == null -> {
                        statusText = if (isLocked) "Sem palpite" else "Defina o placar"
                        statusColor = activeTheme.textSecondaryColor
                        statusBg = activeTheme.surfaceColor
                    }
                    match.status == MatchStatus.SCHEDULED -> {
                        statusText = "Salvo ${savedPrediction.homeScore}×${savedPrediction.awayScore}" +
                            if (isJoker) " ★2x" else ""
                        statusColor = if (isJoker) GoldAccent else EmeraldAccent
                        statusBg = (if (isJoker) GoldAccent else EmeraldAccent).copy(alpha = 0.14f)
                    }
                    earnedPoints >= 10 && (earnedPoints == 10 || earnedPoints == 20) -> {
                        statusText = "+$earnedPoints exato" + if (isJoker) " ★" else ""
                        statusColor = EmeraldAccent
                        statusBg = EmeraldAccent.copy(alpha = 0.16f)
                    }
                    earnedPoints >= 5 -> {
                        statusText = "+$earnedPoints pts" + if (isJoker) " ★" else ""
                        statusColor = GoldAccent
                        statusBg = GoldAccent.copy(alpha = 0.16f)
                    }
                    else -> {
                        statusText = "Salvo ${savedPrediction.homeScore}×${savedPrediction.awayScore} · 0 pts"
                        statusColor = activeTheme.textSecondaryColor
                        statusBg = activeTheme.surfaceColor
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = statusBg,
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                if (isLocked) {
                    Surface(
                        shape = CircleShape,
                        color = activeTheme.surfaceColor,
                        border = BorderStroke(1.dp, activeTheme.borderColor)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Travado",
                                tint = activeTheme.textSecondaryColor,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Travado",
                                color = activeTheme.textSecondaryColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSavePrediction(draftHome, draftAway)
                        },
                        enabled = !isAlreadySavedExact,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ctaBg,
                            contentColor = ctaFg,
                            disabledContainerColor = activeTheme.surfaceColor,
                            disabledContentColor = EmeraldAccent
                        ),
                        border = if (isAlreadySavedExact) BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.45f)) else null,
                        shape = CircleShape,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        if (isAlreadySavedExact) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = if (isAlreadySavedExact) "Salvo ✓" else "Salvar",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Expandable Real Friends' Predictions
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = activeTheme.surfaceColor,
                border = BorderStroke(1.dp, activeTheme.borderColor.copy(alpha = 0.7f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { showFriends = !showFriends }
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Groups,
                                contentDescription = null,
                                tint = activeTheme.textSecondaryColor,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Palpites dos amigos (${friendsWithPrediction.size})",
                                color = activeTheme.textSecondaryColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Icon(
                            imageVector = if (showFriends) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = activeTheme.textSecondaryColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    if (showFriends) {
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(thickness = 1.dp, color = activeTheme.borderColor.copy(alpha = 0.6f))
                        Spacer(modifier = Modifier.height(8.dp))
                        if (friendsWithPrediction.isEmpty()) {
                            Text(
                                text = "Nenhum amigo sincronizado enviou palpite para este jogo ainda. Conecte a sala na aba Grupo ou Perfil.",
                                color = activeTheme.textSecondaryColor,
                                fontSize = 11.sp
                            )
                        } else {
                            friendsWithPrediction.forEach { friend ->
                                val fp = friend.predictions[match.id]!!
                                val fPts = calculatePredictionPoints(match, fp)
                                val isExactHit = fPts == 10 || fPts == 20
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        UserAvatarBadge(
                                            name = friend.name,
                                            colorHex = friend.avatarColorHex,
                                            photoPath = friend.avatarImagePath,
                                            frameId = friend.equippedFrameId,
                                            size = 24.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = friend.name,
                                            color = activeTheme.textPrimaryColor,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        if (isExactHit) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = CircleShape,
                                                color = GoldAccent.copy(alpha = 0.2f),
                                                border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.55f))
                                            ) {
                                                Text(
                                                    text = "CRAVOU!",
                                                    color = GoldAccent,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Black,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "${fp.homeScore}×${fp.awayScore}",
                                            color = GoldAccent,
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (fPts > 0) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "+$fPts",
                                                color = EmeraldAccent,
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TeamColumn(team: TeamInfo, modifier: Modifier = Modifier) {
    val activeTheme = LocalBolaoTheme.current
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TeamCrest(
            teamName = team.name,
            shortName = team.shortName,
            logoUrl = team.logoUrl,
            colorHex = team.colorHex,
            size = 44.dp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = team.name,
            color = activeTheme.textPrimaryColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun KineticScoreStepper(
    label: String,
    value: Int,
    enabled: Boolean,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit
) {
    val activeTheme = LocalBolaoTheme.current
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = activeTheme.surfaceColor,
        border = BorderStroke(1.dp, activeTheme.borderColor.copy(alpha = if (enabled) 1f else 0.45f))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
            IconButton(
                onClick = onDecrement,
                enabled = enabled,
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Diminuir $label",
                    tint = if (enabled) activeTheme.textSecondaryColor else activeTheme.textSecondaryColor.copy(alpha = 0.35f),
                    modifier = Modifier.size(16.dp)
                )
            }
            AnimatedContent(
                targetState = value,
                transitionSpec = {
                    val direction = if (targetState > initialState) 1 else -1
                    (slideInVertically { height -> direction * height / 2 } + fadeIn()) togetherWith
                        (slideOutVertically { height -> -direction * height / 2 } + fadeOut()) using
                        SizeTransform(clip = false)
                },
                label = "score_roll_$label"
            ) { target ->
                Text(
                    text = "$target",
                    color = if (enabled) activeTheme.textPrimaryColor else activeTheme.textSecondaryColor,
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.width(28.dp),
                    textAlign = TextAlign.Center
                )
            }
            IconButton(
                onClick = onIncrement,
                enabled = enabled,
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Aumentar $label",
                    tint = if (enabled) EmeraldAccent else activeTheme.textSecondaryColor.copy(alpha = 0.35f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
