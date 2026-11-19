import Foundation
import SwiftUI
import UIKit

@MainActor
final class QuizStore: ObservableObject {
    // 主导航
    @Published var screen: Screen = .home
    @Published var selectedTab: AppTab = .home

    // 题库
    @Published var banks: [QuizBank] = []
    @Published var recentBankID: UUID?

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
        if let recentBankID, let bank = bank(id: recentBankID) {
            return bank
        }
        return banks.first
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
                answer: pair.element.answer
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
        recentBankID = bankID
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
                answer: question.answer
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
        if recentBankID == id {
            recentBankID = banks.first?.id
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

        var pool = candidates
        if order == .random {
            pool.shuffle()
        }
        if let size, size > 0, size < pool.count {
            pool = Array(pool.prefix(size))
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
            recentBankID = id
            persist()
        }
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

        if question.options.isEmpty {
            if mode == .practice {
                answers[index] = "NA"
                locked[index] = true
            } else {
                answers[index] = answers[index] == "NA" ? nil : "NA"
            }
            return
        }

        if mode == .exam {
            answers[index] = answers[index] == key ? nil : key
            return
        }

        answers[index] = key
        locked[index] = true

        // 题库无答案：只提示，不计分、不进错题本、不加掌握度
        guard question.hasAnswer else { return }

        let correct = key == question.answer
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
            pair.0.hasAnswer && pair.1 == nil
        }.count
    }

    var noAnswerCount: Int {
        queue.filter { !$0.hasAnswer }.count
    }

    // MARK: - 交卷

    func finish() {
        guard !queue.isEmpty else { return }

        if mode == .exam {
            for (position, question) in queue.enumerated() where question.hasAnswer {
                guard let choice = answers[position] else { continue }
                let correct = choice == question.answer
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

        var items: [ResultItem] = []
        for (position, question) in queue.enumerated() where question.hasAnswer {
            if mode == .practice {
                guard let choice = answers[position] else { continue }
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
            ? zip(queue, answers).filter { $0.0.hasAnswer && $0.1 == nil }.count
            : 0
        let noAnswerCount = queue.filter { !$0.hasAnswer }.count
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
            chapterStats: chapterStats
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
        persist()
    }

    func backupData() -> Data? {
        let backup = Backup(
            version: 1,
            exportedAt: Date(),
            banks: banks,
            progress: progress,
            wrong: wrongIDs,
            attempts: attempts
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
        recentBankID = banks.first?.id
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
        var recentBankID: UUID?
        var defaultMode: QuizMode
        var defaultSize: Int
        var defaultOrder: QuizOrder
        var haptics: Bool

        init(banks: [QuizBank],
             wrong: [String],
             progress: [String: QuestionProgress],
             attempts: [Attempt],
             recentBankID: UUID?,
             defaultMode: QuizMode,
             defaultSize: Int,
             defaultOrder: QuizOrder,
             haptics: Bool) {
            self.banks = banks
            self.wrong = wrong
            self.progress = progress
            self.attempts = attempts
            self.recentBankID = recentBankID
            self.defaultMode = defaultMode
            self.defaultSize = defaultSize
            self.defaultOrder = defaultOrder
            self.haptics = haptics
        }

        init(from decoder: Decoder) throws {
            let container = try decoder.container(keyedBy: CodingKeys.self)
            banks = try container.decodeIfPresent([QuizBank].self, forKey: .banks) ?? []
            wrong = try container.decodeIfPresent([String].self, forKey: .wrong) ?? []
            progress = try container.decodeIfPresent([String: QuestionProgress].self, forKey: .progress) ?? [:]
            attempts = try container.decodeIfPresent([Attempt].self, forKey: .attempts) ?? []
            recentBankID = try container.decodeIfPresent(UUID.self, forKey: .recentBankID)
            defaultMode = try container.decodeIfPresent(QuizMode.self, forKey: .defaultMode) ?? .practice
            defaultSize = try container.decodeIfPresent(Int.self, forKey: .defaultSize) ?? 10
            defaultOrder = try container.decodeIfPresent(QuizOrder.self, forKey: .defaultOrder) ?? .sequential
            haptics = try container.decodeIfPresent(Bool.self, forKey: .haptics) ?? true
        }
    }

    private struct Backup: Codable {
        let version: Int
        let exportedAt: Date
        let banks: [QuizBank]
        let progress: [String: QuestionProgress]
        let wrong: [String]
        let attempts: [Attempt]
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
        recentBankID = saved.recentBankID
        defaultMode = saved.defaultMode
        defaultSize = saved.defaultSize
        defaultOrder = saved.defaultOrder
        haptics = saved.haptics
    }

    private func persist() {
        let value = Persisted(
            banks: banks,
            wrong: wrongIDs,
            progress: progress,
            attempts: attempts,
            recentBankID: recentBankID,
            defaultMode: defaultMode,
            defaultSize: defaultSize,
            defaultOrder: defaultOrder,
            haptics: haptics
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
