package com.bolaodosvei.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bolaodosvei.app.model.League
import com.bolaodosvei.app.model.NotificationSettings
import com.bolaodosvei.app.model.RoomState
import com.bolaodosvei.app.model.UserAccount
import com.bolaodosvei.app.model.UserStats
import com.bolaodosvei.app.ui.components.UserAvatarBadge
import com.bolaodosvei.app.ui.theme.*

@Composable
fun ProfileAndNotificationsScreen(
    user: UserAccount,
    totalPoints: Int,
    predictionsCount: Int,
    roomState: RoomState,
    roomSyncStatus: String,
    notificationSettings: NotificationSettings,
    equippedFrameAccentHex: Long? = null,
    onJoinRoom: (String) -> Unit,
    onCreateRoom: () -> Unit,
    onToggleNotification: (League, Boolean) -> Unit,
    onUpdateProfilePhoto: (Uri) -> Unit,
    onRemoveProfilePhoto: () -> Unit,
    onLogout: () -> Unit,
    userStats: UserStats = UserStats()
) {
    val activeTheme = LocalBolaoTheme.current
    val ctaBg = if (activeTheme.isLightTheme) Color(0xFF09090B) else Color.White
    val ctaFg = if (activeTheme.isLightTheme) Color.White else Color(0xFF09090B)

    val context = LocalContext.current
    var roomCodeInput by remember(roomState.roomCode) { mutableStateOf(roomState.roomCode) }
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let(onUpdateProfilePhoto)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Double-Nested Bento User Profile Card
        item(key = "profile_card", contentType = "profile_card") {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = activeTheme.cardColor,
                border = BorderStroke(1.dp, activeTheme.borderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = activeTheme.surfaceColor,
                        border = BorderStroke(1.dp, activeTheme.borderColor.copy(alpha = 0.7f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            UserAvatarBadge(
                                name = user.name,
                                colorHex = user.avatarColorHex,
                                photoPath = user.avatarImagePath,
                                frameAccentHex = equippedFrameAccentHex,
                                size = 58.dp
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = user.name,
                                    color = activeTheme.textPrimaryColor,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = user.email,
                                    color = activeTheme.textSecondaryColor,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Surface(
                                        color = GoldAccent.copy(alpha = 0.14f),
                                        shape = CircleShape,
                                        border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.35f))
                                    ) {
                                        Text(
                                            text = "$totalPoints pts",
                                            color = GoldAccent,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                                        )
                                    }
                                    Surface(
                                        color = EmeraldAccent.copy(alpha = 0.14f),
                                        shape = CircleShape,
                                        border = BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.35f))
                                    ) {
                                        Text(
                                            text = "$predictionsCount palpites",
                                            color = EmeraldAccent,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Templens 4-Stat Telemetry Bento Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TelemetryStatTile(
                            label = "Cravadas",
                            value = "${userStats.exactHits}",
                            accent = GoldAccent,
                            modifier = Modifier.weight(1f)
                        )
                        TelemetryStatTile(
                            label = "Acerto %",
                            value = "${userStats.accuracyPercent}%",
                            accent = EmeraldAccent,
                            modifier = Modifier.weight(1f)
                        )
                        TelemetryStatTile(
                            label = "Sequência 🔥",
                            value = "${userStats.currentStreak}",
                            accent = Color(0xFFF97316),
                            modifier = Modifier.weight(1f)
                        )
                        TelemetryStatTile(
                            label = "Pontos",
                            value = "$totalPoints",
                            accent = activeTheme.textPrimaryColor,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { photoPickerLauncher.launch("image/*") },
                            border = BorderStroke(1.dp, activeTheme.borderColor),
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Foto da Galeria", color = activeTheme.textPrimaryColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        if (!user.avatarImagePath.isNullOrBlank()) {
                            OutlinedButton(
                                onClick = onRemoveProfilePhoto,
                                border = BorderStroke(1.dp, LiveRed.copy(alpha = 0.6f)),
                                shape = CircleShape,
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text("Remover", color = LiveRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Double-Nested Bento Room Code Card
        item(key = "room_card", contentType = "room_card") {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = activeTheme.cardColor,
                border = BorderStroke(1.dp, activeTheme.borderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MeetingRoom,
                            contentDescription = null,
                            tint = GoldAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Código da Sala",
                            color = activeTheme.textPrimaryColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Seus amigos só precisam digitar este código para entrar no mesmo bolão e ver os palpites de todos.",
                        color = activeTheme.textSecondaryColor,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = activeTheme.surfaceColor,
                        border = BorderStroke(1.dp, activeTheme.borderColor.copy(alpha = 0.7f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            OutlinedTextField(
                                value = roomCodeInput,
                                onValueChange = { roomCodeInput = it.uppercase() },
                                label = { Text("Código da Sala (ex: VEI-4829)") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = roomSyncStatus,
                                color = if (roomState.isInRoom) EmeraldAccent else activeTheme.textSecondaryColor,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onJoinRoom(roomCodeInput) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ctaBg,
                                        contentColor = ctaFg
                                    ),
                                    shape = CircleShape,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Entrar na Sala", fontWeight = FontWeight.ExtraBold)
                                }
                                OutlinedButton(
                                    onClick = onCreateRoom,
                                    border = BorderStroke(1.dp, activeTheme.borderColor),
                                    shape = CircleShape
                                ) {
                                    Text("Criar", color = activeTheme.textPrimaryColor, fontWeight = FontWeight.Bold)
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
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Double-Nested Bento Per-league Notification Settings
        item(key = "notif_card", contentType = "notif_card") {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = activeTheme.cardColor,
                border = BorderStroke(1.dp, activeTheme.borderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = EmeraldAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Notificações por Liga",
                            color = activeTheme.textPrimaryColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Escolha de quais ligas deseja receber alertas quando os jogos começarem ou sair gol.",
                        color = activeTheme.textSecondaryColor,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = activeTheme.surfaceColor,
                        border = BorderStroke(1.dp, activeTheme.borderColor.copy(alpha = 0.7f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                            League.entries.forEachIndexed { idx, league ->
                                val enabled = notificationSettings.isEnabled(league)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = league.displayName,
                                            color = activeTheme.textPrimaryColor,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (enabled) "Ativada · Alerta de início de jogo" else "Silenciada",
                                            color = if (enabled) EmeraldAccent else activeTheme.textSecondaryColor,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Switch(
                                        checked = enabled,
                                        onCheckedChange = { onToggleNotification(league, it) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = PitchDark,
                                            checkedTrackColor = EmeraldAccent
                                        )
                                    )
                                }
                                if (idx < League.entries.lastIndex) {
                                    HorizontalDivider(thickness = 1.dp, color = activeTheme.borderColor.copy(alpha = 0.6f))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Logout Pill CTA
        item(key = "logout_btn", contentType = "logout_btn") {
            OutlinedButton(
                onClick = onLogout,
                border = BorderStroke(1.dp, LiveRed.copy(alpha = 0.65f)),
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = LiveRed)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sair da Conta", color = LiveRed, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun TelemetryStatTile(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val activeTheme = LocalBolaoTheme.current
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = activeTheme.surfaceColor,
        border = BorderStroke(1.dp, activeTheme.borderColor.copy(alpha = 0.7f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                color = accent,
                fontSize = 15.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                color = activeTheme.textSecondaryColor,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}
