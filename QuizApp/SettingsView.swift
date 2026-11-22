import SwiftUI
import UniformTypeIdentifiers

enum SettingsPage: Hashable {
    case practice
    case appearance
    case data
    case experience
    case about
    case importHelp
}

struct SettingsView: View {
    @EnvironmentObject var store: QuizStore

    private var page: SettingsPage? {
        get { store.settingsPage }
        nonmutating set { store.settingsPage = newValue }
    }

    @State private var showRestore = false
    @State private var showExporter = false
    @State private var exportDocument: BackupDocument?
    @State private var confirmClear = false
    @State private var message: String?
    @State private var previewTheme: AppTheme?

    var body: some View {
        home
            .paperPush(
                isPresented: Binding(
                    get: { page != nil },
                    set: { if !$0 { page = nil } }
                )
            ) {
                if let page {
                    detailView(page)
                }
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
        .fileExporter(isPresented: $showExporter, document: exportDocument, contentType: .json, defaultFilename: "BookQ备份") { result in
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
        .alert("提示", isPresented: Binding(
            get: { message != nil },
            set: { if !$0 { message = nil } }
        )) {
            Button("知道了") { message = nil }
        } message: {
            Text(message ?? "")
        }
        .sheet(item: $previewTheme) { theme in
            ThemePreviewSheet(
                theme: theme,
                appearance: store.appearance,
                currentTheme: store.theme,
                currentAppearance: store.appearance,
                onApply: {
                    store.theme = theme
                    previewTheme = nil
                }
            )
        }
    }

    // MARK: - 主设置页

    private var home: some View {
        ScrollView(showsIndicators: false) {
            VStack(alignment: .leading, spacing: 20) {
                header

                group("练习") {
                    entryRow(
                        icon: "pencil.line",
                        title: "练习偏好",
                        detail: "默认模式、题量、顺序",
                        value: "\(store.defaultMode.title) · \(sizeText) · \(store.defaultOrder.title)",
                        page: .practice
                    )
                }

                group("个性化") {
                    entryRow(
                        icon: "slider.horizontal.3",
                        title: "外观",
                        detail: "主题与浅色 / 深色模式",
                        value: "\(store.theme.title) · \(store.appearance.title)",
                        page: .appearance
                    )
                }

                group("数据") {
                    entryRow(
                        icon: "shippingbox",
                        title: "题库数据",
                        detail: "导出、恢复、清空答题记录",
                        value: "\(store.banks.count) 个题库 · \(store.allQuestions.count) 题",
                        page: .data
                    )
                }

                group("更多") {
                    entryRow(
                        icon: "hand.tap",
                        title: "体验与帮助",
                        detail: "触感反馈、题库导入说明",
                        value: store.haptics ? "触感开启" : "触感关闭",
                        page: .experience
                    )
                    divider
                    entryRow(
                        icon: "paintpalette",
                        title: "关于 BookQ",
                        detail: "版本、主题与数据说明",
                        value: appVersion,
                        page: .about
                    )
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
    }

    private var header: some View {
        HStack(alignment: .center) {
            VStack(alignment: .leading, spacing: 3) {
                Text("设置")
                    .font(.quizSerif(28))
                    .foregroundColor(Palette.ink)
                Text("分组设置，点进去再改")
                    .font(.quizSans(11.5))
                    .foregroundColor(Palette.muted)
            }
            Spacer()
            BrandMark(size: 38)
        }
    }

    private var sizeText: String {
        store.defaultSize == 0 ? "全部" : "\(store.defaultSize) 题"
    }

    private var appVersion: String {
        Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "0.1.7"
    }

    // MARK: - 详情页

    @ViewBuilder
    private func detailView(_ page: SettingsPage) -> some View {
        switch page {
        case .practice:
            detailScaffold(title: "练习偏好", subtitle: "默认只是起点，每次开始仍可临时调整") {
                practiceContent
            }
        case .appearance:
            detailScaffold(title: "外观", subtitle: "主题决定布料颜色，明暗决定光线") {
                appearanceContent
            }
        case .data:
            detailScaffold(title: "题库数据", subtitle: "备份、恢复和清理都只在本机完成") {
                dataContent
            }
        case .experience:
            detailScaffold(title: "体验与帮助", subtitle: "作答反馈和题库导入说明") {
                experienceContent
            }
        case .about:
            detailScaffold(title: "关于 BookQ", subtitle: "版本、主题与数据说明") {
                aboutContent
            }
        case .importHelp:
            detailScaffold(title: "怎么导入题库", subtitle: "导入按钮只在「题库」页出现") {
                importHelpContent
            }
        }
    }

    private func detailScaffold<Content: View>(
        title: String,
        subtitle: String,
        @ViewBuilder content: () -> Content
    ) -> some View {
        ScrollView(showsIndicators: false) {
            VStack(alignment: .leading, spacing: 20) {
                detailHeader(title: title, subtitle: subtitle)
                content()
            }
            .padding(.horizontal, 18)
            .padding(.top, 8)
            .padding(.bottom, 26)
        }
    }

    private func detailHeader(title: String, subtitle: String) -> some View {
        HStack(spacing: 12) {
            Button {
                page = nil
            } label: {
                Image(systemName: "chevron.left")
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundColor(Palette.primary)
                    .frame(width: 36, height: 36)
                    .background(
                        Circle()
                            .fill(Palette.card)
                            .overlay(Circle().stroke(Palette.line, lineWidth: 1))
                    )
            }
            .buttonStyle(.plain)

            VStack(alignment: .leading, spacing: 3) {
                Text(title)
                    .font(.quizSerif(25))
                    .foregroundColor(Palette.ink)
                Text(subtitle)
                    .font(.quizSans(10.5))
                    .foregroundColor(Palette.muted)
            }
            Spacer(minLength: 0)
        }
    }

    private var practiceContent: some View {
        VStack(alignment: .leading, spacing: 20) {
            group("默认模式") {
                choiceRow(
                    title: QuizMode.practice.longTitle,
                    detail: QuizMode.practice.subtitle,
                    selected: store.defaultMode == .practice
                ) {
                    store.defaultMode = .practice
                }
                divider
                choiceRow(
                    title: QuizMode.exam.longTitle,
                    detail: QuizMode.exam.subtitle,
                    selected: store.defaultMode == .exam
                ) {
                    store.defaultMode = .exam
                }
            }

            group("默认题量") {
                VStack(alignment: .leading, spacing: 10) {
                    HStack(spacing: 8) {
                        ForEach([10, 20, 50, 0], id: \.self) { size in
                            sizeOption(size)
                        }
                    }
                    Text("开始练习时还可以再改，这里只决定默认值。")
                        .font(.quizSans(10))
                        .foregroundColor(Palette.muted)
                }
                .padding(14)
            }

            group("默认顺序") {
                choiceRow(
                    title: QuizOrder.sequential.title,
                    detail: QuizOrder.sequential.subtitle,
                    selected: store.defaultOrder == .sequential
                ) {
                    store.defaultOrder = .sequential
                }
                divider
                choiceRow(
                    title: QuizOrder.random.title,
                    detail: QuizOrder.random.subtitle,
                    selected: store.defaultOrder == .random
                ) {
                    store.defaultOrder = .random
                }
            }
        }
    }

    private var appearanceContent: some View {
        VStack(alignment: .leading, spacing: 20) {
            group("明暗模式") {
                ForEach(Array(AppAppearance.allCases.enumerated()), id: \.element) { index, appearance in
                    choiceRow(
                        title: appearance.title,
                        detail: appearance.subtitle,
                        selected: store.appearance == appearance
                    ) {
                        store.appearance = appearance
                    }
                    if index != AppAppearance.allCases.count - 1 { divider }
                }
            }

            group("主题") {
                ForEach(Array(AppTheme.allCases.enumerated()), id: \.element) { index, theme in
                    themeRow(theme)
                    if index != AppTheme.allCases.count - 1 { divider }
                }
            }

            Text("明暗会立即应用；主题先预览，按「使用这个主题」才会切换。题库、进度和错题本不受影响。")
                .font(.quizSans(10.5))
                .foregroundColor(Palette.muted)
                .lineSpacing(4)
                .padding(.horizontal, 4)
        }
    }

    private func themeRow(_ theme: AppTheme) -> some View {
        let colors = BookQPalettes.of(theme, store.appearance)
        let selected = store.theme == theme
        return Button {
            previewTheme = theme
        } label: {
            HStack(spacing: 12) {
                HStack(spacing: 0) {
                    Rectangle().fill(colors.paper)
                    Rectangle().fill(colors.primary)
                    Rectangle().fill(colors.accent)
                }
                .frame(width: 30, height: 30)
                .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: 8, style: .continuous)
                        .stroke(Palette.line, lineWidth: 1)
                )

                VStack(alignment: .leading, spacing: 2) {
                    Text(theme.title)
                        .font(.quizSans(13, weight: .medium))
                        .foregroundColor(Palette.ink)
                    Text(theme.subtitle)
                        .font(.quizSans(10))
                        .foregroundColor(Palette.muted)
                }
                Spacer()
                if selected {
                    ZStack {
                        Circle().fill(Palette.primary)
                        Circle().stroke(Palette.primary, lineWidth: 1)
                        Image(systemName: "checkmark")
                            .font(.system(size: 10, weight: .bold))
                            .foregroundColor(.white)
                    }
                    .frame(width: 22, height: 22)
                } else {
                    Image(systemName: "chevron.right")
                        .font(.system(size: 11, weight: .semibold))
                        .foregroundColor(Palette.line)
                        .frame(width: 22, height: 22)
                }
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 12)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }

    private var dataContent: some View {
        VStack(alignment: .leading, spacing: 20) {
            HStack(spacing: 0) {
                stat("\(store.banks.count)", "题库")
                statDivider
                stat("\(store.allQuestions.count)", "题目")
                statDivider
                stat("\(store.attempts.count)", "次练习")
            }
            .padding(.vertical, 16)
            .frame(maxWidth: .infinity)
            .background(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .fill(Palette.card)
                    .overlay(
                        RoundedRectangle(cornerRadius: 16, style: .continuous)
                            .stroke(Palette.line, lineWidth: 1)
                    )
            )

            group("备份与恢复") {
                actionRow(icon: "square.and.arrow.up", title: "导出备份", detail: "题库、进度、错题本一起打包", color: Palette.primary) {
                    exportBackup()
                }
                divider
                actionRow(icon: "square.and.arrow.down", title: "从备份恢复", detail: "会覆盖当前题库和答题记录", color: Palette.primary) {
                    showRestore = true
                }
            }

            group("清理") {
                actionRow(icon: "trash", title: "清空答题记录", detail: "保留题库，只清进度和错题", color: Palette.red, destructive: true) {
                    confirmClear = true
                }
            }

            Text("备份文件是 JSON，由你自己保存。恢复前会覆盖当前数据。")
                .font(.quizSans(10.5))
                .foregroundColor(Palette.muted)
                .lineSpacing(4)
                .padding(.horizontal, 4)
        }
    }

    private var experienceContent: some View {
        VStack(alignment: .leading, spacing: 20) {
            group("作答反馈") {
                HStack(spacing: 11) {
                    iconBadge("hand.tap", color: Palette.primary)
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
            }

            group("题库导入") {
                entryRow(
                    icon: "questionmark.circle",
                    title: "怎么导入题库",
                    detail: "支持 DOCX / TXT / MD / JSON",
                    value: "查看",
                    page: .importHelp
                )
            }
        }
    }

    private var aboutContent: some View {
        VStack(alignment: .leading, spacing: 20) {
            HStack(spacing: 14) {
                BrandMark(size: 54)
                VStack(alignment: .leading, spacing: 4) {
                    Text("BookQ")
                        .font(.quizSerif(27))
                        .foregroundColor(Palette.ink)
                    Text("不内置题库的离线刷题 App")
                        .font(.quizSans(11))
                        .foregroundColor(Palette.muted)
                }
                Spacer(minLength: 0)
            }
            .padding(20)
            .background(
                RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .fill(Palette.card)
                    .overlay(
                        RoundedRectangle(cornerRadius: 18, style: .continuous)
                            .stroke(Palette.line, lineWidth: 1)
                    )
            )

            group("应用") {
                infoRow(icon: "app.badge", title: "版本", value: appVersion)
                divider
                infoRow(icon: "shippingbox", title: "题库", value: "全部由你导入")
                divider
                infoRow(icon: "paintpalette", title: "视觉", value: "\(store.theme.title) · \(store.appearance.title)")
            }

            Text("题库、答题记录和错题本只保存在本机。\n不登录、不联网、不上传任何文档。")
                .font(.quizSans(10.5))
                .foregroundColor(Palette.muted)
                .lineSpacing(4)
                .padding(.horizontal, 4)
        }
    }

    private var importHelpContent: some View {
        VStack(alignment: .leading, spacing: 20) {
            VStack(alignment: .leading, spacing: 16) {
                stepRow("1", title: "打开「题库」页", detail: "导入按钮只在题库页右上角出现。")
                stepRow("2", title: "点「导入」", detail: "选择手机里的 DOCX、TXT、MD 或 JSON 文件。")
                stepRow("3", title: "确认后保存", detail: "先预览题库名、题数和少量样题，再决定导入。")
            }
            .padding(18)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .fill(Palette.card)
                    .overlay(
                        RoundedRectangle(cornerRadius: 16, style: .continuous)
                            .stroke(Palette.line, lineWidth: 1)
                    )
            )

            group("支持格式") {
                HStack(spacing: 8) {
                    ForEach(["DOCX", "TXT", "MD", "JSON"], id: \.self) { format in
                        Text(format)
                            .font(.quizSans(11, weight: .semibold))
                            .foregroundColor(Palette.primary)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 6)
                            .background(
                                RoundedRectangle(cornerRadius: 9, style: .continuous)
                                    .fill(Palette.track)
                            )
                    }
                }
                .padding(14)
            }

            Text("一份文件就是一个题库，分类名默认取文件名。\n文档里的小标题只作分隔，不额外分类。")
                .font(.quizSans(10.5))
                .foregroundColor(Palette.muted)
                .lineSpacing(4)
                .padding(.horizontal, 4)
        }
    }

    // MARK: - 组件

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

    private func entryRow(icon: String, title: String, detail: String, value: String, page target: SettingsPage) -> some View {
        Button {
            withAnimation(.easeInOut(duration: 0.18)) { page = target }
        } label: {
            HStack(spacing: 11) {
                iconBadge(icon, color: Palette.primary)
                VStack(alignment: .leading, spacing: 2) {
                    Text(title)
                        .font(.quizSans(13.5, weight: .medium))
                        .foregroundColor(Palette.ink)
                    Text(detail)
                        .font(.quizSans(10))
                        .foregroundColor(Palette.muted)
                }
                Spacer(minLength: 8)
                Text(value)
                    .font(.quizSans(10.5))
                    .foregroundColor(Palette.muted)
                    .lineLimit(1)
                Image(systemName: "chevron.right")
                    .font(.system(size: 10, weight: .semibold))
                    .foregroundColor(Palette.line)
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 12)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }

    private func choiceRow(title: String, detail: String, selected: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: 12) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(title)
                        .font(.quizSans(13, weight: .medium))
                        .foregroundColor(Palette.ink)
                    Text(detail)
                        .font(.quizSans(10))
                        .foregroundColor(Palette.muted)
                }
                Spacer()
                ZStack {
                    Circle()
                        .fill(selected ? Palette.primary : Color.clear)
                    Circle()
                        .stroke(selected ? Palette.primary : Palette.line, lineWidth: 1)
                    if selected {
                        Image(systemName: "checkmark")
                            .font(.system(size: 10, weight: .bold))
                            .foregroundColor(.white)
                    }
                }
                .frame(width: 22, height: 22)
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 13)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }

    private func sizeOption(_ size: Int) -> some View {
        let selected = store.defaultSize == size
        let label = size == 0 ? "全部" : "\(size) 题"
        return Button {
            store.defaultSize = size
        } label: {
            Text(label)
                .font(.quizSans(12.5, weight: .semibold))
                .foregroundColor(selected ? .white : Palette.ink2)
                .frame(maxWidth: .infinity)
                .frame(height: 46)
                .background(
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .fill(selected ? Palette.primary : Palette.card)
                        .overlay(
                            RoundedRectangle(cornerRadius: 12, style: .continuous)
                                .stroke(selected ? Palette.primary : Palette.line, lineWidth: 1)
                        )
                )
        }
        .buttonStyle(.plain)
    }

    private func stat(_ value: String, _ label: String) -> some View {
        VStack(spacing: 4) {
            Text(value)
                .font(.quizSerif(21))
                .foregroundColor(Palette.primary)
            Text(label)
                .font(.quizSans(10))
                .foregroundColor(Palette.muted)
        }
        .frame(maxWidth: .infinity)
    }

    private var statDivider: some View {
        Rectangle()
            .fill(Palette.line2)
            .frame(width: 1, height: 34)
    }

    private func actionRow(icon: String, title: String, detail: String, color: Color, destructive: Bool = false, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: 11) {
                iconBadge(icon, color: color)
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
            iconBadge(icon, color: Palette.gold)
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

    private func iconBadge(_ name: String, color: Color) -> some View {
        Image(systemName: name)
            .font(.system(size: 13, weight: .medium))
            .foregroundColor(color)
            .frame(width: 26, height: 26)
            .background(RoundedRectangle(cornerRadius: 8).fill(color.opacity(0.07)))
    }

    private func stepRow(_ number: String, title: String, detail: String) -> some View {
        HStack(alignment: .top, spacing: 12) {
            Text(number)
                .font(.quizSerif(13))
                .foregroundColor(Palette.primary)
                .frame(width: 26, height: 26)
                .background(Circle().fill(Palette.primary.opacity(0.10)))
            VStack(alignment: .leading, spacing: 3) {
                Text(title)
                    .font(.quizSans(13, weight: .medium))
                    .foregroundColor(Palette.ink)
                Text(detail)
                    .font(.quizSans(10))
                    .foregroundColor(Palette.muted)
                    .lineSpacing(3)
            }
        }
    }

    // MARK: - 数据

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

// MARK: - 主题预览

struct ThemePreviewSheet: View {
    @Environment(\.dismiss) private var dismiss

    let theme: AppTheme
    let appearance: AppAppearance
    let currentTheme: AppTheme
    let currentAppearance: AppAppearance
    let onApply: () -> Void

    private var palette: BookQPalette {
        BookQPalettes.of(theme, appearance)
    }

    var body: some View {
        ZStack {
            palette.paper.ignoresSafeArea()

            VStack(spacing: 0) {
                SheetGrabber()
                    .padding(.top, 8)

                HStack(alignment: .top, spacing: 12) {
                    VStack(alignment: .leading, spacing: 5) {
                        Text("主题预览 · \(appearance.title)")
                            .font(.quizSans(10.5, weight: .semibold))
                            .tracking(1.2)
                            .foregroundColor(palette.gold)
                        Text(theme.title)
                            .font(.quizSerif(28))
                            .foregroundColor(palette.ink)
                        Text(theme.subtitle)
                            .font(.quizSans(11.5))
                            .foregroundColor(palette.muted)
                    }
                    Spacer(minLength: 8)
                    Button {
                        dismiss()
                    } label: {
                        Image(systemName: "xmark")
                            .font(.system(size: 12, weight: .semibold))
                            .foregroundColor(palette.ink2)
                            .frame(width: 30, height: 30)
                            .background(
                                Circle()
                                    .fill(palette.card)
                                    .overlay(Circle().stroke(palette.line, lineWidth: 1))
                            )
                    }
                    .buttonStyle(.plain)
                }
                .padding(.horizontal, 20)
                .padding(.top, 10)

                ScrollView(showsIndicators: false) {
                    VStack(alignment: .leading, spacing: 10) {
                        HStack {
                            Text("答题页")
                                .font(.quizSans(11.5, weight: .semibold))
                                .foregroundColor(palette.ink2)
                                .tracking(0.5)
                            Spacer()
                            Text("预览")
                                .font(.quizSans(9, weight: .semibold))
                                .foregroundColor(palette.gold)
                                .tracking(0.8)
                                .padding(.horizontal, 7)
                                .padding(.vertical, 3)
                                .background(
                                    Capsule()
                                        .fill(palette.gold.opacity(0.12))
                                        .overlay(Capsule().stroke(palette.gold.opacity(0.35), lineWidth: 1))
                                )
                        }
                        .padding(.horizontal, 20)
                        .padding(.top, 16)

                        quizSample
                            .padding(.horizontal, 20)
                    }
                    .padding(.bottom, 12)
                }

                Text("当前仍是 \(currentTheme.title) · \(currentAppearance.title)。按「使用这个主题」才会切换；题库和进度不受影响。")
                    .font(.quizSans(10.5))
                    .foregroundColor(palette.muted)
                    .lineSpacing(3)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.horizontal, 20)
                    .padding(.bottom, 10)

                HStack(spacing: 10) {
                    Button {
                        dismiss()
                    } label: {
                        Text("取消")
                            .font(.quizSans(15, weight: .medium))
                            .foregroundColor(palette.ink2)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                            .background(
                                RoundedRectangle(cornerRadius: 12, style: .continuous)
                                    .fill(palette.card)
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 12, style: .continuous)
                                            .stroke(palette.line, lineWidth: 1)
                                    )
                            )
                    }
                    .buttonStyle(.plain)

                    Button {
                        onApply()
                    } label: {
                        Text("使用这个主题")
                            .font(.quizSans(15, weight: .semibold))
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                            .background(
                                RoundedRectangle(cornerRadius: 12, style: .continuous)
                                    .fill(palette.primary)
                            )
                    }
                    .buttonStyle(.plain)
                }
                .padding(.horizontal, 20)
                .padding(.bottom, 18)
            }
        }
        .presentationDetents([.fraction(0.68), .large])
        .presentationDragIndicator(.hidden)
    }

    private var quizSample: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(alignment: .center, spacing: 8) {
                Text("BookQ")
                    .font(.quizSans(10.5, weight: .semibold))
                    .foregroundColor(palette.muted)
                    .tracking(0.8)
                Rectangle()
                    .fill(palette.gold.opacity(0.7))
                    .frame(width: 22, height: 2)
                Spacer()
                Text("03 / 10")
                    .font(.quizSans(10.5))
                    .foregroundColor(palette.muted)
            }

            GeometryReader { geometry in
                ZStack(alignment: .leading) {
                    Rectangle().fill(palette.track).frame(height: 2)
                    Rectangle().fill(palette.primary).frame(width: geometry.size.width * 0.3, height: 2)
                }
            }
            .frame(height: 2)
            .padding(.top, 10)

            Text("梭织物的三原组织不包括以下哪一项？")
                .font(.quizSerif(19))
                .foregroundColor(palette.ink)
                .lineSpacing(4)
                .padding(.top, 18)

            VStack(spacing: 7) {
                sampleOption("A", "平纹", selected: false)
                sampleOption("B", "斜纹", selected: false)
                sampleOption("C", "缎纹", selected: false)
                sampleOption("D", "提花", selected: true)
            }
            .padding(.top, 16)

            HStack {
                Text("单选 · 题库原文")
                    .font(.quizSans(9.5))
                    .foregroundColor(palette.muted)
                Spacer()
                Text("下一题 ›")
                    .font(.quizSans(9.5, weight: .medium))
                    .foregroundColor(palette.gold)
            }
            .padding(.top, 12)
            .overlay(alignment: .top) {
                Rectangle().fill(palette.line2).frame(height: 1)
            }
            .padding(.top, 12)
        }
        .padding(16)
        .background(
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .fill(palette.card)
                .overlay(
                    RoundedRectangle(cornerRadius: 14, style: .continuous)
                        .stroke(palette.line, lineWidth: 1)
                )
        )
        .shadow(color: palette.shadow.opacity(palette.isDark ? 0.22 : 0.08), radius: 14, x: 0, y: 8)
    }

    private func sampleOption(_ label: String, _ text: String, selected: Bool) -> some View {
        HStack(spacing: 10) {
            Text(label)
                .font(.quizSans(10.5, weight: .semibold))
                .foregroundColor(selected ? .white : palette.ink2)
                .frame(width: 22, height: 22)
                .background(
                    Circle()
                        .fill(selected ? palette.primary : palette.paper)
                        .overlay(Circle().stroke(selected ? palette.primary : palette.line, lineWidth: 1))
                )
            Text(text)
                .font(.quizSans(12.5))
                .foregroundColor(selected ? palette.ink : palette.ink2)
            Spacer()
            if selected {
                Image(systemName: "checkmark")
                    .font(.system(size: 9, weight: .bold))
                    .foregroundColor(.white)
                    .frame(width: 16, height: 16)
                    .background(Circle().fill(palette.accent))
            }
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 8)
        .background(
            RoundedRectangle(cornerRadius: 9, style: .continuous)
                .fill(selected ? palette.primary.opacity(0.08) : palette.paper.opacity(palette.isDark ? 0.35 : 0.55))
                .overlay(
                    RoundedRectangle(cornerRadius: 9, style: .continuous)
                        .stroke(selected ? palette.primary : palette.line, lineWidth: 1)
                )
        )
    }
}
