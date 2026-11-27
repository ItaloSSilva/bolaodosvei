package com.bolaodosvei.app.model

enum class League(
    val id: String,
    val displayName: String,
    val shortName: String,
    val country: String,
    val espnSlug: String,
    val badgeTag: String,
    val accentColorHex: Long
) {
    BRASILEIRAO(
        id = "brasileirao",
        displayName = "Brasileirão Série A",
        shortName = "Brasileirão",
        country = "Brasil",
        espnSlug = "bra.1",
        badgeTag = "BRA",
        accentColorHex = 0xFF00E676
    ),
    PREMIER_LEAGUE(
        id = "premier_league",
        displayName = "Premier League",
        shortName = "Premier",
        country = "Inglaterra",
        espnSlug = "eng.1",
        badgeTag = "ING",
        accentColorHex = 0xFFA78BFA
    ),
    LA_LIGA(
        id = "la_liga",
        displayName = "La Liga",
        shortName = "La Liga",
        country = "Espanha",
        espnSlug = "esp.1",
        badgeTag = "ESP",
        accentColorHex = 0xFFFF6B6B
    ),
    CHAMPIONS_LEAGUE(
        id = "champions_league",
        displayName = "UEFA Champions League",
        shortName = "Champions",
        country = "Europa",
        espnSlug = "uefa.champions",
        badgeTag = "UCL",
        accentColorHex = 0xFF38BDF8
    ),
    LIBERTADORES(
        id = "libertadores",
        displayName = "Copa Libertadores",
        shortName = "Libertadores",
        country = "América do Sul",
        espnSlug = "conmebol.libertadores",
        badgeTag = "LIB",
        accentColorHex = 0xFFF59E0B
    ),
    COPA_DO_BRASIL(
        id = "copa_do_brasil",
        displayName = "Copa do Brasil",
        shortName = "Copa BR",
        country = "Brasil",
        espnSlug = "bra.copa_do_brazil",
        badgeTag = "CDB",
        accentColorHex = 0xFF22C55E
    ),
    SUL_AMERICANA(
        id = "sul_americana",
        displayName = "Copa Sul-Americana",
        shortName = "Sul-Americana",
        country = "América do Sul",
        espnSlug = "conmebol.sudamericana",
        badgeTag = "SUL",
        accentColorHex = 0xFF06B6D4
    )
}

enum class MatchStatus {
    SCHEDULED,
    LIVE,
    HALFTIME,
    FINISHED
}

enum class MatchDayFilter(val label: String) {
    TODAY("Hoje"),
    TOMORROW("Amanhã"),
    UPCOMING("Próximos")
}

data class TeamInfo(
    val name: String,
    val shortName: String,
    val colorHex: Long,
    val logoUrl: String = ""
)

data class Match(
    val id: String,
    val league: League,
    val homeTeam: TeamInfo,
    val awayTeam: TeamInfo,
    val homeScore: Int,
    val awayScore: Int,
    val status: MatchStatus,
    val minute: Int,
    val kickoffTime: String,
    val stadium: String,
    val lastScorer: String? = null,
    val isLiveApi: Boolean = false,
    val dayOffset: Int = 0,
    val dateLabel: String = "Hoje"
)

data class StandingRow(
    val position: Int,
    val teamName: String,
    val shortName: String,
    val colorHex: Long,
    val logoUrl: String = "",
    val played: Int,
    val won: Int,
    val drawn: Int,
    val lost: Int,
    val goalsFor: Int,
    val goalsAgainst: Int,
    val points: Int
) {
    val goalDifference: Int get() = goalsFor - goalsAgainst
}

data class Prediction(
    val matchId: String,
    val homeScore: Int,
    val awayScore: Int,
    val isJoker: Boolean = false
)

data class UserAccount(
    val id: String,
    val name: String,
    val email: String,
    val password: String,
    val avatarColorHex: Long = 0xFF00C853,
    val favoriteLeague: League = League.BRASILEIRAO,
    val avatarImagePath: String? = null
)

data class FriendBolao(
    val id: String,
    val name: String,
    val subtitle: String,
    val avatarColorHex: Long,
    val predictions: Map<String, Prediction>,
    val isCurrentUser: Boolean = false,
    val avatarImagePath: String? = null,
    val equippedFrameId: String? = null,
    val equippedTitle: String? = null
)

data class RoomState(
    val roomCode: String = ""
) {
    val isInRoom: Boolean
        get() = roomCode.trim().isNotEmpty()
}

data class GoalEvent(
    val id: Long,
    val match: Match,
    val scoringTeamName: String,
    val isHomeTeam: Boolean,
    val scorerName: String,
    val minute: Int
)

data class NotificationSettings(
    val enabledLeagues: Map<League, Boolean> = League.entries.associateWith { true }
) {
    fun isEnabled(league: League): Boolean = enabledLeagues[league] ?: true
}

enum class ShopCategory(val label: String) {
    AVATAR_FRAME("Molduras"),
    TITLE_BADGE("Títulos"),
    CHAT_FLAIR("Chat VIP")
}

data class ShopItem(
    val id: String,
    val name: String,
    val description: String,
    val price: Int,
    val category: ShopCategory,
    val accentHex: Long,
    val badgeText: String
)

data class UserShopState(
    val availablePoints: Int = 0,
    val ownedItemIds: Set<String> = emptySet(),
    val equippedFrameId: String? = null,
    val equippedTitleId: String? = null,
    val equippedFlairId: String? = null
)

object ShopCatalog {
    val items: List<ShopItem> = listOf(
        ShopItem(
            id = "frame_gold",
            name = "Moldura Ouro",
            description = "Brilho dourado de campeão ao redor do seu avatar.",
            price = 20,
            category = ShopCategory.AVATAR_FRAME,
            accentHex = 0xFFFFD600L,
            badgeText = "OURO"
        ),
        ShopItem(
            id = "frame_champions",
            name = "Moldura Champions",
            description = "Aro azul estelar inspirado nas noites europeias.",
            price = 35,
            category = ShopCategory.AVATAR_FRAME,
            accentHex = 0xFF38BDF8L,
            badgeText = "UCL"
        ),
        ShopItem(
            id = "frame_fire",
            name = "Moldura Fogo",
            description = "Borda flamejante para quem está em sequência quente.",
            price = 50,
            category = ShopCategory.AVATAR_FRAME,
            accentHex = 0xFFFF5722L,
            badgeText = "FOGO"
        ),
        ShopItem(
            id = "frame_legend",
            name = "Moldura Lenda",
            description = "Aro roxo lendário reservado aos mitos do bolão.",
            price = 80,
            category = ShopCategory.AVATAR_FRAME,
            accentHex = 0xFFA855F7L,
            badgeText = "LENDA"
        ),
        ShopItem(
            id = "title_profeta",
            name = "Profeta",
            description = "Título exibido ao lado do seu nome no chat.",
            price = 15,
            category = ShopCategory.TITLE_BADGE,
            accentHex = 0xFF10B981L,
            badgeText = "Profeta"
        ),
        ShopItem(
            id = "title_artilheiro",
            name = "Artilheiro",
            description = "Para quem acerta placares em sequência.",
            price = 30,
            category = ShopCategory.TITLE_BADGE,
            accentHex = 0xFFEF4444L,
            badgeText = "Artilheiro"
        ),
        ShopItem(
            id = "title_capitao",
            name = "Capitão do Grupo",
            description = "Liderança reconhecida na resenha da sala.",
            price = 45,
            category = ShopCategory.TITLE_BADGE,
            accentHex = 0xFF3B82F6L,
            badgeText = "Capitão"
        ),
        ShopItem(
            id = "title_rei_cravada",
            name = "Rei da Cravada",
            description = "O terror dos palpites exatos de 10 pontos.",
            price = 70,
            category = ShopCategory.TITLE_BADGE,
            accentHex = 0xFFF59E0BL,
            badgeText = "Rei da Cravada"
        ),
        ShopItem(
            id = "flair_golaco",
            name = "Destaque Golaço",
            description = "Seu balão no chat ganha destaque esmeralda.",
            price = 25,
            category = ShopCategory.CHAT_FLAIR,
            accentHex = 0xFF00E676L,
            badgeText = "GOLAÇO"
        ),
        ShopItem(
            id = "flair_vip",
            name = "Camarote VIP",
            description = "Balão VIP dourado exclusivo no bate-papo.",
            price = 60,
            category = ShopCategory.CHAT_FLAIR,
            accentHex = 0xFFFFD600L,
            badgeText = "VIP"
        )
    )

    fun findById(id: String?): ShopItem? =
        if (id.isNullOrBlank()) null else items.firstOrNull { it.id == id }
}

data class ChatMessage(
    val id: String,
    val senderId: String,
    val senderName: String,
    val senderAvatarColorHex: Long,
    val senderTitle: String? = null,
    val senderFlair: String? = null,
    val text: String,
    val timestampMillis: Long,
    val isCurrentUser: Boolean = false
)

data class DailyChallenge(
    val id: String,
    val title: String,
    val description: String,
    val rewardPoints: Int,
    val progress: Int,
    val target: Int,
    val claimed: Boolean
)

data class UserStats(
    val exactHits: Int = 0,
    val outcomeHits: Int = 0,
    val totalEvaluated: Int = 0,
    val currentStreak: Int = 0,
    val accuracyPercent: Int = 0
)

fun calculatePredictionPoints(match: Match, prediction: Prediction?): Int {
    if (prediction == null || match.status == MatchStatus.SCHEDULED) return 0
    val base = if (prediction.homeScore == match.homeScore && prediction.awayScore == match.awayScore) {
        10
    } else {
        val actualDiff = match.homeScore - match.awayScore
        val predDiff = prediction.homeScore - prediction.awayScore
        val sameOutcome = (actualDiff > 0 && predDiff > 0) ||
            (actualDiff < 0 && predDiff < 0) ||
            (actualDiff == 0 && predDiff == 0)
        if (!sameOutcome) 0 else if (actualDiff == predDiff) 7 else 5
    }
    return if (prediction.isJoker) base * 2 else base
}
