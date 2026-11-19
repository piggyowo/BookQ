import SwiftUI

struct HomeView: View {
    @EnvironmentObject var store: QuizStore
    @State private var showSetup = false
    @State private var presetBankID: UUID?

    var body: some View {
        ScrollView(showsIndicators: false) {
            VStack(spacing: 20) {
                topBar

                if store.banks.isEmpty {
                    emptyHero
                } else {
                    practiceHero
                    myBanks
                    weeklyCard
                    weakSection
                }
            }
            .padding(.horizontal, 18)
            .padding(.top, 8)
            .padding(.bottom, 26)
        }
        .background(PaperBackground())
        .sheet(isPresented: $showSetup) {
            StartQuizSheet(presetBankID: presetBankID)
                .presentationDetents([.large])
                .presentationDragIndicator(.hidden)
        }
    }

    // MARK: - 顶栏

    private var topBar: some View {
        HStack(spacing: 11) {
            BrandMark(size: 42)
            VStack(alignment: .leading, spacing: 2) {
                Text("工艺题库")
                    .font(.quizSerif(20))
                    .foregroundColor(Palette.ink)
                Text("CRAFT QUIZ · 随身练")
                    .font(.quizSans(9, weight: .medium))
                    .tracking(1.4)
                    .foregroundColor(Palette.muted)
            }
            Spacer(minLength: 8)
            Button {
                store.screen = .wrong
            } label: {
                HStack(spacing: 5) {
                    Image(systemName: "bookmark")
                        .font(.system(size: 12, weight: .semibold))
                    Text("错题本")
                        .font(.quizSans(12.5, weight: .medium))
                    if !store.wrongIDs.isEmpty {
                        Text("\(store.wrongIDs.count)")
                            .font(.quizSans(10, weight: .semibold))
                            .foregroundColor(.white)
                            .padding(.horizontal, 5)
                            .padding(.vertical, 2)
                            .background(Capsule().fill(Palette.accent))
                    }
                }
                .foregroundColor(Palette.ink2)
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(
                    Capsule()
                        .fill(Palette.card.opacity(0.9))
                        .overlay(Capsule().stroke(Palette.line, lineWidth: 1))
                )
            }
            .buttonStyle(.plain)
        }
    }

    // MARK: - 空题库

    private var emptyHero: some View {
        VStack(alignment: .leading, spacing: 0) {
            ZStack(alignment: .topTrailing) {
                LinearGradient(
                    colors: [Color(hex: "152b3a"), Color(hex: "1c455c"), Color(hex: "25596a")],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
                FabricGrid(spacing: 5, opacity: 0.045, light: true)

                PaperStackArt()
                    .scaleEffect(0.72)
                    .offset(x: 30, y: 52)
                    .opacity(0.9)
                    .allowsHitTesting(false)

                VStack(alignment: .leading, spacing: 13) {
                    Text("LIBRARY EMPTY")
                        .font(.quizSans(9.5, weight: .semibold))
                        .tracking(2)
                        .foregroundColor(Palette.cream.opacity(0.6))

                    Text("你的题库，\n还在文件里")
                        .font(.quizSerif(30))
                        .foregroundColor(Palette.cream)
                        .lineSpacing(3)

                    Text("导入一份 Word 或文本题库，\n把工艺知识装进手机。")
                        .font(.quizSans(13.5))
                        .foregroundColor(Palette.cream.opacity(0.72))
                        .lineSpacing(4)

                    HStack(spacing: 7) {
                        formatChip("DOCX")
                        formatChip("TXT")
                        formatChip("MD")
                        formatChip("JSON")
                    }
                    .padding(.top, 2)
                }
                .padding(24)
                .padding(.trailing, 42)

            }
            .frame(minHeight: 286)
            .clipShape(RoundedRectangle(cornerRadius: 26, style: .continuous))
            .shadow(color: Palette.primary.opacity(0.18), radius: 22, x: 0, y: 12)

            VStack(alignment: .leading, spacing: 14) {
                Text("导入之后")
                    .font(.quizSerif(17))
                    .foregroundColor(Palette.ink)
                featureRow(icon: "doc.text.magnifyingglass", title: "解析后先确认", subtitle: "题库名、题数、样例都能检查，再保存")
                featureRow(icon: "iphone", title: "全部存在本机", subtitle: "不联网也能刷，答题记录只留在这台设备")
                featureRow(icon: "exclamationmark.bubble", title: "无答案也会提示", subtitle: "题库里没写答案的题，不计分、不进错题本")
            }
            .padding(.top, 22)

            Button {
                store.selectedTab = .library
            } label: {
                HStack(spacing: 8) {
                    Image(systemName: "square.and.arrow.down")
                    Text("去题库导入")
                }
                .font(.quizSans(15, weight: .semibold))
                .foregroundColor(.white)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 14)
                .background(
                    RoundedRectangle(cornerRadius: 13, style: .continuous)
                        .fill(Palette.primary)
                )
            }
            .buttonStyle(.plain)
            .padding(.top, 20)
        }
    }

    private func formatChip(_ text: String) -> some View {
        Text(text)
            .font(.quizSans(10, weight: .semibold))
            .foregroundColor(Palette.cream.opacity(0.85))
            .padding(.horizontal, 8)
            .padding(.vertical, 3)
            .background(
                RoundedRectangle(cornerRadius: 5, style: .continuous)
                    .stroke(Palette.cream.opacity(0.28), lineWidth: 1)
            )
    }

    private func featureRow(icon: String, title: String, subtitle: String) -> some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: icon)
                .font(.system(size: 14, weight: .medium))
                .foregroundColor(Palette.primary)
                .frame(width: 30, height: 30)
                .background(
                    RoundedRectangle(cornerRadius: 9, style: .continuous)
                        .fill(Palette.primary.opacity(0.08))
                )
            VStack(alignment: .leading, spacing: 3) {
                Text(title)
                    .font(.quizSans(13.5, weight: .semibold))
                    .foregroundColor(Palette.ink)
                Text(subtitle)
                    .font(.quizSans(12))
                    .foregroundColor(Palette.muted)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
    }

    // MARK: - 有题库时的主视觉

    private var practiceHero: some View {
        let bank = store.lastBank
        let progress = bank.map { store.bankProgress($0) }
        let count = store.defaultSize == 0 ? "全部" : "\(store.defaultSize) 道"

        return ZStack(alignment: .topLeading) {
            LinearGradient(
                colors: [Color(hex: "152b3a"), Color(hex: "1c455c"), Color(hex: "25596a")],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            FabricGrid(spacing: 5, opacity: 0.045, light: true)

            VStack(alignment: .leading, spacing: 14) {
                HStack(alignment: .top) {
                    VStack(alignment: .leading, spacing: 5) {
                        Text("TODAY'S PRACTICE")
                            .font(.quizSans(9.5, weight: .semibold))
                            .tracking(2)
                            .foregroundColor(Palette.cream.opacity(0.6))
                        Text("今天，练 \(count)")
                            .font(.quizSerif(29))
                            .foregroundColor(Palette.cream)
                    }
                    Spacer(minLength: 10)
                    if let progress {
                        RingProgress(value: progress.percent, size: 72, lineWidth: 7)
                    }
                }

                if let bank {
                    HStack(spacing: 7) {
                        Image(systemName: "books.vertical.fill")
                            .font(.system(size: 10, weight: .semibold))
                        Text(bank.name)
                            .lineLimit(1)
                        Text("· 共 \(bank.count) 题")
                            .foregroundColor(Palette.cream.opacity(0.55))
                    }
                    .font(.quizSans(11.5, weight: .medium))
                    .foregroundColor(Palette.cream.opacity(0.85))
                }

                HStack(spacing: 10) {
                    Button {
                        presetBankID = nil
                        showSetup = true
                    } label: {
                        HStack(spacing: 8) {
                            Image(systemName: "play.fill")
                                .font(.system(size: 11))
                            Text("开始练习")
                                .font(.quizSans(14.5, weight: .semibold))
                        }
                        .foregroundColor(Palette.primary)
                        .padding(.horizontal, 22)
                        .padding(.vertical, 12)
                        .background(Capsule().fill(Palette.cream))
                    }
                    .buttonStyle(.plain)

                    if !store.wrongIDs.isEmpty {
                        Button {
                            store.screen = .wrong
                        } label: {
                            Text("\(store.wrongIDs.count) 道错题")
                                .font(.quizSans(11.5))
                                .foregroundColor(Palette.cream.opacity(0.75))
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(.top, 2)
            }
            .padding(22)
        }
        .frame(minHeight: 212)
        .clipShape(RoundedRectangle(cornerRadius: 26, style: .continuous))
        .shadow(color: Palette.primary.opacity(0.20), radius: 22, x: 0, y: 12)
    }

    // MARK: - 我的题库

    private var myBanks: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("我的题库")
                    .font(.quizSerif(19))
                    .foregroundColor(Palette.ink)
                Spacer()
                Button("管理") {
                    store.selectedTab = .library
                }
                .font(.quizSans(12.5))
                .foregroundColor(Palette.primary)
                .buttonStyle(.plain)
            }

            VStack(spacing: 0) {
                ForEach(Array(store.banks.prefix(3))) { bank in
                    Button {
                        presetBankID = bank.id
                        showSetup = true
                    } label: {
                        bankRow(bank)
                    }
                    .buttonStyle(.plain)

                    if bank.id != store.banks.prefix(3).last?.id {
                        Rectangle()
                            .fill(Palette.line2)
                            .frame(height: 1)
                            .padding(.leading, 54)
                    }
                }
            }
            .padding(.vertical, 4)
            .background(
                RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .fill(Palette.card)
                    .overlay(
                        RoundedRectangle(cornerRadius: 18, style: .continuous)
                            .stroke(Palette.line, lineWidth: 1)
                    )
            )

            if store.banks.count > 3 {
                Button {
                    store.selectedTab = .library
                } label: {
                    Text("查看全部 \(store.banks.count) 个题库")
                        .font(.quizSans(12))
                        .foregroundColor(Palette.muted)
                }
                .buttonStyle(.plain)
                .padding(.leading, 4)
            }
        }
    }

    private func bankRow(_ bank: QuizBank) -> some View {
        let progress = store.bankProgress(bank)
        return HStack(spacing: 12) {
            ZStack {
                RoundedRectangle(cornerRadius: 10, style: .continuous)
                    .fill(bank.colorIndex == 0 ? Palette.primary : Palette.accent)
                Image(systemName: "doc.text")
                    .font(.system(size: 15, weight: .medium))
                    .foregroundColor(Palette.cream)
            }
            .frame(width: 38, height: 42)

            VStack(alignment: .leading, spacing: 5) {
                Text(bank.name)
                    .font(.quizSans(14, weight: .semibold))
                    .foregroundColor(Palette.ink)
                    .lineLimit(1)
                HStack(spacing: 6) {
                    Text("\(bank.count) 题")
                    Text("·")
                    Text(progress.rate == nil ? "未练习" : "正确率 \(progress.rate ?? 0)%")
                }
                .font(.quizSans(11))
                .foregroundColor(Palette.muted)
                .lineLimit(1)
            }

            Spacer(minLength: 8)

            VStack(alignment: .trailing, spacing: 5) {
                Text("\(Int(progress.percent * 100))%")
                    .font(.quizSerif(15))
                    .foregroundColor(Palette.primary)
                ProgressTrack(value: progress.percent, height: 4)
                    .frame(width: 54)
            }

            Image(systemName: "chevron.right")
                .font(.system(size: 10, weight: .semibold))
                .foregroundColor(Palette.line)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .contentShape(Rectangle())
    }

    // MARK: - 本周

    private var weeklyCard: some View {
        let stats = store.weeklyStats
        return VStack(alignment: .leading, spacing: 13) {
            HStack {
                Text("这周的手感")
                    .font(.quizSerif(18))
                    .foregroundColor(Palette.ink)
                Spacer()
                Text(stats.days == 0 ? "还没开始" : "练了 \(stats.days) 天")
                    .font(.quizSans(11))
                    .foregroundColor(Palette.muted)
            }

            HStack(alignment: .center, spacing: 16) {
                VStack(alignment: .leading, spacing: 2) {
                    Text("\(stats.total)")
                        .font(.quizSerif(36))
                        .foregroundColor(Palette.primary)
                    Text("本周答题")
                        .font(.quizSans(11))
                        .foregroundColor(Palette.muted)
                }
                .frame(width: 86, alignment: .leading)

                Rectangle()
                    .fill(Palette.line2)
                    .frame(width: 1, height: 42)

                VStack(alignment: .leading, spacing: 3) {
                    Text(stats.rate == nil ? "—" : "\(stats.rate ?? 0)%")
                        .font(.quizSerif(22))
                        .foregroundColor(Palette.accent)
                    Text("正确率")
                        .font(.quizSans(11))
                        .foregroundColor(Palette.muted)
                }

                Spacer()

                ZStack {
                    Circle()
                        .fill(Palette.primary.opacity(0.07))
                        .frame(width: 46, height: 46)
                    Image(systemName: stats.days >= 5 ? "flame.fill" : "leaf.fill")
                        .font(.system(size: 18, weight: .medium))
                        .foregroundColor(stats.days >= 5 ? Palette.accent : Palette.primary)
                }
            }
        }
        .padding(18)
        .background(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .fill(Palette.card)
                .overlay(
                    RoundedRectangle(cornerRadius: 18, style: .continuous)
                        .stroke(Palette.line, lineWidth: 1)
                )
        )
    }

    // MARK: - 薄弱

    private var weakSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("薄弱环节")
                .font(.quizSerif(19))
                .foregroundColor(Palette.ink)

            if store.weakestBanks.isEmpty {
                Text("完成一轮练习后，这里会按题库正确率给出排序。")
                    .font(.quizSans(12))
                    .foregroundColor(Palette.muted)
                    .padding(.vertical, 6)
            } else {
                VStack(spacing: 13) {
                    ForEach(store.weakestBanks) { bank in
                        let progress = store.bankProgress(bank)
                        HStack(spacing: 10) {
                            Text(bank.name)
                                .font(.quizSans(12.5, weight: .medium))
                                .foregroundColor(Palette.ink2)
                                .frame(width: 104, alignment: .leading)
                                .lineLimit(1)
                            ProgressTrack(value: progress.percent, height: 5, low: true)
                            Text(progress.rate == nil ? "未练" : "\(progress.rate ?? 0)%")
                                .font(.quizSans(11.5, weight: .semibold))
                                .foregroundColor(Palette.accent)
                                .frame(width: 38, alignment: .trailing)
                        }
                    }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 15)
                .background(
                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                        .fill(Palette.card)
                        .overlay(
                            RoundedRectangle(cornerRadius: 16, style: .continuous)
                                .stroke(Palette.line, lineWidth: 1)
                        )
                )
            }
        }
    }
}
