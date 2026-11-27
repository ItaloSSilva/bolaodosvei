package com.bolaodosvei.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bolaodosvei.app.model.ChatMessage
import com.bolaodosvei.app.model.RoomState
import com.bolaodosvei.app.ui.components.UserAvatarBadge
import com.bolaodosvei.app.ui.theme.EmeraldAccent
import com.bolaodosvei.app.ui.theme.GoldAccent
import com.bolaodosvei.app.ui.theme.LocalBolaoTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val quickReactions = listOf(
    "⚽ GOLAÇO!",
    "🤡 ZIKOU!",
    "🔥 CRAVEI!",
    "👀 CHAMA O VAR!"
)

@Composable
fun ChatScreen(
    messages: List<ChatMessage>,
    roomState: RoomState,
    onSendMessage: (String) -> Unit,
    onRefreshChat: () -> Unit,
    onSendQuickReaction: (String) -> Unit = onSendMessage
) {
    val activeTheme = LocalBolaoTheme.current
    val haptic = LocalHapticFeedback.current
    val ctaBg = if (activeTheme.isLightTheme) Color(0xFF09090B) else Color.White
    val ctaFg = if (activeTheme.isLightTheme) Color.White else Color(0xFF09090B)

    var draft by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 92.dp)
    ) {
        // Double-Nested Bento Chat Header
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = activeTheme.cardColor,
            border = BorderStroke(1.dp, activeTheme.borderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = activeTheme.surfaceColor,
                border = BorderStroke(1.dp, activeTheme.borderColor.copy(alpha = 0.7f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (roomState.isInRoom) "Chat · ${roomState.roomCode}" else "Chat Geral do Bolão",
                            color = activeTheme.textPrimaryColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = if (roomState.isInRoom) {
                                "Resenha em tempo real com os amigos da sala"
                            } else {
                                "Entre em uma sala para conversar com seus amigos"
                            },
                            color = activeTheme.textSecondaryColor,
                            fontSize = 11.sp
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = activeTheme.cardColor,
                        border = BorderStroke(1.dp, activeTheme.borderColor),
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .clickable { onRefreshChat() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Atualizar chat",
                                tint = activeTheme.textPrimaryColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Messages List
        if (messages.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = activeTheme.cardColor,
                    border = BorderStroke(1.dp, activeTheme.borderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = activeTheme.surfaceColor,
                        border = BorderStroke(1.dp, activeTheme.borderColor.copy(alpha = 0.7f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChatBubbleOutline,
                                contentDescription = null,
                                tint = activeTheme.accentColor,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Nenhuma mensagem ainda",
                                color = activeTheme.textPrimaryColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Mande a primeira corneta ou palpite para começar a resenha!",
                                color = activeTheme.textSecondaryColor,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(
                    items = messages,
                    key = { it.id },
                    contentType = { "chat_bubble" }
                ) { msg ->
                    ChatBubbleItem(message = msg)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 1-Tap Quick Reaction Bar
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(quickReactions) { reaction ->
                Surface(
                    shape = CircleShape,
                    color = activeTheme.cardColor,
                    border = BorderStroke(1.dp, activeTheme.borderColor),
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSendQuickReaction(reaction)
                        }
                ) {
                    Text(
                        text = reaction,
                        color = activeTheme.textPrimaryColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Bottom Input Bar
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = activeTheme.cardColor,
            border = BorderStroke(1.dp, activeTheme.borderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { if (it.length <= 500) draft = it },
                    placeholder = { Text("Escreva sua mensagem...", fontSize = 13.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                FilledIconButton(
                    onClick = {
                        val trimmed = draft.trim()
                        if (trimmed.isNotEmpty()) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSendMessage(trimmed)
                            draft = ""
                        }
                    },
                    enabled = draft.isNotBlank(),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = ctaBg,
                        contentColor = ctaFg,
                        disabledContainerColor = activeTheme.surfaceColor,
                        disabledContentColor = activeTheme.textSecondaryColor
                    ),
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Enviar mensagem",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatBubbleItem(message: ChatMessage) {
    val activeTheme = LocalBolaoTheme.current
    val hasFlair = !message.senderFlair.isNullOrBlank()
    val timeText = remember(message.timestampMillis) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestampMillis))
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isCurrentUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!message.isCurrentUser) {
            UserAvatarBadge(
                name = message.senderName,
                colorHex = message.senderAvatarColorHex,
                size = 32.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (message.isCurrentUser) 18.dp else 6.dp,
                bottomEnd = if (message.isCurrentUser) 6.dp else 18.dp
            ),
            color = when {
                message.isCurrentUser -> activeTheme.accentColor.copy(alpha = if (activeTheme.isLightTheme) 0.14f else 0.18f)
                else -> activeTheme.cardColor
            },
            border = BorderStroke(
                width = if (hasFlair) 1.5.dp else 1.dp,
                color = when {
                    hasFlair -> GoldAccent
                    message.isCurrentUser -> activeTheme.accentColor.copy(alpha = 0.5f)
                    else -> activeTheme.borderColor
                }
            ),
            modifier = Modifier.widthIn(max = 285.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = message.senderName,
                        color = activeTheme.textPrimaryColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    if (!message.senderTitle.isNullOrBlank()) {
                        Surface(
                            color = EmeraldAccent.copy(alpha = 0.16f),
                            shape = CircleShape
                        ) {
                            Text(
                                text = message.senderTitle,
                                color = if (activeTheme.isLightTheme) Color(0xFF047857) else EmeraldAccent,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                        }
                    }
                    if (!message.senderFlair.isNullOrBlank()) {
                        Surface(
                            color = GoldAccent.copy(alpha = 0.2f),
                            shape = CircleShape
                        ) {
                            Text(
                                text = message.senderFlair,
                                color = if (activeTheme.isLightTheme) Color(0xFFB45309) else GoldAccent,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message.text,
                    color = activeTheme.textPrimaryColor,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = timeText,
                    color = activeTheme.textSecondaryColor,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }

        if (message.isCurrentUser) {
            Spacer(modifier = Modifier.width(8.dp))
            UserAvatarBadge(
                name = message.senderName,
                colorHex = message.senderAvatarColorHex,
                size = 32.dp
            )
        }
    }
}
