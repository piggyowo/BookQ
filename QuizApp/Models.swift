import Foundation

// MARK: - 题目

struct Question: Codable, Identifiable, Hashable {
    let id: String
    let bankID: UUID
    let chapter: String
    let num: String
    let stem: String
    /// [["A", "选项内容"], ...]
    let options: [[String]]
    let answer: String?

    var keys: [String] {
        options.compactMap { $0.count >= 2 ? $0[0] : nil }
    }

    func text(for key: String) -> String {
        guard let option = options.first(where: { $0.first == key }), option.count >= 2 else {
            return ""
        }
        return option[1]
    }

    var hasAnswer: Bool {
        guard let answer = answer, !answer.isEmpty else { return false }
        return keys.contains(answer)
    }

    var answerText: String {
        guard let answer = answer else { return "" }
        return text(for: answer)
    }

    var metaLine: String {
        let optionText = options.isEmpty ? "无选项" : "\(options.count) 个选项"
        return "题库第 \(num) 题 · \(optionText)" + (hasAnswer ? "" : " · 题库无答案")
    }
}

// MARK: - 题库

struct QuizBank: Codable, Identifiable, Hashable {
    let id: UUID
    var name: String
    var fileName: String
    var importedAt: Date
    var questions: [Question]

    var count: Int { questions.count }

    var colorIndex: Int {
        // 用一个稳定、可复现的值给题库卡片的书脊挑颜色
        let value = id.uuidString.unicodeScalars.reduce(0) { $0 + Int($1.value) }
        return abs(value) % 2
    }
}

// MARK: - 模式

enum QuizMode: String, CaseIterable, Codable, Hashable {
    case practice
    case exam

    var title: String {
        switch self {
        case .practice: return "练习"
        case .exam: return "考试"
        }
    }

    var subtitle: String {
        switch self {
        case .practice: return "答完立即看对错"
        case .exam: return "交卷后统一评分"
        }
    }

    var longTitle: String {
        switch self {
        case .practice: return "练习模式"
        case .exam: return "考试模式"
        }
    }
}

enum QuizOrder: String, CaseIterable, Codable, Hashable {
    case random
    case sequential

    var title: String {
        switch self {
        case .random: return "随机"
        case .sequential: return "顺序"
        }
    }

    var subtitle: String {
        switch self {
        case .random: return "所有题打乱后抽取"
        case .sequential: return "从第一题往后做"
        }
    }
}

enum AppTab: String, CaseIterable, Identifiable, Hashable {
    case home
    case library
    case settings

    var id: String { rawValue }

    var title: String {
        switch self {
        case .home: return "主页"
        case .library: return "题库"
        case .settings: return "设置"
        }
    }

    var icon: String {
        switch self {
        case .home: return "house"
        case .library: return "books.vertical"
        case .settings: return "slider.horizontal.3"
        }
    }
}

enum Screen: Equatable {
    case home
    case quiz
    case result
    case wrong
}

// MARK: - 范围

enum QuizScope: Hashable {
    case all
    case bank(UUID)
    case wrong
}

// MARK: - 记录

struct QuestionProgress: Codable {
    var attempts: Int
    var correct: Int

    var rate: Int {
        attempts == 0 ? 0 : Int((Double(correct) / Double(attempts) * 100).rounded())
    }
}

struct Attempt: Codable, Identifiable {
    var id: UUID = UUID()
    let total: Int
    let correct: Int
    let date: Date
    let scope: String

    var percent: Int {
        total == 0 ? 0 : Int((Double(correct) / Double(total) * 100).rounded())
    }
}

// MARK: - 结果

struct ResultItem: Identifiable {
    let question: Question
    let choice: String?

    var id: String { question.id }

    var isCorrect: Bool {
        guard let choice = choice else { return false }
        return choice == question.answer
    }
}

struct ChapterStat: Identifiable {
    let name: String
    let correct: Int
    let total: Int

    var id: String { name }

    var rate: Int {
        total == 0 ? 0 : Int((Double(correct) / Double(total) * 100).rounded())
    }
}

struct QuizResult {
    let total: Int
    let correct: Int
    let unanswered: Int
    let noAnswerCount: Int
    let duration: TimeInterval
    let scopeLabel: String
    let mode: QuizMode
    let items: [ResultItem]
    let chapterStats: [ChapterStat]

    var percent: Int {
        total == 0 ? 0 : Int((Double(correct) / Double(total) * 100).rounded())
    }

    var grade: String {
        if total == 0 { return "暂无评分" }
        switch percent {
        case 90...: return "熟练"
        case 80..<90: return "掌握良好"
        case 60..<80: return "仍需巩固"
        default: return "建议重练"
        }
    }
}

// MARK: - 导入

struct ParsedQuestion {
    let num: String
    let stem: String
    let options: [[String]]
    let answer: String?
}

struct ParsedBank: Identifiable {
    let id = UUID()
    let name: String
    let fileName: String
    let questions: [ParsedQuestion]
    let warnings: [String]

    var questionCount: Int { questions.count }
    var noAnswerCount: Int { questions.filter { question in
        guard let answer = question.answer else { return true }
        return !question.options.contains { $0.first == answer }
    }.count }
}
