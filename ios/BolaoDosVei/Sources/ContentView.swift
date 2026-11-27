import PhotosUI
import SwiftUI
import UIKit

// MARK: - Design Tokens (OriginKit / Templens / Skiper Matte Obsidian)

private enum ObsidianTheme {
    static let background = Color(rgbHex: 0x09090B)
    static let surface = Color(rgbHex: 0x121216)
    static let surfaceElevated = Color(rgbHex: 0x18181D)
    static let border = Color.white.opacity(0.08)
    static let emerald = Color(rgbHex: 0x00E676)
    static let gold = Color(rgbHex: 0xFFD600)
    static let crimson = Color(rgbHex: 0xEF4444)
    static let textPrimary = Color.white
    static let textSecondary = Color(rgbHex: 0xA1A1AA)
}

private enum AppTab: String, CaseIterable, Identifiable {
    case bolao = "Bolão"
    case standings = "Tabelas"
    case chat = "Chat"
    case shop = "Loja"
    case group = "Grupo"

    var id: String { rawValue }

    var iconName: String {
        switch self {
        case .bolao: return "sportscourt.fill"
        case .standings: return "list.number"
        case .chat: return "bubble.left.and.bubble.right.fill"
        case .shop: return "bag.fill"
        case .group: return "person.3.fill"
        }
    }
}

// MARK: - Root ContentView

struct ContentView: View {
    @EnvironmentObject private var viewModel: BolaoViewModel
    @State private var selectedTab: AppTab = .bolao

    var body: some View {
        ZStack(alignment: .bottom) {
            ObsidianTheme.background.ignoresSafeArea()

            if viewModel.currentUser == nil {
                AuthGateView()
            } else {
                VStack(spacing: 0) {
                    DynamicIslandTicker(matches: viewModel.matches)
                        .padding(.horizontal, 16)
                        .padding(.top, 8)
                        .padding(.bottom, 6)

                    Group {
                        switch selectedTab {
                        case .bolao:
                            BolaoMatchesView()
                        case .standings:
                            StandingsView()
                        case .chat:
                            ChatRoomView()
                        case .shop:
                            ShopAndChallengesView()
                        case .group:
                            GroupAndProfileView()
                        }
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                }

                FloatingPillDock(selectedTab: $selectedTab)
                    .padding(.horizontal, 16)
                    .padding(.bottom, 8)
            }

            if let banner = viewModel.bannerMessage {
                VStack {
                    Text(banner)
                        .font(.caption.weight(.semibold))
                        .foregroundColor(.black)
                        .padding(.horizontal, 14)
                        .padding(.vertical, 8)
                        .background(ObsidianTheme.emerald, in: Capsule())
                        .shadow(color: .black.opacity(0.45), radius: 10, y: 4)
                        .padding(.top, 12)
                    Spacer()
                }
                .transition(.move(edge: .top).combined(with: .opacity))
                .animation(.spring(response: 0.3), value: viewModel.bannerMessage)
            }
        }
    }
}

// MARK: - Dynamic Island Live Ticker

private struct DynamicIslandTicker: View {
    let matches: [Match]

    var liveMatches: [Match] {
        matches.filter { $0.isLiveNow }
    }

    var body: some View {
        HStack(spacing: 10) {
            Circle()
                .fill(liveMatches.isEmpty ? ObsidianTheme.textSecondary : ObsidianTheme.emerald)
                .frame(width: 8, height: 8)

            if let first = liveMatches.first {
                Text("AO VIVO (\(liveMatches.count))")
                    .font(.caption2.weight(.bold))
                    .foregroundColor(ObsidianTheme.emerald)
                Text("\(first.homeTeam.shortName) \(first.homeScore)-\(first.awayScore) \(first.awayTeam.shortName) • \(first.minute)'")
                    .font(.caption.weight(.semibold))
                    .foregroundColor(ObsidianTheme.textPrimary)
                    .lineLimit(1)
            } else {
                Text("BOLÃO DOS VÉI")
                    .font(.caption2.weight(.bold))
                    .foregroundColor(ObsidianTheme.emerald)
                Text("\(matches.count) jogos hoje")
                    .font(.caption)
                    .foregroundColor(ObsidianTheme.textSecondary)
            }

            Spacer()

            Text("iOS")
                .font(.caption2.weight(.bold))
                .foregroundColor(.black)
                .padding(.horizontal, 7)
                .padding(.vertical, 3)
                .background(ObsidianTheme.emerald, in: Capsule())
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 10)
        .background(ObsidianTheme.surfaceElevated, in: Capsule())
        .overlay(Capsule().stroke(ObsidianTheme.border, lineWidth: 1))
    }
}

// MARK: - Floating Pill Dock

private struct FloatingPillDock: View {
    @Binding var selectedTab: AppTab

    var body: some View {
        HStack(spacing: 4) {
            ForEach(AppTab.allCases) { tab in
                let isSelected = (selectedTab == tab)
                Button {
                    UIImpactFeedbackGenerator(style: .light).impactOccurred()
                    withAnimation(.spring(response: 0.25, dampingFraction: 0.8)) {
                        selectedTab = tab
                    }
                } label: {
                    VStack(spacing: 3) {
                        Image(systemName: tab.iconName)
                            .font(.system(size: 15, weight: .semibold))
                        Text(tab.rawValue)
                            .font(.system(size: 10, weight: .semibold))
                    }
                    .foregroundColor(isSelected ? .black : ObsidianTheme.textSecondary)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 8)
                    .background(
                        isSelected ? ObsidianTheme.emerald : Color.clear,
                        in: Capsule()
                    )
                }
                .buttonStyle(.plain)
            }
        }
        .padding(6)
        .background(.ultraThinMaterial, in: Capsule())
        .background(ObsidianTheme.surface.opacity(0.9), in: Capsule())
        .overlay(Capsule().stroke(ObsidianTheme.border, lineWidth: 1))
        .shadow(color: .black.opacity(0.55), radius: 16, y: 8)
    }
}

// MARK: - Auth Gate View

private struct AuthGateView: View {
    @EnvironmentObject private var viewModel: BolaoViewModel
    @State private var isRegisterMode = false
    @State private var name = ""
    @State private var email = ""
    @State private var password = ""
    @State private var selectedLeague: League = .brasileirao
    @State private var selectedColorHex: UInt64 = 0xFF00C853

    private let avatarPalette: [UInt64] = [
        0xFF00C853, 0xFF38BDF8, 0xFFF59E0B, 0xFFEF4444, 0xFFA855F7, 0xFF06B6D4
    ]

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                VStack(spacing: 8) {
                    Image(systemName: "trophy.fill")
                        .font(.system(size: 44))
                        .foregroundColor(ObsidianTheme.emerald)
                    Text("Bolão dos Véi")
                        .font(.title.bold())
                        .foregroundColor(ObsidianTheme.textPrimary)
                    Text("Palpites ao vivo, Coringa 2x e sala sincronizada com Android.")
                        .font(.subheadline)
                        .foregroundColor(ObsidianTheme.textSecondary)
                        .multilineTextAlignment(.center)
                }
                .padding(.top, 36)

                VStack(spacing: 14) {
                    Picker("Modo", selection: $isRegisterMode) {
                        Text("Entrar").tag(false)
                        Text("Criar Conta").tag(true)
                    }
                    .pickerStyle(.segmented)

                    if isRegisterMode {
                        TextField("Seu nome ou apelido", text: $name)
                            .textFieldStyle(ObsidianTextFieldStyle())
                    }

                    TextField("E-mail", text: $email)
                        .keyboardType(.emailAddress)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .textFieldStyle(ObsidianTextFieldStyle())

                    SecureField("Senha (mín. 4 caracteres)", text: $password)
                        .textFieldStyle(ObsidianTextFieldStyle())

                    if isRegisterMode {
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Liga favorita")
                                .font(.caption.weight(.semibold))
                                .foregroundColor(ObsidianTheme.textSecondary)
                            ScrollView(.horizontal, showsIndicators: false) {
                                HStack(spacing: 8) {
                                    ForEach(League.allCases) { league in
                                        Button {
                                            selectedLeague = league
                                        } label: {
                                            Text(league.shortName)
                                                .font(.caption.weight(.semibold))
                                                .foregroundColor(selectedLeague == league ? .black : ObsidianTheme.textPrimary)
                                                .padding(.horizontal, 12)
                                                .padding(.vertical, 6)
                                                .background(
                                                    selectedLeague == league ? league.accentColor : ObsidianTheme.surfaceElevated,
                                                    in: Capsule()
                                                )
                                        }
                                    }
                                }
                            }
                        }

                        HStack(spacing: 12) {
                            Text("Cor:")
                                .font(.caption.weight(.semibold))
                                .foregroundColor(ObsidianTheme.textSecondary)
                            ForEach(avatarPalette, id: \.self) { hex in
                                Circle()
                                    .fill(Color(argbHex: hex))
                                    .frame(width: 26, height: 26)
                                    .overlay(
                                        Circle().stroke(Color.white, lineWidth: selectedColorHex == hex ? 2.5 : 0)
                                    )
                                    .onTapGesture { selectedColorHex = hex }
                            }
                            Spacer()
                        }
                    }

                    if let error = viewModel.authError {
                        Text(error)
                            .font(.caption.weight(.medium))
                            .foregroundColor(ObsidianTheme.crimson)
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }

                    Button {
                        if isRegisterMode {
                            viewModel.register(
                                name: name,
                                email: email,
                                password: password,
                                avatarColorHex: selectedColorHex,
                                favoriteLeague: selectedLeague
                            )
                        } else {
                            viewModel.login(email: email, password: password)
                        }
                    } label: {
                        Text(isRegisterMode ? "Criar Conta e Entrar" : "Entrar no Bolão")
                            .font(.headline)
                            .foregroundColor(.black)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                            .background(ObsidianTheme.emerald, in: RoundedRectangle(cornerRadius: 14))
                    }
                }
                .padding(18)
                .background(ObsidianTheme.surface, in: RoundedRectangle(cornerRadius: 20))
                .overlay(RoundedRectangle(cornerRadius: 20).stroke(ObsidianTheme.border, lineWidth: 1))
            }
            .padding(.horizontal, 20)
        }
    }
}

// MARK: - Bolão Matches View

private struct BolaoMatchesView: View {
    @EnvironmentObject private var viewModel: BolaoViewModel
    @State private var compareMatch: Match?

    private var filteredMatches: [Match] {
        let base: [Match]
        switch viewModel.selectedDayFilter {
        case .today:
            base = viewModel.matches
        case .tomorrow:
            base = viewModel.upcomingMatches.filter { $0.dayOffset == 1 }
        case .upcoming:
            base = viewModel.upcomingMatches.filter { $0.dayOffset >= 2 }
        }
        if let league = viewModel.selectedLeague {
            return base.filter { $0.league == league }
        }
        return base
    }

    private var pendingCount: Int {
        filteredMatches.filter { $0.status == .scheduled && viewModel.predictions[$0.id] == nil }.count
    }

    var body: some View {
        ScrollView {
            LazyVStack(spacing: 14) {
                // Header + pending badge
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Palpites da Rodada")
                            .font(.title3.bold())
                            .foregroundColor(ObsidianTheme.textPrimary)
                        Text("10 pts cravada • 7 pts saldo • 5 pts vencedor")
                            .font(.caption2)
                            .foregroundColor(ObsidianTheme.textSecondary)
                    }
                    Spacer()
                    if pendingCount > 0 {
                        Text("Faltam \(pendingCount)")
                            .font(.caption2.weight(.bold))
                            .foregroundColor(.black)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 5)
                            .background(ObsidianTheme.gold, in: Capsule())
                    }
                }

                // Day Filter Segmented Row
                Picker("Dia", selection: $viewModel.selectedDayFilter) {
                    ForEach(MatchDayFilter.allCases) { filter in
                        Text(filter.label).tag(filter)
                    }
                }
                .pickerStyle(.segmented)

                // 7 Leagues Filter Chips
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        LeagueChip(
                            title: "Todas",
                            accent: ObsidianTheme.emerald,
                            isSelected: viewModel.selectedLeague == nil
                        ) {
                            viewModel.selectedLeague = nil
                        }
                        ForEach(League.allCases) { league in
                            LeagueChip(
                                title: league.shortName,
                                accent: league.accentColor,
                                isSelected: viewModel.selectedLeague == league
                            ) {
                                viewModel.selectedLeague = league
                            }
                        }
                    }
                }

                if filteredMatches.isEmpty {
                    VStack(spacing: 8) {
                        Image(systemName: "sportscourt")
                            .font(.title)
                            .foregroundColor(ObsidianTheme.textSecondary)
                        Text("Nenhum jogo encontrado para este filtro.")
                            .font(.subheadline)
                            .foregroundColor(ObsidianTheme.textSecondary)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 40)
                    .background(ObsidianTheme.surface, in: RoundedRectangle(cornerRadius: 18))
                    .overlay(RoundedRectangle(cornerRadius: 18).stroke(ObsidianTheme.border, lineWidth: 1))
                } else {
                    ForEach(filteredMatches) { match in
                        MatchBentoCard(
                            match: match,
                            savedPrediction: viewModel.predictions[match.id],
                            onSave: { h, a in
                                viewModel.savePrediction(matchId: match.id, homeScore: h, awayScore: a)
                            },
                            onToggleJoker: {
                                viewModel.toggleJoker(matchId: match.id)
                            },
                            onCompare: {
                                compareMatch = match
                            }
                        )
                    }
                }
            }
            .padding(.horizontal, 16)
            .padding(.top, 8)
            .padding(.bottom, 96)
        }
        .refreshable {
            await viewModel.refreshLiveData()
        }
        .sheet(item: $compareMatch) { match in
            CompareFriendsSheet(match: match)
                .environmentObject(viewModel)
        }
    }
}

private struct MatchBentoCard: View {
    let match: Match
    let savedPrediction: Prediction?
    let onSave: (Int, Int) -> Void
    let onToggleJoker: () -> Void
    let onCompare: () -> Void

    @State private var draftHome: Int = 0
    @State private var draftAway: Int = 0

    private var isLocked: Bool { match.status != .scheduled }
    private var isSavedUnchanged: Bool {
        guard let saved = savedPrediction else { return false }
        return saved.homeScore == draftHome && saved.awayScore == draftAway
    }
    private var earnedPoints: Int {
        calculatePredictionPoints(match: match, prediction: savedPrediction)
    }

    var body: some View {
        VStack(spacing: 12) {
            // Top bar: League + Status / Time
            HStack {
                Text(match.league.badgeTag)
                    .font(.caption2.weight(.bold))
                    .foregroundColor(.black)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 3)
                    .background(match.league.accentColor, in: Capsule())

                Text("\(match.dateLabel) • \(match.kickoffTime)")
                    .font(.caption2)
                    .foregroundColor(ObsidianTheme.textSecondary)

                Spacer()

                if match.isLiveNow {
                    Text(match.status == .halftime ? "INTERVALO" : "AO VIVO \(match.minute)'")
                        .font(.caption2.weight(.bold))
                        .foregroundColor(ObsidianTheme.emerald)
                } else if match.status == .finished {
                    Text("ENCERRADO")
                        .font(.caption2.weight(.bold))
                        .foregroundColor(ObsidianTheme.textSecondary)
                } else {
                    Text("ABERTO")
                        .font(.caption2.weight(.bold))
                        .foregroundColor(ObsidianTheme.emerald)
                }
            }

            // Teams & Live Score
            HStack(spacing: 10) {
                TeamColumn(team: match.homeTeam)
                VStack(spacing: 4) {
                    if match.status == .scheduled {
                        Text("VS")
                            .font(.headline.bold())
                            .foregroundColor(ObsidianTheme.textSecondary)
                    } else {
                        Text("\(match.homeScore) - \(match.awayScore)")
                            .font(.title2.bold().monospacedDigit())
                            .foregroundColor(ObsidianTheme.textPrimary)
                    }
                    if earnedPoints > 0 {
                        Text("+\(earnedPoints) pts")
                            .font(.caption2.weight(.bold))
                            .foregroundColor(ObsidianTheme.gold)
                    }
                }
                .frame(minWidth: 68)
                TeamColumn(team: match.awayTeam)
            }

            // Nested Prediction Controls
            VStack(spacing: 10) {
                HStack(spacing: 16) {
                    ScoreStepper(value: $draftHome, disabled: isLocked)
                    Text("x")
                        .font(.subheadline.bold())
                        .foregroundColor(ObsidianTheme.textSecondary)
                    ScoreStepper(value: $draftAway, disabled: isLocked)
                }

                HStack(spacing: 8) {
                    if isLocked {
                        Label("Travado", systemImage: "lock.fill")
                            .font(.caption.weight(.semibold))
                            .foregroundColor(ObsidianTheme.textSecondary)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 7)
                            .background(ObsidianTheme.surface, in: Capsule())
                    } else {
                        Button {
                            UIImpactFeedbackGenerator(style: .medium).impactOccurred()
                            onToggleJoker()
                        } label: {
                            let isJoker = savedPrediction?.isJoker ?? false
                            Text("★ Coringa 2x")
                                .font(.caption.weight(.bold))
                                .foregroundColor(isJoker ? .black : ObsidianTheme.gold)
                                .padding(.horizontal, 10)
                                .padding(.vertical, 7)
                                .background(
                                    isJoker ? ObsidianTheme.gold : ObsidianTheme.surface,
                                    in: Capsule()
                                )
                                .overlay(Capsule().stroke(ObsidianTheme.gold.opacity(0.5), lineWidth: 1))
                        }

                        Button {
                            UIImpactFeedbackGenerator(style: .medium).impactOccurred()
                            onSave(draftHome, draftAway)
                        } label: {
                            Text(isSavedUnchanged ? "Salvo ✓" : "Salvar")
                                .font(.caption.weight(.bold))
                                .foregroundColor(isSavedUnchanged ? ObsidianTheme.textSecondary : .black)
                                .padding(.horizontal, 14)
                                .padding(.vertical, 7)
                                .background(
                                    isSavedUnchanged ? ObsidianTheme.surface : ObsidianTheme.emerald,
                                    in: Capsule()
                                )
                        }
                        .disabled(isSavedUnchanged)
                    }

                    Spacer()

                    Button {
                        onCompare()
                    } label: {
                        Label("Amigos", systemImage: "person.2.fill")
                            .font(.caption.weight(.semibold))
                            .foregroundColor(ObsidianTheme.textPrimary)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 7)
                            .background(ObsidianTheme.surface, in: Capsule())
                    }
                }
            }
            .padding(12)
            .background(ObsidianTheme.surfaceElevated, in: RoundedRectangle(cornerRadius: 14))
        }
        .padding(14)
        .background(ObsidianTheme.surface, in: RoundedRectangle(cornerRadius: 20))
        .overlay(RoundedRectangle(cornerRadius: 20).stroke(ObsidianTheme.border, lineWidth: 1))
        .onAppear {
            if let saved = savedPrediction {
                draftHome = saved.homeScore
                draftAway = saved.awayScore
            }
        }
        .onChange(of: savedPrediction) { newValue in
            if let saved = newValue {
                draftHome = saved.homeScore
                draftAway = saved.awayScore
            }
        }
    }
}

private struct TeamColumn: View {
    let team: TeamInfo

    var body: some View {
        VStack(spacing: 6) {
            TeamCrestView(logoUrl: team.logoUrl, shortName: team.shortName, colorHex: team.colorHex, size: 38)
            Text(team.name)
                .font(.caption.weight(.semibold))
                .foregroundColor(ObsidianTheme.textPrimary)
                .lineLimit(1)
        }
        .frame(maxWidth: .infinity)
    }
}

private struct ScoreStepper: View {
    @Binding var value: Int
    let disabled: Bool

    var body: some View {
        HStack(spacing: 8) {
            Button {
                if value > 0 {
                    UIImpactFeedbackGenerator(style: .light).impactOccurred()
                    value -= 1
                }
            } label: {
                Image(systemName: "minus")
                    .font(.caption.bold())
                    .foregroundColor(disabled ? ObsidianTheme.textSecondary : .white)
                    .frame(width: 28, height: 28)
                    .background(ObsidianTheme.surface, in: Circle())
            }
            .disabled(disabled)

            Text("\(value)")
                .font(.headline.monospacedDigit())
                .foregroundColor(ObsidianTheme.textPrimary)
                .frame(minWidth: 22)

            Button {
                if value < 15 {
                    UIImpactFeedbackGenerator(style: .light).impactOccurred()
                    value += 1
                }
            } label: {
                Image(systemName: "plus")
                    .font(.caption.bold())
                    .foregroundColor(disabled ? ObsidianTheme.textSecondary : .white)
                    .frame(width: 28, height: 28)
                    .background(ObsidianTheme.surface, in: Circle())
            }
            .disabled(disabled)
        }
    }
}

private struct CompareFriendsSheet: View {
    @EnvironmentObject private var viewModel: BolaoViewModel
    let match: Match

    var body: some View {
        NavigationStack {
            List {
                ForEach(viewModel.friends) { friend in
                    let pred = friend.predictions[match.id]
                    let pts = calculatePredictionPoints(match: match, prediction: pred)
                    HStack {
                        UserAvatarView(
                            name: friend.name,
                            colorHex: friend.avatarColorHex,
                            photoPath: friend.avatarImagePath,
                            frameId: friend.equippedFrameId,
                            size: 36
                        )
                        VStack(alignment: .leading, spacing: 2) {
                            HStack(spacing: 6) {
                                Text(friend.name)
                                    .font(.subheadline.weight(.semibold))
                                if let title = friend.equippedTitle {
                                    Text(title)
                                        .font(.caption2.bold())
                                        .foregroundColor(ObsidianTheme.emerald)
                                }
                            }
                            if let p = pred {
                                Text("Palpite: \(p.homeScore) x \(p.awayScore)\(p.isJoker ? " ★2x" : "")")
                                    .font(.caption)
                                    .foregroundColor(ObsidianTheme.textSecondary)
                            } else {
                                Text("Sem palpite")
                                    .font(.caption)
                                    .foregroundColor(ObsidianTheme.textSecondary)
                            }
                        }
                        Spacer()
                        if pts >= 10 {
                            Text("CRAVOU! +\(pts)")
                                .font(.caption2.bold())
                                .foregroundColor(.black)
                                .padding(.horizontal, 8)
                                .padding(.vertical, 4)
                                .background(ObsidianTheme.gold, in: Capsule())
                        } else if pts > 0 {
                            Text("+\(pts) pts")
                                .font(.caption.bold())
                                .foregroundColor(ObsidianTheme.emerald)
                        }
                    }
                    .listRowBackground(ObsidianTheme.surface)
                }
            }
            .scrollContentBackground(.hidden)
            .background(ObsidianTheme.background)
            .navigationTitle("\(match.homeTeam.shortName) x \(match.awayTeam.shortName)")
            .navigationBarTitleDisplayMode(.inline)
        }
    }
}

// MARK: - Standings View

private struct StandingsView: View {
    @EnvironmentObject private var viewModel: BolaoViewModel

    private var rows: [StandingRow] {
        viewModel.standingsByLeague[viewModel.selectedStandingsLeague] ?? []
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 14) {
                Text("Tabelas & Classificação")
                    .font(.title3.bold())
                    .foregroundColor(ObsidianTheme.textPrimary)
                    .frame(maxWidth: .infinity, alignment: .leading)

                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(League.allCases) { league in
                            LeagueChip(
                                title: league.shortName,
                                accent: league.accentColor,
                                isSelected: viewModel.selectedStandingsLeague == league
                            ) {
                                viewModel.selectedStandingsLeague = league
                            }
                        }
                    }
                }

                VStack(spacing: 0) {
                    HStack {
                        Text("#").frame(width: 26, alignment: .leading)
                        Text("Time").frame(maxWidth: .infinity, alignment: .leading)
                        Text("P").frame(width: 30)
                        Text("J").frame(width: 26)
                        Text("V").frame(width: 26)
                        Text("SG").frame(width: 32)
                    }
                    .font(.caption2.bold())
                    .foregroundColor(ObsidianTheme.textSecondary)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 10)

                    Divider().background(ObsidianTheme.border)

                    ForEach(rows) { row in
                        HStack(spacing: 6) {
                            Text("\(row.position)")
                                .font(.caption.bold().monospacedDigit())
                                .foregroundColor(row.position <= 4 ? ObsidianTheme.emerald : ObsidianTheme.textSecondary)
                                .frame(width: 26, alignment: .leading)

                            TeamCrestView(logoUrl: row.logoUrl, shortName: row.shortName, colorHex: row.colorHex, size: 22)

                            Text(row.teamName)
                                .font(.caption.weight(.semibold))
                                .foregroundColor(ObsidianTheme.textPrimary)
                                .lineLimit(1)
                                .frame(maxWidth: .infinity, alignment: .leading)

                            Text("\(row.points)")
                                .font(.caption.bold().monospacedDigit())
                                .foregroundColor(ObsidianTheme.textPrimary)
                                .frame(width: 30)
                            Text("\(row.played)")
                                .font(.caption.monospacedDigit())
                                .foregroundColor(ObsidianTheme.textSecondary)
                                .frame(width: 26)
                            Text("\(row.won)")
                                .font(.caption.monospacedDigit())
                                .foregroundColor(ObsidianTheme.textSecondary)
                                .frame(width: 26)
                            Text("\(row.goalDifference)")
                                .font(.caption.monospacedDigit())
                                .foregroundColor(ObsidianTheme.textSecondary)
                                .frame(width: 32)
                        }
                        .padding(.horizontal, 12)
                        .padding(.vertical, 9)

                        Divider().background(ObsidianTheme.border)
                    }
                }
                .background(ObsidianTheme.surface, in: RoundedRectangle(cornerRadius: 18))
                .overlay(RoundedRectangle(cornerRadius: 18).stroke(ObsidianTheme.border, lineWidth: 1))
            }
            .padding(.horizontal, 16)
            .padding(.top, 8)
            .padding(.bottom, 96)
        }
        .refreshable {
            await viewModel.refreshLiveData()
        }
    }
}

// MARK: - Chat Room View

private struct ChatRoomView: View {
    @EnvironmentObject private var viewModel: BolaoViewModel
    @State private var draftMessage = ""

    private let quickReactions = [
        "⚽ GOLAÇO!",
        "🤡 ZIKOU!",
        "🔥 CRAVEI!",
        "👀 CHAMA O VAR!"
    ]

    var body: some View {
        VStack(spacing: 10) {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Resenha da Sala")
                        .font(.title3.bold())
                        .foregroundColor(ObsidianTheme.textPrimary)
                    Text(viewModel.roomCode.isEmpty ? "Chat local (entre em uma sala para sincronizar)" : "Sala \(viewModel.roomCode) • Sincronizado")
                        .font(.caption2)
                        .foregroundColor(ObsidianTheme.textSecondary)
                }
                Spacer()
            }
            .padding(.horizontal, 16)
            .padding(.top, 8)

            ScrollViewReader { proxy in
                ScrollView {
                    LazyVStack(spacing: 10) {
                        if viewModel.chatMessages.isEmpty {
                            Text("Nenhuma mensagem ainda. Mande a primeira resenha!")
                                .font(.subheadline)
                                .foregroundColor(ObsidianTheme.textSecondary)
                                .padding(.top, 40)
                        } else {
                            ForEach(viewModel.chatMessages) { msg in
                                ChatBubbleRow(message: msg)
                                    .id(msg.id)
                            }
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 8)
                }
                .onChange(of: viewModel.chatMessages.count) { _ in
                    if let last = viewModel.chatMessages.last {
                        withAnimation { proxy.scrollTo(last.id, anchor: .bottom) }
                    }
                }
            }

            // Quick Reactions Bar
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(quickReactions, id: \.self) { reaction in
                        Button {
                            UIImpactFeedbackGenerator(style: .light).impactOccurred()
                            viewModel.sendQuickReaction(reaction)
                        } label: {
                            Text(reaction)
                                .font(.caption.weight(.semibold))
                                .foregroundColor(ObsidianTheme.textPrimary)
                                .padding(.horizontal, 12)
                                .padding(.vertical, 6)
                                .background(ObsidianTheme.surfaceElevated, in: Capsule())
                                .overlay(Capsule().stroke(ObsidianTheme.border, lineWidth: 1))
                        }
                    }
                }
                .padding(.horizontal, 16)
            }

            // Composer
            HStack(spacing: 10) {
                TextField("Mande uma mensagem...", text: $draftMessage)
                    .textFieldStyle(ObsidianTextFieldStyle())

                Button {
                    let text = draftMessage
                    draftMessage = ""
                    viewModel.sendChatMessage(text)
                } label: {
                    Image(systemName: "paperplane.fill")
                        .font(.headline)
                        .foregroundColor(.black)
                        .padding(12)
                        .background(ObsidianTheme.emerald, in: Circle())
                }
                .disabled(draftMessage.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 86)
        }
        .onAppear { viewModel.setChatScreenVisible(true) }
        .onDisappear { viewModel.setChatScreenVisible(false) }
    }
}

private struct ChatBubbleRow: View {
    let message: ChatMessage

    private var flairColor: Color {
        switch message.senderFlair {
        case "flair_vip": return ObsidianTheme.gold
        case "flair_golaco": return ObsidianTheme.emerald
        default: return message.isCurrentUser ? ObsidianTheme.emerald.opacity(0.22) : ObsidianTheme.surface
        }
    }

    private var isVipFlair: Bool {
        message.senderFlair == "flair_vip" || message.senderFlair == "flair_golaco"
    }

    var body: some View {
        HStack {
            if message.isCurrentUser { Spacer(minLength: 48) }

            VStack(alignment: message.isCurrentUser ? .trailing : .leading, spacing: 4) {
                HStack(spacing: 6) {
                    Text(message.senderName)
                        .font(.caption2.bold())
                        .foregroundColor(Color(argbHex: message.senderAvatarColorHex))
                    if let title = message.senderTitle {
                        Text(title)
                            .font(.caption2.bold())
                            .foregroundColor(.black)
                            .padding(.horizontal, 6)
                            .padding(.vertical, 2)
                            .background(ObsidianTheme.gold, in: Capsule())
                    }
                }

                Text(message.text)
                    .font(.subheadline)
                    .foregroundColor(isVipFlair ? .black : ObsidianTheme.textPrimary)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 9)
                    .background(flairColor, in: RoundedRectangle(cornerRadius: 16))
                    .overlay(
                        RoundedRectangle(cornerRadius: 16)
                            .stroke(ObsidianTheme.border, lineWidth: isVipFlair ? 0 : 1)
                    )
            }

            if !message.isCurrentUser { Spacer(minLength: 48) }
        }
    }
}

// MARK: - Shop & Daily Challenges View

private struct ShopAndChallengesView: View {
    @EnvironmentObject private var viewModel: BolaoViewModel

    var body: some View {
        ScrollView {
            LazyVStack(spacing: 16) {
                // Live Profile Preview
                if let user = viewModel.currentUser {
                    HStack(spacing: 14) {
                        UserAvatarView(
                            name: user.name,
                            colorHex: user.avatarColorHex,
                            photoPath: user.avatarImagePath,
                            frameId: viewModel.shopState.equippedFrameId,
                            size: 54
                        )
                        VStack(alignment: .leading, spacing: 4) {
                            HStack(spacing: 6) {
                                Text(user.name)
                                    .font(.headline)
                                    .foregroundColor(ObsidianTheme.textPrimary)
                                if let title = ShopCatalog.findById(viewModel.shopState.equippedTitleId)?.badgeText {
                                    Text(title)
                                        .font(.caption2.bold())
                                        .foregroundColor(.black)
                                        .padding(.horizontal, 7)
                                        .padding(.vertical, 2)
                                        .background(ObsidianTheme.gold, in: Capsule())
                                }
                            }
                            Text("Saldo: \(viewModel.shopState.availablePoints) pts")
                                .font(.subheadline.bold())
                                .foregroundColor(ObsidianTheme.emerald)
                        }
                        Spacer()
                    }
                    .padding(16)
                    .background(ObsidianTheme.surface, in: RoundedRectangle(cornerRadius: 20))
                    .overlay(RoundedRectangle(cornerRadius: 20).stroke(ObsidianTheme.border, lineWidth: 1))
                }

                // Daily Challenges
                if !viewModel.dailyChallenges.isEmpty {
                    VStack(alignment: .leading, spacing: 10) {
                        Text("Desafios Diários")
                            .font(.headline)
                            .foregroundColor(ObsidianTheme.textPrimary)

                        ForEach(viewModel.dailyChallenges) { challenge in
                            HStack(spacing: 12) {
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(challenge.title)
                                        .font(.subheadline.weight(.semibold))
                                        .foregroundColor(ObsidianTheme.textPrimary)
                                    ProgressView(value: Double(challenge.progress), total: Double(challenge.target))
                                        .tint(ObsidianTheme.emerald)
                                    Text("\(challenge.progress)/\(challenge.target) • +\(challenge.rewardPoints) pts")
                                        .font(.caption2)
                                        .foregroundColor(ObsidianTheme.textSecondary)
                                }

                                Button {
                                    UIImpactFeedbackGenerator(style: .medium).impactOccurred()
                                    viewModel.claimDailyChallenge(challenge.id)
                                } label: {
                                    Text(challenge.claimed ? "Resgatado ✓" : "Resgatar")
                                        .font(.caption.bold())
                                        .foregroundColor(challenge.isCompleted && !challenge.claimed ? .black : ObsidianTheme.textSecondary)
                                        .padding(.horizontal, 12)
                                        .padding(.vertical, 7)
                                        .background(
                                            challenge.isCompleted && !challenge.claimed ? ObsidianTheme.emerald : ObsidianTheme.surface,
                                            in: Capsule()
                                        )
                                }
                                .disabled(!challenge.isCompleted || challenge.claimed)
                            }
                            .padding(12)
                            .background(ObsidianTheme.surfaceElevated, in: RoundedRectangle(cornerRadius: 14))
                        }
                    }
                    .padding(16)
                    .background(ObsidianTheme.surface, in: RoundedRectangle(cornerRadius: 20))
                    .overlay(RoundedRectangle(cornerRadius: 20).stroke(ObsidianTheme.border, lineWidth: 1))
                }

                // Catalog Items
                ForEach(ShopCategory.allCases, id: \.self) { category in
                    VStack(alignment: .leading, spacing: 10) {
                        Text(category.label)
                            .font(.headline)
                            .foregroundColor(ObsidianTheme.textPrimary)

                        ForEach(ShopCatalog.items.filter { $0.category == category }) { item in
                            let owned = viewModel.shopState.ownedItemIds.contains(item.id)
                            let equipped = viewModel.shopState.equippedFrameId == item.id ||
                                viewModel.shopState.equippedTitleId == item.id ||
                                viewModel.shopState.equippedFlairId == item.id

                            HStack(spacing: 12) {
                                Text(item.badgeText)
                                    .font(.caption2.bold())
                                    .foregroundColor(.black)
                                    .padding(.horizontal, 8)
                                    .padding(.vertical, 5)
                                    .background(Color(argbHex: item.accentHex), in: Capsule())

                                VStack(alignment: .leading, spacing: 2) {
                                    Text(item.name)
                                        .font(.subheadline.weight(.semibold))
                                        .foregroundColor(ObsidianTheme.textPrimary)
                                    Text(item.description)
                                        .font(.caption2)
                                        .foregroundColor(ObsidianTheme.textSecondary)
                                }

                                Spacer()

                                Button {
                                    UIImpactFeedbackGenerator(style: .medium).impactOccurred()
                                    if owned {
                                        viewModel.equipShopItem(item)
                                    } else {
                                        viewModel.purchaseShopItem(item)
                                    }
                                } label: {
                                    Text(equipped ? "Equipado ✓" : (owned ? "Equipar" : "\(item.price) pts"))
                                        .font(.caption.bold())
                                        .foregroundColor(equipped ? ObsidianTheme.textSecondary : .black)
                                        .padding(.horizontal, 12)
                                        .padding(.vertical, 7)
                                        .background(
                                            equipped ? ObsidianTheme.surfaceElevated : ObsidianTheme.emerald,
                                            in: Capsule()
                                        )
                                }
                                .disabled(equipped)
                            }
                            .padding(12)
                            .background(ObsidianTheme.surface, in: RoundedRectangle(cornerRadius: 16))
                            .overlay(RoundedRectangle(cornerRadius: 16).stroke(ObsidianTheme.border, lineWidth: 1))
                        }
                    }
                }
            }
            .padding(.horizontal, 16)
            .padding(.top, 8)
            .padding(.bottom, 96)
        }
    }
}

// MARK: - Group, Room Sync & Profile View

private struct GroupAndProfileView: View {
    @EnvironmentObject private var viewModel: BolaoViewModel
    @State private var roomInput = ""
    @State private var selectedPhotoItem: PhotosPickerItem?

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                if let user = viewModel.currentUser {
                    // Profile + PhotosPicker
                    HStack(spacing: 14) {
                        UserAvatarView(
                            name: user.name,
                            colorHex: user.avatarColorHex,
                            photoPath: user.avatarImagePath,
                            frameId: viewModel.shopState.equippedFrameId,
                            size: 56
                        )
                        VStack(alignment: .leading, spacing: 4) {
                            Text(user.name)
                                .font(.headline)
                                .foregroundColor(ObsidianTheme.textPrimary)
                            Text("\(user.email) • \(user.favoriteLeague.shortName)")
                                .font(.caption)
                                .foregroundColor(ObsidianTheme.textSecondary)

                            PhotosPicker(selection: $selectedPhotoItem, matching: .images) {
                                Label("Trocar Foto", systemImage: "photo.on.rectangle")
                                    .font(.caption.weight(.semibold))
                                    .foregroundColor(ObsidianTheme.emerald)
                            }
                        }
                        Spacer()
                        Button {
                            viewModel.logout()
                        } label: {
                            Image(systemName: "rectangle.portrait.and.arrow.right")
                                .foregroundColor(ObsidianTheme.crimson)
                                .padding(8)
                                .background(ObsidianTheme.surfaceElevated, in: Circle())
                        }
                    }
                    .padding(16)
                    .background(ObsidianTheme.surface, in: RoundedRectangle(cornerRadius: 20))
                    .overlay(RoundedRectangle(cornerRadius: 20).stroke(ObsidianTheme.border, lineWidth: 1))
                    .onChange(of: selectedPhotoItem) { newItem in
                        guard let newItem else { return }
                        Task {
                            if let data = try? await newItem.loadTransferable(type: Data.self) {
                                viewModel.saveAvatarPhoto(data: data)
                            }
                        }
                    }

                    // 4-Stat Telemetry Bento Row
                    HStack(spacing: 8) {
                        StatTile(title: "Cravadas", value: "\(viewModel.userStats.exactHits)")
                        StatTile(title: "Acerto", value: "\(viewModel.userStats.accuracyPercent)%")
                        StatTile(title: "Seq 🔥", value: "\(viewModel.userStats.currentStreak)")
                        StatTile(title: "Pontos", value: "\(viewModel.totalPoints(for: viewModel.predictions))")
                    }
                }

                // Room Sync Card
                VStack(alignment: .leading, spacing: 12) {
                    Text("Sala dos Amigos")
                        .font(.headline)
                        .foregroundColor(ObsidianTheme.textPrimary)

                    HStack(spacing: 8) {
                        TextField("Código da sala (ex: VEI-ABCD)", text: $roomInput)
                            .textInputAutocapitalization(.characters)
                            .autocorrectionDisabled()
                            .textFieldStyle(ObsidianTextFieldStyle())

                        Button {
                            if let clip = UIPasteboard.general.string {
                                roomInput = clip.uppercased()
                            }
                        } label: {
                            Image(systemName: "doc.on.clipboard")
                                .foregroundColor(ObsidianTheme.textPrimary)
                                .padding(10)
                                .background(ObsidianTheme.surfaceElevated, in: RoundedRectangle(cornerRadius: 10))
                        }
                    }

                    HStack(spacing: 8) {
                        Button {
                            viewModel.joinOrCreateRoom(roomInput)
                        } label: {
                            Text("Entrar na Sala")
                                .font(.caption.bold())
                                .foregroundColor(.black)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 10)
                                .background(ObsidianTheme.emerald, in: RoundedRectangle(cornerRadius: 12))
                        }

                        Button {
                            viewModel.createNewRoom()
                            roomInput = viewModel.roomCode
                        } label: {
                            Text("Criar Nova")
                                .font(.caption.bold())
                                .foregroundColor(ObsidianTheme.textPrimary)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 10)
                                .background(ObsidianTheme.surfaceElevated, in: RoundedRectangle(cornerRadius: 12))
                        }
                    }

                    if !viewModel.roomCode.isEmpty {
                        HStack {
                            Text("Sala ativa: \(viewModel.roomCode)")
                                .font(.caption.bold())
                                .foregroundColor(ObsidianTheme.emerald)
                            Spacer()
                            Button("Sincronizar") {
                                Task { await viewModel.syncRoom() }
                            }
                            .font(.caption.bold())
                            .foregroundColor(ObsidianTheme.gold)
                            Button("Sair") {
                                viewModel.leaveRoom()
                            }
                            .font(.caption.bold())
                            .foregroundColor(ObsidianTheme.crimson)
                        }

                        if let shareURL = whatsappShareURL(roomCode: viewModel.roomCode) {
                            Link(destination: shareURL) {
                                Label("Convidar no WhatsApp", systemImage: "square.and.arrow.up")
                                    .font(.caption.bold())
                                    .foregroundColor(.black)
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 9)
                                    .background(ObsidianTheme.emerald, in: RoundedRectangle(cornerRadius: 12))
                            }
                        }
                    }

                    if let status = viewModel.syncStatusText {
                        Text(status)
                            .font(.caption2)
                            .foregroundColor(ObsidianTheme.textSecondary)
                    }
                }
                .padding(16)
                .background(ObsidianTheme.surface, in: RoundedRectangle(cornerRadius: 20))
                .overlay(RoundedRectangle(cornerRadius: 20).stroke(ObsidianTheme.border, lineWidth: 1))
                .onAppear {
                    if roomInput.isEmpty { roomInput = viewModel.roomCode }
                }

                // Leaderboard
                VStack(alignment: .leading, spacing: 10) {
                    Text("Classificação da Sala")
                        .font(.headline)
                        .foregroundColor(ObsidianTheme.textPrimary)

                    let sorted = viewModel.friends.sorted {
                        viewModel.totalPoints(for: $0.predictions) > viewModel.totalPoints(for: $1.predictions)
                    }

                    ForEach(Array(sorted.enumerated()), id: \.element.id) { index, friend in
                        HStack(spacing: 12) {
                            Text("#\(index + 1)")
                                .font(.caption.bold().monospacedDigit())
                                .foregroundColor(index == 0 ? ObsidianTheme.gold : ObsidianTheme.textSecondary)
                                .frame(width: 26)

                            UserAvatarView(
                                name: friend.name,
                                colorHex: friend.avatarColorHex,
                                photoPath: friend.avatarImagePath,
                                frameId: friend.equippedFrameId,
                                size: 38
                            )

                            VStack(alignment: .leading, spacing: 2) {
                                HStack(spacing: 6) {
                                    Text(friend.name)
                                        .font(.subheadline.weight(.semibold))
                                        .foregroundColor(ObsidianTheme.textPrimary)
                                    if let title = friend.equippedTitle {
                                        Text(title)
                                            .font(.caption2.bold())
                                            .foregroundColor(.black)
                                            .padding(.horizontal, 6)
                                            .padding(.vertical, 2)
                                            .background(ObsidianTheme.gold, in: Capsule())
                                    }
                                }
                                Text(friend.subtitle)
                                    .font(.caption2)
                                    .foregroundColor(ObsidianTheme.textSecondary)
                            }

                            Spacer()

                            Text("\(viewModel.totalPoints(for: friend.predictions)) pts")
                                .font(.subheadline.bold().monospacedDigit())
                                .foregroundColor(ObsidianTheme.emerald)
                        }
                        .padding(12)
                        .background(ObsidianTheme.surfaceElevated, in: RoundedRectangle(cornerRadius: 14))
                    }
                }
                .padding(16)
                .background(ObsidianTheme.surface, in: RoundedRectangle(cornerRadius: 20))
                .overlay(RoundedRectangle(cornerRadius: 20).stroke(ObsidianTheme.border, lineWidth: 1))
            }
            .padding(.horizontal, 16)
            .padding(.top, 8)
            .padding(.bottom, 96)
        }
    }

    private func whatsappShareURL(roomCode: String) -> URL? {
        let msg = "Bora pro Bolão dos Véi! Entre na minha sala usando o código: \(roomCode)"
        guard let encoded = msg.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) else {
            return nil
        }
        return URL(string: "https://wa.me/?text=\(encoded)")
    }
}

// MARK: - Shared UI Components

private struct LeagueChip: View {
    let title: String
    let accent: Color
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.caption.weight(.semibold))
                .foregroundColor(isSelected ? .black : ObsidianTheme.textPrimary)
                .padding(.horizontal, 12)
                .padding(.vertical, 7)
                .background(isSelected ? accent : ObsidianTheme.surface, in: Capsule())
                .overlay(Capsule().stroke(ObsidianTheme.border, lineWidth: isSelected ? 0 : 1))
        }
    }
}

private struct StatTile: View {
    let title: String
    let value: String

    var body: some View {
        VStack(spacing: 4) {
            Text(value)
                .font(.headline.bold().monospacedDigit())
                .foregroundColor(ObsidianTheme.emerald)
            Text(title)
                .font(.caption2)
                .foregroundColor(ObsidianTheme.textSecondary)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 12)
        .background(ObsidianTheme.surface, in: RoundedRectangle(cornerRadius: 14))
        .overlay(RoundedRectangle(cornerRadius: 14).stroke(ObsidianTheme.border, lineWidth: 1))
    }
}

private struct TeamCrestView: View {
    let logoUrl: String
    let shortName: String
    let colorHex: UInt64
    let size: CGFloat

    var body: some View {
        if let url = URL(string: logoUrl), !logoUrl.isEmpty {
            AsyncImage(url: url) { phase in
                switch phase {
                case .success(let image):
                    image
                        .resizable()
                        .scaledToFit()
                        .frame(width: size, height: size)
                default:
                    fallbackBadge
                }
            }
        } else {
            fallbackBadge
        }
    }

    private var fallbackBadge: some View {
        Circle()
            .fill(Color(argbHex: colorHex))
            .frame(width: size, height: size)
            .overlay(
                Text(shortName)
                    .font(.system(size: size * 0.3, weight: .bold))
                    .foregroundColor(.white)
            )
    }
}

private struct UserAvatarView: View {
    let name: String
    let colorHex: UInt64
    let photoPath: String?
    let frameId: String?
    let size: CGFloat

    private var frameColor: Color? {
        ShopCatalog.findById(frameId).map { Color(argbHex: $0.accentHex) }
    }

    var body: some View {
        Group {
            if let path = photoPath,
               let uiImage = UIImage(contentsOfFile: path) {
                Image(uiImage: uiImage)
                    .resizable()
                    .scaledToFill()
            } else {
                Circle()
                    .fill(Color(argbHex: colorHex))
                    .overlay(
                        Text(String(name.prefix(1)).uppercased())
                            .font(.system(size: size * 0.42, weight: .bold))
                            .foregroundColor(.white)
                    )
            }
        }
        .frame(width: size, height: size)
        .clipShape(Circle())
        .overlay(
            Circle().stroke(frameColor ?? .clear, lineWidth: frameColor != nil ? 3 : 0)
        )
    }
}

private struct ObsidianTextFieldStyle: TextFieldStyle {
    func _body(configuration: TextField<Self._Label>) -> some View {
        configuration
            .padding(.horizontal, 12)
            .padding(.vertical, 10)
            .background(ObsidianTheme.surfaceElevated, in: RoundedRectangle(cornerRadius: 12))
            .overlay(RoundedRectangle(cornerRadius: 12).stroke(ObsidianTheme.border, lineWidth: 1))
            .foregroundColor(ObsidianTheme.textPrimary)
    }
}
