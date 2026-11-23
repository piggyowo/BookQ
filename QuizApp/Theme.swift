import SwiftUI
import Foundation

// MARK: - 颜色

struct BookQPalette {
    let isDark: Bool
    let ink: Color
    let ink2: Color
    let muted: Color
    let paper: Color
    let card: Color
    let line: Color
    let line2: Color
    let primary: Color
    let primary2: Color
    let accent: Color
    let gold: Color
    let green: Color
    let greenBG: Color
    let red: Color
    let redBG: Color
    let amber: Color
    let amberLine: Color
    let amberBG: Color
    let track: Color
    let cream: Color
    let hero1: Color
    let hero2: Color
    let hero3: Color
    let glow1: Color
    let glow2: Color
    let shadow: Color
}

enum BookQPalettes {
    static let indigoLight = BookQPalette(
        isDark: false,
        ink: Color(hex: "16232f"), ink2: Color(hex: "44525f"), muted: Color(hex: "7c8794"),
        paper: Color(hex: "f5f0e7"), card: Color(hex: "fffdf8"), line: Color(hex: "e5dccb"), line2: Color(hex: "efe7d8"),
        primary: Color(hex: "1a455c"), primary2: Color(hex: "2a6b80"), accent: Color(hex: "b9613c"), gold: Color(hex: "bd9350"),
        green: Color(hex: "1c7a5b"), greenBG: Color(hex: "e4f2ea"), red: Color(hex: "ab3a2c"), redBG: Color(hex: "fae8e3"),
        amber: Color(hex: "8a6a2f"), amberLine: Color(hex: "d9bd8c"), amberBG: Color(hex: "fbf3e3"),
        track: Color(hex: "eee7d9"), cream: Color(hex: "f6efe2"),
        hero1: Color(hex: "152b3a"), hero2: Color(hex: "1c455c"), hero3: Color(hex: "25596a"),
        glow1: Color(hex: "bd9350"), glow2: Color(hex: "2a6b80"), shadow: Color(hex: "212c36")
    )

    static let rustLight = BookQPalette(
        isDark: false,
        ink: Color(hex: "2b211c"), ink2: Color(hex: "5c493f"), muted: Color(hex: "806f63"),
        paper: Color(hex: "f6ede0"), card: Color(hex: "fff9ef"), line: Color(hex: "e6d2bd"), line2: Color(hex: "f0e1ce"),
        primary: Color(hex: "8d3f2f"), primary2: Color(hex: "b45c43"), accent: Color(hex: "20505d"), gold: Color(hex: "c18a4a"),
        green: Color(hex: "39775a"), greenBG: Color(hex: "e6f0e7"), red: Color(hex: "a83a2a"), redBG: Color(hex: "fae8e3"),
        amber: Color(hex: "8a6a2f"), amberLine: Color(hex: "d9bd8c"), amberBG: Color(hex: "fbf3e3"),
        track: Color(hex: "eee0cf"), cream: Color(hex: "fbf1df"),
        hero1: Color(hex: "57201a"), hero2: Color(hex: "8d3f2f"), hero3: Color(hex: "b45c43"),
        glow1: Color(hex: "c18a4a"), glow2: Color(hex: "20505d"), shadow: Color(hex: "2b1c16")
    )

    static let mossLight = BookQPalette(
        isDark: false,
        ink: Color(hex: "1d2b22"), ink2: Color(hex: "46584b"), muted: Color(hex: "708074"),
        paper: Color(hex: "edf0e4"), card: Color(hex: "fbfcf4"), line: Color(hex: "d7ddc9"), line2: Color(hex: "e6e9dc"),
        primary: Color(hex: "2c4f3c"), primary2: Color(hex: "4f7f5f"), accent: Color(hex: "a6753f"), gold: Color(hex: "b08d55"),
        green: Color(hex: "2e7a55"), greenBG: Color(hex: "e3f0e7"), red: Color(hex: "a84535"), redBG: Color(hex: "fae8e3"),
        amber: Color(hex: "8a6a2f"), amberLine: Color(hex: "d1bf97"), amberBG: Color(hex: "f7f1e2"),
        track: Color(hex: "e3e8da"), cream: Color(hex: "f4ecd9"),
        hero1: Color(hex: "172d22"), hero2: Color(hex: "2c4f3c"), hero3: Color(hex: "4f7f5f"),
        glow1: Color(hex: "b08d55"), glow2: Color(hex: "4f7f5f"), shadow: Color(hex: "1b2a20")
    )

    static let indigoDark = BookQPalette(
        isDark: true,
        ink: Color(hex: "f1f0e7"), ink2: Color(hex: "c5d0d2"), muted: Color(hex: "8fa1a7"),
        paper: Color(hex: "111b22"), card: Color(hex: "192832"), line: Color(hex: "2c3f49"), line2: Color(hex: "23343d"),
        primary: Color(hex: "4c8fa4"), primary2: Color(hex: "70adbb"), accent: Color(hex: "d17a54"), gold: Color(hex: "d2a65f"),
        green: Color(hex: "6db795"), greenBG: Color(hex: "183830"), red: Color(hex: "d9796b"), redBG: Color(hex: "3b2424"),
        amber: Color(hex: "d2a65f"), amberLine: Color(hex: "5c4a31"), amberBG: Color(hex: "2d2518"),
        track: Color(hex: "24343c"), cream: Color(hex: "f3ead9"),
        hero1: Color(hex: "09141b"), hero2: Color(hex: "163a47"), hero3: Color(hex: "255566"),
        glow1: Color(hex: "d2a65f"), glow2: Color(hex: "70adbb"), shadow: Color(hex: "000000")
    )

    static let rustDark = BookQPalette(
        isDark: true,
        ink: Color(hex: "f4e8d8"), ink2: Color(hex: "d0bbaa"), muted: Color(hex: "9f8776"),
        paper: Color(hex: "1d1512"), card: Color(hex: "2a1f19"), line: Color(hex: "463329"), line2: Color(hex: "35261f"),
        primary: Color(hex: "c76b4e"), primary2: Color(hex: "d98b6b"), accent: Color(hex: "5fa0ac"), gold: Color(hex: "d1a15b"),
        green: Color(hex: "6fb38c"), greenBG: Color(hex: "1d3329"), red: Color(hex: "d97b67"), redBG: Color(hex: "3b2422"),
        amber: Color(hex: "d1a15b"), amberLine: Color(hex: "6a5435"), amberBG: Color(hex: "302616"),
        track: Color(hex: "3d2b22"), cream: Color(hex: "fbf1df"),
        hero1: Color(hex: "3a1712"), hero2: Color(hex: "7a3428"), hero3: Color(hex: "a8533d"),
        glow1: Color(hex: "c18a4a"), glow2: Color(hex: "5fa0ac"), shadow: Color(hex: "000000")
    )

    static let mossDark = BookQPalette(
        isDark: true,
        ink: Color(hex: "eef2e4"), ink2: Color(hex: "c4d1c5"), muted: Color(hex: "8b9d8f"),
        paper: Color(hex: "101a14"), card: Color(hex: "18271e"), line: Color(hex: "2d4234"), line2: Color(hex: "23362a"),
        primary: Color(hex: "5f9c74"), primary2: Color(hex: "7dbb8e"), accent: Color(hex: "c99655"), gold: Color(hex: "c6a269"),
        green: Color(hex: "6fb68e"), greenBG: Color(hex: "18362a"), red: Color(hex: "d67c6c"), redBG: Color(hex: "3b2422"),
        amber: Color(hex: "c6a269"), amberLine: Color(hex: "5f4e34"), amberBG: Color(hex: "2c2618"),
        track: Color(hex: "2a3d30"), cream: Color(hex: "f4ecd9"),
        hero1: Color(hex: "0c1a13"), hero2: Color(hex: "1e3b2a"), hero3: Color(hex: "315d43"),
        glow1: Color(hex: "c6a269"), glow2: Color(hex: "5f9c74"), shadow: Color(hex: "000000")
    )

    static func of(_ theme: AppTheme, _ appearance: AppAppearance) -> BookQPalette {
        switch (theme, appearance) {
        case (.indigo, .light): return indigoLight
        case (.indigo, .dark): return indigoDark
        case (.rust, .light): return rustLight
        case (.rust, .dark): return rustDark
        case (.moss, .light): return mossLight
        case (.moss, .dark): return mossDark
        }
    }
}

enum ThemeRuntime {
    static var current: BookQPalette = BookQPalettes.indigoLight

    static func apply(theme: AppTheme, appearance: AppAppearance) {
        current = BookQPalettes.of(theme, appearance)
    }
}

enum Palette {
    static var isDark: Bool { ThemeRuntime.current.isDark }
    static var ink: Color { ThemeRuntime.current.ink }
    static var ink2: Color { ThemeRuntime.current.ink2 }
    static var muted: Color { ThemeRuntime.current.muted }
    static var paper: Color { ThemeRuntime.current.paper }
    static var card: Color { ThemeRuntime.current.card }
    static var line: Color { ThemeRuntime.current.line }
    static var line2: Color { ThemeRuntime.current.line2 }
    static var primary: Color { ThemeRuntime.current.primary }
    static var primary2: Color { ThemeRuntime.current.primary2 }
    static var accent: Color { ThemeRuntime.current.accent }
    static var gold: Color { ThemeRuntime.current.gold }
    static var green: Color { ThemeRuntime.current.green }
    static var greenBG: Color { ThemeRuntime.current.greenBG }
    static var red: Color { ThemeRuntime.current.red }
    static var redBG: Color { ThemeRuntime.current.redBG }
    static var amber: Color { ThemeRuntime.current.amber }
    static var amberLine: Color { ThemeRuntime.current.amberLine }
    static var amberBG: Color { ThemeRuntime.current.amberBG }
    static var track: Color { ThemeRuntime.current.track }
    static var cream: Color { ThemeRuntime.current.cream }
    static var hero1: Color { ThemeRuntime.current.hero1 }
    static var hero2: Color { ThemeRuntime.current.hero2 }
    static var hero3: Color { ThemeRuntime.current.hero3 }
    static var glow1: Color { ThemeRuntime.current.glow1 }
    static var glow2: Color { ThemeRuntime.current.glow2 }
    static var shadow: Color { ThemeRuntime.current.shadow }
}

extension Color {
    init(hex: String) {
        let cleaned = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var value: UInt64 = 0
        Scanner(string: cleaned).scanHexInt64(&value)
        let r: UInt64
        let g: UInt64
        let b: UInt64
        switch cleaned.count {
        case 3:
            let rr = (value >> 8) & 0xF
            let gg = (value >> 4) & 0xF
            let bb = value & 0xF
            r = rr * 17
            g = gg * 17
            b = bb * 17
        default:
            r = (value >> 16) & 0xFF
            g = (value >> 8) & 0xFF
            b = value & 0xFF
        }
        self.init(.sRGB,
                  red: Double(r) / 255,
                  green: Double(g) / 255,
                  blue: Double(b) / 255,
                  opacity: 1)
    }
}

// MARK: - 字体

extension Font {
    static func quizSerif(_ size: CGFloat, weight: Font.Weight = .bold) -> Font {
        .system(size: size, weight: weight, design: .serif)
    }

    static func quizSans(_ size: CGFloat, weight: Font.Weight = .regular) -> Font {
        .system(size: size, weight: weight)
    }
}

// MARK: - 背景

struct FabricGrid: View {
    var spacing: CGFloat = 4
    var opacity: Double = 0.02
    var light: Bool = false

    var body: some View {
        Canvas { context, size in
            let color = light
                ? Color.white.opacity(opacity)
                : Palette.ink.opacity(opacity)
            var path = Path()
            var x: CGFloat = 0
            while x <= size.width {
                path.move(to: CGPoint(x: x, y: 0))
                path.addLine(to: CGPoint(x: x, y: size.height))
                x += spacing
            }
            var y: CGFloat = 0
            while y <= size.height {
                path.move(to: CGPoint(x: 0, y: y))
                path.addLine(to: CGPoint(x: size.width, y: y))
                y += spacing
            }
            context.stroke(path, with: .color(color), lineWidth: 0.5)
        }
        .allowsHitTesting(false)
        // 这张网格在推页/拖回时会被整层移动。缓存成一张纹理，
        // 避免每一帧都重新画上百条横竖线。
        .drawingGroup()
    }
}

struct PaperBackground: View {
    var body: some View {
        ZStack {
            Palette.paper
            RadialGradient(
                colors: [Palette.glow1.opacity(0.10), Color.clear],
                center: .topLeading,
                startRadius: 0,
                endRadius: 720
            )
            RadialGradient(
                colors: [Palette.glow2.opacity(0.10), Color.clear],
                center: .topTrailing,
                startRadius: 0,
                endRadius: 620
            )
            FabricGrid(spacing: 4, opacity: 0.022)
        }
        .ignoresSafeArea()
    }
}

// MARK: - 品牌与通用组件

struct BrandMark: View {
    var size: CGFloat = 46

    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: size * 0.26, style: .continuous)
                .fill(Palette.primary)
            VStack(spacing: size * 0.10) {
                ForEach(0..<3, id: \.self) { _ in
                    Capsule()
                        .fill(Palette.cream)
                        .frame(height: 2)
                }
            }
            .padding(.horizontal, size * 0.26)
        }
        .frame(width: size, height: size)
        .shadow(color: Palette.primary.opacity(0.35), radius: 10, x: 0, y: 6)
    }
}

struct PillLabel: View {
    let text: String
    var color: Color = Palette.primary

    var body: some View {
        Text(text)
            .font(.quizSans(12.5, weight: .medium))
            .foregroundColor(color)
            .lineLimit(1)
            .padding(.horizontal, 11)
            .padding(.vertical, 4)
            .background(
                Capsule().fill(Palette.track)
            )
    }
}

struct SectionHeader: View {
    let title: String
    var trailing: String? = nil

    var body: some View {
        HStack(alignment: .firstTextBaseline) {
            Text(title)
                .font(.quizSerif(19))
                .foregroundColor(Palette.ink)
            Spacer(minLength: 12)
            if let trailing {
                Text(trailing)
                    .font(.quizSans(12.5))
                    .foregroundColor(Palette.muted)
            }
        }
    }
}

struct ProgressTrack: View {
    let value: Double
    var height: CGFloat = 5
    var low: Bool = false

    var body: some View {
        GeometryReader { geo in
            ZStack(alignment: .leading) {
                Capsule().fill(Palette.track)
                Capsule()
                    .fill(
                        LinearGradient(
                            colors: low
                                ? [Color(hex: "c98a6b"), Palette.accent]
                                : [Palette.primary2, Palette.primary],
                            startPoint: .leading,
                            endPoint: .trailing
                        )
                    )
                    .frame(width: CGFloat(max(0, min(1, value))) * geo.size.width)
            }
        }
        .frame(height: height)
    }
}

struct CardSurface: ViewModifier {
    var radius: CGFloat = 22
    var padding: CGFloat = 24

    func body(content: Content) -> some View {
        content
            .padding(padding)
            .background(
                RoundedRectangle(cornerRadius: radius, style: .continuous)
                    .fill(Palette.card)
                    .overlay(
                        RoundedRectangle(cornerRadius: radius, style: .continuous)
                            .stroke(Palette.line, lineWidth: 1)
                    )
            )
            .shadow(color: Palette.shadow.opacity(0.16), radius: 18, x: 0, y: 12)
    }
}

extension View {
    func cardSurface(radius: CGFloat = 22, padding: CGFloat = 24) -> some View {
        modifier(CardSurface(radius: radius, padding: padding))
    }
}

struct PrimaryPillStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.quizSans(15, weight: .semibold))
            .foregroundColor(.white)
            .padding(.horizontal, 24)
            .padding(.vertical, 13)
            .background(Capsule().fill(Palette.primary))
            .opacity(configuration.isPressed ? 0.82 : 1)
    }
}

struct GhostPillStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.quizSans(14, weight: .medium))
            .foregroundColor(Palette.ink)
            .padding(.horizontal, 16)
            .padding(.vertical, 9)
            .background(
                Capsule()
                    .fill(Palette.card.opacity(0.85))
                    .overlay(Capsule().stroke(Palette.line, lineWidth: 1))
            )
            .opacity(configuration.isPressed ? 0.7 : 1)
    }
}

struct ChipButton: View {
    let title: String
    let selected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.quizSans(13.5, weight: .medium))
                .foregroundColor(selected ? .white : Palette.ink2)
                .padding(.horizontal, 15)
                .padding(.vertical, 9)
                .background(
                    RoundedRectangle(cornerRadius: 10, style: .continuous)
                        .fill(selected ? Palette.primary : Palette.track)
                        .overlay(
                            RoundedRectangle(cornerRadius: 10, style: .continuous)
                                .stroke(selected ? Palette.primary : Palette.line, lineWidth: 1)
                        )
                )
        }
        .buttonStyle(.plain)
    }
}

struct ToastView: View {
    let message: String

    var body: some View {
        Text(message)
            .font(.quizSans(13.5))
            .foregroundColor(.white)
            .padding(.horizontal, 20)
            .padding(.vertical, 11)
            .background(Capsule().fill(Palette.isDark ? Palette.card.opacity(0.96) : Color(hex: "17242f").opacity(0.94)))
            .shadow(color: .black.opacity(0.2), radius: 12, x: 0, y: 6)
            .transition(.opacity.combined(with: .move(edge: .bottom)))
    }
}

// MARK: - 时间格式

func formatDuration(_ seconds: TimeInterval) -> String {
    let total = max(0, Int(seconds.rounded()))
    let m = total / 60
    let s = total % 60
    return String(format: "%02d:%02d", m, s)
}

// MARK: - 底栏与新增组件

struct BottomTabBar: View {
    @EnvironmentObject var store: QuizStore

    var body: some View {
        HStack(spacing: 0) {
            ForEach(AppTab.allCases) { tab in
                Button {
                    if store.screen == .wrong { store.goHome() }
                    store.selectedTab = tab
                } label: {
                    VStack(spacing: 4) {
                        ZStack {
                            RoundedRectangle(cornerRadius: 9, style: .continuous)
                                .fill(store.selectedTab == tab ? Palette.primary.opacity(0.10) : Color.clear)
                                .frame(width: 38, height: 30)
                            Image(systemName: tab.icon)
                                .font(.system(size: 18, weight: store.selectedTab == tab ? .semibold : .regular))
                                .foregroundColor(store.selectedTab == tab ? Palette.primary : Palette.muted)
                        }
                        HStack(spacing: 3) {
                            Text(tab.title)
                                .font(.quizSans(11.5, weight: store.selectedTab == tab ? .semibold : .regular))
                                .foregroundColor(store.selectedTab == tab ? Palette.primary : Palette.muted)
                            if tab == .library && !store.banks.isEmpty {
                                Text("\(store.banks.count)")
                                    .font(.quizSans(9.5, weight: .semibold))
                                    .foregroundColor(store.selectedTab == .library ? Palette.primary : Palette.muted)
                                    .padding(.horizontal, 4)
                                    .padding(.vertical, 1)
                                    .background(Capsule().fill(Palette.track))
                            }
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
            }
        }
        .padding(.horizontal, 14)
        .padding(.top, 9)
        .padding(.bottom, 7)
        .background(
            Palette.card.opacity(0.97)
                .overlay(
                    Rectangle().fill(Palette.line).frame(height: 1),
                    alignment: .top
                )
                .ignoresSafeArea(edges: .bottom)
        )
        .shadow(color: Palette.ink.opacity(0.06), radius: 12, x: 0, y: -4)
    }
}

struct SheetGrabber: View {
    var body: some View {
        Capsule()
            .fill(Palette.line)
            .frame(width: 42, height: 5)
            .padding(.top, 9)
            .padding(.bottom, 2)
    }
}

struct PaperStackArt: View {
    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .fill(Palette.line)
                .frame(width: 150, height: 178)
                .rotationEffect(.degrees(-7))
                .offset(x: -20, y: 6)
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .fill(Palette.line2)
                .frame(width: 150, height: 178)
                .rotationEffect(.degrees(4))
                .offset(x: 16, y: 2)
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .fill(Palette.card)
                .overlay(
                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                        .stroke(Palette.line, lineWidth: 1)
                )
                .frame(width: 150, height: 178)
                .shadow(color: Palette.ink.opacity(0.12), radius: 18, x: 0, y: 10)
            VStack(alignment: .leading, spacing: 11) {
                HStack(spacing: 7) {
                    RoundedRectangle(cornerRadius: 4, style: .continuous)
                        .fill(Palette.primary)
                        .frame(width: 24, height: 24)
                    VStack(alignment: .leading, spacing: 3) {
                        Capsule().fill(Palette.ink.opacity(0.75)).frame(width: 54, height: 6)
                        Capsule().fill(Palette.line).frame(width: 38, height: 4)
                    }
                }
                ForEach(0..<4, id: \.self) { row in
                    HStack(spacing: 6) {
                        Circle()
                            .stroke(Palette.line, lineWidth: 1)
                            .frame(width: 9, height: 9)
                        Capsule()
                            .fill(Palette.line2)
                            .frame(width: row == 2 ? 68 : 94, height: 5)
                    }
                }
                Spacer(minLength: 0)
                HStack(spacing: 5) {
                    ForEach(0..<3, id: \.self) { _ in
                        Capsule()
                            .fill(Palette.gold.opacity(0.35))
                            .frame(width: 22, height: 5)
                    }
                }
            }
            .padding(20)
            .frame(width: 150, height: 178)
        }
        .frame(height: 210)
        .accessibilityHidden(true)
    }
}

struct RingProgress: View {
    let value: Double
    var size: CGFloat = 76
    var lineWidth: CGFloat = 8
    var foreground: Color = Palette.cream
    var background: Color = Color.white.opacity(0.18)

    var body: some View {
        ZStack {
            Circle().stroke(background, lineWidth: lineWidth)
            Circle()
                .trim(from: 0, to: CGFloat(max(0, min(1, value))))
                .stroke(foreground, style: StrokeStyle(lineWidth: lineWidth, lineCap: .round))
                .rotationEffect(.degrees(-90))
            Text("\(Int(value * 100))%")
                .font(.quizSerif(size * 0.25, weight: .bold))
                .foregroundColor(Palette.cream)
        }
        .frame(width: size, height: size)
    }
}
