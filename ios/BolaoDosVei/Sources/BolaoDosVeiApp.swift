import SwiftUI

@main
struct BolaoDosVeiApp: App {
    @StateObject private var viewModel = BolaoViewModel()

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(viewModel)
                .preferredColorScheme(.dark)
        }
    }
}
