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
            switch store.screen {
            case .home:
                MainTabsView()
                    .transition(.opacity)
            case .quiz:
                QuizView()
                    .transition(.opacity)
            case .result:
                ResultView()
                    .transition(.opacity)
            case .wrong:
                WrongBookView()
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
        case .home: return 0
        case .quiz: return 1
        case .result: return 2
        case .wrong: return 3
        }
    }

    private var toastBottom: CGFloat {
        store.screen == .home ? 92 : 30
    }
}

struct MainTabsView: View {
    @EnvironmentObject private var store: QuizStore

    var body: some View {
        ZStack {
            switch store.selectedTab {
            case .home:
                HomeView()
            case .library:
                LibraryView()
            case .settings:
                SettingsView()
            }
        }
        .safeAreaInset(edge: .bottom, spacing: 0) {
            BottomTabBar()
        }
        .animation(.easeInOut(duration: 0.18), value: store.selectedTab)
    }
}
