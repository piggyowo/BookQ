import SwiftUI
import Foundation

// MARK: - 颜色

enum Palette {
    static let ink = Color(hex: "16232f")
    static let ink2 = Color(hex: "44525f")
    static let muted = Color(hex: "7c8794")
    static let paper = Color(hex: "f5f0e7")
    static let card = Color(hex: "fffdf8")
    static let line = Color(hex: "e5dccb")
    static let line2 = Color(hex: "efe7d8")
    static let primary = Color(hex: "1a455c")
    static let primary2 = Color(hex: "2a6b80")
    static let accent = Color(hex: "b9613c")
    static let gold = Color(hex: "bd9350")
    static let green = Color(hex: "1c7a5b")
    static let greenBG = Color(hex: "e4f2ea")
    static let red = Color(hex: "ab3a2c")
    static let redBG = Color(hex: "fae8e3")
    static let amber = Color(hex: "8a6a2f")
    static let amberLine = Color(hex: "d9bd8c")
    static let amberBG = Color(hex: "fbf3e3")
    static let track = Color(hex: "eee7d9")
    static let cream = Color(hex: "f6efe2")
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
    }
}

struct PaperBackground: View {
    var body: some View {
        ZStack {
            Palette.paper
            RadialGradient(
                colors: [Color(hex: "efe2cd").opacity(0.95), Color.clear],
                center: .topLeading,
                startRadius: 0,
                endRadius: 720
            )
            RadialGradient(
                colors: [Color(hex: "e8eef0").opacity(0.9), Color.clear],
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
                Capsule().fill(Color(hex: "eee6d6"))
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
            .shadow(color: Color(hex: "212c36").opacity(0.16), radius: 18, x: 0, y: 12)
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
                        .fill(selected ? Palette.primary : Color(hex: "faf6ee"))
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
            .background(Capsule().fill(Color(hex: "17242f").opacity(0.94)))
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
                .fill(Color(hex: "e8ddc9"))
                .frame(width: 150, height: 178)
                .rotationEffect(.degrees(-7))
                .offset(x: -20, y: 6)
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .fill(Color(hex: "f2e9d9"))
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
