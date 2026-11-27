package com.bolaodosvei.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bolaodosvei.app.model.DailyChallenge
import com.bolaodosvei.app.model.ShopCatalog
import com.bolaodosvei.app.model.ShopCategory
import com.bolaodosvei.app.model.ShopItem
import com.bolaodosvei.app.model.UserAccount
import com.bolaodosvei.app.model.UserShopState
import com.bolaodosvei.app.ui.components.ShopItemIcon
import com.bolaodosvei.app.ui.components.UserAvatarBadge
import com.bolaodosvei.app.ui.theme.GoldAccent
import com.bolaodosvei.app.ui.theme.LocalBolaoTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val StudioObsidianShell = Color(0xFF111114)
private val StudioObsidianInset = Color(0xFF09090B)
private val StudioHairlineDark = Color(0xFF232326)
private val StudioInnerBorderDark = Color(0xFF1C1C20)

@Composable
fun ShopScreen(
    user: UserAccount,
    shopState: UserShopState,
    onBuyItem: (String) -> Unit,
    onEquipItem: (String) -> Unit,
    dailyChallenges: List<DailyChallenge> = emptyList(),
    onClaimChallenge: (String) -> Unit = {}
) {
    val activeTheme = LocalBolaoTheme.current
    val haptic = LocalHapticFeedback.current
    var selectedCategory by remember { mutableStateOf<ShopCategory?>(null) }

    var previewFrameId by remember { mutableStateOf<String?>(null) }
    var previewTitleId by remember { mutableStateOf<String?>(null) }
    var previewFlairId by remember { mutableStateOf<String?>(null) }
    var lastPreviewedItemId by remember { mutableStateOf<String?>(null) }

    val effectiveFrameId = previewFrameId ?: shopState.equippedFrameId
    val effectiveTitleId = previewTitleId ?: shopState.equippedTitleId
    val effectiveFlairId = previewFlairId ?: shopState.equippedFlairId

    val effectiveFrame = remember(effectiveFrameId) { ShopCatalog.findById(effectiveFrameId) }
    val effectiveTitle = remember(effectiveTitleId) { ShopCatalog.findById(effectiveTitleId) }
    val effectiveFlair = remember(effectiveFlairId) { ShopCatalog.findById(effectiveFlairId) }

    val isPreviewing = previewFrameId != null || previewTitleId != null || previewFlairId != null
    val activePreviewItem = remember(lastPreviewedItemId, previewFrameId, previewTitleId, previewFlairId) {
        val id = lastPreviewedItemId?.takeIf {
            it == previewFrameId || it == previewTitleId || it == previewFlairId
        } ?: previewFrameId ?: previewTitleId ?: previewFlairId
        ShopCatalog.findById(id)
    }

    val filteredItems = remember(selectedCategory) {
        if (selectedCategory == null) ShopCatalog.items
        else ShopCatalog.items.filter { it.category == selectedCategory }
    }

    fun clearPreviews() {
        previewFrameId = null
        previewTitleId = null
        previewFlairId = null
        lastPreviewedItemId = null
    }

    fun togglePreview(item: ShopItem) {
        when (item.category) {
            ShopCategory.AVATAR_FRAME -> {
                if (previewFrameId == item.id) {
                    previewFrameId = null
                } else {
                    previewFrameId = item.id
                    lastPreviewedItemId = item.id
                }
            }
            ShopCategory.TITLE_BADGE -> {
                if (previewTitleId == item.id) {
                    previewTitleId = null
                } else {
                    previewTitleId = item.id
                    lastPreviewedItemId = item.id
                }
            }
            ShopCategory.CHAT_FLAIR -> {
                if (previewFlairId == item.id) {
                    previewFlairId = null
                } else {
                    previewFlairId = item.id
                    lastPreviewedItemId = item.id
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 108.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top Live Profile & Chat Studio Stage (Double-Nested Bento)
        item {
            LiveProfileStudioStage(
                user = user,
                availablePoints = shopState.availablePoints,
                effectiveFrame = effectiveFrame,
                effectiveTitle = effectiveTitle,
                effectiveFlair = effectiveFlair,
                isPreviewing = isPreviewing,
                activePreviewItem = activePreviewItem,
                isPreviewItemOwned = activePreviewItem?.id in shopState.ownedItemIds,
                onClearPreview = ::clearPreviews,
                onBuyPreviewItem = { itemId ->
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onBuyItem(itemId)
                    clearPreviews()
                },
                onEquipPreviewItem = { itemId ->
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onEquipItem(itemId)
                    clearPreviews()
                }
            )
        }

        // Desafios Diários (Double-Nested Bento)
        if (dailyChallenges.isNotEmpty()) {
            item(contentType = "daily_challenges") {
                DailyChallengesBentoSection(
                    challenges = dailyChallenges,
                    onClaimChallenge = { challengeId ->
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onClaimChallenge(challengeId)
                    }
                )
            }
        }

        // OriginKit-style Category Filter Pills with Item Counts
        item {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    OriginCategoryPill(
                        label = "Todas · ${ShopCatalog.items.size}",
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null }
                    )
                }
                items(ShopCategory.entries) { category ->
                    val count = remember(category) {
                        ShopCatalog.items.count { it.category == category }
                    }
                    OriginCategoryPill(
                        label = "${category.label} · $count",
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category }
                    )
                }
            }
        }

        // Double-Nested Bento Shop Item Cards
        items(filteredItems, key = { it.id }) { item ->
            val isOwned = item.id in shopState.ownedItemIds
            val isEquipped = item.id == shopState.equippedFrameId ||
                item.id == shopState.equippedTitleId ||
                item.id == shopState.equippedFlairId
            val isItemPreviewed = item.id == previewFrameId ||
                item.id == previewTitleId ||
                item.id == previewFlairId

            BentoShopItemCard(
                item = item,
                isOwned = isOwned,
                isEquipped = isEquipped,
                isPreviewed = isItemPreviewed,
                canAfford = shopState.availablePoints >= item.price,
                onTogglePreview = { togglePreview(item) },
                onBuy = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onBuyItem(item.id)
                    if (isItemPreviewed) clearPreviews()
                },
                onEquip = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onEquipItem(item.id)
                    if (isItemPreviewed) clearPreviews()
                }
            )
        }
    }
}

@Composable
private fun OriginCategoryPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val activeTheme = LocalBolaoTheme.current
    val containerColor = when {
        selected && activeTheme.isLightTheme -> StudioObsidianInset
        selected -> Color.White
        activeTheme.isLightTheme -> activeTheme.cardColor
        else -> StudioObsidianShell
    }
    val contentColor = when {
        selected && activeTheme.isLightTheme -> Color.White
        selected -> StudioObsidianInset
        else -> activeTheme.textSecondaryColor
    }
    val borderColor = when {
        selected -> containerColor
        activeTheme.isLightTheme -> activeTheme.borderColor
        else -> StudioHairlineDark
    }

    Surface(
        shape = CircleShape,
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            color = contentColor,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun LiveProfileStudioStage(
    user: UserAccount,
    availablePoints: Int,
    effectiveFrame: ShopItem?,
    effectiveTitle: ShopItem?,
    effectiveFlair: ShopItem?,
    isPreviewing: Boolean,
    activePreviewItem: ShopItem?,
    isPreviewItemOwned: Boolean,
    onClearPreview: () -> Unit,
    onBuyPreviewItem: (String) -> Unit,
    onEquipPreviewItem: (String) -> Unit
) {
    val activeTheme = LocalBolaoTheme.current
    val outerShellColor = if (activeTheme.isLightTheme) activeTheme.cardColor else StudioObsidianShell
    val outerBorderColor = if (activeTheme.isLightTheme) activeTheme.borderColor else StudioHairlineDark
    val insetStageColor = if (activeTheme.isLightTheme) activeTheme.surfaceColor else StudioObsidianInset
    val insetBorderColor = if (activeTheme.isLightTheme) activeTheme.borderColor.copy(alpha = 0.7f) else StudioInnerBorderDark

    val primaryCtaBg = if (activeTheme.isLightTheme) StudioObsidianInset else Color.White
    val primaryCtaFg = if (activeTheme.isLightTheme) Color.White else StudioObsidianInset

    // Outer 22.dp matte obsidian shell
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = outerShellColor,
        border = BorderStroke(1.dp, outerBorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header row: Studio tag + Monospace Balance Pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "STUDIO & LOJA",
                    color = activeTheme.textSecondaryColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.4.sp
                )

                Surface(
                    color = insetStageColor,
                    shape = CircleShape,
                    border = BorderStroke(1.dp, outerBorderColor)
                ) {
                    Text(
                        text = "$availablePoints PTS",
                        color = if (activeTheme.isLightTheme) Color(0xFFB45309) else GoldAccent,
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontFeatureSettings = "tnum",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Inset 14.dp preview stage
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = insetStageColor,
                border = BorderStroke(1.dp, insetBorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar with Templens radial dot-matrix Canvas aura
                    Box(
                        modifier = Modifier.size(84.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val auraColor = effectiveFrame?.let { Color(it.accentHex) } ?: activeTheme.accentColor
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val rings = listOf(0.32f, 0.42f, 0.48f)
                            rings.forEachIndexed { ringIdx, factor ->
                                val r = size.minDimension * factor
                                val dots = 12 + ringIdx * 6
                                val alpha = 0.42f - ringIdx * 0.12f
                                for (i in 0 until dots) {
                                    val rad = (i * (360f / dots)) * (PI / 180.0)
                                    val dotPos = Offset(
                                        center.x + r * cos(rad).toFloat(),
                                        center.y + r * sin(rad).toFloat()
                                    )
                                    drawCircle(
                                        color = auraColor.copy(alpha = alpha.coerceAtLeast(0.08f)),
                                        radius = (2.1f - ringIdx * 0.4f).dp.toPx() * 0.5f,
                                        center = dotPos
                                    )
                                }
                            }
                        }

                        UserAvatarBadge(
                            name = user.name,
                            colorHex = user.avatarColorHex,
                            size = 64.dp,
                            photoPath = user.avatarImagePath,
                            frameAccentHex = effectiveFrame?.accentHex,
                            frameId = effectiveFrame?.id
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = user.name,
                                color = activeTheme.textPrimaryColor,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (effectiveTitle != null) {
                                val titleColor = Color(effectiveTitle.accentHex)
                                Surface(
                                    color = titleColor.copy(alpha = 0.16f),
                                    shape = CircleShape,
                                    border = BorderStroke(1.dp, titleColor.copy(alpha = 0.55f))
                                ) {
                                    Text(
                                        text = effectiveTitle.badgeText,
                                        color = if (activeTheme.isLightTheme && titleColor.luminance() > 0.6f) {
                                            Color(0xFFB45309)
                                        } else {
                                            titleColor
                                        },
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(3.dp))

                        Text(
                            text = "Moldura: ${effectiveFrame?.name ?: "Padrão"}",
                            color = activeTheme.textSecondaryColor,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        LiveChatBubblePreview(
                            flair = effectiveFlair,
                            title = effectiveTitle
                        )
                    }
                }
            }

            // Crisp PRÉVIA / Limpar controls when previewing
            if (isPreviewing && activePreviewItem != null) {
                Spacer(modifier = Modifier.height(10.dp))
                val previewAccent = Color(activePreviewItem.accentHex)

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = insetStageColor,
                    border = BorderStroke(1.dp, previewAccent.copy(alpha = 0.55f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    color = previewAccent.copy(alpha = 0.18f),
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = "PRÉVIA",
                                        color = if (activeTheme.isLightTheme && previewAccent.luminance() > 0.6f) {
                                            Color(0xFFB45309)
                                        } else {
                                            previewAccent
                                        },
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.8.sp,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = activePreviewItem.name,
                                    color = activeTheme.textPrimaryColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Surface(
                                shape = CircleShape,
                                color = Color.Transparent,
                                border = BorderStroke(1.dp, outerBorderColor),
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable(onClick = onClearPreview)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = null,
                                        tint = activeTheme.textSecondaryColor,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Limpar",
                                        color = activeTheme.textSecondaryColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (!isPreviewItemOwned) {
                            Button(
                                onClick = { onBuyPreviewItem(activePreviewItem.id) },
                                enabled = availablePoints >= activePreviewItem.price,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = primaryCtaBg,
                                    contentColor = primaryCtaFg,
                                    disabledContainerColor = outerShellColor,
                                    disabledContentColor = activeTheme.textSecondaryColor
                                ),
                                shape = CircleShape,
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingBag,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Comprar (${activePreviewItem.price} PTS)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Button(
                                onClick = { onEquipPreviewItem(activePreviewItem.id) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = primaryCtaBg,
                                    contentColor = primaryCtaFg
                                ),
                                shape = CircleShape,
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(36.dp)
                            ) {
                                Text(
                                    text = "Equipar ${activePreviewItem.name}",
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
}

@Composable
private fun LiveChatBubblePreview(
    flair: ShopItem?,
    title: ShopItem?
) {
    val activeTheme = LocalBolaoTheme.current
    val isVip = flair?.id == "flair_vip"
    val isGolaco = flair?.id == "flair_golaco"

    val bubbleBg = when {
        isVip -> Color(0xFF17130A)
        isGolaco -> Color(0xFF062519)
        activeTheme.isLightTheme -> activeTheme.cardColor
        else -> StudioObsidianShell
    }
    val bubbleBorder = when {
        isVip -> GoldAccent.copy(alpha = 0.8f)
        isGolaco -> Color(0xFF00E676).copy(alpha = 0.7f)
        flair != null -> Color(flair.accentHex).copy(alpha = 0.65f)
        activeTheme.isLightTheme -> activeTheme.borderColor
        else -> StudioHairlineDark
    }
    val messageColor = when {
        isVip -> Color(0xFFFFF8E1)
        isGolaco -> Color(0xFFD1FAE5)
        else -> activeTheme.textPrimaryColor
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bubbleBg)
            .border(1.dp, bubbleBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (flair != null) {
                val fColor = Color(flair.accentHex)
                Surface(
                    color = fColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = flair.badgeText,
                        color = fColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }
            Text(
                text = if (title != null) {
                    "\"Cravei 2x1 hoje! Confia no ${title.badgeText}!\""
                } else {
                    "\"Hoje tem golaço e 10 pts na cravada!\""
                },
                color = messageColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun BentoShopItemCard(
    item: ShopItem,
    isOwned: Boolean,
    isEquipped: Boolean,
    isPreviewed: Boolean,
    canAfford: Boolean,
    onTogglePreview: () -> Unit,
    onBuy: () -> Unit,
    onEquip: () -> Unit
) {
    val activeTheme = LocalBolaoTheme.current
    val itemAccent = Color(item.accentHex)
    val readableAccent = if (activeTheme.isLightTheme && itemAccent.luminance() > 0.6f) {
        Color(0xFFB45309)
    } else {
        itemAccent
    }

    val outerShellColor = if (activeTheme.isLightTheme) activeTheme.cardColor else StudioObsidianShell
    val insetPedestalColor = if (activeTheme.isLightTheme) activeTheme.surfaceColor else StudioObsidianInset
    val hairlineBorder = if (activeTheme.isLightTheme) activeTheme.borderColor else StudioHairlineDark
    val insetBorder = if (activeTheme.isLightTheme) activeTheme.borderColor.copy(alpha = 0.7f) else StudioInnerBorderDark

    val primaryCtaBg = if (activeTheme.isLightTheme) StudioObsidianInset else Color.White
    val primaryCtaFg = if (activeTheme.isLightTheme) Color.White else StudioObsidianInset

    // Double-nested bento card: 22.dp outer matte shell
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = outerShellColor,
        border = BorderStroke(
            width = 1.dp,
            color = when {
                isPreviewed -> GoldAccent
                isEquipped -> itemAccent.copy(alpha = 0.75f)
                else -> hairlineBorder
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Inset 14.dp #09090B Pedestal for handcrafted ShopItemIcon
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(insetPedestalColor)
                        .border(1.dp, insetBorder, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    ShopItemIcon(
                        itemId = item.id,
                        accentColor = itemAccent,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = item.name,
                            color = activeTheme.textPrimaryColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Surface(
                            color = itemAccent.copy(alpha = 0.14f),
                            shape = CircleShape
                        ) {
                            Text(
                                text = item.category.label,
                                color = readableAccent,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = item.description,
                        color = activeTheme.textSecondaryColor,
                        fontSize = 11.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom Action Row: Monospace Price Pill + Testar Ghost Pill + High-Contrast Primary CTA
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = insetPedestalColor,
                    shape = CircleShape,
                    border = BorderStroke(1.dp, insetBorder)
                ) {
                    Text(
                        text = "${item.price} PTS",
                        color = if (activeTheme.isLightTheme) Color(0xFFB45309) else GoldAccent,
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontFeatureSettings = "tnum",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Ghost pill "Testar"
                    OutlinedButton(
                        onClick = onTogglePreview,
                        border = BorderStroke(
                            1.dp,
                            if (isPreviewed) GoldAccent else hairlineBorder
                        ),
                        shape = CircleShape,
                        contentPadding = PaddingValues(horizontal = 11.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            tint = if (isPreviewed) {
                                if (activeTheme.isLightTheme) Color(0xFFB45309) else GoldAccent
                            } else {
                                activeTheme.textSecondaryColor
                            },
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isPreviewed) "Testando" else "Testar",
                            color = if (isPreviewed) {
                                if (activeTheme.isLightTheme) Color(0xFFB45309) else GoldAccent
                            } else {
                                activeTheme.textSecondaryColor
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    when {
                        isEquipped -> {
                            Surface(
                                color = itemAccent.copy(alpha = 0.16f),
                                shape = CircleShape,
                                border = BorderStroke(1.dp, itemAccent.copy(alpha = 0.7f))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = readableAccent,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Equipado ✓",
                                        color = readableAccent,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        isOwned -> {
                            Button(
                                onClick = onEquip,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = primaryCtaBg,
                                    contentColor = primaryCtaFg
                                ),
                                shape = CircleShape,
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text(
                                    text = "Equipar",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        else -> {
                            Button(
                                onClick = onBuy,
                                enabled = canAfford,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = primaryCtaBg,
                                    contentColor = primaryCtaFg,
                                    disabledContainerColor = insetPedestalColor,
                                    disabledContentColor = activeTheme.textSecondaryColor
                                ),
                                shape = CircleShape,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingBag,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Comprar",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyChallengesBentoSection(
    challenges: List<DailyChallenge>,
    onClaimChallenge: (String) -> Unit
) {
    val activeTheme = LocalBolaoTheme.current
    val outerShellColor = if (activeTheme.isLightTheme) activeTheme.cardColor else StudioObsidianShell
    val insetColor = if (activeTheme.isLightTheme) activeTheme.surfaceColor else StudioObsidianInset
    val hairlineBorder = if (activeTheme.isLightTheme) activeTheme.borderColor else StudioHairlineDark
    val insetBorder = if (activeTheme.isLightTheme) activeTheme.borderColor.copy(alpha = 0.7f) else StudioInnerBorderDark
    val primaryCtaBg = if (activeTheme.isLightTheme) StudioObsidianInset else Color.White
    val primaryCtaFg = if (activeTheme.isLightTheme) Color.White else StudioObsidianInset
    val claimedCount = challenges.count { it.claimed }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = outerShellColor,
        border = BorderStroke(1.dp, hairlineBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DESAFIOS DIÁRIOS",
                    color = activeTheme.textSecondaryColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.4.sp
                )
                Surface(
                    color = insetColor,
                    shape = CircleShape,
                    border = BorderStroke(1.dp, insetBorder)
                ) {
                    Text(
                        text = "$claimedCount/${challenges.size}",
                        color = activeTheme.textPrimaryColor,
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontFeatureSettings = "tnum",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                    )
                }
            }

            challenges.forEach { challenge ->
                val targetSafe = challenge.target.coerceAtLeast(1)
                val progressRatio = (challenge.progress.toFloat() / targetSafe.toFloat()).coerceIn(0f, 1f)
                val canClaim = challenge.progress >= challenge.target && !challenge.claimed

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = insetColor,
                    border = BorderStroke(
                        1.dp,
                        when {
                            canClaim -> GoldAccent.copy(alpha = 0.75f)
                            challenge.claimed -> Color(0xFF00E676).copy(alpha = 0.45f)
                            else -> insetBorder
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = challenge.title,
                                    color = activeTheme.textPrimaryColor,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = challenge.description,
                                    color = activeTheme.textSecondaryColor,
                                    fontSize = 11.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = GoldAccent.copy(alpha = 0.14f),
                                shape = CircleShape
                            ) {
                                Text(
                                    text = "+${challenge.rewardPoints} PTS",
                                    color = if (activeTheme.isLightTheme) Color(0xFFB45309) else GoldAccent,
                                    style = TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontFeatureSettings = "tnum",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            LinearProgressIndicator(
                                progress = { progressRatio },
                                color = if (challenge.claimed || canClaim) Color(0xFF00E676) else activeTheme.accentColor,
                                trackColor = outerShellColor,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .clip(CircleShape)
                            )

                            Text(
                                text = "${challenge.progress}/${challenge.target}",
                                color = activeTheme.textSecondaryColor,
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontFeatureSettings = "tnum",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )

                            if (challenge.claimed) {
                                Surface(
                                    color = Color(0xFF00E676).copy(alpha = 0.16f),
                                    shape = CircleShape,
                                    border = BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.55f))
                                ) {
                                    Text(
                                        text = "Resgatado ✓",
                                        color = Color(0xFF00E676),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            } else {
                                Button(
                                    onClick = { onClaimChallenge(challenge.id) },
                                    enabled = canClaim,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = primaryCtaBg,
                                        contentColor = primaryCtaFg,
                                        disabledContainerColor = outerShellColor,
                                        disabledContentColor = activeTheme.textSecondaryColor
                                    ),
                                    shape = CircleShape,
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text(
                                        text = "Resgatar",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
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
