import SwiftUI

@main
@MainActor
struct CraftQuizApp: App {
    @StateObject private var store = QuizStore()

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(store)
                .preferredColorScheme(store.appearance == .dark ? .dark : .light)
                .id(store.themeKey)
        }
    }
}

struct RootView: View {
    @EnvironmentObject private var store: QuizStore

    var body: some View {
        ZStack {
            if store.screen == .quiz {
                QuizView()
                    .transition(.opacity)
            } else if store.screen == .result {
                ResultView()
                    .transition(.opacity)
            } else {
                MainTabsView()
                    .transition(.opacity)
            }
        }
        .animation(.easeInOut(duration: 0.22), value: screenKey)
        .overlay(alignment: .bottom) {
            if let toast = store.toast {
                ToastView(message: toast)
                    .padding(.bottom, toastBottom)
                    .zIndex(10)
            }
        }
    }

    private var screenKey: Int {
        switch store.screen {
        case .home, .wrong: return 0
        case .quiz: return 1
        case .result: return 2
        }
    }

    private var toastBottom: CGFloat {
        store.screen == .home || store.screen == .wrong ? 92 : 30
    }
}

struct MainTabsView: View {
    @EnvironmentObject private var store: QuizStore

    var body: some View {
        tabContent
            .paperPush(
                isPresented: Binding(
                    get: { store.screen == .wrong },
                    set: { if !$0 { store.goHome() } }
                )
            ) {
                WrongBookView()
            }
            .safeAreaInset(edge: .bottom, spacing: 0) {
                BottomTabBar()
            }
            .animation(.easeInOut(duration: 0.18), value: store.selectedTab)
    }

    @ViewBuilder
    private var tabContent: some View {
        switch store.selectedTab {
        case .home:
            HomeView()
        case .library:
            LibraryView()
        case .settings:
            SettingsView()
        }
    }
}
