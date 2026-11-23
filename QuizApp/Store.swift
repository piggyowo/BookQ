import Foundation
import SwiftUI
import UIKit

@MainActor
final class QuizStore: ObservableObject {
    // 主导航
    @Published var screen: Screen = .home
    @Published var selectedTab: AppTab = .home {
        didSet {
            if selectedTab != .settings { settingsPage = nil }
        }
    }
    @Published var settingsPage: SettingsPage?

    // 题库
    @Published var banks: [QuizBank] = []
    @Published var recentBankIDs: [UUID] = []
    @Published var sequenceCursors: [String: SequenceCursor] = [:]

    // 设置
    @Published var defaultMode: QuizMode = .practice {
        didSet { persist() }
    }
    @Published var defaultSize: Int = 10 { // 0 表示全部
        didSet { persist() }
    }
    @Published var defaultOrder: QuizOrder = .sequential {
        didSet { persist() }
    }
    @Published var haptics: Bool = true {
        didSet { persist() }
    }

    @Published var theme: AppTheme = .indigo {
        didSet {
            applyTheme()
            persist()
        }
    }
    @Published var appearance: AppAppearance = .light {
        didSet {
            applyTheme()
            persist()
        }
    }

    var themeKey: String { "\(theme.rawValue)-\(appearance.rawValue)" }

    // 本轮
    @Published var currentScope: QuizScope = .all
    @Published var mode: QuizMode = .practice
    @Published var size: Int? = 10
    @Published var order: QuizOrder = .sequential
    @Published var sheetOpen: Bool = false

    @Published var queue: [Question] = []
    @Published var answers: [String?] = []
    @Published var locked: [Bool] = []
    @Published var index: Int = 0
    @Published var startTime: Date = Date()
    @Published var result: QuizResult?

    // 记录
    @Published var wrongIDs: [String] = []
    @Published var progress: [String: QuestionProgress] = [:]
    @Published var attempts: [Attempt] = []

    @Published var toast: String?

    init() {
        load()
        applyTheme()
    }

    private func applyTheme() {
        ThemeRuntime.apply(theme: theme, appearance: appearance)
    }

    // MARK: - 题库读取

    var allQuestions: [Question] {
        banks.flatMap(\.questions)
    }

    private var questionMap: [String: Question] {
        var map: [String: Question] = [:]
        for question in allQuestions {
            map[question.id] = question
        }
        return map
    }

    var hasBanks: Bool { !banks.isEmpty }

    func bank(id: UUID) -> QuizBank? {
        banks.first { $0.id == id }
    }

    func questions(in scope: QuizScope) -> [Question] {
        switch scope {
        case .all:
            return allQuestions
        case .bank(let id):
            return bank(id: id)?.questions ?? []
        case .wrong:
            return wrongQuestions
        }
    }

    var wrongQuestions: [Question] {
        let map = questionMap
        return wrongIDs.compactMap { map[$0] }
    }

    var lastBank: QuizBank? {
        for id in recentBankIDs {
            if let bank = bank(id: id) { return bank }
        }
        return banks.first
    }

    var homeBanks: [QuizBank] {
        var seen = Set<UUID>()
        var result: [QuizBank] = []
        for id in recentBankIDs {
            guard let bank = bank(id: id), seen.insert(id).inserted else { continue }
            result.append(bank)
            if result.count == 2 { break }
        }
        if result.count < 2 {
            for bank in banks.sorted(by: { $0.importedAt > $1.importedAt }) {
                guard seen.insert(bank.id).inserted else { continue }
                result.append(bank)
                if result.count == 2 { break }
            }
        }
        return result
    }

    private func touchBank(_ id: UUID) {
        recentBankIDs.removeAll { $0 == id }
        recentBankIDs.insert(id, at: 0)
        if recentBankIDs.count > 3 { recentBankIDs.removeLast(recentBankIDs.count - 3) }
    }

    // MARK: - 顺序游标

    private func sequenceKey(_ scope: QuizScope) -> String {
        switch scope {
        case .all: return "all"
        case .bank(let id): return id.uuidString
        case .wrong: return "wrong"
        }
    }

    func sequenceInfo(for scope: QuizScope) -> SequenceInfo {
        let list = questions(in: scope)
        let key = sequenceKey(scope)
        let cursor = sequenceCursors[key]
        let nextIndex = cursor?.nextQuestionID.flatMap { id in
            list.firstIndex { $0.id == id }
        }
        let completed = cursor?.completed == true && nextIndex == nil
        let start = nextIndex ?? 0
        let remaining = max(0, list.count - start)
        let done = list.filter { progress[$0.id] != nil }.count
        let lastNumber: String? = {
            guard let nextIndex, nextIndex > 0, list.indices.contains(nextIndex - 1) else { return nil }
            return list[nextIndex - 1].num
        }()
        return SequenceInfo(
            total: list.count,
            nextIndex: nextIndex,
            remaining: remaining,
            done: done,
            completed: completed,
            lastNumber: lastNumber
        )
    }

    func resetSequenceCursor(for scope: QuizScope) {
        let key = sequenceKey(scope)
        if let first = questions(in: scope).first {
            sequenceCursors[key] = SequenceCursor(nextQuestionID: first.id, completed: false)
        } else {
            sequenceCursors[key] = nil
        }
        persist()
    }

    private func setCursor(after questionID: String, scope: QuizScope) {
        let list = questions(in: scope)
        guard let index = list.firstIndex(where: { $0.id == questionID }) else { return }
        let key = sequenceKey(scope)
        if index + 1 < list.count {
            sequenceCursors[key] = SequenceCursor(nextQuestionID: list[index + 1].id, completed: false)
        } else {
            sequenceCursors[key] = SequenceCursor(nextQuestionID: nil, completed: true)
        }
        persist()
    }

    private func advanceCursorIfCurrent(_ questionID: String, scope: QuizScope) {
        let key = sequenceKey(scope)
        guard sequenceCursors[key]?.nextQuestionID == questionID else { return }
        setCursor(after: questionID, scope: scope)
    }

    var scopeLabel: String {
        switch currentScope {
        case .all: return "全部题库"
        case .bank(let id): return bank(id: id)?.name ?? "题库"
        case .wrong: return "错题重练"
        }
    }

    // MARK: - 导入与管理

    func uniqueBankName(_ proposed: String) -> String {
        let base = proposed.trimmingCharacters(in: .whitespacesAndNewlines)
        let fallback = base.isEmpty ? "未命名题库" : base
        guard banks.contains(where: { $0.name == fallback }) else { return fallback }
        var index = 2
        while banks.contains(where: { $0.name == "\(fallback) (\(index))" }) {
            index += 1
        }
        return "\(fallback) (\(index))"
    }

    @discardableResult
    func addBank(name: String, fileName: String, parsed: [ParsedQuestion]) -> QuizBank? {
        guard !parsed.isEmpty else {
            showToast("没有解析到题目")
            return nil
        }
        let uniqueName = uniqueBankName(name)
        let bankID = UUID()
        let questions = parsed.enumerated().map { pair in
            Question(
                id: "\(bankID.uuidString)-\(pair.offset + 1)",
                bankID: bankID,
                chapter: uniqueName,
                num: pair.element.num,
                stem: pair.element.stem,
                options: pair.element.options,
                answer: pair.element.answer,
                kind: pair.element.kind
            )
        }
        let bank = QuizBank(
            id: bankID,
            name: uniqueName,
            fileName: fileName,
            importedAt: Date(),
            questions: questions
        )
        banks.append(bank)
        touchBank(bankID)
        persist()
        return bank
    }

    func renameBank(id: UUID, to newName: String) {
        let trimmed = newName.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty, let index = banks.firstIndex(where: { $0.id == id }) else { return }
        let oldName = banks[index].name
        guard trimmed != oldName else { return }
        guard !banks.contains(where: { $0.id != id && $0.name == trimmed }) else {
            showToast("已经有同名题库")
            return
        }
        banks[index].name = trimmed
        banks[index].questions = banks[index].questions.map { question in
            Question(
                id: question.id,
                bankID: question.bankID,
                chapter: trimmed,
                num: question.num,
                stem: question.stem,
                options: question.options,
                answer: question.answer,
                kind: question.kind
            )
        }
        if case .bank(let activeID) = currentScope, activeID == id {
            // 名称由 scopeLabel 动态读取，无需额外处理
        }
        attempts = attempts.map { attempt in
            guard attempt.scope == oldName else { return attempt }
            return Attempt(id: attempt.id, total: attempt.total, correct: attempt.correct, date: attempt.date, scope: trimmed)
        }
        persist()
    }

    func deleteBank(id: UUID) {
        guard let bank = bank(id: id) else { return }
        let questionIDs = Set(bank.questions.map(\.id))
        banks.removeAll { $0.id == id }
        wrongIDs.removeAll { questionIDs.contains($0) }
        progress = progress.filter { !questionIDs.contains($0.key) }
        attempts.removeAll { $0.scope == bank.name }
        recentBankIDs.removeAll { $0 == id }
        sequenceCursors[sequenceKey(.bank(id))] = nil
        if recentBankIDs.isEmpty, let first = banks.first {
            recentBankIDs = [first.id]
        }
        if case .bank(let activeID) = currentScope, activeID == id {
            currentScope = .all
        }
        persist()
    }

    func wrongCount(in bank: QuizBank) -> Int {
        let ids = Set(bank.questions.map(\.id))
        return wrongIDs.filter { ids.contains($0) }.count
    }

    // MARK: - 进度

    func scopeProgress(_ scope: QuizScope) -> (done: Int, total: Int, percent: Double, rate: Int?) {
        let list = questions(in: scope)
        let done = list.filter { progress[$0.id] != nil }.count
        let percent = list.isEmpty ? 0 : Double(done) / Double(list.count)
        let attemptsTotal = list.reduce(0) { $0 + (progress[$1.id]?.attempts ?? 0) }
        let correct = list.reduce(0) { $0 + (progress[$1.id]?.correct ?? 0) }
        let rate = attemptsTotal == 0 ? nil : Int((Double(correct) / Double(attemptsTotal) * 100).rounded())
        return (done, list.count, percent, rate)
    }

    func bankProgress(_ bank: QuizBank) -> (done: Int, total: Int, percent: Double, rate: Int?) {
        let list = bank.questions
        let done = list.filter { progress[$0.id] != nil }.count
        let percent = list.isEmpty ? 0 : Double(done) / Double(list.count)
        let attemptsTotal = list.reduce(0) { $0 + (progress[$1.id]?.attempts ?? 0) }
        let correct = list.reduce(0) { $0 + (progress[$1.id]?.correct ?? 0) }
        let rate = attemptsTotal == 0 ? nil : Int((Double(correct) / Double(attemptsTotal) * 100).rounded())
        return (done, list.count, percent, rate)
    }

    var totalAnswered: Int {
        allQuestions.filter { progress[$0.id] != nil }.count
    }

    var overallRate: Int? {
        let attemptsTotal = progress.values.reduce(0) { $0 + $1.attempts }
        let correct = progress.values.reduce(0) { $0 + $1.correct }
        guard attemptsTotal > 0 else { return nil }
        return Int((Double(correct) / Double(attemptsTotal) * 100).rounded())
    }

    var weeklyStats: (total: Int, correct: Int, rate: Int?, days: Int) {
        let calendar = Calendar.current
        let today = calendar.startOfDay(for: Date())
        guard let cutoff = calendar.date(byAdding: .day, value: -6, to: today) else {
            return (0, 0, nil, 0)
        }
        let recent = attempts.filter { $0.date >= cutoff }
        let total = recent.reduce(0) { $0 + $1.total }
        let correct = recent.reduce(0) { $0 + $1.correct }
        let rate = total == 0 ? nil : Int((Double(correct) / Double(total) * 100).rounded())
        let days = Set(recent.map { calendar.startOfDay(for: $0.date) }).count
        return (total, correct, rate, days)
    }

    var weakestBanks: [QuizBank] {
        banks
            .filter { bank in
                bank.questions.contains { (progress[$0.id]?.attempts ?? 0) > 0 }
            }
            .sorted { lhs, rhs in
                (bankProgress(lhs).rate ?? 0) < (bankProgress(rhs).rate ?? 0)
            }
            .prefix(2)
            .map { $0 }
    }

    // MARK: - 开始

    private func resolvedDefaultSize() -> Int? {
        defaultSize == 0 ? nil : defaultSize
    }

    func startQuiz(scope: QuizScope, mode: QuizMode, size: Int?, order: QuizOrder) {
        let candidates = questions(in: scope)
        guard !candidates.isEmpty else {
            showToast(scope == .wrong ? "错题本还是空的" : "这个题库还没有题目")
            return
        }

        self.currentScope = scope
        self.mode = mode
        self.size = size
        self.order = order

        var pool: [Question]
        if order == .sequential {
            let info = sequenceInfo(for: scope)
            var start = info.nextIndex ?? 0
            if info.completed {
                showToast("已完成一轮，重新从第 1 题开始")
                start = 0
                let key = sequenceKey(scope)
                sequenceCursors[key] = candidates.first.map {
                    SequenceCursor(nextQuestionID: $0.id, completed: false)
                }
            } else if info.nextIndex == nil {
                let key = sequenceKey(scope)
                sequenceCursors[key] = candidates.first.map {
                    SequenceCursor(nextQuestionID: $0.id, completed: false)
                }
            }
            if start >= candidates.count { start = 0 }
            let remaining = candidates.count - start
            let requested = size.flatMap { $0 > 0 ? $0 : nil }
            let take = min(requested ?? remaining, remaining)
            pool = Array(candidates[start..<(start + take)])
        } else {
            pool = candidates.shuffled()
            if let size, size > 0, size < pool.count {
                pool = Array(pool.prefix(size))
            }
        }

        queue = pool
        answers = Array(repeating: nil, count: pool.count)
        locked = Array(repeating: false, count: pool.count)
        index = 0
        sheetOpen = false
        startTime = Date()
        result = nil
        screen = .quiz
        if case .bank(let id) = scope {
            touchBank(id)
        }
        persist()
    }

    func startWrongPractice() {
        startQuiz(scope: .wrong, mode: .practice, size: nil, order: .random)
    }

    func restart() {
        startQuiz(scope: currentScope, mode: mode, size: size, order: order)
    }

    func leaveQuiz() {
        sheetOpen = false
        selectedTab = .home
        screen = .home
    }

    func goHome() {
        selectedTab = .home
        screen = .home
    }

    // MARK: - 作答

    func choose(_ key: String) {
        guard screen == .quiz, queue.indices.contains(index) else { return }
        let question = queue[index]

        if mode == .practice && locked[index] { return }

        if question.isFillLike {
            if mode == .practice {
                answers[index] = "FILL"
                locked[index] = true
                advanceCursorIfCurrent(question.id, scope: currentScope)
            } else {
                answers[index] = answers[index] == "FILL" ? nil : "FILL"
            }
            return
        }

        if question.options.isEmpty {
            if mode == .practice {
                answers[index] = "NA"
                locked[index] = true
                advanceCursorIfCurrent(question.id, scope: currentScope)
            } else {
                answers[index] = answers[index] == "NA" ? nil : "NA"
            }
            return
        }

        if question.isMulti {
            let selected = answers[index] ?? ""
            let next: String
            if selected.contains(key) {
                next = selected.replacingOccurrences(of: key, with: "")
            } else {
                next = String((selected + key).sorted())
            }
            answers[index] = next.isEmpty ? nil : next
            return
        }

        if mode == .exam {
            answers[index] = answers[index] == key ? nil : key
            return
        }

        answers[index] = key
        locked[index] = true
        advanceCursorIfCurrent(question.id, scope: currentScope)
        recordGrade(question, selected: key)
    }

    func submitMulti() {
        guard screen == .quiz, queue.indices.contains(index) else { return }
        guard mode == .practice, !locked[index] else { return }
        let question = queue[index]
        guard question.isMulti, let selected = answers[index], !selected.isEmpty else { return }

        locked[index] = true
        advanceCursorIfCurrent(question.id, scope: currentScope)
        recordGrade(question, selected: selected)
    }

    private func recordGrade(_ question: Question, selected: String) {
        guard question.hasAnswer else { return }
        let correct = question.isCorrect(selected)
        var item = progress[question.id] ?? QuestionProgress(attempts: 0, correct: 0)
        item.attempts += 1
        if correct {
            item.correct += 1
            if let position = wrongIDs.firstIndex(of: question.id) {
                wrongIDs.remove(at: position)
            }
        } else if !wrongIDs.contains(question.id) {
            wrongIDs.insert(question.id, at: 0)
        }
        progress[question.id] = item
        persist()
        if haptics {
            UIImpactFeedbackGenerator(style: correct ? .light : .rigid).impactOccurred()
        }
    }

    func jump(to position: Int) {
        guard queue.indices.contains(position) else { return }
        index = position
        sheetOpen = false
    }

    func next() {
        guard screen == .quiz else { return }
        if index + 1 < queue.count {
            index += 1
        } else {
            finish()
        }
    }

    func previous() {
        guard screen == .quiz, index > 0 else { return }
        index -= 1
    }

    var unansweredCount: Int {
        zip(queue, answers).filter { pair in
            pair.0.isGraded && pair.1 == nil
        }.count
    }

    var noAnswerCount: Int {
        queue.filter { !$0.isFillLike && !$0.hasAnswer }.count
    }

    var fillCount: Int {
        queue.filter { $0.isFill }.count
    }

    // MARK: - 交卷

    func finish() {
        guard !queue.isEmpty else { return }

        if mode == .exam {
            for (position, question) in queue.enumerated() where question.isGraded {
                guard let choice = answers[position] else { continue }
                let correct = question.isCorrect(choice)
                var item = progress[question.id] ?? QuestionProgress(attempts: 0, correct: 0)
                item.attempts += 1
                if correct {
                    item.correct += 1
                    if let wrongIndex = wrongIDs.firstIndex(of: question.id) {
                        wrongIDs.remove(at: wrongIndex)
                    }
                } else if !wrongIDs.contains(question.id) {
                    wrongIDs.insert(question.id, at: 0)
                }
                progress[question.id] = item
            }
        }

        if mode == .exam, order == .sequential, let last = queue.last {
            setCursor(after: last.id, scope: currentScope)
        }

        var items: [ResultItem] = []
        for (position, question) in queue.enumerated() where question.isGraded {
            if mode == .practice {
                guard let choice = answers[position] else { continue }
                if question.isMulti && !locked[position] { continue }
                items.append(ResultItem(question: question, choice: choice))
            } else {
                items.append(ResultItem(question: question, choice: answers[position]))
            }
        }

        if mode == .practice && items.isEmpty {
            showToast("本轮还没有可评分的作答")
            return
        }

        let total = items.count
        let correct = items.filter(\.isCorrect).count
        let unanswered = mode == .exam
            ? zip(queue, answers).filter { $0.0.isGraded && $0.1 == nil }.count
            : 0
        let noAnswerCount = queue.filter { !$0.isFillLike && !$0.hasAnswer }.count
        let fillCount = queue.filter { $0.isFill }.count
        let duration = Date().timeIntervalSince(startTime)

        var statsByChapter: [String: (correct: Int, total: Int)] = [:]
        var chapterOrder: [String] = []
        for item in items {
            if statsByChapter[item.question.chapter] == nil {
                chapterOrder.append(item.question.chapter)
            }
            var value = statsByChapter[item.question.chapter] ?? (0, 0)
            value.total += 1
            if item.isCorrect { value.correct += 1 }
            statsByChapter[item.question.chapter] = value
        }

        let chapterStats = chapterOrder.map { chapter -> ChapterStat in
            let value = statsByChapter[chapter] ?? (0, 0)
            return ChapterStat(name: chapter, correct: value.correct, total: value.total)
        }

        if total > 0 {
            attempts.append(Attempt(total: total, correct: correct, date: Date(), scope: scopeLabel))
            if attempts.count > 100 {
                attempts.removeFirst(attempts.count - 100)
            }
            persist()
        }

        result = QuizResult(
            total: total,
            correct: correct,
            unanswered: unanswered,
            noAnswerCount: noAnswerCount,
            duration: duration,
            scopeLabel: scopeLabel,
            mode: mode,
            items: items,
            chapterStats: chapterStats,
            fillCount: fillCount
        )
        sheetOpen = false
        screen = .result
    }

    // MARK: - 错题本

    func removeWrong(_ id: String) {
        wrongIDs.removeAll { $0 == id }
        persist()
    }

    func clearWrong() {
        wrongIDs = []
        persist()
    }

    // MARK: - 设置与数据

    func clearProgress() {
        progress = [:]
        wrongIDs = []
        attempts = []
        sequenceCursors = [:]
        persist()
    }

    func backupData() -> Data? {
        let backup = Backup(
            version: 1,
            exportedAt: Date(),
            banks: banks,
            progress: progress,
            wrong: wrongIDs,
            attempts: attempts,
            sequenceCursors: sequenceCursors,
            recentBankIDs: recentBankIDs
        )
        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601
        encoder.outputFormatting = [.prettyPrinted, .sortedKeys]
        return try? encoder.encode(backup)
    }

    func restoreBackup(_ data: Data) throws {
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601
        let backup = try decoder.decode(Backup.self, from: data)
        guard !backup.banks.isEmpty else {
            throw BackupError.empty
        }
        banks = backup.banks
        progress = backup.progress
        wrongIDs = backup.wrong.filter { id in
            backup.banks.contains { $0.questions.contains { $0.id == id } }
        }
        attempts = backup.attempts
        sequenceCursors = (backup.sequenceCursors ?? [:]).filter { (key, _) in
            key == "all" || key == "wrong" || backup.banks.contains { $0.id.uuidString == key }
        }
        let restoredRecent = (backup.recentBankIDs ?? []).filter { id in
            backup.banks.contains { $0.id == id }
        }
        recentBankIDs = Array(restoredRecent.prefix(3))
        if recentBankIDs.isEmpty {
            recentBankIDs = Array(banks.prefix(2).map(\.id))
        }
        persist()
    }

    enum BackupError: LocalizedError {
        case empty
        var errorDescription: String? {
            switch self {
            case .empty: return "备份里没有题库"
            }
        }
    }

    // MARK: - 持久化

    private struct Persisted: Codable {
        var banks: [QuizBank]
        var wrong: [String]
        var progress: [String: QuestionProgress]
        var attempts: [Attempt]
        var recentBankIDs: [UUID]
        var sequenceCursors: [String: SequenceCursor]
        var defaultMode: QuizMode
        var defaultSize: Int
        var defaultOrder: QuizOrder
        var haptics: Bool
        var theme: AppTheme
        var appearance: AppAppearance

        private enum CodingKeys: String, CodingKey {
            case banks, wrong, progress, attempts
            case recentBankID, recentBankIDs, sequenceCursors
            case defaultMode, defaultSize, defaultOrder, haptics, theme, appearance
        }

        init(banks: [QuizBank],
             wrong: [String],
             progress: [String: QuestionProgress],
             attempts: [Attempt],
             recentBankIDs: [UUID],
             sequenceCursors: [String: SequenceCursor],
             defaultMode: QuizMode,
             defaultSize: Int,
             defaultOrder: QuizOrder,
             haptics: Bool,
             theme: AppTheme,
             appearance: AppAppearance) {
            self.banks = banks
            self.wrong = wrong
            self.progress = progress
            self.attempts = attempts
            self.recentBankIDs = recentBankIDs
            self.sequenceCursors = sequenceCursors
            self.defaultMode = defaultMode
            self.defaultSize = defaultSize
            self.defaultOrder = defaultOrder
            self.haptics = haptics
            self.theme = theme
            self.appearance = appearance
        }

        init(from decoder: Decoder) throws {
            let container = try decoder.container(keyedBy: CodingKeys.self)
            banks = try container.decodeIfPresent([QuizBank].self, forKey: .banks) ?? []
            wrong = try container.decodeIfPresent([String].self, forKey: .wrong) ?? []
            progress = try container.decodeIfPresent([String: QuestionProgress].self, forKey: .progress) ?? [:]
            attempts = try container.decodeIfPresent([Attempt].self, forKey: .attempts) ?? []
            if let ids = try container.decodeIfPresent([UUID].self, forKey: .recentBankIDs) {
                recentBankIDs = ids
            } else if let old = try container.decodeIfPresent(UUID.self, forKey: .recentBankID) {
                recentBankIDs = [old]
            } else {
                recentBankIDs = []
            }
            sequenceCursors = try container.decodeIfPresent([String: SequenceCursor].self, forKey: .sequenceCursors) ?? [:]
            defaultMode = try container.decodeIfPresent(QuizMode.self, forKey: .defaultMode) ?? .practice
            defaultSize = try container.decodeIfPresent(Int.self, forKey: .defaultSize) ?? 10
            defaultOrder = try container.decodeIfPresent(QuizOrder.self, forKey: .defaultOrder) ?? .sequential
            haptics = try container.decodeIfPresent(Bool.self, forKey: .haptics) ?? true
            theme = try container.decodeIfPresent(AppTheme.self, forKey: .theme) ?? .indigo
            appearance = try container.decodeIfPresent(AppAppearance.self, forKey: .appearance) ?? .light
        }

        func encode(to encoder: Encoder) throws {
            var container = encoder.container(keyedBy: CodingKeys.self)
            try container.encode(banks, forKey: .banks)
            try container.encode(wrong, forKey: .wrong)
            try container.encode(progress, forKey: .progress)
            try container.encode(attempts, forKey: .attempts)
            try container.encode(recentBankIDs, forKey: .recentBankIDs)
            try container.encode(sequenceCursors, forKey: .sequenceCursors)
            try container.encode(defaultMode, forKey: .defaultMode)
            try container.encode(defaultSize, forKey: .defaultSize)
            try container.encode(defaultOrder, forKey: .defaultOrder)
            try container.encode(haptics, forKey: .haptics)
            try container.encode(theme, forKey: .theme)
            try container.encode(appearance, forKey: .appearance)
        }
    }

    private struct Backup: Codable {
        let version: Int
        let exportedAt: Date
        let banks: [QuizBank]
        let progress: [String: QuestionProgress]
        let wrong: [String]
        let attempts: [Attempt]
        let sequenceCursors: [String: SequenceCursor]?
        let recentBankIDs: [UUID]?
    }

    private static var stateURL: URL {
        let manager = FileManager.default
        let base = manager.urls(for: .applicationSupportDirectory, in: .userDomainMask).first
            ?? manager.urls(for: .documentDirectory, in: .userDomainMask).first!
        let directory = base.appendingPathComponent("CraftQuiz", isDirectory: true)
        if !manager.fileExists(atPath: directory.path) {
            try? manager.createDirectory(at: directory, withIntermediateDirectories: true)
        }
        return directory.appendingPathComponent("state.json")
    }

    private func load() {
        guard let data = try? Data(contentsOf: Self.stateURL) else { return }
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601
        guard let saved = try? decoder.decode(Persisted.self, from: data) else { return }
        banks = saved.banks
        wrongIDs = saved.wrong
        progress = saved.progress
        attempts = saved.attempts
        recentBankIDs = saved.recentBankIDs.filter { id in
            saved.banks.contains { $0.id == id }
        }
        if recentBankIDs.isEmpty {
            recentBankIDs = Array(saved.banks.sorted(by: { $0.importedAt > $1.importedAt }).prefix(2).map(\.id))
        }
        sequenceCursors = saved.sequenceCursors
        defaultMode = saved.defaultMode
        defaultSize = saved.defaultSize
        defaultOrder = saved.defaultOrder
        haptics = saved.haptics
        theme = saved.theme
        appearance = saved.appearance
        applyTheme()
    }

    private func persist() {
        let value = Persisted(
            banks: banks,
            wrong: wrongIDs,
            progress: progress,
            attempts: attempts,
            recentBankIDs: recentBankIDs,
            sequenceCursors: sequenceCursors,
            defaultMode: defaultMode,
            defaultSize: defaultSize,
            defaultOrder: defaultOrder,
            haptics: haptics,
            theme: theme,
            appearance: appearance
        )
        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601
        guard let data = try? encoder.encode(value) else { return }
        try? data.write(to: Self.stateURL, options: .atomic)
    }

    // MARK: - 提示

    func showToast(_ message: String) {
        withAnimation(.easeOut(duration: 0.2)) {
            toast = message
        }
        let current = message
        Task { [weak self] in
            try? await Task.sleep(nanoseconds: 2_000_000_000)
            guard let self = self else { return }
            if self.toast == current {
                withAnimation(.easeIn(duration: 0.2)) {
                    self.toast = nil
                }
            }
        }
    }
}
