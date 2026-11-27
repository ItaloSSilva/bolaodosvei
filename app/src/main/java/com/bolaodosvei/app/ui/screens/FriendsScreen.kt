package com.bolaodosvei.app.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bolaodosvei.app.model.*
import com.bolaodosvei.app.ui.components.TeamCrest
import com.bolaodosvei.app.ui.components.UserAvatarBadge
import com.bolaodosvei.app.ui.theme.*

@Composable
fun FriendsScreen(
    participants: List<FriendBolao>,
    matches: List<Match>,
    roomState: RoomState,
    roomSyncStatus: String,
    onJoinRoom: (String) -> Unit,
    onCreateRoom: () -> Unit,
    onLeaveRoom: () -> Unit,
    onSyncNow: () -> Unit
) {
    val activeTheme = LocalBolaoTheme.current
    val ctaBg = if (activeTheme.isLightTheme) Color(0xFF09090B) else Color.White
    val ctaFg = if (activeTheme.isLightTheme) Color.White else Color(0xFF09090B)

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var codeInput by remember(roomState.roomCode) { mutableStateOf(roomState.roomCode) }

    val ranked = remember(participants, matches) {
        participants.map { person ->
            val livePts = matches.sumOf { m -> calculatePredictionPoints(m, person.predictions[m.id]) }
            person to livePts
        }.sortedByDescending { it.second }
    }

    var expandedId by remember(ranked.size) { mutableStateOf(ranked.firstOrNull()?.first?.id) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Double-Nested Bento Room Code Card
        item(key = "room_header", contentType = "room_header") {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = activeTheme.cardColor,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (roomState.isInRoom) EmeraldAccent.copy(alpha = 0.5f) else activeTheme.borderColor
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.MeetingRoom,
                                contentDescription = null,
                                tint = if (roomState.isInRoom) EmeraldAccent else GoldAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (roomState.isInRoom) "Sala ${roomState.roomCode}" else "Sala do Bolão",
                                color = activeTheme.textPrimaryColor,
                                fontSize = 15.sp,
                                fontFamily = if (roomState.isInRoom) FontFamily.Monospace else FontFamily.Default,
                                fontWeight = FontWeight.Black
                            )
                        }
                        if (roomState.isInRoom) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = activeTheme.surfaceColor,
                                    border = BorderStroke(1.dp, activeTheme.borderColor),
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .clickable {
                                            val shareText = "Bora jogar no Bolão dos Véi! Baixe o app e entre na nossa sala usando o código: ${roomState.roomCode}"
                                            val intent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, shareText)
                                            }
                                            context.startActivity(Intent.createChooser(intent, "Compartilhar código da sala"))
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "Compartilhar código",
                                            tint = GoldAccent,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                                Surface(
                                    shape = CircleShape,
                                    color = activeTheme.surfaceColor,
                                    border = BorderStroke(1.dp, activeTheme.borderColor),
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .clickable { onSyncNow() }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Atualizar sala",
                                            tint = EmeraldAccent,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = roomSyncStatus,
                        color = activeTheme.textSecondaryColor,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Inset Room Controls Stage
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = activeTheme.surfaceColor,
                        border = BorderStroke(1.dp, activeTheme.borderColor.copy(alpha = 0.7f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = codeInput,
                                    onValueChange = { codeInput = it.uppercase() },
                                    placeholder = { Text("Ex: VEI-4829", fontSize = 13.sp, fontFamily = FontFamily.Monospace) },
                                    label = { Text("Código da Sala", fontSize = 11.sp) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    trailingIcon = {
                                        IconButton(
                                            onClick = {
                                                val pasted = clipboardManager.getText()?.text?.trim()?.uppercase()
                                                if (!pasted.isNullOrBlank()) {
                                                    codeInput = pasted
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentPaste,
                                                contentDescription = "Colar código",
                                                tint = activeTheme.textSecondaryColor,
                                                modifier = Modifier.size(17.dp)
                                            )
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = { onJoinRoom(codeInput) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ctaBg,
                                        contentColor = ctaFg
                                    ),
                                    shape = CircleShape,
                                    contentPadding = PaddingValues(horizontal = 18.dp),
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Text("Entrar", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onCreateRoom,
                                    border = BorderStroke(1.dp, activeTheme.borderColor),
                                    shape = CircleShape,
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AddCircleOutline,
                                        contentDescription = null,
                                        tint = GoldAccent,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Criar Nova Sala", color = activeTheme.textPrimaryColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                if (roomState.isInRoom) {
                                    OutlinedButton(
                                        onClick = onLeaveRoom,
                                        border = BorderStroke(1.dp, LiveRed.copy(alpha = 0.55f)),
                                        shape = CircleShape,
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Text("Sair", color = LiveRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            if (roomState.isInRoom) {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = {
                                        val shareText = "Bora jogar no Bolão dos Véi! Entre na nossa sala usando o código: ${roomState.roomCode}"
                                        val waIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, shareText)
                                            setPackage("com.whatsapp")
                                        }
                                        try {
                                            context.startActivity(waIntent)
                                        } catch (_: Exception) {
                                            val fallback = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, shareText)
                                            }
                                            context.startActivity(Intent.createChooser(fallback, "Convidar no WhatsApp"))
                                        }
                                    },
                                    border = BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.55f)),
                                    shape = CircleShape,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Convidar no WhatsApp (${roomState.roomCode})",
                                        color = EmeraldAccent,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Ranking Header
        item(key = "ranking_title", contentType = "ranking_title") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp, start = 4.dp)
            ) {
                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(17.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Ranking Real (${ranked.size} participante${if (ranked.size == 1) "" else "s"})",
                    color = activeTheme.textPrimaryColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (ranked.size <= 1) {
            item(key = "empty_friends", contentType = "empty_friends") {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = activeTheme.cardColor,
                    border = BorderStroke(1.dp, activeTheme.borderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Envie o Código da Sala para seus amigos! Assim que eles digitarem o mesmo código no app, os palpites de todos aparecerão aqui automaticamente.",
                        color = activeTheme.textSecondaryColor,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }

        itemsIndexed(
            items = ranked,
            key = { _, pair -> pair.first.id },
            contentType = { _, _ -> "friend_card" }
        ) { index, (person, totalPoints) ->
            val isExpanded = expandedId == person.id

            Surface(
                shape = RoundedCornerShape(22.dp),
                color = activeTheme.cardColor,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (person.isCurrentUser) EmeraldAccent.copy(alpha = 0.65f) else activeTheme.borderColor
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .clickable { expandedId = if (isExpanded) null else person.id }
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "${index + 1}º",
                                color = if (index == 0) GoldAccent else activeTheme.textSecondaryColor,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.width(28.dp)
                            )
                            UserAvatarBadge(
                                name = person.name,
                                colorHex = person.avatarColorHex,
                                photoPath = person.avatarImagePath,
                                frameId = person.equippedFrameId,
                                size = 38.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = person.name,
                                        color = activeTheme.textPrimaryColor,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (!person.equippedTitle.isNullOrBlank()) {
                                        Surface(
                                            shape = CircleShape,
                                            color = EmeraldAccent.copy(alpha = 0.16f),
                                            border = BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.4f))
                                        ) {
                                            Text(
                                                text = person.equippedTitle,
                                                color = EmeraldAccent,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = person.subtitle,
                                    color = activeTheme.textSecondaryColor,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "$totalPoints pts",
                                    color = GoldAccent,
                                    fontSize = 15.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "${person.predictions.size} palpites",
                                    color = activeTheme.textSecondaryColor,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = activeTheme.textSecondaryColor
                            )
                        }
                    }

                    if (isExpanded) {
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(thickness = 1.dp, color = activeTheme.borderColor)
                        Spacer(modifier = Modifier.height(10.dp))

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = activeTheme.surfaceColor,
                            border = BorderStroke(1.dp, activeTheme.borderColor.copy(alpha = 0.7f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                if (person.predictions.isEmpty()) {
                                    Text(
                                        text = "Nenhum palpite salvo ainda.",
                                        color = activeTheme.textSecondaryColor,
                                        fontSize = 12.sp
                                    )
                                } else {
                                    matches.forEach { match ->
                                        val pred = person.predictions[match.id]
                                        if (pred != null) {
                                            val pts = calculatePredictionPoints(match, pred)
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 5.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    TeamCrest(match.homeTeam.name, match.homeTeam.shortName, match.homeTeam.logoUrl, match.homeTeam.colorHex, 18.dp)
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    TeamCrest(match.awayTeam.name, match.awayTeam.shortName, match.awayTeam.logoUrl, match.awayTeam.colorHex, 18.dp)
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Column {
                                                        Text(
                                                            text = "${match.homeTeam.shortName} × ${match.awayTeam.shortName}",
                                                            color = activeTheme.textPrimaryColor,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                        val liveText = if (match.status == MatchStatus.SCHEDULED) {
                                                            "Começa às ${match.kickoffTime}"
                                                        } else {
                                                            "Real: ${match.homeScore}×${match.awayScore}"
                                                        }
                                                        Text(
                                                            text = liveText,
                                                            color = activeTheme.textSecondaryColor,
                                                            fontSize = 10.sp,
                                                            fontFamily = FontFamily.Monospace
                                                        )
                                                    }
                                                }

                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Surface(
                                                        color = activeTheme.cardColor,
                                                        shape = CircleShape,
                                                        border = BorderStroke(1.dp, activeTheme.borderColor)
                                                    ) {
                                                        Text(
                                                            text = "${pred.homeScore}×${pred.awayScore}",
                                                            color = GoldAccent,
                                                            fontSize = 12.sp,
                                                            fontFamily = FontFamily.Monospace,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                                        )
                                                    }
                                                    if (pts > 0) {
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            text = "+$pts",
                                                            color = EmeraldAccent,
                                                            fontSize = 12.sp,
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
        }
    }
}
