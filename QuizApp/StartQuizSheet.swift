import SwiftUI

struct StartQuizSheet: View {
    @EnvironmentObject var store: QuizStore
    @Environment(\.dismiss) private var dismiss

    let presetBankID: UUID?

    @State private var scope: QuizScope = .all
    @State private var mode: QuizMode = .practice
    @State private var size: Int? = nil
    @State private var order: QuizOrder = .sequential
    @State private var initialized = false

    var body: some View {
        ZStack {
            Palette.paper.ignoresSafeArea()
            ScrollView(showsIndicators: false) {
                VStack(alignment: .leading, spacing: 20) {
                    SheetGrabber()
                        .frame(maxWidth: .infinity)

                    HStack(alignment: .top) {
                        VStack(alignment: .leading, spacing: 4) {
                            Text("开始练习")
                                .font(.quizSerif(26))
                                .foregroundColor(Palette.ink)
                            Text("范围、模式、题量、顺序都可以在这一步调整")
                                .font(.quizSans(12))
                                .foregroundColor(Palette.muted)
                        }
                        Spacer()
                        Button {
                            dismiss()
                        } label: {
                            Image(systemName: "xmark")
                                .font(.system(size: 12, weight: .semibold))
                                .foregroundColor(Palette.ink2)
                                .frame(width: 30, height: 30)
                                .background(Circle().fill(Palette.card).overlay(Circle().stroke(Palette.line, lineWidth: 1)))
                        }
                        .buttonStyle(.plain)
                    }

                    section("练习范围") {
                        VStack(spacing: 0) {
                            scopeRow(scope: .all, title: "全部题库", detail: "\(store.banks.count) 个 · \(store.allQuestions.count) 题")
                            ForEach(store.banks) { bank in
                                Rectangle().fill(Palette.line2).frame(height: 1).padding(.leading, 46)
                                scopeRow(scope: .bank(bank.id), title: bank.name, detail: "\(bank.count) 题")
                            }
                        }
                        .background(
                            RoundedRectangle(cornerRadius: 16, style: .continuous)
                                .fill(Palette.card)
                                .overlay(RoundedRectangle(cornerRadius: 16, style: .continuous).stroke(Palette.line, lineWidth: 1))
                        )
                    }

                    section("模式") {
                        HStack(spacing: 10) {
                            modeCard(.practice, icon: "pencil.line")
                            modeCard(.exam, icon: "checkmark.seal")
                        }
                    }

                    section("题量") {
                        HStack(spacing: 8) {
                            ForEach(countOptions, id: \.self) { value in
                                ChipButton(title: countLabel(value), selected: size == value) {
                                    size = value
                                }
                            }
                        }
                    }

                    section("顺序") {
                        HStack(spacing: 10) {
                            orderCard(.sequential, icon: "list.number", detail: "从第一题往后")
                            orderCard(.random, icon: "shuffle", detail: "打乱后抽取")
                        }
                    }

                    VStack(alignment: .leading, spacing: 6) {
                        Text(summaryLine)
                            .font(.quizSans(12))
                            .foregroundColor(Palette.muted)
                        if let warning = scopeWarning {
                            Text(warning)
                                .font(.quizSans(11.5))
                                .foregroundColor(Palette.accent)
                        }
                    }
                    .padding(.horizontal, 4)

                    Button {
                        store.startQuiz(scope: scope, mode: mode, size: size, order: order)
                        dismiss()
                    } label: {
                        HStack {
                            Spacer()
                            Text(mode == .exam ? "开始考试" : "开始练习")
                                .font(.quizSans(16, weight: .semibold))
                            Spacer()
                        }
                        .foregroundColor(.white)
                        .padding(.vertical, 15)
                        .background(
                            RoundedRectangle(cornerRadius: 14, style: .continuous)
                                .fill(availableCount == 0 ? Palette.muted : Palette.primary)
                        )
                    }
                    .buttonStyle(.plain)
                    .disabled(availableCount == 0)

                    Text("题库里没有答案的题会提示「题库中无答案」，不计分，也不会进入错题本。")
                        .font(.quizSans(10.5))
                        .foregroundColor(Palette.muted)
                        .lineSpacing(3)
                        .padding(.horizontal, 4)
                }
                .padding(.horizontal, 20)
                .padding(.bottom, 28)
            }
        }
        .onAppear {
            guard !initialized else { return }
            initialized = true
            mode = store.defaultMode
            order = store.defaultOrder
            size = store.defaultSize == 0 ? nil : store.defaultSize
            if let presetBankID, store.bank(id: presetBankID) != nil {
                scope = .bank(presetBankID)
            } else {
                scope = .all
            }
            normalizeSize()
        }
    }

    private var availableCount: Int {
        store.questions(in: scope).count
    }

    private var countOptions: [Int?] {
        let available = availableCount
        var values: [Int?] = []
        for value in [10, 20, 50] where value <= available {
            values.append(value)
        }
        if available > 0 {
            values.append(nil)
        }
        if values.isEmpty {
            values.append(nil)
        }
        return values
    }

    private func countLabel(_ value: Int?) -> String {
        guard let value else { return "全部 \(availableCount)" }
        return "\(value) 题"
    }

    private var summaryLine: String {
        let total = min(size ?? availableCount, availableCount)
        return "\(scopeTitle) · \(mode.title)模式 · \(total) 题 · \(order.title)"
    }

    private var scopeWarning: String? {
        guard availableCount > 0 else { return nil }
        let noAnswer = store.questions(in: scope).filter { !$0.hasAnswer }.count
        return noAnswer == 0 ? nil : "其中 \(noAnswer) 题题库未提供答案，会单独提示"
    }

    private var scopeTitle: String {
        switch scope {
        case .all: return "全部题库"
        case .bank(let id): return store.bank(id: id)?.name ?? "题库"
        case .wrong: return "错题重练"
        }
    }

    private func normalizeSize() {
        let available = availableCount
        if let value = size, value > available {
            size = nil
        }
        if available == 0 {
            size = nil
        }
    }

    private func section<Content: View>(_ title: String, @ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 9) {
            Text(title)
                .font(.quizSans(12, weight: .semibold))
                .foregroundColor(Palette.ink2)
                .tracking(0.6)
            content()
        }
    }

    private func scopeRow(scope target: QuizScope, title: String, detail: String) -> some View {
        Button {
            scope = target
            normalizeSize()
        } label: {
            HStack(spacing: 11) {
                ZStack {
                    Circle()
                        .stroke(scope == target ? Palette.primary : Palette.line, lineWidth: 1)
                        .frame(width: 20, height: 20)
                    if scope == target {
                        Circle().fill(Palette.primary).frame(width: 10, height: 10)
                    }
                }
                VStack(alignment: .leading, spacing: 2) {
                    Text(title)
                        .font(.quizSans(13.5, weight: .medium))
                        .foregroundColor(Palette.ink)
                        .lineLimit(1)
                    Text(detail)
                        .font(.quizSans(10.5))
                        .foregroundColor(Palette.muted)
                }
                Spacer()
                if scope == target {
                    Image(systemName: "checkmark")
                        .font(.system(size: 11, weight: .semibold))
                        .foregroundColor(Palette.primary)
                }
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 11)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }

    private func modeCard(_ value: QuizMode, icon: String) -> some View {
        Button {
            mode = value
        } label: {
            VStack(alignment: .leading, spacing: 8) {
                HStack {
                    Image(systemName: icon)
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(mode == value ? Palette.primary : Palette.muted)
                    Spacer()
                    if mode == value {
                        Image(systemName: "checkmark.circle.fill")
                            .font(.system(size: 14))
                            .foregroundColor(Palette.primary)
                    }
                }
                Text(value.longTitle)
                    .font(.quizSans(14, weight: .semibold))
                    .foregroundColor(Palette.ink)
                Text(value.subtitle)
                    .font(.quizSans(10.5))
                    .foregroundColor(Palette.muted)
                    .lineLimit(2)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(13)
            .background(
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .fill(mode == value ? Palette.primary.opacity(0.07) : Palette.card)
                    .overlay(
                        RoundedRectangle(cornerRadius: 14, style: .continuous)
                            .stroke(mode == value ? Palette.primary : Palette.line, lineWidth: mode == value ? 1.4 : 1)
                    )
            )
        }
        .buttonStyle(.plain)
    }

    private func orderCard(_ value: QuizOrder, icon: String, detail: String) -> some View {
        Button {
            order = value
        } label: {
            HStack(spacing: 9) {
                Image(systemName: icon)
                    .font(.system(size: 13, weight: .medium))
                    .foregroundColor(order == value ? Palette.primary : Palette.muted)
                VStack(alignment: .leading, spacing: 2) {
                    Text(value.title)
                        .font(.quizSans(13, weight: .semibold))
                        .foregroundColor(Palette.ink)
                    Text(detail)
                        .font(.quizSans(10))
                        .foregroundColor(Palette.muted)
                }
                Spacer()
            }
            .padding(12)
            .background(
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .fill(order == value ? Palette.primary.opacity(0.07) : Palette.card)
                    .overlay(
                        RoundedRectangle(cornerRadius: 12, style: .continuous)
                            .stroke(order == value ? Palette.primary : Palette.line, lineWidth: 1)
                    )
            )
        }
        .buttonStyle(.plain)
    }
}
