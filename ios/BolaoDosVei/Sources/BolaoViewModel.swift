import Foundation
import SwiftUI

@MainActor
final class BolaoViewModel: ObservableObject {
    private let repository = AppRepository()

    // Auth & Profile
    @Published var currentUser: UserAccount?
    @Published var authError: String?

    // Matches & Standings
    @Published var matches: [Match] = []
    @Published var upcomingMatches: [Match] = []
    @Published var standingsByLeague: [League: [StandingRow]] = [:]
    @Published var selectedLeague: League? = nil
    @Published var selectedStandingsLeague: League = .brasileirao
    @Published var selectedDayFilter: MatchDayFilter = .today
    @Published var isRefreshingMatches: Bool = false

    // Predictions & Stats
    @Published var predictions: [String: Prediction] = [:]
    @Published var userStats: UserStats = UserStats()

    // Shop & Daily Challenges
    @Published var shopState: UserShopState = UserShopState()
    @Published var dailyChallenges: [DailyChallenge] = []

    // Room & Friends
    @Published var roomCode: String = ""
    @Published var friends: [FriendBolao] = []
    @Published var isSyncingRoom: Bool = false
    @Published var syncStatusText: String?

    // Chat
    @Published var chatMessages: [ChatMessage] = []
    @Published var isSendingChat: Bool = false
    private var chatPollingTask: Task<Void, Never>?

    // Feedback Banner
    @Published var bannerMessage: String?
    private var bannerDismissTask: Task<Void, Never>?

    init() {
        bootstrap()
    }

    private func bootstrap() {
        let user = repository.getCurrentUser()
        let savedRoom = repository.loadRoomCode()
        currentUser = user
        roomCode = savedRoom
        selectedStandingsLeague = user?.favoriteLeague ?? .brasileirao

        let initialMatches = repository.defaultMatches()
        matches = initialMatches.filter { $0.dayOffset == 0 || $0.isLiveNow }
        upcomingMatches = initialMatches

        var initialStandings: [League: [StandingRow]] = [:]
        for league in League.allCases {
            initialStandings[league] = repository.defaultStandings(for: league)
        }
        standingsByLeague = initialStandings

        if let user = user {
            predictions = repository.loadPredictions(userId: user.id)
            shopState = repository.loadShopState(userId: user.id)
        }
        chatMessages = repository.loadLocalChat(roomCode: savedRoom, currentUserId: user?.id)
        rebuildFriendsAndStats()

        Task {
            await refreshLiveData()
            if !savedRoom.isEmpty {
                await syncRoom()
            }
        }
    }

    // MARK: - Banner Helper

    func showBanner(_ text: String) {
        bannerMessage = text
        bannerDismissTask?.cancel()
        bannerDismissTask = Task {
            try? await Task.sleep(nanoseconds: 3_000_000_000)
            if !Task.isCancelled {
                bannerMessage = nil
            }
        }
    }

    // MARK: - Auth

    func login(email: String, password: String) {
        authError = nil
        switch repository.loginUser(email: email, password: password) {
        case .success(let user):
            currentUser = user
            selectedStandingsLeague = user.favoriteLeague
            predictions = repository.loadPredictions(userId: user.id)
            shopState = repository.loadShopState(userId: user.id)
            chatMessages = repository.loadLocalChat(roomCode: roomCode, currentUserId: user.id)
            rebuildFriendsAndStats()
            Task { await syncRoom() }
        case .failure(let err):
            authError = err.localizedDescription
        }
    }

    func register(
        name: String,
        email: String,
        password: String,
        avatarColorHex: UInt64,
        favoriteLeague: League
    ) {
        authError = nil
        switch repository.registerUser(
            name: name,
            email: email,
            password: password,
            avatarColorHex: avatarColorHex,
            favoriteLeague: favoriteLeague
        ) {
        case .success(let user):
            currentUser = user
            selectedStandingsLeague = user.favoriteLeague
            predictions = [:]
            shopState = repository.loadShopState(userId: user.id)
            chatMessages = repository.loadLocalChat(roomCode: roomCode, currentUserId: user.id)
            rebuildFriendsAndStats()
            Task { await syncRoom() }
        case .failure(let err):
            authError = err.localizedDescription
        }
    }

    func logout() {
        chatPollingTask?.cancel()
        repository.logout()
        currentUser = nil
        predictions = [:]
        shopState = UserShopState()
        friends = []
        authError = nil
    }

    func updateProfile(name: String, colorHex: UInt64, favoriteLeague: League) {
        guard let user = currentUser else { return }
        switch repository.updateUserProfile(
            userId: user.id,
            newName: name,
            newColorHex: colorHex,
            newFavoriteLeague: favoriteLeague
        ) {
        case .success(let updated):
            currentUser = updated
            rebuildFriendsAndStats()
            showBanner("Perfil atualizado!")
            Task { await syncRoom() }
        case .failure(let err):
            showBanner(err.localizedDescription)
        }
    }

    func saveAvatarPhoto(data: Data) {
        guard let user = currentUser else { return }
        guard let path = repository.saveProfilePhotoData(userId: user.id, imageData: data),
              let updated = repository.updateUserAvatarPhoto(userId: user.id, photoPath: path) else {
            showBanner("Não foi possível salvar a foto.")
            return
        }
        currentUser = updated
        rebuildFriendsAndStats()
        showBanner("Foto de perfil atualizada!")
    }

    // MARK: - Live Matches & Standings

    func refreshLiveData() async {
        isRefreshingMatches = true
        async let fetchedSchedule = repository.fetchScheduleMatches()
        async let fetchedStandings = repository.fetchStandings()

        let schedule = await fetchedSchedule
        let standings = await fetchedStandings

        if !schedule.isEmpty {
            upcomingMatches = schedule
            let todayOnly = schedule.filter { $0.dayOffset == 0 || $0.isLiveNow }
            matches = todayOnly.isEmpty ? schedule : todayOnly
        }
        if !standings.isEmpty {
            standingsByLeague = standings
        }

        if let user = currentUser {
            shopState = repository.creditMatchPoints(userId: user.id, matches: upcomingMatches, predictions: predictions)
        }
        rebuildFriendsAndStats()
        isRefreshingMatches = false
    }

    // MARK: - Predictions & Coringa 2x

    func savePrediction(matchId: String, homeScore: Int, awayScore: Int, isJoker: Bool? = nil) {
        guard let user = currentUser else { return }
        let allKnown = upcomingMatches + matches
        if let match = allKnown.first(where: { $0.id == matchId }), match.status != .scheduled {
            showBanner("Palpite travado: o jogo já começou!")
            return
        }

        var updated = predictions
        let targetDayOffset = allKnown.first(where: { $0.id == matchId })?.dayOffset ?? 0
        let finalJoker = isJoker ?? (updated[matchId]?.isJoker ?? false)

        if finalJoker {
            // Enforce max 1 Coringa 2x per dayOffset
            for (otherId, otherPred) in updated where otherId != matchId && otherPred.isJoker {
                let otherDay = allKnown.first(where: { $0.id == otherId })?.dayOffset ?? 0
                if otherDay == targetDayOffset {
                    updated[otherId] = Prediction(
                        matchId: otherId,
                        homeScore: otherPred.homeScore,
                        awayScore: otherPred.awayScore,
                        isJoker: false
                    )
                }
            }
        }

        updated[matchId] = Prediction(
            matchId: matchId,
            homeScore: max(0, homeScore),
            awayScore: max(0, awayScore),
            isJoker: finalJoker
        )
        predictions = updated
        repository.savePredictions(userId: user.id, predictions: updated)
        rebuildFriendsAndStats()

        if !roomCode.isEmpty {
            Task {
                _ = await repository.pushUserBolaoToRoom(
                    roomCode: roomCode,
                    user: user,
                    predictions: updated,
                    shopState: shopState
                )
            }
        }
    }

    func toggleJoker(matchId: String) {
        let allKnown = upcomingMatches + matches
        guard let match = allKnown.first(where: { $0.id == matchId }), match.status == .scheduled else {
            showBanner("Palpite travado: o jogo já começou!")
            return
        }
        let existing = predictions[matchId]
        let nextJoker = !(existing?.isJoker ?? false)
        savePrediction(
            matchId: matchId,
            homeScore: existing?.homeScore ?? 0,
            awayScore: existing?.awayScore ?? 0,
            isJoker: nextJoker
        )
        showBanner(nextJoker ? "Coringa 2x ativado neste jogo!" : "Coringa 2x removido.")
    }

    // MARK: - Shop & Daily Challenges

    func purchaseShopItem(_ item: ShopItem) {
        guard let user = currentUser else { return }
        switch repository.purchaseShopItem(userId: user.id, item: item) {
        case .success(let newState):
            shopState = newState
            rebuildFriendsAndStats()
            showBanner("\(item.name) desbloqueado!")
            if !roomCode.isEmpty {
                Task { await syncRoom() }
            }
        case .failure(let err):
            showBanner(err.localizedDescription)
        }
    }

    func equipShopItem(_ item: ShopItem) {
        guard let user = currentUser else { return }
        shopState = repository.equipShopItem(userId: user.id, item: item)
        rebuildFriendsAndStats()
        showBanner("\(item.name) equipado!")
        if !roomCode.isEmpty {
            Task { await syncRoom() }
        }
    }

    func claimDailyChallenge(_ challengeId: String) {
        guard let user = currentUser,
              let challenge = dailyChallenges.first(where: { $0.id == challengeId }),
              challenge.isCompleted, !challenge.claimed else {
            return
        }
        shopState = repository.markChallengeClaimed(
            userId: user.id,
            challengeId: challengeId,
            rewardPoints: challenge.rewardPoints
        )
        rebuildFriendsAndStats()
        showBanner("+\(challenge.rewardPoints) pts resgatados!")
    }

    // MARK: - Room Sync

    func joinOrCreateRoom(_ rawCode: String) {
        let clean = repository.saveRoomCode(rawCode)
        roomCode = clean
        chatMessages = repository.loadLocalChat(roomCode: clean, currentUserId: currentUser?.id)
        if clean.isEmpty {
            syncStatusText = nil
            rebuildFriendsAndStats()
            return
        }
        Task { await syncRoom() }
    }

    func createNewRoom() {
        joinOrCreateRoom(repository.generateRoomCode())
    }

    func leaveRoom() {
        _ = repository.saveRoomCode("")
        roomCode = ""
        syncStatusText = nil
        chatMessages = repository.loadLocalChat(roomCode: "", currentUserId: currentUser?.id)
        rebuildFriendsAndStats()
    }

    func syncRoom() async {
        guard !roomCode.isEmpty else {
            rebuildFriendsAndStats()
            return
        }
        isSyncingRoom = true
        if let user = currentUser {
            _ = await repository.pushUserBolaoToRoom(
                roomCode: roomCode,
                user: user,
                predictions: predictions,
                shopState: shopState
            )
        }
        if let fetchedFriends = await repository.fetchRoomParticipants(roomCode: roomCode, currentUserId: currentUser?.id) {
            friends = mergeFriendsWithCurrentUser(fetchedFriends)
            syncStatusText = "Sala \(roomCode) sincronizada (\(friends.count) participantes)"
        } else {
            syncStatusText = "Sem conexão com o servidor — exibindo dados locais"
            rebuildFriendsAndStats()
        }
        await refreshChat()
        isSyncingRoom = false
    }

    private func mergeFriendsWithCurrentUser(_ remote: [FriendBolao]) -> [FriendBolao] {
        var result = remote
        if let user = currentUser, !result.contains(where: { $0.id == user.id }) {
            result.append(
                FriendBolao(
                    id: user.id,
                    name: "\(user.name) (Você)",
                    subtitle: "Sala \(roomCode) • \(user.favoriteLeague.shortName)",
                    avatarColorHex: user.avatarColorHex,
                    predictions: predictions,
                    isCurrentUser: true,
                    avatarImagePath: user.avatarImagePath,
                    equippedFrameId: shopState.equippedFrameId,
                    equippedTitle: ShopCatalog.findById(shopState.equippedTitleId)?.badgeText
                )
            )
        }
        return result
    }

    // MARK: - Chat

    func setChatScreenVisible(_ visible: Bool) {
        chatPollingTask?.cancel()
        chatPollingTask = nil
        guard visible else { return }
        chatPollingTask = Task {
            while !Task.isCancelled {
                await refreshChat()
                try? await Task.sleep(nanoseconds: 5_000_000_000)
            }
        }
    }

    func refreshChat() async {
        guard !roomCode.isEmpty else { return }
        if let remote = await repository.fetchRoomChat(roomCode: roomCode, currentUserId: currentUser?.id) {
            chatMessages = remote
            repository.saveLocalChat(roomCode: roomCode, messages: remote)
        }
    }

    func sendChatMessage(_ rawText: String) {
        let trimmed = rawText.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty, let user = currentUser else { return }

        let titleBadge = ShopCatalog.findById(shopState.equippedTitleId)?.badgeText
        let flairBadge = ShopCatalog.findById(shopState.equippedFlairId)?.id
        let msg = ChatMessage(
            id: "\(Int64(Date().timeIntervalSince1970 * 1000))-\(UUID().uuidString.prefix(6))",
            senderId: user.id,
            senderName: user.name,
            senderAvatarColorHex: user.avatarColorHex,
            senderTitle: titleBadge,
            senderFlair: flairBadge,
            text: String(trimmed.prefix(500)),
            timestampMillis: Int64(Date().timeIntervalSince1970 * 1000),
            isCurrentUser: true
        )

        var next = chatMessages
        next.append(msg)
        if next.count > 100 { next = Array(next.suffix(100)) }
        chatMessages = next
        repository.saveLocalChat(roomCode: roomCode, messages: next)
        rebuildFriendsAndStats()

        if !roomCode.isEmpty {
            isSendingChat = true
            Task {
                _ = await repository.pushRoomChatMessage(roomCode: roomCode, message: msg)
                isSendingChat = false
            }
        }
    }

    func sendQuickReaction(_ reactionText: String) {
        sendChatMessage(reactionText)
    }

    // MARK: - Stats & Challenges

    func totalPoints(for participantPredictions: [String: Prediction]) -> Int {
        let allMatches = Dictionary(grouping: upcomingMatches + matches, by: \.id).compactMapValues(\.first).values
        return allMatches.reduce(0) { sum, match in
            sum + calculatePredictionPoints(match: match, prediction: participantPredictions[match.id])
        }
    }

    private func rebuildFriendsAndStats() {
        if roomCode.isEmpty {
            friends = repository.buildLocalRealParticipants(
                currentUser: currentUser,
                currentPredictions: predictions,
                shopState: shopState
            )
        } else {
            friends = mergeFriendsWithCurrentUser(friends)
        }

        let allMatches = Dictionary(grouping: upcomingMatches + matches, by: \.id).compactMapValues(\.first).values
        var exact = 0
        var outcome = 0
        var evaluated = 0
        var streak = 0
        var maxStreak = 0

        for match in allMatches where match.status != .scheduled {
            guard let pred = predictions[match.id] else { continue }
            evaluated += 1
            let pts = calculatePredictionPoints(match: match, prediction: pred)
            if pts >= 10 {
                exact += 1
                outcome += 1
                streak += 1
                maxStreak = max(maxStreak, streak)
            } else if pts > 0 {
                outcome += 1
                streak += 1
                maxStreak = max(maxStreak, streak)
            } else {
                streak = 0
            }
        }

        let accuracy = evaluated > 0 ? Int((Double(outcome) / Double(evaluated)) * 100.0) : 0
        userStats = UserStats(
            exactHits: exact,
            outcomeHits: outcome,
            totalEvaluated: evaluated,
            currentStreak: maxStreak,
            accuracyPercent: accuracy
        )

        if let user = currentUser {
            let claimed = repository.loadClaimedChallenges(userId: user.id)
            let todayIds = Set(matches.map(\.id))
            let todayPredCount = predictions.keys.filter { todayIds.contains($0) }.count
            let sentChatCount = chatMessages.filter(\.isCurrentUser).count
            let hitCount = outcome

            dailyChallenges = [
                DailyChallenge(
                    id: "daily_predict_3",
                    title: "Palpite em 3 jogos hoje",
                    description: "Salve palpites em 3 partidas da rodada de hoje.",
                    rewardPoints: 10,
                    progress: min(3, todayPredCount),
                    target: 3,
                    claimed: claimed.contains("daily_predict_3")
                ),
                DailyChallenge(
                    id: "daily_chat_1",
                    title: "Mande uma mensagem no chat",
                    description: "Participe da resenha da sala no bate-papo.",
                    rewardPoints: 5,
                    progress: min(1, sentChatCount),
                    target: 1,
                    claimed: claimed.contains("daily_chat_1")
                ),
                DailyChallenge(
                    id: "daily_hit_1",
                    title: "Acerte 1 resultado ou cravada",
                    description: "Pontue em pelo menos uma partida encerrada/ao vivo.",
                    rewardPoints: 15,
                    progress: min(1, hitCount),
                    target: 1,
                    claimed: claimed.contains("daily_hit_1")
                )
            ]
        } else {
            dailyChallenges = []
        }
    }
}
