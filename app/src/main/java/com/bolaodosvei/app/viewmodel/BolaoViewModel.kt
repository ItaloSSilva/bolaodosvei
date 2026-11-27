package com.bolaodosvei.app.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bolaodosvei.app.data.AppRepository
import com.bolaodosvei.app.model.*
import com.bolaodosvei.app.notifications.MatchNotificationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BolaoUiState(
    val currentUser: UserAccount? = null,
    val matches: List<Match> = emptyList(),
    val standings: Map<League, List<StandingRow>> = emptyMap(),
    val userPredictions: Map<String, Prediction> = emptyMap(),
    val participants: List<FriendBolao> = emptyList(),
    val roomState: RoomState = RoomState(),
    val roomSyncStatus: String = "Entre ou crie uma sala usando um Código da Sala.",
    val isServerConnected: Boolean = false,
    val notificationSettings: NotificationSettings = NotificationSettings(),
    val selectedLeague: League = League.BRASILEIRAO,
    val activeGoalEvent: GoalEvent? = null,
    val inAppBanner: String? = null,
    val isRefreshing: Boolean = false,
    val lastUpdated: String = "Agora",
    val selectedThemeId: String = "default_black",
    val shopState: UserShopState = UserShopState(),
    val chatMessages: List<ChatMessage> = emptyList(),
    val upcomingMatches: List<Match> = emptyList(),
    val dailyChallenges: List<DailyChallenge> = emptyList(),
    val userStats: UserStats = UserStats()
) {
    val userTotalPoints: Int
        get() = matches.sumOf { calculatePredictionPoints(it, userPredictions[it.id]) }
}

class BolaoViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository(application)
    private val notificationManager = MatchNotificationManager(application)

    private val _uiState = MutableStateFlow(BolaoUiState())
    val uiState: StateFlow<BolaoUiState> = _uiState.asStateFlow()

    private var refreshJob: Job? = null
    private var syncJob: Job? = null
    private var chatPollingJob: Job? = null

    private val scorersByLeague = mapOf(
        League.BRASILEIRAO to listOf("Pedro", "Estêvão", "Luiz Henrique", "Lucas Moura", "Yuri Alberto", "Hulk"),
        League.PREMIER_LEAGUE to listOf("Saka", "Salah", "Haaland", "Cole Palmer", "Bruno Guimarães", "Ødegaard"),
        League.LA_LIGA to listOf("Vini Jr", "Mbappé", "Lamine Yamal", "Raphinha", "Griezmann", "Bellingham"),
        League.CHAMPIONS_LEAGUE to listOf("Mbappé", "Vini Jr", "Haaland", "Salah", "Kane", "Lamine Yamal"),
        League.LIBERTADORES to listOf("Luiz Henrique", "Pedro", "Estêvão", "Hulk", "Calleri", "Borja"),
        League.COPA_DO_BRASIL to listOf("Pedro", "Hulk", "Yuri Alberto", "Vegetti", "Luciano", "Paulinho"),
        League.SUL_AMERICANA to listOf("Matheus Pereira", "Yuri Alberto", "Borré", "Garro", "Alan Patrick", "Kaio Jorge")
    )

    init {
        val savedUser = repository.getCurrentUser()
        val initialMatches = emptyList<Match>()
        val initialStandings = League.entries.associateWith { repository.getDefaultStandings(it) }
        val preds = savedUser?.let { repository.loadPredictions(it.id) } ?: emptyMap()
        val notifSettings = repository.loadNotificationSettings()
        val savedRoom = repository.loadRoomCode()
        val hasServer = repository.hasSyncServer()
        val initialShop = savedUser?.let { repository.creditMatchPoints(it.id, initialMatches, preds) } ?: UserShopState()
        val initialChat = repository.loadLocalChat(savedRoom, savedUser?.id)

        _uiState.value = BolaoUiState(
            currentUser = savedUser,
            matches = initialMatches,
            standings = initialStandings,
            userPredictions = preds,
            participants = repository.buildLocalRealParticipants(savedUser, preds),
            roomState = RoomState(savedRoom),
            isServerConnected = hasServer,
            roomSyncStatus = buildRoomStatusText(savedRoom, hasServer, null),
            notificationSettings = notifSettings,
            lastUpdated = currentTimeString(),
            selectedThemeId = repository.loadSelectedThemeId(),
            shopState = initialShop,
            chatMessages = initialChat,
            upcomingMatches = initialMatches,
            dailyChallenges = computeDailyChallenges(savedUser?.id, initialMatches, preds),
            userStats = computeUserStats(initialMatches, preds)
        )

        refreshLiveData()
        startLiveEngine()
        if (savedRoom.isNotBlank() && hasServer) {
            refreshChat()
        }
    }

    private fun buildRoomStatusText(roomCode: String, hasServer: Boolean, count: Int?): String {
        if (roomCode.isBlank()) {
            return "Digite o Código da Sala dos seus amigos ou crie uma nova sala."
        }
        if (!hasServer) {
            return "Sala $roomCode ativa neste aparelho. Para sincronizar entre celulares diferentes, publique o servidor da pasta server/ e informe o endereço no APK."
        }
        return if (count != null) {
            "Sala $roomCode sincronizada às ${currentTimeString()} ($count participante${if (count == 1) "" else "s"})"
        } else {
            "Conectado à sala $roomCode"
        }
    }

    private fun currentTimeString(): String =
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

    fun refreshLiveData() {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            try {
                val previousMatches = _uiState.value.matches.associateBy { it.id }
                val allSchedule = repository.fetchScheduleMatches()
                val fetchedMatches = allSchedule.filter {
                    it.dayOffset == 0 || it.status == MatchStatus.LIVE || it.status == MatchStatus.HALFTIME
                }
                val fetchedStandings = repository.fetchStandings()

                fetchedMatches.forEach { updated ->
                    val prev = previousMatches[updated.id]
                    if (prev != null) {
                        if (prev.status == MatchStatus.SCHEDULED && updated.status == MatchStatus.LIVE) {
                            triggerMatchStartedNotification(updated)
                        }
                        if (updated.homeScore > prev.homeScore) {
                            emitGoalEvent(updated, isHome = true, scorer = updated.lastScorer ?: "Gol do Mandante")
                        } else if (updated.awayScore > prev.awayScore) {
                            emitGoalEvent(updated, isHome = false, scorer = updated.lastScorer ?: "Gol do Visitante")
                        }
                    }
                }

                _uiState.update { state ->
                    val updatedShop = state.currentUser?.let { user ->
                        repository.creditMatchPoints(user.id, fetchedMatches, state.userPredictions)
                    } ?: state.shopState
                    state.copy(
                        matches = fetchedMatches,
                        upcomingMatches = allSchedule,
                        standings = fetchedStandings,
                        isRefreshing = false,
                        lastUpdated = currentTimeString(),
                        shopState = updatedShop,
                        dailyChallenges = computeDailyChallenges(state.currentUser?.id, fetchedMatches, state.userPredictions),
                        userStats = computeUserStats(allSchedule, state.userPredictions)
                    )
                }
                syncRoomNow()
            } finally {
                _uiState.update { if (it.isRefreshing) it.copy(isRefreshing = false) else it }
            }
        }
    }

    private fun startLiveEngine() {
        viewModelScope.launch {
            var tick = 0
            while (true) {
                delay(10_000L)
                tick++
                _uiState.update { state ->
                    val hasSimulatedLive = state.matches.any { it.status == MatchStatus.LIVE && !it.isLiveApi }
                    if (!hasSimulatedLive) {
                        state
                    } else {
                        val updated = state.matches.map { match ->
                            if (match.status == MatchStatus.LIVE && !match.isLiveApi) {
                                val nextMin = if (match.minute >= 90) 1 else match.minute + 1
                                match.copy(minute = nextMin)
                            } else {
                                match
                            }
                        }
                        state.copy(matches = updated)
                    }
                }
                if (tick % 6 == 0) {
                    refreshLiveData()
                } else if (tick % 3 == 0 && _uiState.value.roomState.isInRoom && repository.hasSyncServer()) {
                    syncRoomNow()
                }
            }
        }
    }

    // --- Room Code Management (Friends Only Need the Room Code!) ---

    fun createNewRoom() {
        val code = repository.generateRoomCode()
        joinRoom(code)
    }

    fun joinRoom(rawCode: String) {
        val clean = repository.saveRoomCode(rawCode)
        val hasServer = repository.hasSyncServer()
        _uiState.update {
            it.copy(
                roomState = RoomState(clean),
                roomSyncStatus = buildRoomStatusText(clean, hasServer, null),
                chatMessages = repository.loadLocalChat(clean, it.currentUser?.id)
            )
        }
        viewModelScope.launch {
            if (clean.isNotEmpty()) {
                val snap = _uiState.value
                val user = snap.currentUser
                if (user != null && hasServer) {
                    repository.pushUserBolaoToRoom(
                        clean,
                        user,
                        snap.userPredictions,
                        snap.shopState.equippedFrameId,
                        ShopCatalog.findById(snap.shopState.equippedTitleId)?.badgeText
                    )
                }
                syncRoomNow()
                refreshChat()
                showBanner("Você entrou na sala $clean")
            } else {
                _uiState.update { state ->
                    state.copy(
                        participants = repository.buildLocalRealParticipants(
                            state.currentUser,
                            state.userPredictions
                        )
                    )
                }
                showBanner("Você saiu da sala")
            }
        }
    }

    fun leaveRoom() {
        joinRoom("")
    }

    fun syncRoomNow() {
        syncJob?.cancel()
        syncJob = viewModelScope.launch {
            val state = _uiState.value
            val room = state.roomState.roomCode
            val hasServer = repository.hasSyncServer()

            if (room.isBlank() || !hasServer) {
                _uiState.update { cur ->
                    if (cur.roomState.roomCode != room) return@update cur
                    cur.copy(
                        participants = repository.buildLocalRealParticipants(
                            cur.currentUser,
                            cur.userPredictions
                        ),
                        roomSyncStatus = buildRoomStatusText(room, hasServer, null)
                    )
                }
                return@launch
            }

            val remoteList = repository.fetchRoomParticipants(room, state.currentUser?.id)
            _uiState.update { cur ->
                if (cur.roomState.roomCode != room) return@update cur
                if (remoteList != null) {
                    val merged = remoteList.toMutableList()
                    val me = cur.currentUser
                    if (me != null && merged.none { it.id == me.id }) {
                        merged.add(
                            FriendBolao(
                                id = me.id,
                                name = "${me.name} (Você)",
                                subtitle = "Sala $room • ${me.favoriteLeague.shortName}",
                                avatarColorHex = me.avatarColorHex,
                                predictions = cur.userPredictions,
                                isCurrentUser = true
                            )
                        )
                    }
                    cur.copy(
                        participants = merged,
                        roomSyncStatus = buildRoomStatusText(room, true, merged.size)
                    )
                } else {
                    cur.copy(roomSyncStatus = "Sem conexão com o servidor da sala $room. Tente novamente.")
                }
            }
        }
    }

    // --- Authentication ---

    fun login(email: String, password: String): Result<UserAccount> {
        val result = repository.loginUser(email, password)
        result.onSuccess { user -> onUserAuthenticated(user) }
        return result
    }

    fun register(
        name: String,
        email: String,
        password: String,
        avatarColorHex: Long,
        favoriteLeague: League
    ): Result<UserAccount> {
        val result = repository.registerUser(name, email, password, avatarColorHex, favoriteLeague)
        result.onSuccess { user -> onUserAuthenticated(user) }
        return result
    }

    private fun onUserAuthenticated(user: UserAccount) {
        val preds = repository.loadPredictions(user.id)
        val room = _uiState.value.roomState.roomCode
        val updatedShop = repository.creditMatchPoints(user.id, _uiState.value.matches, preds)
        val localChat = repository.loadLocalChat(room, user.id)
        _uiState.update { state ->
            state.copy(
                currentUser = user,
                userPredictions = preds,
                participants = repository.buildLocalRealParticipants(user, preds),
                shopState = updatedShop,
                chatMessages = localChat,
                dailyChallenges = computeDailyChallenges(user.id, state.matches, preds),
                userStats = computeUserStats(state.upcomingMatches.ifEmpty { state.matches }, preds)
            )
        }
        viewModelScope.launch {
            if (room.isNotBlank() && repository.hasSyncServer()) {
                repository.pushUserBolaoToRoom(
                    room,
                    user,
                    preds,
                    updatedShop.equippedFrameId,
                    ShopCatalog.findById(updatedShop.equippedTitleId)?.badgeText
                )
                syncRoomNow()
                refreshChat()
            }
        }
    }

    fun logout() {
        repository.logout()
        _uiState.update { state ->
            state.copy(
                currentUser = null,
                userPredictions = emptyMap(),
                participants = repository.buildLocalRealParticipants(null, emptyMap()),
                shopState = UserShopState(),
                chatMessages = repository.loadLocalChat(state.roomState.roomCode, null),
                dailyChallenges = computeDailyChallenges(null, state.matches, emptyMap()),
                userStats = UserStats()
            )
        }
    }

    fun updateProfile(name: String, avatarColorHex: Long, favoriteLeague: League): Result<UserAccount> {
        val current = _uiState.value.currentUser
            ?: return Result.failure(IllegalStateException("Nenhum usuário conectado."))
        val result = repository.updateUserProfile(current.id, name, avatarColorHex, favoriteLeague)
        result.onSuccess { updatedUser ->
            _uiState.update { state ->
                state.copy(
                    currentUser = updatedUser,
                    participants = state.participants.map { p ->
                        if (p.isCurrentUser) p.copy(
                            name = "${updatedUser.name} (Você)",
                            avatarColorHex = updatedUser.avatarColorHex
                        ) else p
                    }
                )
            }
            showBanner("Perfil atualizado com sucesso")
            viewModelScope.launch {
                val room = _uiState.value.roomState.roomCode
                if (room.isNotBlank() && repository.hasSyncServer()) {
                    repository.pushUserBolaoToRoom(room, updatedUser, _uiState.value.userPredictions)
                    syncRoomNow()
                }
            }
        }
        return result
    }

    fun updateProfilePhoto(uri: Uri) {
        val current = _uiState.value.currentUser ?: return
        viewModelScope.launch {
            val savedPath = withContext(Dispatchers.IO) {
                repository.saveProfilePhotoFromUri(current.id, uri)
            }
            if (savedPath == null) {
                showBanner("Não foi possível carregar a foto selecionada.")
                return@launch
            }
            val updated = repository.updateUserAvatarPhoto(current.id, savedPath) ?: return@launch
            _uiState.update { state ->
                state.copy(
                    currentUser = updated,
                    participants = state.participants.map { p ->
                        if (p.isCurrentUser) p.copy(avatarImagePath = savedPath) else p
                    }
                )
            }
            showBanner("Foto de perfil atualizada!")
        }
    }

    fun removeProfilePhoto() {
        val current = _uiState.value.currentUser ?: return
        val updated = repository.updateUserAvatarPhoto(current.id, null) ?: return
        _uiState.update { state ->
            state.copy(
                currentUser = updated,
                participants = state.participants.map { p ->
                    if (p.isCurrentUser) p.copy(avatarImagePath = null) else p
                }
            )
        }
        showBanner("Foto de perfil removida")
    }

    // --- Points Shop ---

    fun buyShopItem(itemId: String) {
        val user = _uiState.value.currentUser ?: return
        val item = ShopCatalog.findById(itemId) ?: return
        val result = repository.purchaseShopItem(user.id, item)
        result.onSuccess { newShop ->
            _uiState.update { it.copy(shopState = newShop) }
            showBanner("Item adquirido: ${item.name}!")
        }.onFailure { err ->
            showBanner(err.message ?: "Pontos insuficientes.")
        }
    }

    fun equipShopItem(itemId: String) {
        val user = _uiState.value.currentUser ?: return
        val item = ShopCatalog.findById(itemId) ?: return
        val newShop = repository.equipShopItem(user.id, item)
        _uiState.update { it.copy(shopState = newShop) }
        showBanner("Equipado: ${item.name}")
        viewModelScope.launch {
            val snap = _uiState.value
            val room = snap.roomState.roomCode
            if (room.isNotBlank() && repository.hasSyncServer()) {
                repository.pushUserBolaoToRoom(
                    room,
                    user,
                    snap.userPredictions,
                    newShop.equippedFrameId,
                    ShopCatalog.findById(newShop.equippedTitleId)?.badgeText
                )
            }
        }
    }

    fun claimDailyChallenge(challengeId: String) {
        val state = _uiState.value
        val user = state.currentUser ?: return
        val challenge = state.dailyChallenges.firstOrNull { it.id == challengeId } ?: return
        if (challenge.claimed || challenge.progress < challenge.target) return
        repository.markChallengeClaimedToday(user.id, challengeId)
        val newShop = repository.creditBonusPoints(user.id, challengeId, challenge.rewardPoints)
        _uiState.update { cur ->
            cur.copy(
                shopState = newShop,
                dailyChallenges = computeDailyChallenges(user.id, cur.matches, cur.userPredictions)
            )
        }
        showBanner("Desafio resgatado: +${challenge.rewardPoints} pts!")
    }

    private fun computeUserStats(allMatches: List<Match>, predictions: Map<String, Prediction>): UserStats {
        val evaluated = allMatches.distinctBy { it.id }
            .filter { it.status != MatchStatus.SCHEDULED && predictions[it.id] != null }
        if (evaluated.isEmpty()) return UserStats()
        var exactHits = 0
        var outcomeHits = 0
        var currentStreak = 0
        var streakBroken = false
        evaluated.reversed().forEach { m ->
            val pred = predictions[m.id]
            val pts = calculatePredictionPoints(m, pred)
            val basePts = if (pred?.isJoker == true) pts / 2 else pts
            if (basePts == 10) exactHits++
            if (basePts >= 5) {
                outcomeHits++
                if (!streakBroken) currentStreak++
            } else {
                streakBroken = true
            }
        }
        val accuracy = (outcomeHits * 100) / evaluated.size
        return UserStats(
            exactHits = exactHits,
            outcomeHits = outcomeHits,
            totalEvaluated = evaluated.size,
            currentStreak = currentStreak,
            accuracyPercent = accuracy
        )
    }

    private fun computeDailyChallenges(
        userId: String?,
        todayMatches: List<Match>,
        predictions: Map<String, Prediction>
    ): List<DailyChallenge> {
        val claimed = if (userId != null) repository.loadClaimedChallengesToday(userId) else emptySet()
        val todayIds = todayMatches.map { it.id }.toSet()
        val todayPredCount = predictions.keys.count { it in todayIds }
        val sentChat = userId != null && repository.hasSentChatToday(userId)
        val hitToday = todayMatches.count { m ->
            m.status != MatchStatus.SCHEDULED && calculatePredictionPoints(m, predictions[m.id]) >= 5
        }
        return listOf(
            DailyChallenge(
                id = "pred_3_today",
                title = "Palpite em 3 jogos hoje",
                description = "Registre palpites em pelo menos 3 partidas de hoje.",
                rewardPoints = 10,
                progress = todayPredCount.coerceAtMost(3),
                target = 3,
                claimed = "pred_3_today" in claimed
            ),
            DailyChallenge(
                id = "send_chat",
                title = "Mande uma mensagem no chat",
                description = "Participe da resenha enviando 1 mensagem no grupo.",
                rewardPoints = 5,
                progress = if (sentChat) 1 else 0,
                target = 1,
                claimed = "send_chat" in claimed
            ),
            DailyChallenge(
                id = "hit_1_result",
                title = "Acerte 1 resultado ou cravada",
                description = "Pontue em qualquer partida de hoje.",
                rewardPoints = 15,
                progress = if (hitToday > 0) 1 else 0,
                target = 1,
                claimed = "hit_1_result" in claimed
            )
        )
    }

    // --- Chat / Bate-Papo ---

    fun refreshChat() {
        viewModelScope.launch {
            val state = _uiState.value
            val room = state.roomState.roomCode
            val userId = state.currentUser?.id
            if (room.isBlank() || !repository.hasSyncServer()) {
                _uiState.update { cur ->
                    if (cur.roomState.roomCode != room) return@update cur
                    cur.copy(chatMessages = repository.loadLocalChat(room, cur.currentUser?.id))
                }
                return@launch
            }
            val remote = repository.fetchRoomChat(room, userId)
            if (remote != null) {
                repository.saveLocalChat(room, remote)
                _uiState.update { cur ->
                    if (cur.roomState.roomCode != room) return@update cur
                    cur.copy(
                        chatMessages = remote.map { it.copy(isCurrentUser = it.senderId == cur.currentUser?.id) }
                    )
                }
            }
        }
    }

    fun sendChatMessage(rawText: String) {
        val text = rawText.trim().take(500)
        if (text.isEmpty()) return
        val state = _uiState.value
        val user = state.currentUser ?: return
        val room = state.roomState.roomCode
        val title = ShopCatalog.findById(state.shopState.equippedTitleId)?.badgeText
        val flair = ShopCatalog.findById(state.shopState.equippedFlairId)?.badgeText
        val msg = ChatMessage(
            id = "msg_${user.id}_${System.currentTimeMillis()}",
            senderId = user.id,
            senderName = user.name,
            senderAvatarColorHex = user.avatarColorHex,
            senderTitle = title,
            senderFlair = flair,
            text = text,
            timestampMillis = System.currentTimeMillis(),
            isCurrentUser = true
        )
        val updatedList = (state.chatMessages + msg).takeLast(100)
        repository.saveLocalChat(room, updatedList)
        repository.markChatSentToday(user.id)
        _uiState.update { cur ->
            cur.copy(
                chatMessages = updatedList,
                dailyChallenges = computeDailyChallenges(user.id, cur.matches, cur.userPredictions)
            )
        }

        if (room.isNotBlank() && repository.hasSyncServer()) {
            viewModelScope.launch {
                val ok = repository.pushRoomChatMessage(room, msg)
                if (ok) refreshChat()
            }
        }
    }

    fun sendQuickReaction(reactionText: String) {
        sendChatMessage(reactionText)
    }

    fun setChatScreenVisible(visible: Boolean) {
        if (visible) {
            if (chatPollingJob?.isActive == true) return
            chatPollingJob = viewModelScope.launch {
                while (true) {
                    refreshChat()
                    delay(5_000L)
                }
            }
        } else {
            chatPollingJob?.cancel()
            chatPollingJob = null
        }
    }

    fun selectTheme(themeId: String, themeName: String) {
        repository.saveSelectedThemeId(themeId)
        _uiState.update { it.copy(selectedThemeId = themeId) }
        showBanner("Tema aplicado: $themeName")
    }

    // --- League & Predictions ---

    fun selectLeague(league: League) {
        _uiState.update { it.copy(selectedLeague = league) }
    }

    fun savePrediction(matchId: String, homeScore: Int, awayScore: Int, isJoker: Boolean = false) {
        val stateSnap = _uiState.value
        val user = stateSnap.currentUser ?: return
        val allKnownMatches = (stateSnap.upcomingMatches + stateSnap.matches).distinctBy { it.id }
        val targetMatch = allKnownMatches.firstOrNull { it.id == matchId }
        if (targetMatch != null && targetMatch.status != MatchStatus.SCHEDULED) {
            showBanner("Palpite travado: o jogo já começou!")
            return
        }

        val clampedHome = homeScore.coerceIn(0, 15)
        val clampedAway = awayScore.coerceIn(0, 15)
        val targetDayOffset = targetMatch?.dayOffset ?: 0
        val sameDayIds = allKnownMatches.filter { it.dayOffset == targetDayOffset }.map { it.id }.toSet()

        val updatedPreds = stateSnap.userPredictions.toMutableMap().apply {
            if (isJoker) {
                sameDayIds.forEach { id ->
                    val existing = this[id]
                    if (existing != null && existing.isJoker && id != matchId) {
                        this[id] = existing.copy(isJoker = false)
                    }
                }
            }
            put(matchId, Prediction(matchId, clampedHome, clampedAway, isJoker))
        }
        repository.savePredictions(user.id, updatedPreds)
        _uiState.update { state ->
            val updatedShop = repository.creditMatchPoints(user.id, state.matches, updatedPreds)
            state.copy(
                userPredictions = updatedPreds,
                shopState = updatedShop,
                dailyChallenges = computeDailyChallenges(user.id, state.matches, updatedPreds),
                userStats = computeUserStats(state.upcomingMatches.ifEmpty { state.matches }, updatedPreds),
                participants = if (state.roomState.isInRoom && repository.hasSyncServer()) {
                    state.participants.map { p ->
                        if (p.isCurrentUser) p.copy(predictions = updatedPreds) else p
                    }
                } else {
                    repository.buildLocalRealParticipants(user, updatedPreds)
                }
            )
        }
        showBanner(
            if (isJoker) "Palpite Coringa 2x salvo: $clampedHome x $clampedAway"
            else "Palpite salvo: $clampedHome x $clampedAway"
        )

        viewModelScope.launch {
            val snap = _uiState.value
            val room = snap.roomState.roomCode
            if (room.isNotBlank() && repository.hasSyncServer()) {
                val ok = repository.pushUserBolaoToRoom(
                    room,
                    user,
                    updatedPreds,
                    snap.shopState.equippedFrameId,
                    ShopCatalog.findById(snap.shopState.equippedTitleId)?.badgeText
                )
                if (ok) syncRoomNow()
            }
        }
    }

    fun toggleJoker(matchId: String) {
        val stateSnap = _uiState.value
        val targetMatch = (stateSnap.upcomingMatches + stateSnap.matches).firstOrNull { it.id == matchId }
        if (targetMatch != null && targetMatch.status != MatchStatus.SCHEDULED) {
            showBanner("Palpite travado: o jogo já começou!")
            return
        }
        val existing = stateSnap.userPredictions[matchId]
        if (existing == null) {
            showBanner("Salve um placar antes de ativar o Coringa 2x!")
            return
        }
        savePrediction(matchId, existing.homeScore, existing.awayScore, !existing.isJoker)
    }

    // --- Notifications ---

    fun toggleLeagueNotification(league: League, enabled: Boolean) {
        repository.saveNotificationToggle(league, enabled)
        _uiState.update { state ->
            val updatedMap = state.notificationSettings.enabledLeagues.toMutableMap().apply {
                put(league, enabled)
            }
            state.copy(notificationSettings = NotificationSettings(updatedMap))
        }
        showBanner(
            if (enabled) "Notificações ativadas para ${league.displayName}"
            else "Notificações silenciadas para ${league.displayName}"
        )
    }

    private fun triggerMatchStartedNotification(match: Match) {
        val settings = _uiState.value.notificationSettings
        notificationManager.notifyMatchStarted(match, settings)
        if (settings.isEnabled(match.league)) {
            showBanner("Bola rolando (${match.league.shortName}): ${match.homeTeam.shortName} x ${match.awayTeam.shortName}")
        }
    }

    fun simulateMatchStart(league: League = _uiState.value.selectedLeague) {
        val target = _uiState.value.matches.firstOrNull {
            it.league == league && it.status == MatchStatus.SCHEDULED
        } ?: _uiState.value.matches.firstOrNull { it.league == league } ?: return

        val startedMatch = target.copy(status = MatchStatus.LIVE, minute = 1)
        _uiState.update { state ->
            val updatedList = state.matches.map { if (it.id == target.id) startedMatch else it }
            state.copy(matches = updatedList)
        }

        val settings = _uiState.value.notificationSettings
        if (settings.isEnabled(league)) {
            notificationManager.notifyMatchStarted(startedMatch, settings)
            showBanner("Jogo iniciado (${league.displayName}): ${startedMatch.homeTeam.name} x ${startedMatch.awayTeam.name}")
        } else {
            showBanner("Jogo iniciado, mas alertas de ${league.shortName} estão desligados")
        }
    }

    fun simulateGoal(matchId: String? = null) {
        val state = _uiState.value
        val target = (matchId?.let { id -> state.matches.firstOrNull { it.id == id } }
            ?: state.matches.firstOrNull { it.status == MatchStatus.LIVE }
            ?: state.matches.firstOrNull()) ?: return

        val isHome = (System.currentTimeMillis() % 2L == 0L)
        val scorers = scorersByLeague[target.league] ?: listOf("Craque")
        val scorer = scorers[(System.currentTimeMillis() % scorers.size).toInt()]
        val newMinute = if (target.minute == 0) 12 else (target.minute + 1).coerceAtMost(90)

        val updatedMatch = target.copy(
            status = MatchStatus.LIVE,
            minute = newMinute,
            homeScore = if (isHome) target.homeScore + 1 else target.homeScore,
            awayScore = if (!isHome) target.awayScore + 1 else target.awayScore,
            lastScorer = scorer
        )

        _uiState.update { cur ->
            val updatedMatches = cur.matches.map { if (it.id == target.id) updatedMatch else it }
            cur.copy(
                matches = updatedMatches,
                lastUpdated = currentTimeString()
            )
        }

        emitGoalEvent(updatedMatch, isHome, scorer)
    }

    private fun emitGoalEvent(match: Match, isHome: Boolean, scorer: String) {
        val event = GoalEvent(
            id = System.currentTimeMillis(),
            match = match,
            scoringTeamName = if (isHome) match.homeTeam.name else match.awayTeam.name,
            isHomeTeam = isHome,
            scorerName = scorer,
            minute = match.minute.coerceAtLeast(1)
        )
        notificationManager.notifyGoalScored(event, _uiState.value.notificationSettings)
        _uiState.update { it.copy(activeGoalEvent = event) }
    }

    fun dismissGoalAnimation() {
        _uiState.update { it.copy(activeGoalEvent = null) }
    }

    fun dismissBanner() {
        _uiState.update { it.copy(inAppBanner = null) }
    }

    private fun showBanner(message: String) {
        _uiState.update { it.copy(inAppBanner = message) }
        viewModelScope.launch {
            delay(4000L)
            _uiState.update { if (it.inAppBanner == message) it.copy(inAppBanner = null) else it }
        }
    }
}
