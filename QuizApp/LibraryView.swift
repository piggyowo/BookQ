import SwiftUI
import UniformTypeIdentifiers

private enum LibraryFilter: String, CaseIterable, Identifiable {
    case all
    case unstarted
    case practiced

    var id: String { rawValue }

    var title: String {
        switch self {
        case .all: return "全部"
        case .unstarted: return "还没练"
        case .practiced: return "练过的"
        }
    }
}

struct LibraryView: View {
    @EnvironmentObject var store: QuizStore

    @State private var showImporter = false
    @State private var parsedBank: ParsedBank?
    @State private var selectedBank: QuizBank?
    @State private var importError: String?
    @State private var loading = false
    @State private var search = ""
    @State private var filter: LibraryFilter = .all

    var body: some View {
        ScrollView(showsIndicators: false) {
            VStack(alignment: .leading, spacing: 18) {
                topBar

                if store.banks.isEmpty {
                    emptyState
                } else {
                    summaryStrip
                    searchField
                    filterRow
                    bankList
                    importCard
                }
            }
            .padding(.horizontal, 18)
            .padding(.top, 8)
            .padding(.bottom, 26)
        }
        .background(PaperBackground())
        .overlay {
            if loading {
                ZStack {
                    Color.black.opacity(0.18).ignoresSafeArea()
                    VStack(spacing: 12) {
                        ProgressView()
                            .tint(Palette.primary)
                        Text("正在解析题库…")
                            .font(.quizSans(13))
                            .foregroundColor(Palette.ink2)
                    }
                    .padding(24)
                    .background(RoundedRectangle(cornerRadius: 16).fill(Palette.card))
                }
            }
        }
        .fileImporter(isPresented: $showImporter, allowedContentTypes: importTypes) { result in
            switch result {
            case .success(let url):
                importFile(url)
            case .failure(let error):
                importError = error.localizedDescription
            }
        }
        .sheet(item: $parsedBank) { parsed in
            ImportPreviewView(parsed: parsed)
                .presentationDetents([.large])
                .presentationDragIndicator(.hidden)
        }
        .sheet(item: $selectedBank) { bank in
            BankDetailView(bankID: bank.id)
                .presentationDetents([.large])
                .presentationDragIndicator(.hidden)
        }
        .alert("导入失败", isPresented: Binding(
            get: { importError != nil },
            set: { if !$0 { importError = nil } }
        )) {
            Button("知道了") { importError = nil }
        } message: {
            Text(importError ?? "")
        }
    }

    // MARK: - 顶栏

    private var topBar: some View {
        HStack(alignment: .center) {
            VStack(alignment: .leading, spacing: 3) {
                Text("题库")
                    .font(.quizSerif(28))
                    .foregroundColor(Palette.ink)
                Text("导入、查看和管理你的题库")
                    .font(.quizSans(11.5))
                    .foregroundColor(Palette.muted)
            }
            Spacer()
            Button {
                showImporter = true
            } label: {
                HStack(spacing: 6) {
                    Image(systemName: "plus")
                        .font(.system(size: 12, weight: .semibold))
                    Text("导入")
                        .font(.quizSans(13.5, weight: .semibold))
                }
                .foregroundColor(.white)
                .padding(.horizontal, 15)
                .padding(.vertical, 9)
                .background(Capsule().fill(Palette.primary))
            }
            .buttonStyle(.plain)
        }
    }

    // MARK: - 空状态

    private var emptyState: some View {
        VStack(spacing: 16) {
            ZStack {
                Circle()
                    .fill(Palette.primary.opacity(0.07))
                    .frame(width: 76, height: 76)
                Image(systemName: "tray.and.arrow.down")
                    .font(.system(size: 28, weight: .medium))
                    .foregroundColor(Palette.primary)
            }
            Text("还没有题库")
                .font(.quizSerif(23))
                .foregroundColor(Palette.ink)
            Text("从「文件」里选一份题目文档，\n解析后先确认，再保存到题库。")
                .font(.quizSans(13))
                .foregroundColor(Palette.muted)
                .multilineTextAlignment(.center)
                .lineSpacing(4)
            Button {
                showImporter = true
            } label: {
                Text("导入第一份题库")
                    .font(.quizSans(14, weight: .semibold))
                    .foregroundColor(.white)
                    .padding(.horizontal, 24)
                    .padding(.vertical, 12)
                    .background(Capsule().fill(Palette.primary))
            }
            .buttonStyle(.plain)

            HStack(spacing: 7) {
                ForEach(["DOCX", "TXT", "MD", "JSON"], id: \.self) { text in
                    Text(text)
                        .font(.quizSans(9.5, weight: .semibold))
                        .foregroundColor(Palette.ink2)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 3)
                        .background(
                            RoundedRectangle(cornerRadius: 5)
                                .fill(Palette.card)
                                .overlay(RoundedRectangle(cornerRadius: 5).stroke(Palette.line, lineWidth: 1))
                        )
                }
            }
            .padding(.top, 2)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 38)
        .padding(.horizontal, 22)
        .background(
            RoundedRectangle(cornerRadius: 22, style: .continuous)
                .fill(Palette.card.opacity(0.7))
                .overlay(
                    RoundedRectangle(cornerRadius: 22, style: .continuous)
                        .stroke(Palette.line, style: StrokeStyle(lineWidth: 1, dash: [5, 4]))
                )
        )
        .padding(.top, 18)
    }

    // MARK: - 概览

    private var summaryStrip: some View {
        HStack(spacing: 13) {
            ZStack {
                RoundedRectangle(cornerRadius: 11, style: .continuous)
                    .fill(Palette.primary)
                Image(systemName: "books.vertical.fill")
                    .font(.system(size: 16, weight: .medium))
                    .foregroundColor(Palette.cream)
            }
            .frame(width: 42, height: 42)

            VStack(alignment: .leading, spacing: 3) {
                Text("全部题库")
                    .font(.quizSans(14, weight: .semibold))
                    .foregroundColor(Palette.ink)
                Text("\(store.banks.count) 个 · \(store.allQuestions.count) 题")
                    .font(.quizSans(11))
                    .foregroundColor(Palette.muted)
            }

            Spacer()

            VStack(alignment: .trailing, spacing: 3) {
                Text(store.totalAnswered == 0 ? "未开始" : "\(store.totalAnswered) / \(store.allQuestions.count)")
                    .font(.quizSerif(16))
                    .foregroundColor(Palette.primary)
                Text("已作答")
                    .font(.quizSans(10))
                    .foregroundColor(Palette.muted)
            }
        }
        .padding(14)
        .background(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .fill(Palette.card)
                .overlay(RoundedRectangle(cornerRadius: 16, style: .continuous).stroke(Palette.line, lineWidth: 1))
        )
    }

    private var searchField: some View {
        HStack(spacing: 8) {
            Image(systemName: "magnifyingglass")
                .font(.system(size: 13, weight: .medium))
                .foregroundColor(Palette.muted)
            TextField("搜索题库名或文件名", text: $search)
                .font(.quizSans(13))
                .foregroundColor(Palette.ink)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
            if !search.isEmpty {
                Button {
                    search = ""
                } label: {
                    Image(systemName: "xmark.circle.fill")
                        .font(.system(size: 13))
                        .foregroundColor(Palette.line)
                }
                .buttonStyle(.plain)
            }
        }
        .padding(.horizontal, 13)
        .padding(.vertical, 11)
        .background(
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .fill(Palette.card)
                .overlay(RoundedRectangle(cornerRadius: 12, style: .continuous).stroke(Palette.line, lineWidth: 1))
        )
    }

    private var filterRow: some View {
        HStack(spacing: 8) {
            ForEach(LibraryFilter.allCases) { value in
                ChipButton(title: value.title, selected: filter == value) {
                    filter = value
                }
            }
            Spacer()
            Text("\(filteredBanks.count) 个")
                .font(.quizSans(10.5))
                .foregroundColor(Palette.muted)
        }
    }

    // MARK: - 题库列表

    private var filteredBanks: [QuizBank] {
        let sorted = store.banks.sorted { $0.importedAt > $1.importedAt }
        let searched: [QuizBank]
        if search.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            searched = sorted
        } else {
            let keyword = search.lowercased()
            searched = sorted.filter {
                $0.name.lowercased().contains(keyword) || $0.fileName.lowercased().contains(keyword)
            }
        }
        switch filter {
        case .all:
            return searched
        case .unstarted:
            return searched.filter { store.bankProgress($0).done == 0 }
        case .practiced:
            return searched.filter { store.bankProgress($0).done > 0 }
        }
    }

    private var bankList: some View {
        VStack(spacing: 12) {
            ForEach(filteredBanks) { bank in
                bankCard(bank)
            }
            if filteredBanks.isEmpty {
                Text(filter == .all && !search.isEmpty ? "没有匹配的题库" : "这个筛选下还没有题库")
                    .font(.quizSans(12.5))
                    .foregroundColor(Palette.muted)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 22)
            }
        }
    }

    private func bankCard(_ bank: QuizBank) -> some View {
        let progress = store.bankProgress(bank)
        return Button {
            selectedBank = bank
        } label: {
            VStack(alignment: .leading, spacing: 12) {
                HStack(alignment: .top, spacing: 12) {
                    RoundedRectangle(cornerRadius: 3, style: .continuous)
                        .fill(bank.colorIndex == 0 ? Palette.primary : Palette.accent)
                        .frame(width: 4, height: 42)

                    ZStack {
                        RoundedRectangle(cornerRadius: 10, style: .continuous)
                            .fill(Palette.primary.opacity(0.07))
                        Image(systemName: "doc.text.fill")
                            .font(.system(size: 15, weight: .medium))
                            .foregroundColor(Palette.primary)
                    }
                    .frame(width: 38, height: 42)

                    VStack(alignment: .leading, spacing: 4) {
                        Text(bank.name)
                            .font(.quizSans(15, weight: .semibold))
                            .foregroundColor(Palette.ink)
                            .lineLimit(1)
                        Text(bank.fileName)
                            .font(.quizSans(10.5))
                            .foregroundColor(Palette.muted)
                            .lineLimit(1)
                    }

                    Spacer(minLength: 6)

                    Text("\(bank.count) 题")
                        .font(.quizSans(10.5, weight: .medium))
                        .foregroundColor(Palette.ink2)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 3)
                        .background(Capsule().fill(Palette.track))
                }

                HStack(spacing: 9) {
                    Text("已作答 \(progress.done) / \(progress.total)")
                    Text("·")
                    Text(progress.rate == nil ? "正确率待生成" : "正确率 \(progress.rate ?? 0)%")
                    if store.wrongCount(in: bank) > 0 {
                        Text("·")
                        Text("\(store.wrongCount(in: bank)) 错题")
                            .foregroundColor(Palette.accent)
                    }
                    Spacer()
                }
                .font(.quizSans(11))
                .foregroundColor(Palette.muted)

                ProgressTrack(value: progress.percent, height: 5)

                HStack {
                    Text("导入于 \(dateText(bank.importedAt))")
                        .font(.quizSans(10))
                        .foregroundColor(Palette.muted)
                    Spacer()
                    Text("详情")
                        .font(.quizSans(11.5, weight: .medium))
                        .foregroundColor(Palette.primary)
                    Image(systemName: "chevron.right")
                        .font(.system(size: 9, weight: .semibold))
                        .foregroundColor(Palette.primary)
                }
            }
            .padding(14)
            .background(
                RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .fill(Palette.card)
                    .overlay(RoundedRectangle(cornerRadius: 18, style: .continuous).stroke(Palette.line, lineWidth: 1))
            )
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }

    // MARK: - 导入卡

    private var importCard: some View {
        Button {
            showImporter = true
        } label: {
            HStack(spacing: 10) {
                Image(systemName: "plus")
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundColor(Palette.primary)
                    .frame(width: 30, height: 30)
                    .background(Circle().fill(Palette.primary.opacity(0.08)))
                VStack(alignment: .leading, spacing: 2) {
                    Text("导入新题库")
                        .font(.quizSans(13.5, weight: .semibold))
                        .foregroundColor(Palette.ink)
                    Text("DOCX / TXT / MD / JSON")
                        .font(.quizSans(10))
                        .foregroundColor(Palette.muted)
                }
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.system(size: 10, weight: .semibold))
                    .foregroundColor(Palette.line)
            }
            .padding(14)
            .background(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .fill(Palette.card.opacity(0.6))
                    .overlay(
                        RoundedRectangle(cornerRadius: 16, style: .continuous)
                            .stroke(Palette.line, style: StrokeStyle(lineWidth: 1, dash: [5, 4]))
                    )
            )
        }
        .buttonStyle(.plain)
        .padding(.top, 2)
    }

    // MARK: - 文件选择

    private var importTypes: [UTType] {
        var types: [UTType] = [.plainText, .json]
        if let docx = UTType(filenameExtension: "docx") { types.append(docx) }
        if let markdown = UTType(filenameExtension: "md") { types.append(markdown) }
        return types
    }

    private func importFile(_ url: URL) {
        loading = true
        Task.detached(priority: .userInitiated) {
            let access = url.startAccessingSecurityScopedResource()
            let result: Result<ParsedBank, Error>
            do {
                result = .success(try ImportService.parse(url: url))
            } catch {
                result = .failure(error)
            }
            if access {
                url.stopAccessingSecurityScopedResource()
            }
            await MainActor.run {
                self.loading = false
                switch result {
                case .success(let parsed):
                    self.parsedBank = parsed
                case .failure(let error):
                    self.importError = error.localizedDescription
                }
            }
        }
    }

    private func dateText(_ date: Date) -> String {
        Self.formatter.string(from: date)
    }

    private static let formatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy.MM.dd"
        return formatter
    }()
}

// MARK: - 导入确认

struct ImportPreviewView: View {
    @EnvironmentObject var store: QuizStore
    @Environment(\.dismiss) private var dismiss

    let parsed: ParsedBank

    @State private var name: String
    @State private var saveError: String?

    init(parsed: ParsedBank) {
        self.parsed = parsed
        _name = State(initialValue: parsed.name)
    }

    var body: some View {
        ZStack {
            Palette.paper.ignoresSafeArea()
            ScrollView(showsIndicators: false) {
                VStack(alignment: .leading, spacing: 18) {
                    SheetGrabber().frame(maxWidth: .infinity)

                    HStack(alignment: .top) {
                        VStack(alignment: .leading, spacing: 4) {
                            Text("确认题库")
                                .font(.quizSerif(26))
                                .foregroundColor(Palette.ink)
                            Text("检查解析结果，再保存到题库")
                                .font(.quizSans(12))
                                .foregroundColor(Palette.muted)
                        }
                        Spacer()
                        Button { dismiss() } label: {
                            Image(systemName: "xmark")
                                .font(.system(size: 12, weight: .semibold))
                                .foregroundColor(Palette.ink2)
                                .frame(width: 30, height: 30)
                                .background(Circle().fill(Palette.card).overlay(Circle().stroke(Palette.line, lineWidth: 1)))
                        }
                        .buttonStyle(.plain)
                    }

                    VStack(alignment: .leading, spacing: 7) {
                        Text("题库名称")
                            .font(.quizSans(11, weight: .semibold))
                            .foregroundColor(Palette.ink2)
                        TextField("题库名称", text: $name)
                            .font(.quizSans(15, weight: .medium))
                            .foregroundColor(Palette.ink)
                            .padding(.horizontal, 13)
                            .padding(.vertical, 11)
                            .background(
                                RoundedRectangle(cornerRadius: 12, style: .continuous)
                                    .fill(Palette.card)
                                    .overlay(RoundedRectangle(cornerRadius: 12, style: .continuous).stroke(Palette.line, lineWidth: 1))
                            )
                        Text(parsed.fileName)
                            .font(.quizSans(10))
                            .foregroundColor(Palette.muted)
                    }

                    HStack(spacing: 10) {
                        statTile(value: "\(parsed.questionCount)", label: "题")
                        statTile(value: "\(parsed.noAnswerCount)", label: "无答案", accent: parsed.noAnswerCount > 0)
                        statTile(value: "\(parsed.warnings.count)", label: "待核对", accent: !parsed.warnings.isEmpty)
                    }

                    if !parsed.warnings.isEmpty {
                        VStack(alignment: .leading, spacing: 7) {
                            Text("解析提示")
                                .font(.quizSans(12, weight: .semibold))
                                .foregroundColor(Palette.accent)
                            ForEach(Array(parsed.warnings.prefix(5)), id: \.self) { warning in
                                Text("· \(warning)")
                                    .font(.quizSans(11))
                                    .foregroundColor(Palette.ink2)
                                    .fixedSize(horizontal: false, vertical: true)
                            }
                        }
                        .padding(13)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(
                            RoundedRectangle(cornerRadius: 12, style: .continuous)
                                .fill(Palette.amberBG)
                                .overlay(RoundedRectangle(cornerRadius: 12, style: .continuous).stroke(Palette.amberLine, lineWidth: 1))
                        )
                    }

                    VStack(alignment: .leading, spacing: 10) {
                        Text("题目样例")
                            .font(.quizSans(12, weight: .semibold))
                            .foregroundColor(Palette.ink2)
                        ForEach(Array(parsed.questions.prefix(3).indices), id: \.self) { index in
                            sampleCard(parsed.questions[index])
                        }
                    }

                    Button {
                        guard let bank = store.addBank(name: name, fileName: parsed.fileName, parsed: parsed.questions) else {
                            saveError = "保存失败，请重试"
                            return
                        }
                        dismiss()
                        store.showToast("已导入《\(bank.name)》· \(bank.count) 题")
                    } label: {
                        HStack {
                            Spacer()
                            Text("保存到题库 · \(parsed.questionCount) 题")
                                .font(.quizSans(15, weight: .semibold))
                            Spacer()
                        }
                        .foregroundColor(.white)
                        .padding(.vertical, 14)
                        .background(
                            RoundedRectangle(cornerRadius: 14, style: .continuous)
                                .fill(Palette.primary)
                        )
                    }
                    .buttonStyle(.plain)
                    .disabled(parsed.questionCount == 0)

                    Text("保存后可以到主页开始练习；导入按钮只在题库页。")
                        .font(.quizSans(10.5))
                        .foregroundColor(Palette.muted)
                        .padding(.horizontal, 4)
                }
                .padding(.horizontal, 20)
                .padding(.bottom, 28)
            }
        }
        .alert("保存失败", isPresented: Binding(
            get: { saveError != nil },
            set: { if !$0 { saveError = nil } }
        )) {
            Button("知道了") { saveError = nil }
        } message: {
            Text(saveError ?? "")
        }
    }

    private func statTile(value: String, label: String, accent: Bool = false) -> some View {
        VStack(spacing: 3) {
            Text(value)
                .font(.quizSerif(22))
                .foregroundColor(accent ? Palette.accent : Palette.primary)
            Text(label)
                .font(.quizSans(10.5))
                .foregroundColor(Palette.muted)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 12)
        .background(
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .fill(Palette.card)
                .overlay(RoundedRectangle(cornerRadius: 12, style: .continuous).stroke(Palette.line, lineWidth: 1))
        )
    }

    private func sampleCard(_ question: ParsedQuestion) -> some View {
        VStack(alignment: .leading, spacing: 7) {
            HStack(spacing: 7) {
                Text("第 \(question.num) 题")
                    .font(.quizSans(10, weight: .medium))
                    .foregroundColor(Palette.gold)
                if question.answer == nil {
                    Text("无答案")
                        .font(.quizSans(9.5))
                        .foregroundColor(Palette.accent)
                        .padding(.horizontal, 5)
                        .padding(.vertical, 1)
                        .background(Capsule().fill(Palette.amberBG))
                }
            }
            Text(question.stem)
                .font(.quizSans(12.5, weight: .medium))
                .foregroundColor(Palette.ink)
                .fixedSize(horizontal: false, vertical: true)
            ForEach(question.options, id: \.self) { option in
                HStack(alignment: .top, spacing: 5) {
                    Text(option.first ?? "?")
                        .font(.quizSans(10, weight: .semibold))
                        .foregroundColor(Palette.ink2)
                    Text(option.count > 1 ? option[1] : "")
                        .font(.quizSans(10.5))
                        .foregroundColor(Palette.ink2)
                        .fixedSize(horizontal: false, vertical: true)
                }
            }
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .fill(Palette.card)
                .overlay(RoundedRectangle(cornerRadius: 12, style: .continuous).stroke(Palette.line, lineWidth: 1))
        )
    }
}

// MARK: - 题库详情

struct BankDetailView: View {
    @EnvironmentObject var store: QuizStore
    @Environment(\.dismiss) private var dismiss

    let bankID: UUID

    @State private var showRename = false
    @State private var confirmDelete = false
    @State private var draftName = ""

    var body: some View {
        ZStack {
            Palette.paper.ignoresSafeArea()
            if let bank = store.bank(id: bankID) {
                ScrollView(showsIndicators: false) {
                    VStack(alignment: .leading, spacing: 18) {
                        SheetGrabber().frame(maxWidth: .infinity)

                        HStack(alignment: .top) {
                            VStack(alignment: .leading, spacing: 4) {
                                Text("题库详情")
                                    .font(.quizSerif(24))
                                    .foregroundColor(Palette.ink)
                                Text(bank.fileName)
                                    .font(.quizSans(11))
                                    .foregroundColor(Palette.muted)
                            }
                            Spacer()
                            Button { dismiss() } label: {
                                Image(systemName: "xmark")
                                    .font(.system(size: 12, weight: .semibold))
                                    .foregroundColor(Palette.ink2)
                                    .frame(width: 30, height: 30)
                                    .background(Circle().fill(Palette.card).overlay(Circle().stroke(Palette.line, lineWidth: 1)))
                            }
                            .buttonStyle(.plain)
                        }

                        VStack(alignment: .leading, spacing: 10) {
                            HStack(spacing: 12) {
                                ZStack {
                                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                                        .fill(bank.colorIndex == 0 ? Palette.primary : Palette.accent)
                                    Image(systemName: "doc.text.fill")
                                        .font(.system(size: 20, weight: .medium))
                                        .foregroundColor(Palette.cream)
                                }
                                .frame(width: 52, height: 58)
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(bank.name)
                                        .font(.quizSerif(21))
                                        .foregroundColor(Palette.ink)
                                    Text("\(bank.count) 题 · 导入于 \(dateText(bank.importedAt))")
                                        .font(.quizSans(11))
                                        .foregroundColor(Palette.muted)
                                }
                                Spacer()
                            }

                            let progress = store.bankProgress(bank)
                            ProgressTrack(value: progress.percent, height: 6)
                            HStack {
                                Text("已作答 \(progress.done) / \(progress.total)")
                                Spacer()
                                Text(progress.rate == nil ? "正确率待生成" : "正确率 \(progress.rate ?? 0)%")
                            }
                            .font(.quizSans(10.5))
                            .foregroundColor(Palette.muted)
                        }
                        .padding(16)
                        .background(
                            RoundedRectangle(cornerRadius: 18, style: .continuous)
                                .fill(Palette.card)
                                .overlay(RoundedRectangle(cornerRadius: 18, style: .continuous).stroke(Palette.line, lineWidth: 1))
                        )

                        HStack(spacing: 10) {
                            metric("\(bank.count)", "题目")
                            metric("\(store.wrongCount(in: bank))", "错题", accent: store.wrongCount(in: bank) > 0)
                            metric("\(store.bankProgress(bank).done)", "已作答")
                        }

                        HStack(spacing: 10) {
                            Button {
                                draftName = bank.name
                                showRename = true
                            } label: {
                                HStack(spacing: 6) {
                                    Image(systemName: "pencil")
                                    Text("重命名")
                                }
                                .font(.quizSans(12.5, weight: .medium))
                                .foregroundColor(Palette.primary)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 11)
                                .background(
                                    RoundedRectangle(cornerRadius: 11, style: .continuous)
                                        .fill(Palette.primary.opacity(0.07))
                                )
                            }
                            .buttonStyle(.plain)

                            Button {
                                confirmDelete = true
                            } label: {
                                HStack(spacing: 6) {
                                    Image(systemName: "trash")
                                    Text("删除")
                                }
                                .font(.quizSans(12.5, weight: .medium))
                                .foregroundColor(Palette.red)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 11)
                                .background(
                                    RoundedRectangle(cornerRadius: 11, style: .continuous)
                                        .fill(Palette.redBG)
                                )
                            }
                            .buttonStyle(.plain)
                        }

                        VStack(alignment: .leading, spacing: 10) {
                            Text("题目预览")
                                .font(.quizSans(12, weight: .semibold))
                                .foregroundColor(Palette.ink2)
                            ForEach(Array(bank.questions.prefix(5))) { question in
                                HStack(alignment: .top, spacing: 8) {
                                    Text(question.num)
                                        .font(.quizSans(9.5, weight: .medium))
                                        .foregroundColor(Palette.gold)
                                        .frame(width: 22, alignment: .trailing)
                                    Text(question.stem)
                                        .font(.quizSans(11.5))
                                        .foregroundColor(Palette.ink2)
                                        .lineLimit(2)
                                        .fixedSize(horizontal: false, vertical: true)
                                    Spacer(minLength: 0)
                                    if !question.hasAnswer {
                                        Text("无答案")
                                            .font(.quizSans(9))
                                            .foregroundColor(Palette.accent)
                                    }
                                }
                            }
                        }
                        .padding(14)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(
                            RoundedRectangle(cornerRadius: 16, style: .continuous)
                                .fill(Palette.card)
                                .overlay(RoundedRectangle(cornerRadius: 16, style: .continuous).stroke(Palette.line, lineWidth: 1))
                        )

                        Text("这里只管理题库，不提供开始入口；练习请回主页开始。")
                            .font(.quizSans(10.5))
                            .foregroundColor(Palette.muted)
                            .padding(.horizontal, 4)
                    }
                    .padding(.horizontal, 20)
                    .padding(.bottom, 28)
                }
            } else {
                Text("题库已不在了")
                    .font(.quizSans(14))
                    .foregroundColor(Palette.muted)
            }
        }
        .alert("重命名题库", isPresented: $showRename) {
            TextField("题库名称", text: $draftName)
            Button("取消", role: .cancel) {}
            Button("保存") {
                store.renameBank(id: bankID, to: draftName)
            }
        } message: {
            Text("只改显示名称，不会修改原文件。")
        }
        .alert("删除这个题库？", isPresented: $confirmDelete) {
            Button("取消", role: .cancel) {}
            Button("删除", role: .destructive) {
                store.deleteBank(id: bankID)
                dismiss()
                store.showToast("题库已删除")
            }
        } message: {
            Text("题库和它的答题进度会一起移除，原文件不受影响。")
        }
    }

    private func metric(_ value: String, _ label: String, accent: Bool = false) -> some View {
        VStack(spacing: 3) {
            Text(value)
                .font(.quizSerif(20))
                .foregroundColor(accent ? Palette.accent : Palette.primary)
            Text(label)
                .font(.quizSans(10))
                .foregroundColor(Palette.muted)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 11)
        .background(
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .fill(Palette.card)
                .overlay(RoundedRectangle(cornerRadius: 12, style: .continuous).stroke(Palette.line, lineWidth: 1))
        )
    }

    private func dateText(_ date: Date) -> String {
        Self.formatter.string(from: date)
    }

    private static let formatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy.MM.dd"
        return formatter
    }()
}
