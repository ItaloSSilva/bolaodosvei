package com.bolaodosvei.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bolaodosvei.app.model.League
import com.bolaodosvei.app.model.UserAccount
import com.bolaodosvei.app.ui.components.BolaoLogoBadge
import com.bolaodosvei.app.ui.components.ClubThemeBackdrop
import com.bolaodosvei.app.ui.components.TeamCrest
import com.bolaodosvei.app.ui.components.UserAvatarBadge
import com.bolaodosvei.app.ui.theme.*

private enum class SettingsTab(val label: String) {
    THEMES("Temas dos Clubes"),
    EDIT_PROFILE("Editar Perfil")
}

private enum class ThemeFilter(val label: String, val league: League?) {
    ALL("Padrão & Todos", null),
    BRASILEIRAO("Brasileirão", League.BRASILEIRAO),
    PREMIER("Premier League", League.PREMIER_LEAGUE),
    LA_LIGA("La Liga", League.LA_LIGA),
    CHAMPIONS("Champions", League.CHAMPIONS_LEAGUE)
}

@Composable
fun SettingsScreen(
    user: UserAccount,
    selectedThemeId: String,
    equippedFrameAccentHex: Long? = null,
    onSelectTheme: (AppThemePreset) -> Unit,
    onUpdateProfile: (String, Long, League) -> Result<UserAccount>,
    onUpdateProfilePhoto: (Uri) -> Unit,
    onRemoveProfilePhoto: () -> Unit,
    onBack: () -> Unit
) {
    val activeTheme = LocalBolaoTheme.current
    val ctaBg = if (activeTheme.isLightTheme) Color(0xFF09090B) else Color.White
    val ctaFg = if (activeTheme.isLightTheme) Color.White else Color(0xFF09090B)
    var currentTab by remember { mutableStateOf(SettingsTab.THEMES) }

    Box(modifier = Modifier.fillMaxSize()) {
        ClubThemeBackdrop(theme = activeTheme, showLargeMascot = true)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(activeTheme.surfaceColor.copy(alpha = 0.94f))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = activeTheme.cardColor,
                    border = BorderStroke(1.dp, activeTheme.borderColor),
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .clickable { onBack() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = activeTheme.textPrimaryColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Configurações",
                    color = activeTheme.textPrimaryColor,
                    fontFamily = BolaoDisplayFontFamily,
                    fontSize = 20.sp
                )
            }

            HorizontalDivider(thickness = 1.dp, color = activeTheme.borderColor.copy(alpha = 0.7f))

            // OriginKit Segmented Switch: Temas vs Editar Perfil
            Surface(
                shape = CircleShape,
                color = activeTheme.cardColor,
                border = BorderStroke(1.dp, activeTheme.borderColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SettingsTab.entries.forEach { tab ->
                        val selected = tab == currentTab
                        Surface(
                            shape = CircleShape,
                            color = if (selected) ctaBg else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clip(CircleShape)
                                .clickable { currentTab = tab }
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (tab == SettingsTab.THEMES) Icons.Default.Palette else Icons.Default.Person,
                                    contentDescription = null,
                                    tint = if (selected) ctaFg else activeTheme.textSecondaryColor,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = tab.label,
                                    color = if (selected) ctaFg else activeTheme.textSecondaryColor,
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            when (currentTab) {
                SettingsTab.THEMES -> ThemesPickerSection(
                    selectedThemeId = selectedThemeId,
                    onSelectTheme = onSelectTheme
                )
                SettingsTab.EDIT_PROFILE -> EditProfileSection(
                    user = user,
                    equippedFrameAccentHex = equippedFrameAccentHex,
                    onSave = onUpdateProfile,
                    onUpdateProfilePhoto = onUpdateProfilePhoto,
                    onRemoveProfilePhoto = onRemoveProfilePhoto
                )
            }
        }
    }
}

@Composable
private fun ThemesPickerSection(
    selectedThemeId: String,
    onSelectTheme: (AppThemePreset) -> Unit
) {
    val activeTheme = LocalBolaoTheme.current
    var filter by remember { mutableStateOf(ThemeFilter.BRASILEIRAO) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredThemes = remember(filter, searchQuery) {
        val base = when (filter) {
            ThemeFilter.ALL -> ClubThemesCatalog.allThemes
            else -> listOf(DefaultAppTheme) + ClubThemesCatalog.allThemes.filter { it.league == filter.league }
        }
        val q = searchQuery.trim()
        if (q.isEmpty()) base
        else ClubThemesCatalog.allThemes.filter { it.name.contains(q, ignoreCase = true) }
    }

    val rows = remember(filteredThemes) { filteredThemes.chunked(2) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        contentPadding = PaddingValues(bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Active Theme Preview Banner (Double-Nested Bento)
        item {
            val onBannerAccent = if (activeTheme.accentColor.luminance() > 0.45f) PitchDark else Color.White
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = activeTheme.cardColor,
                border = BorderStroke(1.dp, activeTheme.borderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.padding(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.Transparent,
                        border = BorderStroke(1.5.dp, activeTheme.accentColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(114.dp)
                            .clip(RoundedCornerShape(14.dp))
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            ClubThemeBackdrop(theme = activeTheme, showLargeMascot = false)
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                (if (activeTheme.isLightTheme) Color.White else Color.Black).copy(alpha = 0.80f),
                                                (if (activeTheme.isLightTheme) Color.White else Color.Black).copy(alpha = 0.35f),
                                                Color.Transparent
                                            )
                                        )
                                    )
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (activeTheme.logoUrl.isNotBlank()) {
                                    TeamCrest(
                                        teamName = activeTheme.name,
                                        shortName = activeTheme.name.take(3),
                                        logoUrl = activeTheme.logoUrl,
                                        colorHex = activeTheme.accentHex,
                                        size = 44.dp
                                    )
                                } else {
                                    BolaoLogoBadge(size = 44.dp)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f).padding(end = 68.dp)) {
                                    Surface(
                                        color = activeTheme.accentColor,
                                        shape = CircleShape
                                    ) {
                                        Text(
                                            text = "TEMA ATIVO",
                                            color = onBannerAccent,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = activeTheme.name,
                                        color = activeTheme.textPrimaryColor,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Black,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = activeTheme.mascotName,
                                        color = activeTheme.accentColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(5.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                        listOf(Color(activeTheme.gradientMidHex), activeTheme.accentColor, activeTheme.secondaryColor).forEach { swatch ->
                                            Box(
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .clip(CircleShape)
                                                    .background(swatch)
                                                    .border(0.8.dp, activeTheme.borderColor, CircleShape)
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

        // Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar time (ex: Flamengo, Arsenal, Bayern...)", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = activeTheme.textSecondaryColor) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // League Filter Pills
        item {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    listOf(
                        ThemeFilter.BRASILEIRAO,
                        ThemeFilter.PREMIER,
                        ThemeFilter.LA_LIGA,
                        ThemeFilter.CHAMPIONS,
                        ThemeFilter.ALL
                    )
                ) { f ->
                    val selected = f == filter && searchQuery.isBlank()
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
                        border = BorderStroke(1.dp, if (selected) pillBg else activeTheme.borderColor),
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable {
                                filter = f
                                searchQuery = ""
                            }
                    ) {
                        Text(
                            text = when (f) {
                                ThemeFilter.ALL -> "Todos"
                                ThemeFilter.BRASILEIRAO -> "Brasileirão"
                                ThemeFilter.PREMIER -> "Premier"
                                ThemeFilter.LA_LIGA -> "La Liga"
                                ThemeFilter.CHAMPIONS -> "Champions"
                            },
                            color = pillFg,
                            fontSize = 11.sp,
                            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp)
                        )
                    }
                }
            }
        }

        // 2-column grid of club theme cards
        items(rows) { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                pair.forEach { preset ->
                    ClubThemeCard(
                        preset = preset,
                        isSelected = preset.id == selectedThemeId,
                        onClick = { onSelectTheme(preset) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (pair.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ClubThemeCard(
    preset: AppThemePreset,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val onPresetAccent = if (preset.accentColor.luminance() > 0.45f) PitchDark else Color.White
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) preset.accentColor else preset.borderColor
        ),
        modifier = modifier
            .height(132.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            ClubThemeBackdrop(theme = preset, showLargeMascot = false)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                (if (preset.isLightTheme) Color.White else Color.Black).copy(alpha = 0.74f),
                                (if (preset.isLightTheme) Color.White else Color.Black).copy(alpha = 0.25f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(11.dp)
            ) {
                if (isSelected) {
                    Surface(
                        shape = CircleShape,
                        color = preset.accentColor,
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Text(
                            text = "ATIVO",
                            color = onPresetAccent,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }

                Column(
                    verticalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(end = if (preset.mascotType != ClubMascotType.NONE) 66.dp else 0.dp)
                ) {
                    if (preset.logoUrl.isNotBlank()) {
                        TeamCrest(
                            teamName = preset.name,
                            shortName = preset.name.take(3),
                            logoUrl = preset.logoUrl,
                            colorHex = preset.accentHex,
                            size = 36.dp
                        )
                    } else {
                        BolaoLogoBadge(size = 36.dp)
                    }

                    Column {
                        Text(
                            text = preset.name,
                            color = preset.textPrimaryColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = preset.mascotName,
                            color = preset.accentColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            lineHeight = 12.sp,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EditProfileSection(
    user: UserAccount,
    equippedFrameAccentHex: Long?,
    onSave: (String, Long, League) -> Result<UserAccount>,
    onUpdateProfilePhoto: (Uri) -> Unit,
    onRemoveProfilePhoto: () -> Unit
) {
    val activeTheme = LocalBolaoTheme.current
    val ctaBg = if (activeTheme.isLightTheme) Color(0xFF09090B) else Color.White
    val ctaFg = if (activeTheme.isLightTheme) Color.White else Color(0xFF09090B)

    var name by remember(user.id, user.name) { mutableStateOf(user.name) }
    var selectedColor by remember(user.id, user.avatarColorHex) { mutableLongStateOf(user.avatarColorHex) }
    var selectedLeague by remember(user.id, user.favoriteLeague) { mutableStateOf(user.favoriteLeague) }
    var statusMsg by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let(onUpdateProfilePhoto)
    }

    val badgeColors = listOf(
        0xFF00C853L,
        0xFFFFD600L,
        0xFF3B82F6L,
        0xFFEF4444L,
        0xFFA855F7L,
        0xFFF97316L
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        contentPadding = PaddingValues(top = 6.dp, bottom = 104.dp)
    ) {
        item {
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
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            UserAvatarBadge(
                                name = name.ifBlank { user.name },
                                colorHex = selectedColor,
                                photoPath = user.avatarImagePath,
                                frameAccentHex = equippedFrameAccentHex,
                                size = 78.dp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = user.email, color = activeTheme.textSecondaryColor, fontSize = 12.sp)

                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { photoPickerLauncher.launch("image/*") },
                                    border = BorderStroke(1.dp, activeTheme.borderColor),
                                    shape = CircleShape,
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text("Foto da Galeria", color = activeTheme.textPrimaryColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                if (!user.avatarImagePath.isNullOrBlank()) {
                                    OutlinedButton(
                                        onClick = onRemoveProfilePhoto,
                                        border = BorderStroke(1.dp, LiveRed.copy(alpha = 0.65f)),
                                        shape = CircleShape,
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Text("Remover", color = LiveRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it; statusMsg = null },
                                label = { Text("Nome ou apelido") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Cor do seu escudo de perfil",
                                color = activeTheme.textSecondaryColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.align(Alignment.Start)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                badgeColors.forEach { hex ->
                                    val chosen = hex == selectedColor
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(Color(hex))
                                            .border(
                                                width = if (chosen) 3.dp else 1.dp,
                                                color = if (chosen) ctaBg else Color.Transparent,
                                                shape = CircleShape
                                            )
                                            .clickable { selectedColor = hex }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Liga favorita",
                                color = activeTheme.textSecondaryColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.align(Alignment.Start)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(League.entries) { league ->
                                    val selected = selectedLeague == league
                                    Surface(
                                        shape = CircleShape,
                                        color = if (selected) ctaBg else activeTheme.cardColor,
                                        border = BorderStroke(1.dp, if (selected) ctaBg else activeTheme.borderColor),
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .clickable { selectedLeague = league }
                                    ) {
                                        Text(
                                            text = league.shortName,
                                            color = if (selected) ctaFg else activeTheme.textSecondaryColor,
                                            fontSize = 11.sp,
                                            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
                                            maxLines = 1,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }

                            if (statusMsg != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = statusMsg!!,
                                    color = activeTheme.accentColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Spacer(modifier = Modifier.height(18.dp))
                            Button(
                                onClick = {
                                    val res = onSave(name, selectedColor, selectedLeague)
                                    statusMsg = res.fold(
                                        onSuccess = { "Alterações salvas!" },
                                        onFailure = { it.message ?: "Erro ao salvar" }
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ctaBg,
                                    contentColor = ctaFg
                                ),
                                shape = CircleShape,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Text("Salvar Perfil", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }
        }
    }
}
