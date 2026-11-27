import CryptoKit
import Foundation

final class AppRepository {
    static let syncBaseURL = "https://bolao-dos-vei-default-rtdb.firebaseio.com"

    private let defaults = UserDefaults.standard
    private let session: URLSession

    init() {
        let config = URLSessionConfiguration.default
        config.timeoutIntervalForRequest = 8
        config.timeoutIntervalForResource = 12
        self.session = URLSession(configuration: config)
    }

    // MARK: - Password Hashing (SHA-256 with Legacy Migration)

    private func hashPassword(_ raw: String) -> String {
        let digest = SHA256.hash(data: Data(raw.utf8))
        let hex = digest.map { String(format: "%02x", $0) }.joined()
        return "sha256:\(hex)"
    }

    private func passwordMatches(stored: String, input: String) -> Bool {
        if stored.hasPrefix("sha256:") {
            return stored == hashPassword(input)
        }
        return stored == input
    }

    // MARK: - Auth & Local User Persistence

    func getCurrentUser() -> UserAccount? {
        guard let currentId = defaults.string(forKey: "current_user_id") else { return nil }
        return getAllUsers().first { $0.id == currentId }
    }

    func getAllUsers() -> [UserAccount] {
        guard let data = defaults.data(forKey: "users_json_v2"),
              let users = try? JSONDecoder().decode([UserAccount].self, from: data) else {
            return []
        }
        return users
    }

    private func saveUsers(_ users: [UserAccount]) {
        if let data = try? JSONEncoder().encode(users) {
            defaults.set(data, forKey: "users_json_v2")
        }
    }

    func registerUser(
        name: String,
        email: String,
        password: String,
        avatarColorHex: UInt64,
        favoriteLeague: League
    ) -> Result<UserAccount, Error> {
        let cleanEmail = email.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        let cleanName = name.trimmingCharacters(in: .whitespacesAndNewlines)

        if cleanName.count < 2 {
            return .failure(RepoError.message("Digite seu nome ou apelido."))
        }
        if !cleanEmail.contains("@") {
            return .failure(RepoError.message("Digite um e-mail válido."))
        }
        if password.count < 4 {
            return .failure(RepoError.message("A senha deve ter pelo menos 4 caracteres."))
        }

        let existing = getAllUsers()
        if existing.contains(where: { $0.email.caseInsensitiveCompare(cleanEmail) == .orderedSame }) {
            return .failure(RepoError.message("Já existe uma conta com este e-mail."))
        }

        let stableId = cleanEmail.replacingOccurrences(
            of: "[^a-z0-9]",
            with: "_",
            options: .regularExpression
        )
        let user = UserAccount(
            id: "u_\(stableId)",
            name: cleanName,
            email: cleanEmail,
            password: hashPassword(password),
            avatarColorHex: avatarColorHex,
            favoriteLeague: favoriteLeague
        )
        saveUsers(existing + [user])
        defaults.set(user.id, forKey: "current_user_id")
        return .success(user)
    }

    func loginUser(email: String, password: String) -> Result<UserAccount, Error> {
        let cleanEmail = email.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        var all = getAllUsers()
        guard let idx = all.firstIndex(where: {
            $0.email.caseInsensitiveCompare(cleanEmail) == .orderedSame &&
            passwordMatches(stored: $0.password, input: password)
        }) else {
            return .failure(RepoError.message("E-mail ou senha incorretos."))
        }

        var user = all[idx]
        // Transparent migration from legacy plaintext password
        if !user.password.hasPrefix("sha256:") {
            user = UserAccount(
                id: user.id,
                name: user.name,
                email: user.email,
                password: hashPassword(password),
                avatarColorHex: user.avatarColorHex,
                favoriteLeague: user.favoriteLeague,
                avatarImagePath: user.avatarImagePath
            )
            all[idx] = user
            saveUsers(all)
        }

        defaults.set(user.id, forKey: "current_user_id")
        return .success(user)
    }

    func logout() {
        defaults.removeObject(forKey: "current_user_id")
    }

    func updateUserProfile(
        userId: String,
        newName: String,
        newColorHex: UInt64,
        newFavoriteLeague: League
    ) -> Result<UserAccount, Error> {
        let cleanName = newName.trimmingCharacters(in: .whitespacesAndNewlines)
        if cleanName.count < 2 {
            return .failure(RepoError.message("Digite um nome com pelo menos 2 letras."))
        }
        var all = getAllUsers()
        guard let idx = all.firstIndex(where: { $0.id == userId }) else {
            return .failure(RepoError.message("Conta não encontrada."))
        }
        let target = all[idx]
        let updated = UserAccount(
            id: target.id,
            name: cleanName,
            email: target.email,
            password: target.password,
            avatarColorHex: newColorHex,
            favoriteLeague: newFavoriteLeague,
            avatarImagePath: target.avatarImagePath
        )
        all[idx] = updated
        saveUsers(all)
        return .success(updated)
    }

    func saveProfilePhotoData(userId: String, imageData: Data) -> String? {
        let fm = FileManager.default
        guard let docsDir = fm.urls(for: .documentDirectory, in: .userDomainMask).first else {
            return nil
        }
        let avatarsDir = docsDir.appendingPathComponent("avatars", isDirectory: true)
        try? fm.createDirectory(at: avatarsDir, withIntermediateDirectories: true)

        let fileName = "avatar_\(userId)_\(Int(Date().timeIntervalSince1970 * 1000)).jpg"
        let fileURL = avatarsDir.appendingPathComponent(fileName)
        do {
            try imageData.write(to: fileURL, options: .atomic)
            return fileURL.path
        } catch {
            return nil
        }
    }

    func updateUserAvatarPhoto(userId: String, photoPath: String?) -> UserAccount? {
        var all = getAllUsers()
        guard let idx = all.firstIndex(where: { $0.id == userId }) else { return nil }
        let target = all[idx]
        let updated = UserAccount(
            id: target.id,
            name: target.name,
            email: target.email,
            password: target.password,
            avatarColorHex: target.avatarColorHex,
            favoriteLeague: target.favoriteLeague,
            avatarImagePath: photoPath
        )
        all[idx] = updated
        saveUsers(all)
        return updated
    }

    // MARK: - Predictions Persistence

    func loadPredictions(userId: String) -> [String: Prediction] {
        guard let data = defaults.data(forKey: "predictions_\(userId)"),
              let raw = try? JSONSerialization.jsonObject(with: data) as? [String: [String: Any]] else {
            return [:]
        }
        var result: [String: Prediction] = [:]
        for (matchId, p) in raw {
            guard let home = p["home"] as? Int, let away = p["away"] as? Int else { continue }
            let joker = (p["joker"] as? Bool) ?? false
            result[matchId] = Prediction(matchId: matchId, homeScore: home, awayScore: away, isJoker: joker)
        }
        return result
    }

    func savePredictions(userId: String, predictions: [String: Prediction]) {
        var dict: [String: [String: Any]] = [:]
        for (matchId, pred) in predictions {
            dict[matchId] = [
                "home": pred.homeScore,
                "away": pred.awayScore,
                "joker": pred.isJoker
            ]
        }
        if let data = try? JSONSerialization.data(withJSONObject: dict) {
            defaults.set(data, forKey: "predictions_\(userId)")
        }
    }

    // MARK: - Room Code

    func loadRoomCode() -> String {
        defaults.string(forKey: "active_room_code") ?? ""
    }

    @discardableResult
    func saveRoomCode(_ roomCode: String) -> String {
        let clean = roomCode
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .uppercased()
            .replacingOccurrences(of: "[^A-Z0-9_-]", with: "", options: .regularExpression)
        defaults.set(clean, forKey: "active_room_code")
        return clean
    }

    func generateRoomCode() -> String {
        let chars = Array("ABCDEFGHJKLMNPQRSTUVWXYZ23456789")
        let suffix = String((0..<4).compactMap { _ in chars.randomElement() })
        return "VEI-\(suffix)"
    }

    private func syncSuffix(for baseUrl: String) -> String {
        baseUrl.lowercased().contains("firebaseio.com") ? ".json" : ""
    }

    // MARK: - Cross-Platform Room Sync (Matches server/server.js & Firebase RTDB)

    func pushUserBolaoToRoom(
        roomCode: String,
        user: UserAccount,
        predictions: [String: Prediction],
        shopState: UserShopState
    ) async -> Bool {
        let baseUrl = Self.syncBaseURL.trimmingCharacters(in: .whitespacesAndNewlines).trimmingCharacters(in: CharacterSet(charactersIn: "/"))
        let room = roomCode.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
        guard baseUrl.hasPrefix("http"), !room.isEmpty else { return false }

        let endpoint = "\(baseUrl)/salas/\(room)/participantes/\(user.id)\(syncSuffix(for: baseUrl))"
        guard let url = URL(string: endpoint) else { return false }

        var predsDict: [String: [String: Any]] = [:]
        for (matchId, p) in predictions {
            predsDict[matchId] = [
                "home": p.homeScore,
                "away": p.awayScore,
                "joker": p.isJoker
            ]
        }

        let equippedTitleName = ShopCatalog.findById(shopState.equippedTitleId)?.badgeText ?? ""
        let payload: [String: Any] = [
            "id": user.id,
            "name": user.name,
            "avatarColorHex": user.avatarColorHex,
            "favoriteLeague": user.favoriteLeague.shortName,
            "equippedFrameId": shopState.equippedFrameId ?? "",
            "equippedTitle": equippedTitleName,
            "updatedAt": Int64(Date().timeIntervalSince1970 * 1000),
            "predictions": predsDict
        ]

        guard let bodyData = try? JSONSerialization.data(withJSONObject: payload) else { return false }

        var request = URLRequest(url: url)
        request.httpMethod = "PUT"
        request.setValue("application/json; charset=UTF-8", forHTTPHeaderField: "Content-Type")
        request.httpBody = bodyData

        do {
            let (_, response) = try await session.data(for: request)
            guard let http = response as? HTTPURLResponse else { return false }
            return (200...299).contains(http.statusCode)
        } catch {
            return false
        }
    }

    func fetchRoomParticipants(roomCode: String, currentUserId: String?) async -> [FriendBolao]? {
        let baseUrl = Self.syncBaseURL.trimmingCharacters(in: .whitespacesAndNewlines).trimmingCharacters(in: CharacterSet(charactersIn: "/"))
        let room = roomCode.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
        guard baseUrl.hasPrefix("http"), !room.isEmpty else { return nil }

        let endpoint = "\(baseUrl)/salas/\(room)/participantes\(syncSuffix(for: baseUrl))"
        guard let data = await httpGetData(urlString: endpoint) else { return nil }
        let trimmed = String(data: data, encoding: .utf8)?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        if trimmed.isEmpty || trimmed == "null" { return [] }

        guard let root = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            return nil
        }

        var list: [FriendBolao] = []
        for (uid, rawVal) in root {
            guard let obj = rawVal as? [String: Any] else { continue }
            var predsMap: [String: Prediction] = [:]
            if let predsObj = obj["predictions"] as? [String: Any] {
                for (mId, pRaw) in predsObj {
                    guard let p = pRaw as? [String: Any] else { continue }
                    let home = (p["home"] as? NSNumber)?.intValue ?? 0
                    let away = (p["away"] as? NSNumber)?.intValue ?? 0
                    let joker = (p["joker"] as? Bool) ?? false
                    predsMap[mId] = Prediction(matchId: mId, homeScore: home, awayScore: away, isJoker: joker)
                }
            }

            let isMe = (uid == currentUserId)
            let rawName = (obj["name"] as? String)?.trimmingCharacters(in: .whitespacesAndNewlines) ?? "Participante"
            let favLeague = (obj["favoriteLeague"] as? String) ?? "Bolão"
            let colorHex = (obj["avatarColorHex"] as? NSNumber)?.uint64Value ?? 0xFF00C853
            let frameId = (obj["equippedFrameId"] as? String)?.nilIfBlank
            let title = (obj["equippedTitle"] as? String)?.nilIfBlank

            list.append(
                FriendBolao(
                    id: uid,
                    name: isMe ? "\(rawName) (Você)" : rawName,
                    subtitle: "Sala \(room) • \(favLeague)",
                    avatarColorHex: colorHex,
                    predictions: predsMap,
                    isCurrentUser: isMe,
                    equippedFrameId: frameId,
                    equippedTitle: title
                )
            )
        }
        return list
    }

    func buildLocalRealParticipants(
        currentUser: UserAccount?,
        currentPredictions: [String: Prediction],
        shopState: UserShopState
    ) -> [FriendBolao] {
        getAllUsers().map { user in
            let isMe = (user.id == currentUser?.id)
            let preds = isMe ? currentPredictions : loadPredictions(userId: user.id)
            let userShop = isMe ? shopState : loadShopState(userId: user.id)
            let titleText = ShopCatalog.findById(userShop.equippedTitleId)?.badgeText
            return FriendBolao(
                id: user.id,
                name: isMe ? "\(user.name) (Você)" : user.name,
                subtitle: isMe ? "Sua conta • \(user.favoriteLeague.shortName)" : "Conta neste aparelho",
                avatarColorHex: user.avatarColorHex,
                predictions: preds,
                isCurrentUser: isMe,
                avatarImagePath: user.avatarImagePath,
                equippedFrameId: userShop.equippedFrameId,
                equippedTitle: titleText
            )
        }
    }

    // MARK: - Shop & Daily Challenges Persistence

    private struct StoredShopData: Codable {
        var ownedItemIds: [String] = []
        var equippedFrameId: String? = nil
        var equippedTitleId: String? = nil
        var equippedFlairId: String? = nil
        var spentPoints: Int = 0
        var creditedMatchPoints: [String: Int] = [:]
        var bonusPoints: Int = 0
    }

    private func readStoredShopData(userId: String) -> StoredShopData {
        guard let data = defaults.data(forKey: "shop_state_\(userId)"),
              let decoded = try? JSONDecoder().decode(StoredShopData.self, from: data) else {
            return StoredShopData()
        }
        return decoded
    }

    private func writeStoredShopData(userId: String, data: StoredShopData) -> UserShopState {
        if let encoded = try? JSONEncoder().encode(data) {
            defaults.set(encoded, forKey: "shop_state_\(userId)")
        }
        let earned = 30 + data.creditedMatchPoints.values.reduce(0, +) + data.bonusPoints
        return UserShopState(
            availablePoints: max(0, earned - data.spentPoints),
            ownedItemIds: Set(data.ownedItemIds),
            equippedFrameId: data.equippedFrameId?.nilIfBlank,
            equippedTitleId: data.equippedTitleId?.nilIfBlank,
            equippedFlairId: data.equippedFlairId?.nilIfBlank
        )
    }

    func loadShopState(userId: String) -> UserShopState {
        let data = readStoredShopData(userId: userId)
        let earned = 30 + data.creditedMatchPoints.values.reduce(0, +) + data.bonusPoints
        return UserShopState(
            availablePoints: max(0, earned - data.spentPoints),
            ownedItemIds: Set(data.ownedItemIds),
            equippedFrameId: data.equippedFrameId?.nilIfBlank,
            equippedTitleId: data.equippedTitleId?.nilIfBlank,
            equippedFlairId: data.equippedFlairId?.nilIfBlank
        )
    }

    func creditMatchPoints(
        userId: String,
        matches: [Match],
        predictions: [String: Prediction]
    ) -> UserShopState {
        var current = readStoredShopData(userId: userId)
        var changed = false
        for match in matches {
            let pts = calculatePredictionPoints(match: match, prediction: predictions[match.id])
            let prev = current.creditedMatchPoints[match.id] ?? 0
            if pts > prev {
                current.creditedMatchPoints[match.id] = pts
                changed = true
            }
        }
        return changed ? writeStoredShopData(userId: userId, data: current) : loadShopState(userId: userId)
    }

    func purchaseShopItem(userId: String, item: ShopItem) -> Result<UserShopState, Error> {
        var current = readStoredShopData(userId: userId)
        if current.ownedItemIds.contains(item.id) {
            return .success(loadShopState(userId: userId))
        }
        let earned = 30 + current.creditedMatchPoints.values.reduce(0, +) + current.bonusPoints
        let available = max(0, earned - current.spentPoints)
        if available < item.price {
            return .failure(RepoError.message("Pontos insuficientes (precisa de \(item.price) pts)."))
        }
        current.ownedItemIds.append(item.id)
        current.spentPoints += item.price
        switch item.category {
        case .avatarFrame where current.equippedFrameId == nil:
            current.equippedFrameId = item.id
        case .titleBadge where current.equippedTitleId == nil:
            current.equippedTitleId = item.id
        case .chatFlair where current.equippedFlairId == nil:
            current.equippedFlairId = item.id
        default:
            break
        }
        return .success(writeStoredShopData(userId: userId, data: current))
    }

    func equipShopItem(userId: String, item: ShopItem) -> UserShopState {
        var current = readStoredShopData(userId: userId)
        guard current.ownedItemIds.contains(item.id) else {
            return loadShopState(userId: userId)
        }
        switch item.category {
        case .avatarFrame: current.equippedFrameId = item.id
        case .titleBadge: current.equippedTitleId = item.id
        case .chatFlair: current.equippedFlairId = item.id
        }
        return writeStoredShopData(userId: userId, data: current)
    }

    func loadClaimedChallenges(userId: String) -> Set<String> {
        let key = "challenges_\(userId)_\(todayKey())"
        let arr = defaults.stringArray(forKey: key) ?? []
        return Set(arr)
    }

    func markChallengeClaimed(userId: String, challengeId: String, rewardPoints: Int) -> UserShopState {
        let key = "challenges_\(userId)_\(todayKey())"
        var claimed = loadClaimedChallenges(userId: userId)
        guard !claimed.contains(challengeId) else {
            return loadShopState(userId: userId)
        }
        claimed.insert(challengeId)
        defaults.set(Array(claimed), forKey: key)
        var shopData = readStoredShopData(userId: userId)
        shopData.bonusPoints += rewardPoints
        return writeStoredShopData(userId: userId, data: shopData)
    }

    private func todayKey() -> String {
        let fmt = DateFormatter()
        fmt.calendar = Calendar(identifier: .gregorian)
        fmt.timeZone = TimeZone.current
        fmt.dateFormat = "yyyyMMdd"
        return fmt.string(from: Date())
    }

    // MARK: - Chat Persistence & Cross-Platform Sync

    private func chatRoomKey(_ roomCode: String) -> String {
        let trimmed = roomCode.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
        return trimmed.isEmpty ? "global" : trimmed
    }

    func loadLocalChat(roomCode: String, currentUserId: String?) -> [ChatMessage] {
        let key = "chat_messages_\(chatRoomKey(roomCode))"
        guard let data = defaults.data(forKey: key),
              let arr = try? JSONSerialization.jsonObject(with: data) as? [[String: Any]] else {
            return []
        }
        return parseChatArray(arr, currentUserId: currentUserId)
    }

    func saveLocalChat(roomCode: String, messages: [ChatMessage]) {
        let key = "chat_messages_\(chatRoomKey(roomCode))"
        let arr: [[String: Any]] = messages.suffix(100).map { msg in
            var dict: [String: Any] = [
                "id": msg.id,
                "senderId": msg.senderId,
                "senderName": msg.senderName,
                "senderAvatarColorHex": msg.senderAvatarColorHex,
                "text": msg.text,
                "timestampMillis": msg.timestampMillis
            ]
            if let t = msg.senderTitle, !t.isEmpty { dict["senderTitle"] = t }
            if let f = msg.senderFlair, !f.isEmpty { dict["senderFlair"] = f }
            return dict
        }
        if let data = try? JSONSerialization.data(withJSONObject: arr) {
            defaults.set(data, forKey: key)
        }
    }

    private func parseChatObject(_ obj: [String: Any], currentUserId: String?) -> ChatMessage? {
        let text = (obj["text"] as? String)?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        guard !text.isEmpty else { return nil }
        let senderId = (obj["senderId"] as? String) ?? ""
        let ts = (obj["timestampMillis"] as? NSNumber)?.int64Value ?? Int64(Date().timeIntervalSince1970 * 1000)
        let id = (obj["id"] as? String)?.nilIfBlank ?? "msg_\(senderId)_\(ts)"
        let senderName = (obj["senderName"] as? String)?.nilIfBlank ?? "Participante"
        let colorHex = (obj["senderAvatarColorHex"] as? NSNumber)?.uint64Value ?? 0xFF00C853
        let title = (obj["senderTitle"] as? String)?.nilIfBlank
        let flair = (obj["senderFlair"] as? String)?.nilIfBlank
        return ChatMessage(
            id: id,
            senderId: senderId,
            senderName: senderName,
            senderAvatarColorHex: colorHex,
            senderTitle: title,
            senderFlair: flair,
            text: text,
            timestampMillis: ts,
            isCurrentUser: currentUserId != nil && senderId == currentUserId
        )
    }

    private func parseChatArray(_ arr: [[String: Any]], currentUserId: String?) -> [ChatMessage] {
        arr.compactMap { parseChatObject($0, currentUserId: currentUserId) }
            .sorted { $0.timestampMillis < $1.timestampMillis }
            .suffix(100)
            .map { $0 }
    }

    func fetchRoomChat(roomCode: String, currentUserId: String?) async -> [ChatMessage]? {
        let baseUrl = Self.syncBaseURL.trimmingCharacters(in: .whitespacesAndNewlines).trimmingCharacters(in: CharacterSet(charactersIn: "/"))
        let room = roomCode.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
        guard baseUrl.hasPrefix("http"), !room.isEmpty else { return nil }

        let endpoint = "\(baseUrl)/salas/\(room)/chat\(syncSuffix(for: baseUrl))"
        guard let data = await httpGetData(urlString: endpoint) else { return nil }
        let trimmed = String(data: data, encoding: .utf8)?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        if trimmed.isEmpty || trimmed == "null" { return [] }

        if let arr = try? JSONSerialization.jsonObject(with: data) as? [[String: Any]] {
            return parseChatArray(arr, currentUserId: currentUserId)
        } else if let dict = try? JSONSerialization.jsonObject(with: data) as? [String: Any] {
            let values = dict.values.compactMap { $0 as? [String: Any] }
            return parseChatArray(values, currentUserId: currentUserId)
        }
        return nil
    }

    func pushRoomChatMessage(roomCode: String, message: ChatMessage) async -> Bool {
        let baseUrl = Self.syncBaseURL.trimmingCharacters(in: .whitespacesAndNewlines).trimmingCharacters(in: CharacterSet(charactersIn: "/"))
        let room = roomCode.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
        guard baseUrl.hasPrefix("http"), !room.isEmpty else { return false }

        let endpoint = "\(baseUrl)/salas/\(room)/chat\(syncSuffix(for: baseUrl))"
        guard let url = URL(string: endpoint) else { return false }

        var payload: [String: Any] = [
            "id": message.id,
            "senderId": message.senderId,
            "senderName": message.senderName,
            "senderAvatarColorHex": message.senderAvatarColorHex,
            "text": message.text,
            "timestampMillis": message.timestampMillis
        ]
        if let t = message.senderTitle, !t.isEmpty { payload["senderTitle"] = t }
        if let f = message.senderFlair, !f.isEmpty { payload["senderFlair"] = f }

        guard let bodyData = try? JSONSerialization.data(withJSONObject: payload) else { return false }

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json; charset=UTF-8", forHTTPHeaderField: "Content-Type")
        request.httpBody = bodyData

        do {
            let (_, response) = try await session.data(for: request)
            guard let http = response as? HTTPURLResponse else { return false }
            return (200...299).contains(http.statusCode)
        } catch {
            return false
        }
    }

    // MARK: - Parallel ESPN Live Scoreboard & Schedule (Local Timezone)

    func fetchScheduleMatches() async -> [Match] {
        let localTz = TimeZone.current
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = localTz
        let now = Date()
        let startOfToday = cal.startOfDay(for: now)

        let espnDateFmt = DateFormatter()
        espnDateFmt.calendar = cal
        espnDateFmt.timeZone = localTz
        espnDateFmt.locale = Locale(identifier: "en_US_POSIX")
        espnDateFmt.dateFormat = "yyyyMMdd"

        // Query days 0..4 to capture late-night UTC games on day+1..day+4
        let dateParams: [String] = (0...4).compactMap { offset in
            cal.date(byAdding: .day, value: offset, to: startOfToday).map { espnDateFmt.string(from: $0) }
        }

        let collected: [Match] = await withTaskGroup(of: [Match].self) { group in
            for league in League.allCases {
                for dateParam in dateParams {
                    group.addTask { [self] in
                        await self.fetchEspnScoreboardForDate(
                            league: league,
                            dateParam: dateParam,
                            startOfToday: startOfToday,
                            localTz: localTz
                        )
                    }
                }
            }
            var merged: [Match] = []
            for await chunk in group {
                merged.append(contentsOf: chunk)
            }
            return merged
        }

        var seen = Set<String>()
        let unique = collected.filter { seen.insert($0.id).inserted }
        return unique.sorted { a, b in
            let rankA = statusRank(a.status)
            let rankB = statusRank(b.status)
            if a.dayOffset != b.dayOffset { return a.dayOffset < b.dayOffset }
            if rankA != rankB { return rankA < rankB }
            return a.kickoffTime < b.kickoffTime
        }
    }

    func fetchStandings() async -> [League: [StandingRow]] {
        await withTaskGroup(of: (League, [StandingRow]).self) { group in
            for league in League.allCases {
                group.addTask { [self] in
                    let fetched = await self.fetchEspnStandings(league: league)
                    return (league, fetched.isEmpty ? self.defaultStandings(for: league) : fetched)
                }
            }
            var result: [League: [StandingRow]] = [:]
            for await (league, rows) in group {
                result[league] = rows
            }
            return result
        }
    }

    private func statusRank(_ status: MatchStatus) -> Int {
        switch status {
        case .live, .halftime: return 0
        case .scheduled: return 1
        case .finished: return 2
        }
    }

    private func httpGetData(urlString: String) async -> Data? {
        guard let url = URL(string: urlString) else { return nil }
        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        request.setValue("BolaoDosVei-iOS/2.0", forHTTPHeaderField: "User-Agent")
        do {
            let (data, response) = try await session.data(for: request)
            guard let http = response as? HTTPURLResponse, (200...299).contains(http.statusCode) else {
                return nil
            }
            return data
        } catch {
            return nil
        }
    }

    private func parseEspnUtcDate(_ raw: String) -> Date? {
        guard !raw.isEmpty else { return nil }
        let iso1 = ISO8601DateFormatter()
        iso1.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        if let d = iso1.date(from: raw) { return d }

        let iso2 = ISO8601DateFormatter()
        iso2.formatOptions = [.withInternetDateTime]
        if let d = iso2.date(from: raw) { return d }

        let fmt = DateFormatter()
        fmt.locale = Locale(identifier: "en_US_POSIX")
        fmt.timeZone = TimeZone(secondsFromGMT: 0)
        for pattern in ["yyyy-MM-dd'T'HH:mm'Z'", "yyyy-MM-dd'T'HH:mm:ss'Z'"] {
            fmt.dateFormat = pattern
            if let d = fmt.date(from: raw) { return d }
        }
        return nil
    }

    private func fetchEspnScoreboardForDate(
        league: League,
        dateParam: String,
        startOfToday: Date,
        localTz: TimeZone
    ) async -> [Match] {
        let url = "https://site.api.espn.com/apis/site/v2/sports/soccer/\(league.espnSlug)/scoreboard?dates=\(dateParam)"
        guard let data = await httpGetData(urlString: url),
              let root = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let events = root["events"] as? [[String: Any]] else {
            return []
        }

        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = localTz

        let localTimeFmt = DateFormatter()
        localTimeFmt.calendar = cal
        localTimeFmt.timeZone = localTz
        localTimeFmt.dateFormat = "HH:mm"

        let shortDateFmt = DateFormatter()
        shortDateFmt.calendar = cal
        shortDateFmt.timeZone = localTz
        shortDateFmt.dateFormat = "dd/MM"

        var list: [Match] = []
        for (i, event) in events.enumerated() {
            let rawId = (event["id"] as? String) ?? "\(i)"
            let id = "\(league.id)_\(rawId)"
            guard let competitions = event["competitions"] as? [[String: Any]],
                  let comp = competitions.first,
                  let competitors = comp["competitors"] as? [[String: Any]],
                  competitors.count >= 2 else {
                continue
            }

            let statusObj = comp["status"] as? [String: Any]
            let typeObj = statusObj?["type"] as? [String: Any]
            let state = (typeObj?["state"] as? String) ?? "pre"
            let detail = (typeObj?["shortDetail"] as? String) ?? ""
            let clockSec = (statusObj?["clock"] as? NSNumber)?.doubleValue ?? 0
            let clockMin = min(95, max(0, Int(clockSec) / 60))

            let matchStatus: MatchStatus
            if state == "in" && detail.localizedCaseInsensitiveContains("HT") {
                matchStatus = .halftime
            } else if state == "in" {
                matchStatus = .live
            } else if state == "post" {
                matchStatus = .finished
            } else {
                matchStatus = .scheduled
            }

            let dateStr = (event["date"] as? String) ?? ""
            let parsedDate = parseEspnUtcDate(dateStr)
            let isLiveNow = (matchStatus == .live || matchStatus == .halftime)

            let dayOffset: Int
            if isLiveNow {
                dayOffset = 0
            } else if let parsedDate = parsedDate {
                let eventStart = cal.startOfDay(for: parsedDate)
                let diff = cal.dateComponents([.day], from: startOfToday, to: eventStart).day ?? -1
                if diff < 0 || diff > 3 { continue }
                dayOffset = diff
            } else {
                continue
            }

            let dateLabel: String
            switch dayOffset {
            case 0: dateLabel = "Hoje"
            case 1: dateLabel = "Amanhã"
            default: dateLabel = parsedDate.map { shortDateFmt.string(from: $0) } ?? "Próximos"
            }

            let kickoff = parsedDate.map { localTimeFmt.string(from: $0) } ?? "Hoje"

            guard let homeObj = competitors.first(where: { ($0["homeAway"] as? String) == "home" }),
                  let awayObj = competitors.first(where: { ($0["homeAway"] as? String) == "away" }),
                  let homeTeamObj = homeObj["team"] as? [String: Any],
                  let awayTeamObj = awayObj["team"] as? [String: Any] else {
                continue
            }

            let venue = (comp["venue"] as? [String: Any])?["fullName"] as? String ?? "Estádio"
            let homeScore = Int((homeObj["score"] as? String) ?? "0") ?? (homeObj["score"] as? NSNumber)?.intValue ?? 0
            let awayScore = Int((awayObj["score"] as? String) ?? "0") ?? (awayObj["score"] as? NSNumber)?.intValue ?? 0

            let homeName = (homeTeamObj["shortDisplayName"] as? String) ?? (homeTeamObj["displayName"] as? String) ?? "Mandante"
            let awayName = (awayTeamObj["shortDisplayName"] as? String) ?? (awayTeamObj["displayName"] as? String) ?? "Visitante"

            list.append(
                Match(
                    id: id,
                    league: league,
                    homeTeam: TeamInfo(
                        name: homeName,
                        shortName: String(((homeTeamObj["abbreviation"] as? String) ?? "MAN").prefix(3)),
                        colorHex: parseColorHex((homeTeamObj["color"] as? String) ?? "", fallback: 0xFF00C853),
                        logoUrl: (homeTeamObj["logo"] as? String) ?? ""
                    ),
                    awayTeam: TeamInfo(
                        name: awayName,
                        shortName: String(((awayTeamObj["abbreviation"] as? String) ?? "VIS").prefix(3)),
                        colorHex: parseColorHex((awayTeamObj["color"] as? String) ?? "", fallback: 0xFF3B82F6),
                        logoUrl: (awayTeamObj["logo"] as? String) ?? ""
                    ),
                    homeScore: homeScore,
                    awayScore: awayScore,
                    status: matchStatus,
                    minute: clockMin,
                    kickoffTime: kickoff,
                    stadium: venue,
                    isLiveApi: true,
                    dayOffset: dayOffset,
                    dateLabel: dateLabel
                )
            )
        }
        return list
    }

    private func fetchEspnStandings(league: League) async -> [StandingRow] {
        let url = "https://site.api.espn.com/apis/v2/sports/soccer/\(league.espnSlug)/standings"
        guard let data = await httpGetData(urlString: url),
              let root = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let children = root["children"] as? [[String: Any]],
              let firstChild = children.first,
              let standings = firstChild["standings"] as? [String: Any],
              let entries = standings["entries"] as? [[String: Any]] else {
            return []
        }

        var rows: [StandingRow] = []
        for (i, entry) in entries.enumerated() {
            guard let team = entry["team"] as? [String: Any],
                  let stats = entry["stats"] as? [[String: Any]] else {
                continue
            }
            var statMap: [String: Int] = [:]
            for st in stats {
                if let name = st["name"] as? String, let val = st["value"] as? NSNumber {
                    statMap[name] = val.intValue
                }
            }
            let name = (team["shortDisplayName"] as? String) ?? (team["displayName"] as? String) ?? "Time"
            let logos = team["logos"] as? [[String: Any]]
            let logoUrl = (logos?.first?["href"] as? String) ?? ""
            let abbr = (team["abbreviation"] as? String) ?? String(name.prefix(3)).uppercased()
            rows.append(
                StandingRow(
                    position: statMap["rank"] ?? (i + 1),
                    teamName: name,
                    shortName: String(abbr.prefix(3)),
                    colorHex: colorForTeamName(name),
                    logoUrl: logoUrl,
                    played: statMap["gamesPlayed"] ?? 0,
                    won: statMap["wins"] ?? 0,
                    drawn: statMap["ties"] ?? 0,
                    lost: statMap["losses"] ?? 0,
                    goalsFor: statMap["pointsFor"] ?? 0,
                    goalsAgainst: statMap["pointsAgainst"] ?? 0,
                    points: statMap["points"] ?? 0
                )
            )
        }
        return rows.sorted { $0.position < $1.position }
    }

    private func parseColorHex(_ raw: String, fallback: UInt64) -> UInt64 {
        let clean = raw.replacingOccurrences(of: "#", with: "").trimmingCharacters(in: .whitespacesAndNewlines)
        guard clean.count == 6, let val = UInt64(clean, radix: 16) else { return fallback }
        return 0xFF000000 | val
    }

    private func colorForTeamName(_ name: String) -> UInt64 {
        let palette: [UInt64] = [
            0xFFE53935, 0xFF1E88E5, 0xFF43A047, 0xFFFB8C00,
            0xFF8E24AA, 0xFF00ACC1, 0xFFFDD835, 0xFF3949AB
        ]
        let hash = abs(name.unicodeScalars.reduce(0) { $0 &* 31 &+ Int($1.value) })
        return palette[hash % palette.count]
    }

    private func espnLogo(_ id: Int) -> String {
        "https://a.espncdn.com/i/teamlogos/soccer/500/\(id).png"
    }

    // MARK: - Fallback Matches & Standings

    func defaultMatches() -> [Match] {
        [
            Match(
                id: "bra_1",
                league: .brasileirao,
                homeTeam: TeamInfo(name: "Flamengo", shortName: "FLA", colorHex: 0xFFE53935, logoUrl: espnLogo(819)),
                awayTeam: TeamInfo(name: "Palmeiras", shortName: "PAL", colorHex: 0xFF2E7D32, logoUrl: espnLogo(2029)),
                homeScore: 2,
                awayScore: 1,
                status: .live,
                minute: 68,
                kickoffTime: "16:00",
                stadium: "Maracanã",
                lastScorer: "Pedro"
            ),
            Match(
                id: "bra_2",
                league: .brasileirao,
                homeTeam: TeamInfo(name: "Botafogo", shortName: "BOT", colorHex: 0xFF37474F, logoUrl: espnLogo(6086)),
                awayTeam: TeamInfo(name: "São Paulo", shortName: "SAO", colorHex: 0xFFD32F2F, logoUrl: espnLogo(2026)),
                homeScore: 1,
                awayScore: 1,
                status: .live,
                minute: 41,
                kickoffTime: "16:30",
                stadium: "Nilton Santos",
                lastScorer: "Luiz Henrique"
            ),
            Match(
                id: "bra_3",
                league: .brasileirao,
                homeTeam: TeamInfo(name: "Corinthians", shortName: "COR", colorHex: 0xFF455A64, logoUrl: espnLogo(874)),
                awayTeam: TeamInfo(name: "Atlético-MG", shortName: "CAM", colorHex: 0xFF263238, logoUrl: espnLogo(7632)),
                homeScore: 0,
                awayScore: 0,
                status: .scheduled,
                minute: 0,
                kickoffTime: "19:30",
                stadium: "Neo Química Arena"
            ),
            Match(
                id: "eng_1",
                league: .premierLeague,
                homeTeam: TeamInfo(name: "Arsenal", shortName: "ARS", colorHex: 0xFFEF4444, logoUrl: espnLogo(359)),
                awayTeam: TeamInfo(name: "Liverpool", shortName: "LIV", colorHex: 0xFFB91C1C, logoUrl: espnLogo(364)),
                homeScore: 2,
                awayScore: 2,
                status: .live,
                minute: 74,
                kickoffTime: "13:30",
                stadium: "Emirates Stadium",
                lastScorer: "Salah"
            ),
            Match(
                id: "esp_1",
                league: .laLiga,
                homeTeam: TeamInfo(name: "Real Madrid", shortName: "RMA", colorHex: 0xFFF59E0B, logoUrl: espnLogo(86)),
                awayTeam: TeamInfo(name: "Barcelona", shortName: "BAR", colorHex: 0xFF2563EB, logoUrl: espnLogo(83)),
                homeScore: 3,
                awayScore: 2,
                status: .live,
                minute: 82,
                kickoffTime: "16:00",
                stadium: "Santiago Bernabéu",
                lastScorer: "Vini Jr"
            ),
            Match(
                id: "lib_1",
                league: .libertadores,
                homeTeam: TeamInfo(name: "Fluminense", shortName: "FLU", colorHex: 0xFF7F1D1D, logoUrl: espnLogo(3445)),
                awayTeam: TeamInfo(name: "River Plate", shortName: "RIV", colorHex: 0xFFDC2626, logoUrl: espnLogo(16)),
                homeScore: 0,
                awayScore: 0,
                status: .scheduled,
                minute: 0,
                kickoffTime: "21:30",
                stadium: "Maracanã",
                dayOffset: 1,
                dateLabel: "Amanhã"
            )
        ]
    }

    func defaultStandings(for league: League) -> [StandingRow] {
        switch league {
        case .brasileirao:
            return [
                StandingRow(position: 1, teamName: "Botafogo", shortName: "BOT", colorHex: 0xFF37474F, logoUrl: espnLogo(6086), played: 34, won: 21, drawn: 7, lost: 6, goalsFor: 56, goalsAgainst: 28, points: 70),
                StandingRow(position: 2, teamName: "Palmeiras", shortName: "PAL", colorHex: 0xFF2E7D32, logoUrl: espnLogo(2029), played: 34, won: 20, drawn: 8, lost: 6, goalsFor: 58, goalsAgainst: 29, points: 68),
                StandingRow(position: 3, teamName: "Flamengo", shortName: "FLA", colorHex: 0xFFE53935, logoUrl: espnLogo(819), played: 34, won: 19, drawn: 8, lost: 7, goalsFor: 55, goalsAgainst: 34, points: 65),
                StandingRow(position: 4, teamName: "Internacional", shortName: "INT", colorHex: 0xFFDC2626, logoUrl: espnLogo(1936), played: 34, won: 17, drawn: 11, lost: 6, goalsFor: 50, goalsAgainst: 31, points: 62),
                StandingRow(position: 5, teamName: "São Paulo", shortName: "SAO", colorHex: 0xFFD32F2F, logoUrl: espnLogo(2026), played: 34, won: 17, drawn: 7, lost: 10, goalsFor: 47, goalsAgainst: 34, points: 58),
                StandingRow(position: 6, teamName: "Cruzeiro", shortName: "CRU", colorHex: 0xFF2563EB, logoUrl: espnLogo(2022), played: 34, won: 14, drawn: 9, lost: 11, goalsFor: 41, goalsAgainst: 36, points: 51)
            ]
        case .premierLeague:
            return [
                StandingRow(position: 1, teamName: "Liverpool", shortName: "LIV", colorHex: 0xFFB91C1C, logoUrl: espnLogo(364), played: 28, won: 21, drawn: 5, lost: 2, goalsFor: 64, goalsAgainst: 24, points: 68),
                StandingRow(position: 2, teamName: "Arsenal", shortName: "ARS", colorHex: 0xFFEF4444, logoUrl: espnLogo(359), played: 28, won: 18, drawn: 7, lost: 3, goalsFor: 56, goalsAgainst: 25, points: 61),
                StandingRow(position: 3, teamName: "Man City", shortName: "MCI", colorHex: 0xFF38BDF8, logoUrl: espnLogo(382), played: 28, won: 17, drawn: 6, lost: 5, goalsFor: 59, goalsAgainst: 31, points: 57),
                StandingRow(position: 4, teamName: "Chelsea", shortName: "CHE", colorHex: 0xFF1D4ED8, logoUrl: espnLogo(363), played: 28, won: 15, drawn: 7, lost: 6, goalsFor: 53, goalsAgainst: 34, points: 52)
            ]
        case .laLiga:
            return [
                StandingRow(position: 1, teamName: "Real Madrid", shortName: "RMA", colorHex: 0xFFF59E0B, logoUrl: espnLogo(86), played: 28, won: 20, drawn: 5, lost: 3, goalsFor: 62, goalsAgainst: 25, points: 65),
                StandingRow(position: 2, teamName: "Barcelona", shortName: "BAR", colorHex: 0xFF2563EB, logoUrl: espnLogo(83), played: 28, won: 20, drawn: 3, lost: 5, goalsFor: 71, goalsAgainst: 29, points: 63),
                StandingRow(position: 3, teamName: "Atlético Madrid", shortName: "ATM", colorHex: 0xFFDC2626, logoUrl: espnLogo(1068), played: 28, won: 17, drawn: 8, lost: 3, goalsFor: 49, goalsAgainst: 22, points: 59)
            ]
        case .championsLeague:
            return [
                StandingRow(position: 1, teamName: "Liverpool", shortName: "LIV", colorHex: 0xFFB91C1C, logoUrl: espnLogo(364), played: 8, won: 7, drawn: 0, lost: 1, goalsFor: 17, goalsAgainst: 5, points: 21),
                StandingRow(position: 2, teamName: "Barcelona", shortName: "BAR", colorHex: 0xFF2563EB, logoUrl: espnLogo(83), played: 8, won: 6, drawn: 1, lost: 1, goalsFor: 28, goalsAgainst: 13, points: 19),
                StandingRow(position: 3, teamName: "Arsenal", shortName: "ARS", colorHex: 0xFFEF4444, logoUrl: espnLogo(359), played: 8, won: 6, drawn: 1, lost: 1, goalsFor: 16, goalsAgainst: 3, points: 19)
            ]
        case .libertadores:
            return [
                StandingRow(position: 1, teamName: "Botafogo", shortName: "BOT", colorHex: 0xFF37474F, logoUrl: espnLogo(6086), played: 6, won: 4, drawn: 1, lost: 1, goalsFor: 11, goalsAgainst: 5, points: 13),
                StandingRow(position: 2, teamName: "Palmeiras", shortName: "PAL", colorHex: 0xFF2E7D32, logoUrl: espnLogo(2029), played: 6, won: 4, drawn: 1, lost: 1, goalsFor: 12, goalsAgainst: 6, points: 13),
                StandingRow(position: 3, teamName: "River Plate", shortName: "RIV", colorHex: 0xFFDC2626, logoUrl: espnLogo(16), played: 6, won: 4, drawn: 0, lost: 2, goalsFor: 10, goalsAgainst: 5, points: 12)
            ]
        case .copaDoBrasil:
            return [
                StandingRow(position: 1, teamName: "Flamengo", shortName: "FLA", colorHex: 0xFFE53935, logoUrl: espnLogo(819), played: 8, won: 6, drawn: 1, lost: 1, goalsFor: 14, goalsAgainst: 5, points: 19),
                StandingRow(position: 2, teamName: "Atlético-MG", shortName: "CAM", colorHex: 0xFF263238, logoUrl: espnLogo(7632), played: 8, won: 5, drawn: 2, lost: 1, goalsFor: 12, goalsAgainst: 6, points: 17),
                StandingRow(position: 3, teamName: "Corinthians", shortName: "COR", colorHex: 0xFF455A64, logoUrl: espnLogo(874), played: 8, won: 4, drawn: 2, lost: 2, goalsFor: 10, goalsAgainst: 7, points: 14)
            ]
        case .sulAmericana:
            return [
                StandingRow(position: 1, teamName: "Cruzeiro", shortName: "CRU", colorHex: 0xFF2563EB, logoUrl: espnLogo(2022), played: 6, won: 4, drawn: 2, lost: 0, goalsFor: 10, goalsAgainst: 3, points: 14),
                StandingRow(position: 2, teamName: "Racing", shortName: "RAC", colorHex: 0xFF06B6D4, logoUrl: espnLogo(15), played: 6, won: 4, drawn: 1, lost: 1, goalsFor: 11, goalsAgainst: 5, points: 13),
                StandingRow(position: 3, teamName: "Corinthians", shortName: "COR", colorHex: 0xFF455A64, logoUrl: espnLogo(874), played: 6, won: 3, drawn: 2, lost: 1, goalsFor: 9, goalsAgainst: 5, points: 11)
            ]
        }
    }
}

enum RepoError: LocalizedError {
    case message(String)
    var errorDescription: String? {
        switch self {
        case .message(let text): return text
        }
    }
}

private extension String {
    var nilIfBlank: String? {
        let trimmed = trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty ? nil : trimmed
    }
}
