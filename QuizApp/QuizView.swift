import SwiftUI

struct QuizView: View {
    @EnvironmentObject private var store: QuizStore
    @Environment(\.horizontalSizeClass) private var sizeClass
    @State private var confirmSubmit = false
    @State private var confirmExit = false

    private var compact: Bool { sizeClass == .compact }

    var body: some View {
        ZStack {
            PaperBackground()

            ScrollView(showsIndicators: false) {
                VStack(spacing: 0) {
                    topBar
                    progressBar
                    questionCard
                    bottomBar
                }
                .frame(maxWidth: 1100)
                .padding(.horizontal, compact ? 16 : 24)
                .padding(.bottom, 40)
                .frame(maxWidth: .infinity)
            }

            if store.sheetOpen {
                answerSheet
            }
        }
        .alert("还有 \(store.unansweredCount) 题未作答", isPresented: $confirmSubmit) {
            Button("继续答题", role: .cancel) {}
            Button("交卷") { store.finish() }
        } message: {
            Text("未作答将计为错误。")
        }
        .alert("结束本轮？", isPresented: $confirmExit) {
            Button("继续答题", role: .cancel) {}
            Button("结束并返回主页", role: .destructive) { store.leaveQuiz() }
        } message: {
            Text("已作答的记录会保留，可以回主页重新开始。")
        }
    }

    // MARK: - 顶栏

    private var topBar: some View {
        VStack(spacing: 13) {
            HStack(spacing: 12) {
                Button {
                    store.sheetOpen = false
                    if store.answers.contains(where: { $0 != nil }) {
                        confirmExit = true
                    } else {
                        store.leaveQuiz()
                    }
                } label: {
                    Image(systemName: "chevron.left")
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundColor(Palette.ink)
                        .frame(width: 38, height: 38)
                        .background(
                            Circle()
                                .fill(Palette.card)
                                .overlay(Circle().stroke(Palette.line, lineWidth: 1))
                        )
                }
                .buttonStyle(.plain)

                HStack(spacing: 10) {
                    PillLabel(text: currentChapter)
                        .layoutPriority(0)
                    HStack(spacing: 5) {
                        Image(systemName: "clock")
                            .font(.system(size: 12))
                            .foregroundColor(Palette.muted)
                        TimelineView(.periodic(from: .now, by: 1)) { context in
                            Text(formatDuration(context.date.timeIntervalSince(store.startTime)))
                                .font(.quizSans(13))
                                .monospacedDigit()
                                .foregroundColor(Palette.ink2)
                        }
                    }
                }
                .lineLimit(1)

                Spacer(minLength: 4)

                HStack(spacing: 1) {
                    Text("\(min(store.index + 1, store.queue.count))")
                        .font(.quizSerif(19))
                        .foregroundColor(Palette.ink)
                    Text("/\(store.queue.count)")
                        .font(.quizSans(13))
                        .foregroundColor(Palette.ink2)
                }

                Button {
                    withAnimation(.easeOut(duration: 0.2)) {
                        store.sheetOpen = true
                    }
                } label: {
                    Image(systemName: "square.grid.3x3")
                        .font(.system(size: 15, weight: .medium))
                        .foregroundColor(Palette.primary)
                        .frame(width: 38, height: 38)
                        .background(
                            Circle()
                                .fill(Palette.card)
                                .overlay(Circle().stroke(Palette.line, lineWidth: 1))
                        )
                }
                .buttonStyle(.plain)
            }
        }
        .padding(.top, compact ? 12 : 16)
    }

    private var currentChapter: String {
        guard store.queue.indices.contains(store.index) else { return store.scopeLabel }
        return store.queue[store.index].chapter
    }

    private var progressBar: some View {
        ProgressTrack(
            value: store.queue.isEmpty ? 0 : Double(store.index + 1) / Double(store.queue.count),
            height: 4,
            low: true
        )
        .padding(.top, 13)
    }

    // MARK: - 题目卡

    @ViewBuilder
    private var questionCard: some View {
        if store.queue.indices.contains(store.index) {
            let question = store.queue[store.index]
            let answer = store.answers[store.index]
            let isLocked = store.mode == .practice && store.locked[store.index]
            let hasAnswer = question.hasAnswer

            VStack(alignment: .leading, spacing: 0) {
                HStack(alignment: .center, spacing: 12) {
                    Text(question.num)
                        .font(.quizSerif(44))
                        .foregroundColor(Color(hex: "d8cbb3"))
                        .lineLimit(1)
                        .minimumScaleFactor(0.6)

                    VStack(alignment: .leading, spacing: 5) {
                        PillLabel(text: question.chapter)
                        Text(question.metaLine)
                            .font(.quizSans(11))
                            .tracking(0.6)
                            .foregroundColor(Palette.muted)
                            .lineLimit(1)
                            .minimumScaleFactor(0.8)
                    }
                }
                .padding(.bottom, 18)

                Text(question.stem)
                    .font(.quizSans(compact ? 18.5 : 22, weight: .semibold))
                    .foregroundColor(Palette.ink)
                    .lineSpacing(6)
                    .fixedSize(horizontal: false, vertical: true)
                    .padding(.bottom, 24)

                if question.options.isEmpty {
                    noOptionBlock(answered: answer != nil)
                } else {
                    VStack(spacing: 11) {
                        ForEach(question.options.indices, id: \.self) { optionIndex in
                            let option = question.options[optionIndex]
                            let key = option.first ?? ""
                            OptionRow(
                                key: key,
                                text: option.count > 1 ? option[1] : "",
                                visual: visual(for: key, question: question, answer: answer, locked: isLocked),
                                locked: isLocked
                            ) {
                                store.choose(key)
                            }
                        }
                    }
                }

                noteView(question: question, answer: answer, locked: isLocked)
                    .padding(.top, 20)
            }
            .padding(compact ? 20 : 30)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(
                RoundedRectangle(cornerRadius: 22, style: .continuous)
                    .fill(Palette.card)
                    .overlay(
                        RoundedRectangle(cornerRadius: 22, style: .continuous)
                            .stroke(Palette.line, lineWidth: 1)
                    )
            )
            .overlay(alignment: .top) {
                LinearGradient(
                    colors: [Palette.primary, Palette.primary2, Palette.gold],
                    startPoint: .leading,
                    endPoint: .trailing
                )
                .frame(height: 4)
                .clipShape(
                    RoundedRectangle(cornerRadius: 22, style: .continuous)
                )
            }
            .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
            .shadow(color: Color(hex: "212c36").opacity(0.16), radius: 18, x: 0, y: 12)
            .padding(.top, 22)
            .id(question.id)
        }
    }

    private func noOptionBlock(answered: Bool) -> some View {
        VStack(spacing: 12) {
            Text("本题题库未提供选项与答案。")
                .font(.quizSans(13.5))
                .foregroundColor(Palette.amber)
            if answered {
                Text("已确认：题库中无答案")
                    .font(.quizSans(13.5, weight: .semibold))
                    .foregroundColor(Palette.amber)
            } else {
                Button("确认作答（题库无答案）") {
                    store.choose("NA")
                }
                .buttonStyle(GhostPillStyle())
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.horizontal, 20)
        .padding(.vertical, 18)
        .background(
            RoundedRectangle(cornerRadius: 13, style: .continuous)
                .fill(Color(hex: "fbf6ec"))
                .overlay(
                    RoundedRectangle(cornerRadius: 13, style: .continuous)
                        .stroke(Palette.amberLine, style: StrokeStyle(lineWidth: 1, dash: [5, 4]))
                )
        )
    }

    // MARK: - 选项状态

    private func visual(for key: String,
                        question: Question,
                        answer: String?,
                        locked: Bool) -> OptionVisual {
        if !question.hasAnswer {
            return answer == key ? .noAnswer : .normal
        }
        if store.mode == .exam {
            return answer == key ? .selected : .normal
        }
        guard locked else {
            return answer == key ? .selected : .normal
        }
        if key == question.answer { return .correct }
        if key == answer { return .wrong }
        return .dim
    }

    // MARK: - 反馈条

    @ViewBuilder
    private func noteView(question: Question, answer: String?, locked: Bool) -> some View {
        if store.mode == .practice && locked {
            if !question.hasAnswer {
                noteBar(
                    icon: "questionmark",
                    color: Palette.amber,
                    content: "题库中无答案，本题不计分"
                )
            } else if answer == question.answer {
                noteBar(
                    icon: "checkmark",
                    color: Palette.green,
                    content: "回答正确，答案 \(question.answer ?? "")"
                )
            } else {
                noteBar(
                    icon: "xmark",
                    color: Palette.red,
                    content: "回答错误，正确答案是 \(question.answer ?? "")．\(question.answerText)"
                )
            }
        } else if store.mode == .exam {
            noteBar(
                icon: "info.circle",
                color: Palette.muted,
                content: question.hasAnswer
                    ? "考试模式：选择后可修改，交卷后统一评分"
                    : "本题题库中无答案，选择后不计分"
            )
        }
    }

    private func noteBar(icon: String, color: Color, content: String) -> some View {
        HStack(alignment: .center, spacing: 10) {
            Image(systemName: icon)
                .font(.system(size: 12, weight: .bold))
                .foregroundColor(.white)
                .frame(width: 22, height: 22)
                .background(Circle().fill(color))
            Text(content)
                .font(.quizSans(13.5))
                .foregroundColor(color)
                .fixedSize(horizontal: false, vertical: true)
            Spacer(minLength: 0)
        }
        .padding(.top, 16)
        .overlay(alignment: .top) {
            Rectangle()
                .fill(Palette.line)
                .frame(height: 1)
        }
    }

    // MARK: - 底部

    private var bottomBar: some View {
        HStack(spacing: 12) {
            Button {
                store.previous()
            } label: {
                Text("上一题")
                    .frame(minWidth: 100)
            }
            .buttonStyle(GhostPillStyle())
            .opacity(store.index == 0 ? 0.45 : 1)
            .allowsHitTesting(store.index > 0)

            Spacer()

            if !compact {
                Text("A–D 作答 · ←/→ 切换")
                    .font(.quizSans(11.5))
                    .foregroundColor(Palette.muted)
            }

            Button {
                if store.index + 1 < store.queue.count {
                    store.next()
                } else if store.mode == .exam && store.unansweredCount > 0 {
                    confirmSubmit = true
                } else {
                    store.finish()
                }
            } label: {
                HStack(spacing: 8) {
                    Text(store.index + 1 < store.queue.count
                         ? "下一题"
                         : (store.mode == .exam ? "交卷" : "查看结果"))
                    Image(systemName: store.index + 1 < store.queue.count ? "arrow.right" : "checkmark")
                }
                .frame(minWidth: 112)
            }
            .buttonStyle(PrimaryPillStyle())
        }
        .padding(.top, 20)
    }

    // MARK: - 答题卡

    private var answerSheet: some View {
        GeometryReader { geo in
            ZStack(alignment: .trailing) {
                Color.black.opacity(0.34)
                    .ignoresSafeArea()
                    .onTapGesture {
                        withAnimation(.easeOut(duration: 0.2)) {
                            store.sheetOpen = false
                        }
                    }

                ScrollView(showsIndicators: false) {
                    VStack(alignment: .leading, spacing: 0) {
                        HStack {
                            Text("答题卡")
                                .font(.quizSerif(18))
                                .foregroundColor(Palette.ink)
                            Spacer()
                            Button {
                                withAnimation(.easeOut(duration: 0.2)) {
                                    store.sheetOpen = false
                                }
                            } label: {
                                Image(systemName: "xmark")
                                    .font(.system(size: 13, weight: .semibold))
                                    .foregroundColor(Palette.ink)
                                    .frame(width: 32, height: 32)
                                    .background(Circle().fill(Color(hex: "faf6ee")))
                            }
                            .buttonStyle(.plain)
                        }

                        Text(sheetSubtitle)
                            .font(.quizSans(12.5))
                            .foregroundColor(Palette.muted)
                            .padding(.top, 4)
                            .padding(.bottom, 18)

                        LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 9), count: 5), spacing: 9) {
                            ForEach(store.queue.indices, id: \.self) { position in
                                sheetCell(position)
                            }
                        }

                        LazyVGrid(
                            columns: [GridItem(.flexible(), alignment: .leading), GridItem(.flexible(), alignment: .leading)],
                            alignment: .leading,
                            spacing: 10
                        ) {
                            legend(color: Color(hex: "faf6ee"), text: "未作答")
                            legend(color: Color(hex: "eaf1f3"), text: "已作答")
                            legend(color: Palette.greenBG, text: "答对")
                            legend(color: Palette.redBG, text: "答错")
                            legend(color: Color(hex: "f9efdc"), text: "题库无答案")
                        }
                        .padding(.top, 18)
                        .padding(.bottom, 22)

                        Button {
                            if store.mode == .exam && store.unansweredCount > 0 {
                                store.sheetOpen = false
                                confirmSubmit = true
                            } else {
                                store.finish()
                            }
                        } label: {
                            Text(store.mode == .exam ? "交卷并查看结果" : "结束本轮")
                                .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(PrimaryPillStyle())
                    }
                    .padding(24)
                }
                .frame(width: min(430, max(0, geo.size.width - 24)), height: geo.size.height)
                .background(Palette.card)
                .shadow(color: .black.opacity(0.3), radius: 30, x: -12, y: 0)
            }
        }
        .transition(.opacity)
    }

    private var sheetSubtitle: String {
        let answered = store.answers.filter { $0 != nil }.count
        var text = "共 \(store.queue.count) 题，已作答 \(answered) 题"
        if store.noAnswerCount > 0 {
            text += " · \(store.noAnswerCount) 题无答案"
        }
        return text
    }

    private func sheetCell(_ position: Int) -> some View {
        let question = store.queue[position]
        let answer = store.answers[position]
        let locked = store.mode == .practice && store.locked[position]
        let current = position == store.index

        var fill = Color(hex: "faf6ee")
        var textColor = Palette.ink2

        if !question.hasAnswer, answer != nil {
            fill = Color(hex: "f9efdc")
            textColor = Palette.amber
        } else if store.mode == .practice && locked {
            if answer == question.answer {
                fill = Palette.greenBG
                textColor = Palette.green
            } else {
                fill = Palette.redBG
                textColor = Palette.red
            }
        } else if answer != nil {
            fill = Color(hex: "eaf1f3")
            textColor = Palette.primary
        }

        return Button {
            store.jump(to: position)
        } label: {
            Text("\(position + 1)")
                .font(.quizSans(13, weight: .semibold))
                .foregroundColor(textColor)
                .frame(maxWidth: .infinity)
                .frame(height: 42)
                .background(
                    RoundedRectangle(cornerRadius: 9, style: .continuous)
                        .fill(fill)
                )
                .overlay(
                    RoundedRectangle(cornerRadius: 9, style: .continuous)
                        .stroke(current ? Palette.primary : Palette.line,
                                lineWidth: current ? 2 : 1)
                )
        }
        .buttonStyle(.plain)
    }

    private func legend(color: Color, text: String) -> some View {
        HStack(spacing: 7) {
            RoundedRectangle(cornerRadius: 4, style: .continuous)
                .fill(color)
                .frame(width: 12, height: 12)
                .overlay(
                    RoundedRectangle(cornerRadius: 4, style: .continuous)
                        .stroke(Palette.line, lineWidth: 1)
                )
            Text(text)
                .font(.quizSans(11.5))
                .foregroundColor(Palette.muted)
        }
    }
}

// MARK: - 选项行

private enum OptionVisual: Equatable {
    case normal
    case selected
    case correct
    case wrong
    case dim
    case noAnswer
}

private struct OptionRow: View {
    let key: String
    let text: String
    let visual: OptionVisual
    let locked: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 14) {
                Text(key)
                    .font(.quizSans(14, weight: .bold))
                    .foregroundColor(keyColor)
                    .frame(width: 30, height: 30)
                    .background(
                        RoundedRectangle(cornerRadius: 8, style: .continuous)
                            .fill(keyBackground)
                    )

                Text(text)
                    .font(.quizSans(15.5))
                    .foregroundColor(Palette.ink)
                    .multilineTextAlignment(.leading)
                    .fixedSize(horizontal: false, vertical: true)

                Spacer(minLength: 6)

                if let mark = markIcon {
                    Image(systemName: mark)
                        .font(.system(size: 17, weight: .semibold))
                        .foregroundColor(markColor)
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 14)
            .background(
                RoundedRectangle(cornerRadius: 13, style: .continuous)
                    .fill(background)
            )
            .overlay(
                RoundedRectangle(cornerRadius: 13, style: .continuous)
                    .stroke(border, lineWidth: 1)
            )
            .opacity(visual == .dim ? 0.55 : 1)
        }
        .buttonStyle(.plain)
        .allowsHitTesting(!locked)
    }

    private var background: Color {
        switch visual {
        case .selected: return Color(hex: "eff5f7")
        case .correct: return Palette.greenBG
        case .wrong: return Palette.redBG
        case .noAnswer: return Palette.amberBG
        default: return Color(hex: "fdfaf3")
        }
    }

    private var border: Color {
        switch visual {
        case .selected: return Palette.primary
        case .correct: return Palette.green
        case .wrong: return Palette.red
        case .noAnswer: return Palette.amberLine
        default: return Palette.line
        }
    }

    private var keyBackground: Color {
        switch visual {
        case .selected: return Palette.primary
        case .correct: return Palette.green
        case .wrong: return Palette.red
        case .noAnswer: return Palette.gold
        default: return Color(hex: "eee6d6")
        }
    }

    private var keyColor: Color {
        switch visual {
        case .selected, .correct, .wrong, .noAnswer: return .white
        default: return Color(hex: "6a5a3c")
        }
    }

    private var markIcon: String? {
        switch visual {
        case .correct: return "checkmark"
        case .wrong: return "xmark"
        default: return nil
        }
    }

    private var markColor: Color {
        visual == .correct ? Palette.green : Palette.red
    }
}
