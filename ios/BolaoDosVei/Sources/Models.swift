import Foundation
import SwiftUI

// MARK: - Color Hex Helper

extension Color {
    init(argbHex: UInt64) {
        let a = Double((argbHex >> 24) & 0xFF) / 255.0
        let r = Double((argbHex >> 16) & 0xFF) / 255.0
        let g = Double((argbHex >> 8) & 0xFF) / 255.0
        let b = Double(argbHex & 0xFF) / 255.0
        self.init(.sRGB, red: r, green: g, blue: b, opacity: a == 0 ? 1.0 : a)
    }

    init(rgbHex: UInt32, opacity: Double = 1.0) {
        let r = Double((rgbHex >> 16) & 0xFF) / 255.0
        let g = Double((rgbHex >> 8) & 0xFF) / 255.0
        let b = Double(rgbHex & 0xFF) / 255.0
        self.init(.sRGB, red: r, green: g, blue: b, opacity: opacity)
    }
}

// MARK: - Leagues (7 Verified ESPN Slugs)

enum League: String, CaseIterable, Identifiable, Codable {
    case brasileirao = "brasileirao"
    case premierLeague = "premier_league"
    case laLiga = "la_liga"
    case championsLeague = "champions_league"
    case libertadores = "libertadores"
    case copaDoBrasil = "copa_do_brasil"
    case sulAmericana = "sul_americana"

    var id: String { rawValue }

    var displayName: String {
        switch self {
        case .brasileirao: return "Brasileirão Série A"
        case .premierLeague: return "Premier League"
        case .laLiga: return "La Liga"
        case .championsLeague: return "UEFA Champions League"
        case .libertadores: return "CONMEBOL Libertadores"
        case .copaDoBrasil: return "Copa do Brasil"
        case .sulAmericana: return "CONMEBOL Sul-Americana"
        }
    }

    var shortName: String {
        switch self {
        case .brasileirao: return "Brasileirão"
        case .premierLeague: return "Premier"
        case .laLiga: return "La Liga"
        case .championsLeague: return "Champions"
        case .libertadores: return "Libertadores"
        case .copaDoBrasil: return "Copa BR"
        case .sulAmericana: return "Sul-Americana"
        }
    }

    var country: String {
        switch self {
        case .brasileirao, .copaDoBrasil: return "Brasil"
        case .premierLeague: return "Inglaterra"
        case .laLiga: return "Espanha"
        case .championsLeague: return "Europa"
        case .libertadores, .sulAmericana: return "América do Sul"
        }
    }

    var espnSlug: String {
        switch self {
        case .brasileirao: return "bra.1"
        case .premierLeague: return "eng.1"
        case .laLiga: return "esp.1"
        case .championsLeague: return "uefa.champions"
        case .libertadores: return "conmebol.libertadores"
        case .copaDoBrasil: return "bra.copa_do_brazil"
        case .sulAmericana: return "conmebol.sudamericana"
        }
    }

    var badgeTag: String {
        switch self {
        case .brasileirao: return "BRA"
        case .premierLeague: return "ING"
        case .laLiga: return "ESP"
        case .championsLeague: return "UCL"
        case .libertadores: return "LIB"
        case .copaDoBrasil: return "CDB"
        case .sulAmericana: return "SUL"
        }
    }

    var accentColorHex: UInt64 {
        switch self {
        case .brasileirao: return 0xFF00E676
        case .premierLeague: return 0xFFA78BFA
        case .laLiga: return 0xFFFF6B6B
        case .championsLeague: return 0xFF38BDF8
        case .libertadores: return 0xFFF59E0B
        case .copaDoBrasil: return 0xFF22C55E
        case .sulAmericana: return 0xFF06B6D4
        }
    }

    var accentColor: Color {
        Color(argbHex: accentColorHex)
    }
}

// MARK: - Match & Standings Models

enum MatchStatus: String, Codable {
    case scheduled
    case live
    case halftime
    case finished
}

enum MatchDayFilter: String, CaseIterable, Identifiable {
    case today = "Hoje"
    case tomorrow = "Amanhã"
    case upcoming = "Próximos"

    var id: String { rawValue }
    var label: String { rawValue }
}

struct TeamInfo: Codable, Hashable {
    let name: String
    let shortName: String
    let colorHex: UInt64
    let logoUrl: String

    init(name: String, shortName: String, colorHex: UInt64, logoUrl: String = "") {
        self.name = name
        self.shortName = shortName
        self.colorHex = colorHex
        self.logoUrl = logoUrl
    }
}

struct Match: Identifiable, Codable, Hashable {
    let id: String
    let league: League
    let homeTeam: TeamInfo
    let awayTeam: TeamInfo
    let homeScore: Int
    let awayScore: Int
    let status: MatchStatus
    let minute: Int
    let kickoffTime: String
    let stadium: String
    let lastScorer: String?
    let isLiveApi: Bool
    let dayOffset: Int
    let dateLabel: String

    init(
        id: String,
        league: League,
        homeTeam: TeamInfo,
        awayTeam: TeamInfo,
        homeScore: Int,
        awayScore: Int,
        status: MatchStatus,
        minute: Int,
        kickoffTime: String,
        stadium: String,
        lastScorer: String? = nil,
        isLiveApi: Bool = false,
        dayOffset: Int = 0,
        dateLabel: String = "Hoje"
    ) {
        self.id = id
        self.league = league
        self.homeTeam = homeTeam
        self.awayTeam = awayTeam
        self.homeScore = homeScore
        self.awayScore = awayScore
        self.status = status
        self.minute = minute
        self.kickoffTime = kickoffTime
        self.stadium = stadium
        self.lastScorer = lastScorer
        self.isLiveApi = isLiveApi
        self.dayOffset = dayOffset
        self.dateLabel = dateLabel
    }

    var isLiveNow: Bool {
        status == .live || status == .halftime
    }
}

struct StandingRow: Identifiable, Hashable {
    var id: String { "\(position)_\(shortName)_\(teamName)" }
    let position: Int
    let teamName: String
    let shortName: String
    let colorHex: UInt64
    let logoUrl: String
    let played: Int
    let won: Int
    let drawn: Int
    let lost: Int
    let goalsFor: Int
    let goalsAgainst: Int
    let points: Int

    var goalDifference: Int { goalsFor - goalsAgainst }
}

// MARK: - Prediction & Scoring

struct Prediction: Codable, Hashable {
    let matchId: String
    let homeScore: Int
    let awayScore: Int
    let isJoker: Bool

    init(matchId: String, homeScore: Int, awayScore: Int, isJoker: Bool = false) {
        self.matchId = matchId
        self.homeScore = homeScore
        self.awayScore = awayScore
        self.isJoker = isJoker
    }
}

/// Exact parity with Android `calculatePredictionPoints`:
/// - 10 pts: exact scoreline
/// - 7 pts: correct outcome + exact goal difference
/// - 5 pts: correct outcome (win/draw/loss)
/// - 0 pts: miss or match still scheduled
/// - x2 multiplier when `prediction.isJoker` is true
func calculatePredictionPoints(match: Match, prediction: Prediction?) -> Int {
    guard let prediction = prediction, match.status != .scheduled else {
        return 0
    }
    let base: Int
    if prediction.homeScore == match.homeScore && prediction.awayScore == match.awayScore {
        base = 10
    } else {
        let actualDiff = match.homeScore - match.awayScore
        let predDiff = prediction.homeScore - prediction.awayScore
        let sameOutcome = (actualDiff > 0 && predDiff > 0) ||
            (actualDiff < 0 && predDiff < 0) ||
            (actualDiff == 0 && predDiff == 0)
        if !sameOutcome {
            base = 0
        } else {
            base = (actualDiff == predDiff) ? 7 : 5
        }
    }
    return prediction.isJoker ? base * 2 : base
}

// MARK: - User, Friends & Stats

struct UserAccount: Identifiable, Codable, Hashable {
    let id: String
    let name: String
    let email: String
    let password: String
    let avatarColorHex: UInt64
    let favoriteLeague: League
    let avatarImagePath: String?

    init(
        id: String,
        name: String,
        email: String,
        password: String,
        avatarColorHex: UInt64 = 0xFF00C853,
        favoriteLeague: League = .brasileirao,
        avatarImagePath: String? = nil
    ) {
        self.id = id
        self.name = name
        self.email = email
        self.password = password
        self.avatarColorHex = avatarColorHex
        self.favoriteLeague = favoriteLeague
        self.avatarImagePath = avatarImagePath
    }
}

struct FriendBolao: Identifiable, Hashable {
    let id: String
    let name: String
    let subtitle: String
    let avatarColorHex: UInt64
    let predictions: [String: Prediction]
    let isCurrentUser: Bool
    let avatarImagePath: String?
    let equippedFrameId: String?
    let equippedTitle: String?

    init(
        id: String,
        name: String,
        subtitle: String,
        avatarColorHex: UInt64,
        predictions: [String: Prediction],
        isCurrentUser: Bool = false,
        avatarImagePath: String? = nil,
        equippedFrameId: String? = nil,
        equippedTitle: String? = nil
    ) {
        self.id = id
        self.name = name
        self.subtitle = subtitle
        self.avatarColorHex = avatarColorHex
        self.predictions = predictions
        self.isCurrentUser = isCurrentUser
        self.avatarImagePath = avatarImagePath
        self.equippedFrameId = equippedFrameId
        self.equippedTitle = equippedTitle
    }
}

struct UserStats: Hashable {
    var exactHits: Int = 0
    var outcomeHits: Int = 0
    var totalEvaluated: Int = 0
    var currentStreak: Int = 0
    var accuracyPercent: Int = 0
}

struct DailyChallenge: Identifiable, Hashable {
    let id: String
    let title: String
    let description: String
    let rewardPoints: Int
    let progress: Int
    let target: Int
    let claimed: Bool

    var isCompleted: Bool { progress >= target }
}

// MARK: - Chat

struct ChatMessage: Identifiable, Codable, Hashable {
    let id: String
    let senderId: String
    let senderName: String
    let senderAvatarColorHex: UInt64
    let senderTitle: String?
    let senderFlair: String?
    let text: String
    let timestampMillis: Int64
    let isCurrentUser: Bool

    init(
        id: String,
        senderId: String,
        senderName: String,
        senderAvatarColorHex: UInt64,
        senderTitle: String? = nil,
        senderFlair: String? = nil,
        text: String,
        timestampMillis: Int64,
        isCurrentUser: Bool = false
    ) {
        self.id = id
        self.senderId = senderId
        self.senderName = senderName
        self.senderAvatarColorHex = senderAvatarColorHex
        self.senderTitle = senderTitle
        self.senderFlair = senderFlair
        self.text = text
        self.timestampMillis = timestampMillis
        self.isCurrentUser = isCurrentUser
    }
}

// MARK: - Shop

enum ShopCategory: String, CaseIterable, Codable {
    case avatarFrame = "Molduras"
    case titleBadge = "Títulos"
    case chatFlair = "Chat VIP"

    var label: String { rawValue }
}

struct ShopItem: Identifiable, Hashable {
    let id: String
    let name: String
    let description: String
    let price: Int
    let category: ShopCategory
    let accentHex: UInt64
    let badgeText: String
}

struct UserShopState: Hashable {
    var availablePoints: Int = 30
    var ownedItemIds: Set<String> = []
    var equippedFrameId: String? = nil
    var equippedTitleId: String? = nil
    var equippedFlairId: String? = nil
}

enum ShopCatalog {
    static let items: [ShopItem] = [
        ShopItem(
            id: "frame_gold",
            name: "Moldura Ouro",
            description: "Brilho dourado de campeão ao redor do seu avatar.",
            price: 20,
            category: .avatarFrame,
            accentHex: 0xFFFFD600,
            badgeText: "OURO"
        ),
        ShopItem(
            id: "frame_champions",
            name: "Moldura Champions",
            description: "Aro azul estelar inspirado nas noites europeias.",
            price: 35,
            category: .avatarFrame,
            accentHex: 0xFF38BDF8,
            badgeText: "UCL"
        ),
        ShopItem(
            id: "frame_fire",
            name: "Moldura Fogo",
            description: "Borda flamejante para quem está em sequência quente.",
            price: 50,
            category: .avatarFrame,
            accentHex: 0xFFFF5722,
            badgeText: "FOGO"
        ),
        ShopItem(
            id: "frame_legend",
            name: "Moldura Lenda",
            description: "Aro roxo lendário reservado aos mitos do bolão.",
            price: 80,
            category: .avatarFrame,
            accentHex: 0xFFA855F7,
            badgeText: "LENDA"
        ),
        ShopItem(
            id: "title_profeta",
            name: "Profeta",
            description: "Título exibido ao lado do seu nome no chat.",
            price: 15,
            category: .titleBadge,
            accentHex: 0xFF10B981,
            badgeText: "Profeta"
        ),
        ShopItem(
            id: "title_artilheiro",
            name: "Artilheiro",
            description: "Para quem acerta placares em sequência.",
            price: 30,
            category: .titleBadge,
            accentHex: 0xFFEF4444,
            badgeText: "Artilheiro"
        ),
        ShopItem(
            id: "title_capitao",
            name: "Capitão do Grupo",
            description: "Liderança reconhecida na resenha da sala.",
            price: 45,
            category: .titleBadge,
            accentHex: 0xFF3B82F6,
            badgeText: "Capitão"
        ),
        ShopItem(
            id: "title_rei_cravada",
            name: "Rei da Cravada",
            description: "O terror dos palpites exatos de 10 pontos.",
            price: 70,
            category: .titleBadge,
            accentHex: 0xFFF59E0B,
            badgeText: "Rei da Cravada"
        ),
        ShopItem(
            id: "flair_golaco",
            name: "Destaque Golaço",
            description: "Seu balão no chat ganha destaque esmeralda.",
            price: 25,
            category: .chatFlair,
            accentHex: 0xFF00E676,
            badgeText: "GOLAÇO"
        ),
        ShopItem(
            id: "flair_vip",
            name: "Camarote VIP",
            description: "Balão VIP dourado exclusivo no bate-papo.",
            price: 60,
            category: .chatFlair,
            accentHex: 0xFFFFD600,
            badgeText: "VIP"
        )
    ]

    static func findById(_ id: String?) -> ShopItem? {
        guard let id = id, !id.isEmpty else { return nil }
        return items.first { $0.id == id }
    }
}
