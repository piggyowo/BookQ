import SwiftUI

struct ResultView: View {
    @EnvironmentObject private var store: QuizStore
    @Environment(\.horizontalSizeClass) private var sizeClass
    @State private var showAll = false
    @State private var expanded = false

    private var compact: Bool { sizeClass == .compact }

    var body: some View {
        ZStack {
            PaperBackground()
            if let result = store.result {
                content(result)
            } else {
                VStack(spacing: 14) {
                    Text("还没有本轮结果")
                        .font(.quizSerif(24))
                        .foregroundColor(Palette.ink)
                    Button("返回首页") { store.goHome() }
                        .buttonStyle(PrimaryPillStyle())
                }
            }
        }
    }

    private func content(_ result: QuizResult) -> some View {
        ScrollView(showsIndicators: false) {
            VStack(alignment: .leading, spacing: 0) {
                backBar
                scoreCard(result)
                statGrid(result)
                    .padding(.top, 16)

                if !result.chapterStats.isEmpty {
                    SectionHeader(title: "分类掌握度", trailing: "本轮作答")
                        .padding(.top, 34)
                        .padding(.bottom, 14)
                    VStack(spacing: 16) {
                        ForEach(result.chapterStats) { stat in
                            chapterRow(stat)
                        }
                    }
                    .cardSurface(radius: 20, padding: 22)
                }

                reviewSection(result)

                actionButtons
                    .padding(.top, 24)

                Text(totalAttemptsText)
                    .font(.quizSans(11.5))
                    .foregroundColor(Palette.muted)
                    .frame(maxWidth: .infinity, alignment: .center)
                    .padding(.top, 24)
            }
            .frame(maxWidth: 1100)
            .padding(.horizontal, compact ? 16 : 24)
            .padding(.bottom, 48)
            .frame(maxWidth: .infinity)
        }
    }

    private var backBar: some View {
        HStack {
            Button {
                store.goHome()
            } label: {
                HStack(spacing: 7) {
                    Image(systemName: "chevron.left")
                    Text("返回首页")
                }
            }
            .buttonStyle(GhostPillStyle())
            Spacer()
        }
        .padding(.top, compact ? 12 : 18)
        .padding(.bottom, 16)
    }

    // MARK: - 得分卡

    private func scoreCard(_ result: QuizResult) -> some View {
        Group {
            if compact {
                VStack(alignment: .leading, spacing: 20) {
                    donut(result)
                    scoreInfo(result)
                }
            } else {
                HStack(alignment: .center, spacing: 36) {
                    donut(result)
                    scoreInfo(result)
                }
            }
        }
        .padding(compact ? 24 : 34)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: 24, style: .continuous)
                .fill(
                    LinearGradient(
                        colors: [Color(hex: "17303f"), Color(hex: "1e4a60")],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )
        )
        .shadow(color: Color(hex: "142837").opacity(0.5), radius: 28, x: 0, y: 22)
    }

    private func donut(_ result: QuizResult) -> some View {
        let size: CGFloat = compact ? 138 : 168
        let color = scoreColor(result.percent)
        return ZStack {
            Circle()
                .stroke(Color.white.opacity(0.12), lineWidth: 13)
            Circle()
                .trim(from: 0, to: CGFloat(result.percent) / 100)
                .stroke(color, style: StrokeStyle(lineWidth: 13, lineCap: .round))
                .rotationEffect(.degrees(-90))
            VStack(spacing: 2) {
                Text("\(result.percent)")
                    .font(.quizSerif(compact ? 44 : 52))
                    .foregroundColor(.white)
                    .monospacedDigit()
                Text("正确率")
                    .font(.quizSans(10))
                    .tracking(2)
                    .foregroundColor(Palette.cream.opacity(0.6))
            }
        }
        .frame(width: size, height: size)
    }

    private func scoreInfo(_ result: QuizResult) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(result.grade)
                .font(.quizSans(12, weight: .medium))
                .tracking(1.6)
                .foregroundColor(Palette.cream.opacity(0.9))
                .padding(.horizontal, 13)
                .padding(.vertical, 5)
                .background(
                    Capsule()
                        .fill(Color.white.opacity(0.12))
                        .overlay(Capsule().stroke(Color.white.opacity(0.18), lineWidth: 1))
                )
                .padding(.bottom, 6)

            Text("\(result.total) 题答对 \(result.correct) 题")
                .font(.quizSerif(compact ? 23 : 26))
                .foregroundColor(Color(hex: "fff8ec"))
                .fixedSize(horizontal: false, vertical: true)

            Text(resultSubtitle(result))
                .font(.quizSans(13))
                .foregroundColor(Palette.cream.opacity(0.66))
                .lineSpacing(4)
                .fixedSize(horizontal: false, vertical: true)
        }
    }

    private func resultSubtitle(_ result: QuizResult) -> String {
        var parts: [String] = [
            result.scopeLabel,
            result.mode.title,
            "用时 \(formatDuration(result.duration))"
        ]
        if result.unanswered > 0 {
            parts.append("未作答 \(result.unanswered) 题")
        }
        if result.noAnswerCount > 0 {
            parts.append("题库无答案 \(result.noAnswerCount) 题（未计入）")
        }
        return parts.joined(separator: " · ")
    }

    private func scoreColor(_ percent: Int) -> Color {
        switch percent {
        case 90...: return Color(hex: "57b99a")
        case 80..<90: return Color(hex: "7fb2c9")
        case 60..<80: return Palette.gold
        default: return Color(hex: "d98a72")
        }
    }

    // MARK: - 统计

    private func statGrid(_ result: QuizResult) -> some View {
        LazyVGrid(
            columns: [GridItem(.adaptive(minimum: compact ? 140 : 200), spacing: 14)],
            alignment: .leading,
            spacing: 14
        ) {
            statCard("正确", "\(result.correct)", "题")
            statCard("错误", "\(max(0, result.total - result.correct))", "题")
            statCard("正确率", "\(result.percent)", "%")
            statCard("错题本累计", "\(store.wrongIDs.count)", "题")
        }
    }

    private func statCard(_ label: String, _ value: String, _ unit: String) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label)
                .font(.quizSans(11.5))
                .tracking(1.1)
                .foregroundColor(Palette.muted)
            HStack(alignment: .firstTextBaseline, spacing: 3) {
                Text(value)
                    .font(.quizSerif(24))
                    .foregroundColor(Palette.ink)
                Text(unit)
                    .font(.quizSans(11.5))
                    .foregroundColor(Palette.muted)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .cardSurface(radius: 16, padding: 16)
    }

    private func chapterRow(_ stat: ChapterStat) -> some View {
        HStack(spacing: 14) {
            Text(stat.name)
                .font(.quizSans(compact ? 12 : 13.5))
                .foregroundColor(Palette.ink)
                .lineLimit(1)
                .frame(width: compact ? 76 : 120, alignment: .leading)

            ProgressTrack(value: stat.total == 0 ? 0 : Double(stat.correct) / Double(stat.total),
                          height: 9,
                          low: stat.rate < 60)

            Text("\(stat.correct)/\(stat.total) · \(stat.rate)%")
                .font(.quizSans(13, weight: .medium))
                .foregroundColor(Palette.ink2)
                .monospacedDigit()
                .lineLimit(1)
                .minimumScaleFactor(0.8)
                .frame(width: compact ? 92 : 108, alignment: .trailing)
        }
    }

    // MARK: - 回顾

    private var filteredItems: [ResultItem] {
        guard let result = store.result else { return [] }
        return showAll ? result.items : result.items.filter { !$0.isCorrect }
    }

    private func reviewSection(_ result: QuizResult) -> some View {
        let wrongCount = result.items.filter { !$0.isCorrect }.count
        return VStack(alignment: .leading, spacing: 0) {
            HStack {
                Text("答题回顾")
                    .font(.quizSerif(19))
                    .foregroundColor(Palette.ink)
                Spacer()
                HStack(spacing: 3) {
                    tabButton("只看错题", selected: !showAll) { showAll = false }
                    tabButton("全部 \(result.items.count) 题", selected: showAll) { showAll = true }
                }
                .padding(3)
                .background(Capsule().fill(Palette.track))
            }
            .padding(.top, 34)
            .padding(.bottom, 14)

            let items = filteredItems
            if items.isEmpty {
                Text(wrongCount == 0 ? "本轮没有错题，保持这个状态。" : "暂无内容")
                    .font(.quizSans(13.5))
                    .foregroundColor(Palette.muted)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 34)
                    .cardSurface(radius: 16, padding: 0)
            } else {
                VStack(spacing: 10) {
                    ForEach(Array(items.prefix(expanded ? items.count : 8))) { item in
                        reviewRow(item)
                    }
                }
                if items.count > 8 {
                    Button(expanded ? "收起" : "展开全部 \(items.count) 条") {
                        withAnimation(.easeOut(duration: 0.2)) {
                            expanded.toggle()
                        }
                    }
                    .buttonStyle(GhostPillStyle())
                    .frame(maxWidth: .infinity)
                    .padding(.top, 14)
                }
            }
        }
    }

    private func tabButton(_ title: String, selected: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(title)
                .font(.quizSans(12, weight: selected ? .semibold : .regular))
                .foregroundColor(selected ? Palette.primary : Palette.ink2)
                .padding(.horizontal, 13)
                .padding(.vertical, 5)
                .background(
                    Capsule().fill(selected ? Color.white : Color.clear)
                )
        }
        .buttonStyle(.plain)
    }

    private func reviewRow(_ item: ResultItem) -> some View {
        VStack(alignment: .leading, spacing: 7) {
            HStack(spacing: 8) {
                Text(item.isCorrect ? "正确" : "错误")
                    .font(.quizSans(11.5, weight: .semibold))
                    .foregroundColor(item.isCorrect ? Palette.green : Palette.red)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 2)
                    .background(
                        Capsule().fill(item.isCorrect ? Palette.greenBG : Palette.redBG)
                    )
                Text("\(item.question.chapter) · 第 \(item.question.num) 题")
                    .font(.quizSans(11.5))
                    .foregroundColor(Palette.muted)
                    .lineLimit(1)
            }

            Text(item.question.stem)
                .font(.quizSans(14.5, weight: .medium))
                .foregroundColor(Palette.ink)
                .lineSpacing(4)
                .fixedSize(horizontal: false, vertical: true)

            VStack(alignment: .leading, spacing: 4) {
                Text("正确答案：\(item.question.answer ?? "")．\(item.question.answerText)")
                    .foregroundColor(Palette.green)
                if let choice = item.choice, choice != item.question.answer {
                    Text("你的选择：\(choice)")
                        .foregroundColor(Palette.red)
                        .strikethrough()
                } else if item.choice == nil {
                    Text("未作答")
                        .foregroundColor(Palette.muted)
                }
            }
            .font(.quizSans(12.5))
            .lineSpacing(3)
            .fixedSize(horizontal: false, vertical: true)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .fill(Color(hex: "fdfaf3"))
                .overlay(
                    RoundedRectangle(cornerRadius: 14, style: .continuous)
                        .stroke(Palette.line, lineWidth: 1)
                )
        )
    }

    // MARK: - 操作

    private var actionButtons: some View {
        LazyVGrid(
            columns: [GridItem(.adaptive(minimum: compact ? 140 : 160), spacing: 12)],
            alignment: .leading,
            spacing: 12
        ) {
            Button {
                store.restart()
            } label: {
                Text("再来一轮")
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(PrimaryPillStyle())

            Button {
                store.screen = .wrong
            } label: {
                Text("去错题本")
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(GhostPillStyle())

            Button {
                store.goHome()
            } label: {
                Text("返回首页")
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(GhostPillStyle())
        }
    }

    private var totalAttemptsText: String {
        "成绩已记录在本机 · 共 \(store.attempts.count) 次作答记录"
    }
}

// MARK: - 错题本

struct WrongBookView: View {
    @EnvironmentObject private var store: QuizStore
    @Environment(\.horizontalSizeClass) private var sizeClass
    @State private var confirmClear = false

    private var compact: Bool { sizeClass == .compact }

    var body: some View {
        ZStack {
            PaperBackground()
            ScrollView(showsIndicators: false) {
                VStack(alignment: .leading, spacing: 0) {
                    topBar
                    header
                    if store.wrongQuestions.isEmpty {
                        emptyState
                    } else {
                        LazyVStack(spacing: 12) {
                            ForEach(store.wrongQuestions) { question in
                                wrongCard(question)
                            }
                        }
                    }
                }
                .frame(maxWidth: 1100)
                .padding(.horizontal, compact ? 16 : 24)
                .padding(.bottom, 48)
                .frame(maxWidth: .infinity)
            }
        }
        .alert("清空错题本？", isPresented: $confirmClear) {
            Button("取消", role: .cancel) {}
            Button("清空", role: .destructive) { store.clearWrong() }
        } message: {
            Text("此操作不可撤销。")
        }
    }

    private var topBar: some View {
        HStack(spacing: 10) {
            Button {
                store.goHome()
            } label: {
                HStack(spacing: 7) {
                    Image(systemName: "chevron.left")
                    Text("返回首页")
                }
            }
            .buttonStyle(GhostPillStyle())
            Spacer()
            if !store.wrongQuestions.isEmpty {
                Button("清空") { confirmClear = true }
                    .buttonStyle(GhostPillStyle())
            }
        }
        .padding(.top, compact ? 12 : 18)
        .padding(.bottom, 18)
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("错题本")
                .font(.quizSerif(28))
                .foregroundColor(Palette.ink)
            Text("共 \(store.wrongQuestions.count) 题 · 答对后会自动移出")
                .font(.quizSans(13))
                .foregroundColor(Palette.muted)
            if !store.wrongQuestions.isEmpty {
                Button {
                    store.startWrongPractice()
                } label: {
                    HStack(spacing: 8) {
                        Text("开始重练")
                        Image(systemName: "arrow.right")
                    }
                }
                .buttonStyle(PrimaryPillStyle())
                .padding(.top, 10)
            }
        }
        .padding(.bottom, 22)
    }

    private var emptyState: some View {
        VStack(spacing: 12) {
            Image(systemName: "book.closed")
                .font(.system(size: 38, weight: .light))
                .foregroundColor(Palette.muted.opacity(0.6))
            Text("错题本是空的")
                .font(.quizSerif(22))
                .foregroundColor(Palette.ink)
            Text("练习中答错的题会自动收集到这里。")
                .font(.quizSans(13))
                .foregroundColor(Palette.muted)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 70)
    }

    private func wrongCard(_ question: Question) -> some View {
        VStack(alignment: .leading, spacing: 9) {
            HStack(spacing: 8) {
                PillLabel(text: question.chapter)
                Text("第 \(question.num) 题")
                    .font(.quizSans(11.5))
                    .foregroundColor(Palette.muted)
                Spacer()
                Button("移除") {
                    store.removeWrong(question.id)
                }
                .font(.quizSans(12))
                .foregroundColor(Palette.muted)
                .buttonStyle(.plain)
            }

            Text(question.stem)
                .font(.quizSans(15.5, weight: .medium))
                .foregroundColor(Palette.ink)
                .lineSpacing(4)
                .fixedSize(horizontal: false, vertical: true)

            HStack(spacing: 6) {
                Image(systemName: "checkmark.circle")
                    .foregroundColor(Palette.green)
                Text("正确答案：\(question.answer ?? "")．\(question.answerText)")
                    .foregroundColor(Palette.green)
                    .font(.quizSans(13, weight: .semibold))
                    .lineSpacing(3)
            }
            .padding(.top, 2)
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .fill(Palette.card)
                .overlay(
                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                        .stroke(Palette.line, lineWidth: 1)
                )
        )
        .shadow(color: Color(hex: "212c36").opacity(0.14), radius: 14, x: 0, y: 9)
    }
}
