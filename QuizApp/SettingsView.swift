import SwiftUI
import UniformTypeIdentifiers

struct SettingsView: View {
    @EnvironmentObject var store: QuizStore

    @State private var showRestore = false
    @State private var showExporter = false
    @State private var exportDocument: BackupDocument?
    @State private var confirmClear = false
    @State private var showHelp = false
    @State private var message: String?

    var body: some View {
        ScrollView(showsIndicators: false) {
            VStack(alignment: .leading, spacing: 20) {
                header

                group("练习偏好") {
                    Menu {
                        ForEach(QuizMode.allCases, id: \.self) { value in
                            Button(value.longTitle) { store.defaultMode = value }
                        }
                    } label: {
                        menuRow(icon: "pencil.line", title: "默认模式", value: store.defaultMode.longTitle)
                    }

                    divider

                    Menu {
                        Button("10 题") { store.defaultSize = 10 }
                        Button("20 题") { store.defaultSize = 20 }
                        Button("50 题") { store.defaultSize = 50 }
                        Button("全部") { store.defaultSize = 0 }
                    } label: {
                        menuRow(icon: "number", title: "默认题量", value: store.defaultSize == 0 ? "全部" : "\(store.defaultSize) 题")
                    }

                    divider

                    Menu {
                        ForEach(QuizOrder.allCases, id: \.self) { value in
                            Button(value.title) { store.defaultOrder = value }
                        }
                    } label: {
                        menuRow(icon: "arrow.up.arrow.down", title: "默认顺序", value: store.defaultOrder.title)
                    }
                }

                group("题库数据") {
                    actionRow(icon: "square.and.arrow.up", title: "导出备份", detail: "题库、进度、错题本一起打包", color: Palette.primary) {
                        exportBackup()
                    }
                    divider
                    actionRow(icon: "square.and.arrow.down", title: "从备份恢复", detail: "会覆盖当前题库和答题记录", color: Palette.primary) {
                        showRestore = true
                    }
                    divider
                    actionRow(icon: "trash", title: "清空答题记录", detail: "保留题库，只清进度和错题", color: Palette.red, destructive: true) {
                        confirmClear = true
                    }

                    Rectangle().fill(Palette.line2).frame(height: 1).padding(.top, 2)

                    HStack {
                        Text("\(store.banks.count) 个题库")
                        Spacer()
                        Text("\(store.allQuestions.count) 题")
                        Text("·")
                        Text("\(store.attempts.count) 次练习")
                    }
                    .font(.quizSans(10.5))
                    .foregroundColor(Palette.muted)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 10)
                }

                group("体验") {
                    HStack(spacing: 11) {
                        Image(systemName: "hand.tap")
                            .font(.system(size: 13, weight: .medium))
                            .foregroundColor(Palette.primary)
                            .frame(width: 26, height: 26)
                            .background(RoundedRectangle(cornerRadius: 8).fill(Palette.primary.opacity(0.07)))
                        VStack(alignment: .leading, spacing: 2) {
                            Text("触感反馈")
                                .font(.quizSans(13.5, weight: .medium))
                                .foregroundColor(Palette.ink)
                            Text("作答时轻轻震一下")
                                .font(.quizSans(10))
                                .foregroundColor(Palette.muted)
                        }
                        Spacer()
                        Toggle("", isOn: $store.haptics)
                            .labelsHidden()
                            .tint(Palette.primary)
                    }
                    .padding(.horizontal, 14)
                    .padding(.vertical, 11)

                    divider

                    actionRow(icon: "questionmark.circle", title: "怎么导入题库", detail: "支持 DOCX / TXT / MD / JSON", color: Palette.primary) {
                        showHelp = true
                    }
                }

                group("关于") {
                    infoRow(icon: "app.badge", title: "版本", value: "1.0")
                    divider
                    infoRow(icon: "shippingbox", title: "题库", value: "全部由你导入")
                    divider
                    infoRow(icon: "paintpalette", title: "视觉", value: "织物靛蓝")
                }

                Text("题库不内置在 App 里。\n导入、备份、恢复都在本机完成，不上传任何文档。")
                    .font(.quizSans(10.5))
                    .foregroundColor(Palette.muted)
                    .lineSpacing(4)
                    .padding(.horizontal, 4)
                    .padding(.bottom, 10)
            }
            .padding(.horizontal, 18)
            .padding(.top, 8)
            .padding(.bottom, 26)
        }
        .background(PaperBackground())
        .fileImporter(isPresented: $showRestore, allowedContentTypes: [.json]) { result in
            switch result {
            case .success(let url):
                restore(url)
            case .failure(let error):
                message = error.localizedDescription
            }
        }
        .fileExporter(isPresented: $showExporter, document: exportDocument, contentType: .json, defaultFilename: "工艺题库备份") { result in
            switch result {
            case .success:
                store.showToast("备份已导出")
            case .failure(let error):
                message = error.localizedDescription
            }
        }
        .alert("清空答题记录？", isPresented: $confirmClear) {
            Button("取消", role: .cancel) {}
            Button("清空", role: .destructive) {
                store.clearProgress()
                store.showToast("答题记录已清空")
            }
        } message: {
            Text("题库会保留，进度、错题本和练习历史会被清空。")
        }
        .alert("导入题库", isPresented: $showHelp) {
            Button("知道了") {}
        } message: {
            Text("到「题库」页点右上角导入，选择 DOCX、TXT、MD 或 JSON。解析后会先让你确认题库名和题目样例，再保存。")
        }
        .alert("提示", isPresented: Binding(
            get: { message != nil },
            set: { if !$0 { message = nil } }
        )) {
            Button("知道了") { message = nil }
        } message: {
            Text(message ?? "")
        }
    }

    private var header: some View {
        HStack(alignment: .center) {
            VStack(alignment: .leading, spacing: 3) {
                Text("设置")
                    .font(.quizSerif(28))
                    .foregroundColor(Palette.ink)
                Text("练习习惯与本机数据")
                    .font(.quizSans(11.5))
                    .foregroundColor(Palette.muted)
            }
            Spacer()
            BrandMark(size: 38)
        }
    }

    private func group<Content: View>(_ title: String, @ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.quizSans(11, weight: .semibold))
                .foregroundColor(Palette.ink2)
                .tracking(0.5)
                .padding(.leading, 4)
            VStack(spacing: 0) {
                content()
            }
            .background(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .fill(Palette.card)
                    .overlay(RoundedRectangle(cornerRadius: 16, style: .continuous).stroke(Palette.line, lineWidth: 1))
            )
            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        }
    }

    private var divider: some View {
        Rectangle()
            .fill(Palette.line2)
            .frame(height: 1)
            .padding(.leading, 50)
    }

    private func menuRow(icon: String, title: String, value: String) -> some View {
        HStack(spacing: 11) {
            Image(systemName: icon)
                .font(.system(size: 13, weight: .medium))
                .foregroundColor(Palette.primary)
                .frame(width: 26, height: 26)
                .background(RoundedRectangle(cornerRadius: 8).fill(Palette.primary.opacity(0.07)))
            Text(title)
                .font(.quizSans(13.5, weight: .medium))
                .foregroundColor(Palette.ink)
            Spacer()
            Text(value)
                .font(.quizSans(11.5))
                .foregroundColor(Palette.muted)
            Image(systemName: "chevron.up.chevron.down")
                .font(.system(size: 9, weight: .semibold))
                .foregroundColor(Palette.muted)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .contentShape(Rectangle())
    }

    private func actionRow(icon: String, title: String, detail: String, color: Color, destructive: Bool = false, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: 11) {
                Image(systemName: icon)
                    .font(.system(size: 13, weight: .medium))
                    .foregroundColor(color)
                    .frame(width: 26, height: 26)
                    .background(RoundedRectangle(cornerRadius: 8).fill(color.opacity(0.07)))
                VStack(alignment: .leading, spacing: 2) {
                    Text(title)
                        .font(.quizSans(13.5, weight: .medium))
                        .foregroundColor(destructive ? Palette.red : Palette.ink)
                    Text(detail)
                        .font(.quizSans(10))
                        .foregroundColor(Palette.muted)
                }
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.system(size: 10, weight: .semibold))
                    .foregroundColor(Palette.line)
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 11)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }

    private func infoRow(icon: String, title: String, value: String) -> some View {
        HStack(spacing: 11) {
            Image(systemName: icon)
                .font(.system(size: 13, weight: .medium))
                .foregroundColor(Palette.gold)
                .frame(width: 26, height: 26)
                .background(RoundedRectangle(cornerRadius: 8).fill(Palette.gold.opacity(0.09)))
            Text(title)
                .font(.quizSans(13.5, weight: .medium))
                .foregroundColor(Palette.ink)
            Spacer()
            Text(value)
                .font(.quizSans(11.5))
                .foregroundColor(Palette.muted)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
    }

    private func exportBackup() {
        guard let data = store.backupData() else {
            message = "生成备份失败"
            return
        }
        exportDocument = BackupDocument(data: data)
        showExporter = true
    }

    private func restore(_ url: URL) {
        let access = url.startAccessingSecurityScopedResource()
        defer { if access { url.stopAccessingSecurityScopedResource() } }
        do {
            let data = try Data(contentsOf: url)
            try store.restoreBackup(data)
            store.showToast("备份已恢复")
        } catch {
            message = "恢复失败：\(error.localizedDescription)"
        }
    }
}

struct BackupDocument: FileDocument {
    static var readableContentTypes: [UTType] { [.json] }

    var data: Data

    init(data: Data) {
        self.data = data
    }

    init(configuration: ReadConfiguration) throws {
        data = configuration.file.regularFileContents ?? Data()
    }

    func fileWrapper(configuration: WriteConfiguration) throws -> FileWrapper {
        FileWrapper(regularFileWithContents: data)
    }
}
