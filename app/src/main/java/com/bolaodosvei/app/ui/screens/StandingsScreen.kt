package com.bolaodosvei.app.ui.screens

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
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bolaodosvei.app.model.League
import com.bolaodosvei.app.model.NotificationSettings
import com.bolaodosvei.app.model.StandingRow
import com.bolaodosvei.app.ui.components.TeamCrest
import com.bolaodosvei.app.ui.theme.*

@Composable
fun StandingsScreen(
    selectedLeague: League,
    standings: Map<League, List<StandingRow>>,
    notificationSettings: NotificationSettings,
    onSelectLeague: (League) -> Unit,
    onToggleNotification: (League, Boolean) -> Unit
) {
    val activeTheme = LocalBolaoTheme.current
    val rows = standings[selectedLeague].orEmpty()
    val notifEnabled = notificationSettings.isEnabled(selectedLeague)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // OriginKit League Selector Pills (all 7 leagues)
        item(key = "league_pills", contentType = "league_pills") {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = League.entries,
                    key = { it.id },
                    contentType = { "league_pill" }
                ) { league ->
                    val selected = league == selectedLeague
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
                    Surface(
                        shape = CircleShape,
                        color = pillBg,
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (selected) pillBg else activeTheme.borderColor
                        ),
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onSelectLeague(league) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(Color(league.accentColorHex))
                            )
                            Text(
                                text = league.shortName,
                                color = pillFg,
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // Double-Nested Bento Per-league Notification Control
        item(key = "notif_control", contentType = "notif_control") {
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
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (notifEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                    contentDescription = null,
                                    tint = if (notifEnabled) EmeraldAccent else activeTheme.textSecondaryColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Alertas · ${selectedLeague.displayName}",
                                        color = activeTheme.textPrimaryColor,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (notifEnabled) "Avisar quando jogo desta liga começar" else "Notificações desta liga desligadas",
                                        color = activeTheme.textSecondaryColor,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            Switch(
                                checked = notifEnabled,
                                onCheckedChange = { onToggleNotification(selectedLeague, it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = PitchDark,
                                    checkedTrackColor = EmeraldAccent
                                )
                            )
                        }
                    }
                }
            }
        }

        if (rows.isEmpty()) {
            item(key = "empty_standings", contentType = "empty_standings") {
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
                                Icon(
                                    imageVector = Icons.Default.Leaderboard,
                                    contentDescription = null,
                                    tint = activeTheme.textSecondaryColor,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Tabela indisponível no momento",
                                    color = activeTheme.textPrimaryColor,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Os dados de classificação da ${selectedLeague.displayName} serão atualizados em breve.",
                                    color = activeTheme.textSecondaryColor,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Templens Data Table inside Double-Nested Bento Shell
            item(key = "standings_table", contentType = "standings_table") {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = activeTheme.cardColor,
                    border = BorderStroke(1.dp, activeTheme.borderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        // Table Header
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = activeTheme.surfaceColor,
                            border = BorderStroke(1.dp, activeTheme.borderColor.copy(alpha = 0.7f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "#",
                                    color = activeTheme.textSecondaryColor,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(26.dp)
                                )
                                Text(
                                    text = "CLUBE",
                                    color = activeTheme.textSecondaryColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.6.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                TableHeaderStat("J", Modifier.width(24.dp))
                                TableHeaderStat("V", Modifier.width(24.dp))
                                TableHeaderStat("E", Modifier.width(24.dp))
                                TableHeaderStat("D", Modifier.width(24.dp))
                                TableHeaderStat("SG", Modifier.width(32.dp))
                                Text(
                                    text = "PTS",
                                    color = GoldAccent,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.ExtraBold,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.width(34.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        rows.forEachIndexed { index, row ->
                            val zoneColor = when {
                                row.position <= 4 -> EmeraldAccent
                                row.position <= 6 -> Color(0xFF38BDF8)
                                row.position >= rows.size - 3 && rows.size >= 10 -> LiveRed
                                else -> Color.Transparent
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(22.dp)
                                        .background(zoneColor, RoundedCornerShape(2.dp))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${row.position}",
                                    color = if (row.position <= 4) EmeraldAccent else activeTheme.textPrimaryColor,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(20.dp)
                                )
                                TeamCrest(
                                    teamName = row.teamName,
                                    shortName = row.shortName,
                                    logoUrl = row.logoUrl,
                                    colorHex = row.colorHex,
                                    size = 22.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = row.teamName,
                                    color = activeTheme.textPrimaryColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                TableRowStat("${row.played}", activeTheme.textSecondaryColor, Modifier.width(24.dp))
                                TableRowStat("${row.won}", activeTheme.textPrimaryColor, Modifier.width(24.dp))
                                TableRowStat("${row.drawn}", activeTheme.textSecondaryColor, Modifier.width(24.dp))
                                TableRowStat("${row.lost}", activeTheme.textSecondaryColor, Modifier.width(24.dp))
                                TableRowStat(
                                    text = if (row.goalDifference > 0) "+${row.goalDifference}" else "${row.goalDifference}",
                                    color = if (row.goalDifference >= 0) EmeraldAccent else LiveRed,
                                    modifier = Modifier.width(32.dp)
                                )
                                Text(
                                    text = "${row.points}",
                                    color = GoldAccent,
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.ExtraBold,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.width(34.dp)
                                )
                            }

                            if (index < rows.lastIndex) {
                                HorizontalDivider(
                                    thickness = 1.dp,
                                    color = activeTheme.borderColor.copy(alpha = 0.45f)
                                )
                            }
                        }
                    }
                }
            }

            // Zone Legend
            item(key = "zone_legend", contentType = "zone_legend") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    LegendDot(EmeraldAccent, "G4 / Classificação")
                    LegendDot(Color(0xFF38BDF8), "Continental")
                    LegendDot(LiveRed, "Zona de Rebaixamento")
                }
            }
        }
    }
}

@Composable
private fun TableHeaderStat(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = LocalBolaoTheme.current.textSecondaryColor,
        fontSize = 10.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = modifier
    )
}

@Composable
private fun TableRowStat(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = color,
        fontSize = 11.sp,
        fontFamily = FontFamily.Monospace,
        textAlign = TextAlign.Center,
        modifier = modifier
    )
}

@Composable
private fun LegendDot(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(text = text, color = LocalBolaoTheme.current.textSecondaryColor, fontSize = 10.sp)
    }
}
