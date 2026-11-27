package com.bolaodosvei.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bolaodosvei.app.model.League
import com.bolaodosvei.app.model.ShopCatalog
import com.bolaodosvei.app.ui.components.BolaoLogoBadge
import com.bolaodosvei.app.ui.components.ClubThemeBackdrop
import com.bolaodosvei.app.ui.components.GoalAnimationOverlay
import com.bolaodosvei.app.ui.components.LiveTickerBar
import com.bolaodosvei.app.ui.components.SplashIntroOverlay
import com.bolaodosvei.app.ui.screens.*
import com.bolaodosvei.app.ui.theme.*
import com.bolaodosvei.app.viewmodel.BolaoViewModel

private enum class MainTab(val label: String, val icon: ImageVector) {
    BOLAO("Bolão", Icons.Default.SportsSoccer),
    TABELAS("Tabelas", Icons.Default.EmojiEvents),
    CHAT("Chat", Icons.AutoMirrored.Filled.Chat),
    LOJA("Loja", Icons.Default.ShoppingBag),
    PERFIL("Grupo", Icons.Default.Groups)
}

private enum class GroupSubTab(val label: String) {
    AMIGOS("Amigos & Sala"),
    PERFIL("Meu Perfil")
}

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Permission result handled gracefully by MatchNotificationManager */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            BolaoAppRoot()
        }
    }
}

@Composable
private fun BolaoAppRoot(viewModel: BolaoViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val activeTheme = remember(state.selectedThemeId) {
        ClubThemesCatalog.findById(state.selectedThemeId)
    }

    BolaoDosVeiTheme(themePreset = activeTheme) {
        var currentTab by rememberSaveable { mutableStateOf(MainTab.BOLAO) }
        var groupSubTab by rememberSaveable { mutableStateOf(GroupSubTab.AMIGOS) }
        var showSettings by rememberSaveable { mutableStateOf(false) }
        var showSplashIntro by rememberSaveable { mutableStateOf(true) }
        var bolaoLeagueFilter by rememberSaveable { mutableStateOf<League?>(null) }
        val onAccentForeground = if (activeTheme.accentColor.luminance() > 0.45f) PitchDark else Color.White
        val equippedFrameAccentHex = remember(state.shopState.equippedFrameId) {
            ShopCatalog.findById(state.shopState.equippedFrameId)?.accentHex
        }

        val user = state.currentUser

        Box(modifier = Modifier.fillMaxSize()) {
            if (user == null) {
                LoginScreen(
                    onLogin = { email, pass -> viewModel.login(email, pass) },
                    onRegister = { name, email, pass, colorHex, league ->
                        viewModel.register(name, email, pass, colorHex, league)
                    }
                )
            } else if (showSettings) {
                SettingsScreen(
                    user = user,
                    selectedThemeId = state.selectedThemeId,
                    equippedFrameAccentHex = equippedFrameAccentHex,
                    onSelectTheme = { preset -> viewModel.selectTheme(preset.id, preset.name) },
                    onUpdateProfile = { name, colorHex, league ->
                        viewModel.updateProfile(name, colorHex, league)
                    },
                    onUpdateProfilePhoto = { uri -> viewModel.updateProfilePhoto(uri) },
                    onRemoveProfilePhoto = { viewModel.removeProfilePhoto() },
                    onBack = { showSettings = false }
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    LaunchedEffect(currentTab) {
                        viewModel.setChatScreenVisible(currentTab == MainTab.CHAT)
                    }
            // Unique Club Backdrop: custom club pattern + vector mascot + giant crest watermark
            ClubThemeBackdrop(theme = activeTheme, showLargeMascot = true)

            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    Column(modifier = Modifier.statusBarsPadding()) {
                        // Minimal OriginKit Top Bar
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(activeTheme.surfaceColor.copy(alpha = 0.94f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    BolaoLogoBadge(size = 36.dp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Bolão dos Véi",
                                            color = activeTheme.textPrimaryColor,
                                            fontFamily = BolaoDisplayFontFamily,
                                            fontSize = 18.sp,
                                            letterSpacing = (-0.5).sp
                                        )
                                        Text(
                                            text = if (activeTheme.league != null) "${user.name} • ${activeTheme.name}" else user.name,
                                            color = activeTheme.textSecondaryColor,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = activeTheme.cardColor,
                                        border = BorderStroke(1.dp, activeTheme.borderColor),
                                        shape = CircleShape
                                    ) {
                                        Text(
                                            text = "${state.shopState.availablePoints} pts",
                                            color = activeTheme.textPrimaryColor,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = CircleShape,
                                        color = activeTheme.cardColor,
                                        border = BorderStroke(1.dp, activeTheme.borderColor),
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        IconButton(
                                            onClick = { showSettings = true },
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Settings,
                                                contentDescription = "Configurações e Temas",
                                                tint = activeTheme.textPrimaryColor,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            HorizontalDivider(
                                thickness = 1.dp,
                                color = activeTheme.borderColor.copy(alpha = 0.7f)
                            )
                        }

                        // Top Live Bar showing ALL games happening today in real time
                        LiveTickerBar(
                            matches = state.matches,
                            lastUpdated = state.lastUpdated,
                            isRefreshing = state.isRefreshing,
                            onRefresh = { viewModel.refreshLiveData() },
                            onMatchClick = { match ->
                                viewModel.selectLeague(match.league)
                                bolaoLeagueFilter = match.league
                                currentTab = MainTab.BOLAO
                            }
                        )

                        AnimatedVisibility(
                            visible = state.inAppBanner != null,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            state.inAppBanner?.let { banner ->
                                Surface(
                                    color = activeTheme.accentColor,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.dismissBanner() }
                                ) {
                                    Text(
                                        text = banner,
                                        color = onAccentForeground,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (currentTab) {
                        MainTab.BOLAO -> MatchesAndPredictionsScreen(
                            matches = state.matches,
                            upcomingMatches = state.upcomingMatches,
                            userPredictions = state.userPredictions,
                            participants = state.participants,
                            selectedLeagueFilter = bolaoLeagueFilter,
                            onSelectLeagueFilter = { bolaoLeagueFilter = it },
                            isRefreshing = state.isRefreshing,
                            onRefresh = { viewModel.refreshLiveData() },
                            onSavePrediction = { id, h, a -> viewModel.savePrediction(id, h, a) },
                            onToggleJoker = { matchId -> viewModel.toggleJoker(matchId) }
                        )
                        MainTab.TABELAS -> StandingsScreen(
                            selectedLeague = state.selectedLeague,
                            standings = state.standings,
                            notificationSettings = state.notificationSettings,
                            onSelectLeague = { viewModel.selectLeague(it) },
                            onToggleNotification = { league, enabled ->
                                viewModel.toggleLeagueNotification(league, enabled)
                            }
                        )
                        MainTab.CHAT -> ChatScreen(
                            messages = state.chatMessages,
                            roomState = state.roomState,
                            onSendMessage = { text -> viewModel.sendChatMessage(text) },
                            onSendQuickReaction = { reaction -> viewModel.sendQuickReaction(reaction) },
                            onRefreshChat = { viewModel.refreshChat() }
                        )
                        MainTab.LOJA -> ShopScreen(
                            user = user,
                            shopState = state.shopState,
                            dailyChallenges = state.dailyChallenges,
                            onClaimChallenge = { challengeId -> viewModel.claimDailyChallenge(challengeId) },
                            onBuyItem = { itemId -> viewModel.buyShopItem(itemId) },
                            onEquipItem = { itemId -> viewModel.equipShopItem(itemId) }
                        )
                        MainTab.PERFIL -> Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                GroupSubTab.entries.forEach { subTab ->
                                    val selected = subTab == groupSubTab
                                    Button(
                                        onClick = { groupSubTab = subTab },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (selected) activeTheme.accentColor else activeTheme.cardColor,
                                            contentColor = if (selected) onAccentForeground else activeTheme.textSecondaryColor
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(subTab.label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            when (groupSubTab) {
                                GroupSubTab.AMIGOS -> FriendsScreen(
                                    participants = state.participants,
                                    matches = state.matches,
                                    roomState = state.roomState,
                                    roomSyncStatus = state.roomSyncStatus,
                                    onJoinRoom = { code -> viewModel.joinRoom(code) },
                                    onCreateRoom = { viewModel.createNewRoom() },
                                    onLeaveRoom = { viewModel.leaveRoom() },
                                    onSyncNow = { viewModel.syncRoomNow() }
                                )
                                GroupSubTab.PERFIL -> ProfileAndNotificationsScreen(
                                    user = user,
                                    totalPoints = state.userTotalPoints,
                                    predictionsCount = state.userPredictions.size,
                                    userStats = state.userStats,
                                    roomState = state.roomState,
                                    roomSyncStatus = state.roomSyncStatus,
                                    notificationSettings = state.notificationSettings,
                                    equippedFrameAccentHex = equippedFrameAccentHex,
                                    onJoinRoom = { code -> viewModel.joinRoom(code) },
                                    onCreateRoom = { viewModel.createNewRoom() },
                                    onToggleNotification = { league, enabled ->
                                        viewModel.toggleLeagueNotification(league, enabled)
                                    },
                                    onUpdateProfilePhoto = { uri -> viewModel.updateProfilePhoto(uri) },
                                    onRemoveProfilePhoto = { viewModel.removeProfilePhoto() },
                                    onLogout = { viewModel.logout() }
                                )
                            }
                        }
                    }
                }
            }

            // Skiper UI Floating Pill Dock Navigation
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 14.dp, start = 16.dp, end = 16.dp)
            ) {
                val dockBg = if (activeTheme.isLightTheme) Color(0xF5FFFFFF) else Color(0xF2111115)
                val pillBg = if (activeTheme.isLightTheme) Color(0xFF09090B) else Color.White
                val pillFg = if (activeTheme.isLightTheme) Color.White else Color(0xFF09090B)
                Surface(
                    shape = CircleShape,
                    color = dockBg,
                    border = BorderStroke(1.dp, activeTheme.borderColor),
                    shadowElevation = 18.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MainTab.entries.forEach { tab ->
                            val selected = tab == currentTab
                            Surface(
                                shape = CircleShape,
                                color = if (selected) pillBg else Color.Transparent,
                                modifier = Modifier.clickable { currentTab = tab }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(
                                        horizontal = if (selected) 14.dp else 11.dp,
                                        vertical = 9.dp
                                    )
                                ) {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.label,
                                        tint = if (selected) pillFg else activeTheme.textSecondaryColor,
                                        modifier = Modifier.size(19.dp)
                                    )
                                    AnimatedVisibility(
                                        visible = selected,
                                        enter = fadeIn(tween(180)) + expandHorizontally(tween(180)),
                                        exit = fadeOut(tween(140)) + shrinkHorizontally(tween(140))
                                    ) {
                                        Row {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = tab.label,
                                                color = pillFg,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Real-time Goal Animation (triggers automatically when a live goal happens)
            state.activeGoalEvent?.let { goalEvent ->
                GoalAnimationOverlay(
                    event = goalEvent,
                    onDismiss = { viewModel.dismissGoalAnimation() }
                )
            }
                }
            }

            // Loading screen: pure black background with a spinning ball in the center
            AnimatedVisibility(
                visible = showSplashIntro,
                enter = EnterTransition.None,
                exit = fadeOut(animationSpec = tween(durationMillis = 250))
            ) {
                SplashIntroOverlay(
                    isLoading = state.isRefreshing,
                    onFinished = { showSplashIntro = false }
                )
            }
        }
    }
}
