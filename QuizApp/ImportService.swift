import Foundation
import ZIPFoundation

enum ImportService {
    enum ImportError: LocalizedError {
        case unsupported(String)
        case unreadable(String)
        case noQuestions
        case backupFile

        var errorDescription: String? {
            switch self {
            case .unsupported(let ext):
                return "暂时不支持 .\(ext) 文件，请用 DOCX、TXT、MD 或 JSON"
            case .unreadable(let reason):
                return reason
            case .noQuestions:
                return "没有解析到题目，请检查文档里是否有「1. 题干（A）」和 A/B/C/D 选项"
            case .backupFile:
                return "这是题库备份文件，请到「设置 → 从备份恢复」里导入"
            }
        }
    }

    static func parse(url: URL) throws -> ParsedBank {
        let ext = url.pathExtension.lowercased()
        switch ext {
        case "docx":
            let text = try docxText(url)
            return try parseText(text, name: url.deletingPathExtension().lastPathComponent, fileName: url.lastPathComponent)
        case "txt", "md", "markdown":
            guard let text = try? String(contentsOf: url, encoding: .utf8) else {
                throw ImportError.unreadable("文本文件读取失败，请确认是 UTF-8 编码")
            }
            return try parseText(text, name: url.deletingPathExtension().lastPathComponent, fileName: url.lastPathComponent)
        case "json":
            return try parseJSON(url)
        default:
            throw ImportError.unsupported(ext.isEmpty ? "未知" : ext)
        }
    }

    // MARK: - DOCX

    private static func docxText(_ url: URL) throws -> String {
        var failures: [String] = []

        // DOCX 本质是 ZIP，优先直接读 word/document.xml，不依赖系统 Word 转换能力。
        if let archive = Archive(url: url, accessMode: .read) {
            if let entry = archive["word/document.xml"] {
                do {
                    var xmlData = Data()
                    try archive.extract(entry, consumer: { chunk in
                        xmlData.append(chunk)
                    })
                    let text = try xmlText(from: xmlData)
                    if !text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                        return text
                    }
                    failures.append("document.xml 里没有文字")
                } catch {
                    failures.append("document.xml 读取失败：\(error.localizedDescription)")
                }
            } else {
                failures.append("DOCX 里没有 word/document.xml")
            }
        } else {
            failures.append("不是有效的 DOCX 压缩包")
        }

        let detail = failures.joined(separator: "；")
        throw ImportError.unreadable("Word 文档解析失败（\(detail)）")
    }

    private static func xmlText(from data: Data) throws -> String {
        let parser = XMLParser(data: data)
        let delegate = DocxXMLTextParser()
        parser.delegate = delegate
        guard parser.parse() else {
            let reason = parser.parserError?.localizedDescription ?? "未知错误"
            throw ImportError.unreadable("Word 文档 XML 解析失败：\(reason)")
        }
        return delegate.text.replacingOccurrences(of: "\u{00a0}", with: " ")
    }

    // MARK: - 文本解析

    private static let answerRegex = try! NSRegularExpression(pattern: "[（(]\\s*([A-Da-d])\\s*[）)]")
    private static let answerTailRegex = try! NSRegularExpression(pattern: "[（(]\\s*([A-Da-d])\\s*$")
    private static let emptyParenRegex = try! NSRegularExpression(pattern: "[（(]\\s*[）)]")
    private static let optionLabelRegex = try! NSRegularExpression(pattern: "(?:^|[\\s　])([A-D])\\s*(?:[、.．,，:：])?\\s*")
    private static let optionPrefixRegex = try! NSRegularExpression(pattern: "^([A-D])\\s*(?:[、.．,，:：])?\\s*(.*)$", options: [.dotMatchesLineSeparators])
    private static let optionContentRegex = try! NSRegularExpression(pattern: "^([A-D])\\s*(?:[、.．,，:：])?\\s*(.*)$", options: [.dotMatchesLineSeparators])
    private static let qnumRegex = try! NSRegularExpression(pattern: "^\\s*(\\d+)\\s*[.、:：]?\\s*")
    private static let sectionBreakRegex = try! NSRegularExpression(pattern: "^\\s*[一二三四五六七八九十]+\\s*[、.．]?\\s*(填空题|判断题|简答题|问答题|论述题)")
    private static let rawParenRegex = try! NSRegularExpression(pattern: "[（(]\\s*([^）)]{1,8})\\s*[）)]")
    private static let sectionHeadingRegex = try! NSRegularExpression(pattern: "^\\s*(?:[一二三四五六七八九十]+\\s*[、.．]?\\s*)?(选择题|单项选择题|多项选择题|判断题|填空题|简答题|问答题|论述题|参考答案|答案速查|答案)(?:\\s|$|[（(：:])")
    private static let judgmentAnswerRegex = try! NSRegularExpression(pattern: "[（(]\\s*(对|错|正确|错误|√|×|T|F|true|false)\\s*[）)]", options: [.caseInsensitive])
    private static let answerKeyRangeRegex = try! NSRegularExpression(pattern: "(\\d{1,3})\\s*[-—~至]\\s*(\\d{1,3})\\s*[：:、.．]?\\s*([A-Da-d]{2,})")
    private static let answerKeyPairRegex = try! NSRegularExpression(pattern: "(\\d{1,3})\\s*[.、:：]\\s*([A-Da-d]|对|错|正确|错误|√|×|T|F)", options: [.caseInsensitive])
    private static let answerKeyLineRegex = try! NSRegularExpression(pattern: "^\\s*(\\d{1,3})\\s*[.、:：]\\s*(.+)$", options: [.dotMatchesLineSeparators])
    private static let fillParenRegex = try! NSRegularExpression(pattern: "[（(]\\s*([^）)]{1,80})\\s*[）)]")
    private static let embeddedAnswerKeyRegex = try! NSRegularExpression(pattern: "\\s\\d{1,3}[.、]")

    private struct ImportOption {
        let key: String
        let value: String
    }

    private struct ImportBlock {
        let question: ParsedQuestion
        let warnings: [String]
    }

    private struct LabelHit {
        let key: String
        let start: Int
    }

    private struct RawSection {
        let kind: String
        var lines: [String]
    }

    private static func parseText(_ text: String, name: String, fileName: String) throws -> ParsedBank {
        let rawLines = text
            .replacingOccurrences(of: "\r\n", with: "\n")
            .replacingOccurrences(of: "\r", with: "\n")
            .components(separatedBy: "\n")
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty }

        guard !rawLines.isEmpty else { throw ImportError.noQuestions }

        let sections = splitSections(rawLines)
        var questions: [ParsedQuestion] = []
        var warnings: [String] = []
        var answerLines: [String]?

        for section in sections {
            switch section.kind {
            case "choice", "judgment":
                var lines: [String] = []
                for line in section.lines {
                    lines.append(contentsOf: splitEmbeddedQuestions(line))
                }
                let blocks = buildBlocks(lines)
                for block in blocks {
                    let parsed = section.kind == "judgment"
                        ? parseJudgmentBlock(block, fallbackNumber: questions.count + 1)
                        : parseBlock(block, fallbackNumber: questions.count + 1)
                    questions.append(parsed.question)
                    warnings.append(contentsOf: parsed.warnings)
                }
            case "fill":
                let blocks = buildFillBlocks(section.lines)
                for block in blocks {
                    questions.append(parseFillBlock(block, fallbackNumber: questions.count + 1))
                }
            case "answers":
                if answerLines == nil { answerLines = [] }
                answerLines?.append(contentsOf: section.lines)
            default:
                continue
            }
        }

        guard !questions.isEmpty else { throw ImportError.noQuestions }
        if let answerLines { applyAnswerKey(&questions, lines: answerLines) }

        return ParsedBank(
            name: name,
            fileName: fileName,
            questions: questions,
            warnings: Array(Set(warnings)).sorted()
        )
    }

    private static func splitSections(_ lines: [String]) -> [RawSection] {
        var sections: [RawSection] = []
        var current = RawSection(kind: "choice", lines: [])
        var hasHeading = false

        func flush() {
            if !current.lines.isEmpty { sections.append(current) }
        }

        for line in lines {
            if let heading = sectionHeadingRegex.firstMatch(in: line, range: fullRange(line)),
               qnumRegex.firstMatch(in: line, range: fullRange(line)) == nil {
                flush()
                hasHeading = true
                let title = capture(heading, group: 1, in: line) ?? ""
                current = RawSection(kind: kind(for: title), lines: [])
                continue
            }
            current.lines.append(line)
        }
        flush()

        if !hasHeading { return [RawSection(kind: "choice", lines: lines)] }
        return sections
    }

    private static func kind(for title: String) -> String {
        switch title {
        case "选择题", "单项选择题", "多项选择题": return "choice"
        case "判断题": return "judgment"
        case "填空题": return "fill"
        case "参考答案", "答案速查", "答案": return "answers"
        default: return "ignore"
        }
    }

    private static func buildBlocks(_ lines: [String]) -> [[String]] {
        var blocks: [[String]] = []
        var current: [String]?
        var currentHasAnswer = false

        func flush() {
            if let current, !current.isEmpty { blocks.append(current) }
            current = nil
            currentHasAnswer = false
        }

        for line in lines {
            let hasAnswer = answerRegex.firstMatch(in: line, range: fullRange(line)) != nil
                || answerTailRegex.firstMatch(in: line, range: fullRange(line)) != nil
                || emptyParenRegex.firstMatch(in: line, range: fullRange(line)) != nil
                || judgmentAnswerRegex.firstMatch(in: line, range: fullRange(line)) != nil
            let startsWithOption = startsWithOptionLabel(line)
            let startsWithNumber = qnumRegex.firstMatch(in: line, range: fullRange(line)) != nil

            if current == nil {
                if startsWithNumber || hasAnswer || startsWithOption {
                    current = [line]
                    currentHasAnswer = hasAnswer
                }
                continue
            }

            if startsWithNumber {
                flush()
                current = [line]
                currentHasAnswer = hasAnswer
            } else if hasAnswer && !startsWithOption && !currentHasAnswer {
                current?.append(line)
                currentHasAnswer = true
            } else if hasAnswer && !startsWithOption {
                flush()
                current = [line]
                currentHasAnswer = true
            } else {
                current?.append(line)
            }
        }
        flush()
        return blocks
    }

    private static func buildFillBlocks(_ lines: [String]) -> [[String]] {
        var blocks: [[String]] = []
        var current: [String]?
        for line in lines {
            if qnumRegex.firstMatch(in: line, range: fullRange(line)) != nil {
                if let current { blocks.append(current) }
                current = [line]
            } else if current != nil {
                current?.append(line)
            }
        }
        if let current { blocks.append(current) }
        return blocks
    }

    private static func parseJudgmentBlock(_ block: [String], fallbackNumber: Int) -> ImportBlock {
        let first = block[0]
        let all = block.joined(separator: " ")
        let number = qnumRegex.firstMatch(in: first, range: fullRange(first))
            .flatMap { capture($0, group: 1, in: first) } ?? "\(fallbackNumber)"
        var stem = all
        if let match = qnumRegex.firstMatch(in: stem, range: fullRange(stem)) {
            stem = (stem as NSString).substring(from: match.range.location + match.range.length)
        }

        let token = judgmentAnswerRegex.firstMatch(in: stem, range: fullRange(stem))
            .flatMap { capture($0, group: 1, in: stem) }
            ?? answerRegex.firstMatch(in: stem, range: fullRange(stem))
                .flatMap { capture($0, group: 1, in: stem) }
        let answer: String?
        switch token?.lowercased() ?? "" {
        case "a", "对", "正确", "√", "t", "true": answer = "A"
        case "b", "错", "错误", "×", "f", "false": answer = "B"
        default: answer = nil
        }
        stem = judgmentAnswerRegex.stringByReplacingMatches(in: stem, range: fullRange(stem), withTemplate: "（　）")
        stem = answerRegex.stringByReplacingMatches(in: stem, range: fullRange(stem), withTemplate: "（　）")
        stem = answerTailRegex.stringByReplacingMatches(in: stem, range: fullRange(stem), withTemplate: "（　）")
        stem = emptyParenRegex.stringByReplacingMatches(in: stem, range: fullRange(stem), withTemplate: "（　）")
        stem = stem.replacingOccurrences(of: "\t", with: " ")
        while stem.contains("  ") { stem = stem.replacingOccurrences(of: "  ", with: " ") }
        stem = stem.trimmingCharacters(in: .whitespacesAndNewlines)
        if stem.hasSuffix("，") || stem.hasSuffix(",") { stem.removeLast() }

        return ImportBlock(
            question: ParsedQuestion(
                num: number,
                stem: stem,
                options: [["A", "正确"], ["B", "错误"]],
                answer: answer,
                kind: "judgment"
            ),
            warnings: []
        )
    }

    private static func parseFillBlock(_ block: [String], fallbackNumber: Int) -> ParsedQuestion {
        let first = block[0]
        let rest = Array(block.dropFirst()).joined(separator: " ").trimmingCharacters(in: .whitespacesAndNewlines)
        let number = qnumRegex.firstMatch(in: first, range: fullRange(first))
            .flatMap { capture($0, group: 1, in: first) } ?? "\(fallbackNumber)"
        var stem = first
        if let match = qnumRegex.firstMatch(in: stem, range: fullRange(stem)) {
            stem = (stem as NSString).substring(from: match.range.location + match.range.length)
        }
        if !rest.isEmpty { stem += " " + rest }

        var answers: [String] = []
        let nsStem = stem as NSString
        let matches = fillParenRegex.matches(in: stem, range: fullRange(stem))
        var rebuilt = ""
        var last = 0
        for match in matches {
            rebuilt += nsStem.substring(with: NSRange(location: last, length: match.range.location - last))
            if let value = capture(match, group: 1, in: stem)?.trimmingCharacters(in: .whitespacesAndNewlines),
               !value.isEmpty {
                answers.append(value)
            }
            rebuilt += "（　）"
            last = match.range.location + match.range.length
        rebuilt += nsStem.substring(from: last)
        }
        stem = rebuilt.replacingOccurrences(of: "\t", with: " ")
        while stem.contains("  ") { stem = stem.replacingOccurrences(of: "  ", with: " ") }
        stem = stem.trimmingCharacters(in: .whitespacesAndNewlines)
        if stem.hasSuffix("，") || stem.hasSuffix(",") { stem.removeLast() }

        // 没有括号时，答案可能直接写在“有……”后面。
        if answers.isEmpty, let marker = stem.range(of: "有", options: .backwards) {
            let tail = String(stem[marker.upperBound...]).trimmingCharacters(in: .whitespacesAndNewlines)
            if (1...40).contains(tail.count) {
                answers.append(String(tail))
                stem = String(stem[..<marker.upperBound]) + "（　）"
            }
        }

        return ParsedQuestion(
            num: number,
            stem: stem,
            options: [],
            answer: answers.joined(separator: "、").isEmpty ? nil : answers.joined(separator: "、"),
            kind: "fill"
        )
    }

    private static func applyAnswerKey(_ questions: inout [ParsedQuestion], lines: [String]) {
        var map: [String: String] = [:]
        let joined = lines.joined(separator: "\n")
        for match in answerKeyRangeRegex.matches(in: joined, range: fullRange(joined)) {
            guard let startText = capture(match, group: 1, in: joined),
                  let endText = capture(match, group: 2, in: joined),
                  let start = Int(startText), let end = Int(endText),
                  let letters = capture(match, group: 3, in: joined)?.uppercased(),
                  end - start + 1 == letters.count else { continue }
            for (offset, char) in letters.enumerated() {
                map["\(start + offset)"] = String(char)
            }
        }
        for line in lines {
            for match in answerKeyPairRegex.matches(in: line, range: fullRange(line)) {
                if let num = capture(match, group: 1, in: line),
                   let token = capture(match, group: 2, in: line) {
                    map[num] = token
                }
            }
            if let match = answerKeyLineRegex.firstMatch(in: line, range: fullRange(line)),
               let num = capture(match, group: 1, in: line),
               let rest = capture(match, group: 2, in: line)?.trimmingCharacters(in: .whitespacesAndNewlines),
               !rest.isEmpty,
               embeddedAnswerKeyRegex.firstMatch(in: rest, range: fullRange(rest)) == nil {
                if map[num] == nil { map[num] = rest }
            }
        }

        for index in questions.indices {
            guard let token = map[questions[index].num] else { continue }
            if questions[index].kind == "fill" {
                if (questions[index].answer ?? "").trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                    questions[index] = ParsedQuestion(
                        num: questions[index].num,
                        stem: questions[index].stem,
                        options: questions[index].options,
                        answer: token.trimmingCharacters(in: .whitespacesAndNewlines),
                        kind: "fill"
                    )
                }
            } else if questions[index].kind == "judgment" {
                if questions[index].answer == nil {
                    let mapped: String?
                    switch token.lowercased() {
                    case "a", "对", "正确", "√", "t", "true": mapped = "A"
                    case "b", "错", "错误", "×", "f", "false": mapped = "B"
                    default: mapped = nil
                    }
                    if let mapped {
                        questions[index] = ParsedQuestion(
                            num: questions[index].num,
                            stem: questions[index].stem,
                            options: questions[index].options,
                            answer: mapped,
                            kind: "judgment"
                        )
                    }
                }
            } else if questions[index].answer == nil {
                let mapped = token.first.map { String($0).uppercased() }
                if let mapped, ["A", "B", "C", "D"].contains(mapped) {
                    questions[index] = ParsedQuestion(
                        num: questions[index].num,
                        stem: questions[index].stem,
                        options: questions[index].options,
                        answer: mapped,
                        kind: "choice"
                    )
                }
            }
        }
    }

    private static func startsWithOptionLabel(_ line: String) -> Bool {
        let trimmed = line.trimmingCharacters(in: .whitespaces)
        guard let first = trimmed.first, "ABCD".contains(first) else { return false }
        if trimmed.count == 1 { return true }
        let second = trimmed[trimmed.index(after: trimmed.startIndex)]
        return second.isWhitespace || "、.．,，:：".contains(second)
    }

    /// 有些 Word 文档会把下一题直接塞进上一题的 D 选项后面，例如
    /// `D、43.下一题（B）`。这里在数字题号前切开，避免整道题被吞掉。
    private static func splitEmbeddedQuestions(_ line: String) -> [String] {
        let ns = line as NSString
        let marker = try! NSRegularExpression(pattern: "(?<!\\d)(\\d{1,3})[.、]\\s*(?=[^\\d])")
        let matches = marker.matches(in: line, range: fullRange(line))
        guard !matches.isEmpty else { return [line] }

        var pieces: [String] = []
        var start = 0
        for match in matches {
            let pos = match.range.location
            if pos == 0 { continue }
            let after = ns.substring(from: pos)
            if answerRegex.firstMatch(in: after, range: fullRange(after)) == nil { continue }
            let before = ns.substring(with: NSRange(location: start, length: pos - start))
                .trimmingCharacters(in: .whitespacesAndNewlines)
            if before.isEmpty { continue }
            pieces.append(before)
            start = pos
        }
        if start == 0 { return [line] }
        pieces.append(ns.substring(from: start).trimmingCharacters(in: .whitespacesAndNewlines))
        return pieces.filter { !$0.isEmpty }
    }

    private static func parseBlock(_ block: [String], fallbackNumber: Int) -> ImportBlock {
        let first = block[0]
        let nsFirst = first as NSString
        var warningList: [String] = []

        let answerMatch = answerRegex.firstMatch(in: first, range: fullRange(first))
            ?? answerTailRegex.firstMatch(in: first, range: fullRange(first))
        let answerEnd = answerMatch.map { $0.range.location + $0.range.length } ?? 0
        let afterAnswer = answerMatch == nil ? first : nsFirst.substring(from: answerEnd)
        let inlineLabel = optionLabelRegex.firstMatch(in: afterAnswer, range: fullRange(afterAnswer))
        let inlineStart = inlineLabel.map { $0.range(at: 1).location }
        let inlineKey = inlineLabel.flatMap { capture($0, group: 1, in: afterAnswer)?.uppercased() }
        let hasInlineOptions = inlineStart != nil

        var stemFirst: String
        if !hasInlineOptions {
            stemFirst = first
        } else if inlineKey == "A", let inlineStart {
            stemFirst = nsFirst.substring(to: min(answerEnd + inlineStart, nsFirst.length))
        } else {
            stemFirst = nsFirst.substring(to: min(answerEnd, nsFirst.length))
        }

        let rest = Array(block.dropFirst())
        let restText = rest.joined(separator: "\n")
        var stemExtra = ""
        var optionSource = ""

        if hasInlineOptions {
            optionSource = afterAnswer.trimmingCharacters(in: .whitespacesAndNewlines)
            if !restText.isEmpty {
                optionSource += "\n" + restText
            }
        } else if !restText.isEmpty {
            if let firstRestLabel = optionLabelRegex.firstMatch(in: restText, range: fullRange(restText)) {
                let restNS = restText as NSString
                let labelStart = firstRestLabel.range(at: 1).location
                let key = capture(firstRestLabel, group: 1, in: restText)?.uppercased() ?? ""
                let prefix = restNS.substring(with: NSRange(location: 0, length: labelStart))
                    .trimmingCharacters(in: .whitespacesAndNewlines)
                if key == "A" && !prefix.isEmpty {
                    stemExtra = prefix
                    optionSource = restNS.substring(from: labelStart)
                } else {
                    optionSource = restText
                }
            } else if rest.count >= 2 {
                optionSource = restText
            } else {
                stemExtra = restText
            }
        }

        let options = extractOptions(optionSource, restLineCount: rest.count)
        let answer = answerMatch.flatMap { capture($0, group: 1, in: first)?.uppercased() }

        var number = "\(fallbackNumber)"
        if let numMatch = qnumRegex.firstMatch(in: first, range: fullRange(first)),
           let value = capture(numMatch, group: 1, in: first) {
            number = value
            stemFirst = qnumRegex.stringByReplacingMatches(
                in: stemFirst,
                range: fullRange(stemFirst),
                withTemplate: ""
            )
        }

        var stem = stemFirst
        if !stemExtra.isEmpty {
            stem += " " + stemExtra
        }
        stem = answerRegex.stringByReplacingMatches(in: stem, range: fullRange(stem), withTemplate: "（　）")
        stem = answerTailRegex.stringByReplacingMatches(in: stem, range: fullRange(stem), withTemplate: "（　）")
        stem = emptyParenRegex.stringByReplacingMatches(in: stem, range: fullRange(stem), withTemplate: "（　）")
        stem = stem.replacingOccurrences(of: "（　）", with: "（　）")
        stem = stem.replacingOccurrences(of: "\t", with: " ")
        stem = stem.replacingOccurrences(of: "  ", with: " ")
        while stem.contains("  ") {
            stem = stem.replacingOccurrences(of: "  ", with: " ")
        }
        stem = stem.trimmingCharacters(in: .whitespacesAndNewlines)
        if stem.hasSuffix("，") || stem.hasSuffix(",") {
            stem.removeLast()
        }

        var finalAnswer = answer
        if finalAnswer == nil {
            if let rawParen = rawParenRegex.firstMatch(in: first, range: fullRange(first)),
               let token = capture(rawParen, group: 1, in: first)?.trimmingCharacters(in: .whitespacesAndNewlines),
               !token.isEmpty,
               let hit = options.firstIndex(where: { $0.value.hasPrefix(token) }) {
                finalAnswer = options[hit].key
            }
        }

        if options.count < 2 {
            warningList.append("第 \(number) 题识别到 \(options.count) 个选项")
        }
        if let finalAnswer, !options.contains(where: { $0.key == finalAnswer }) {
            warningList.append("第 \(number) 题的答案 \(finalAnswer) 不在选项里")
        }

        let cleanOptions = options.map { [$0.key, $0.value] }
        return ImportBlock(
            question: ParsedQuestion(num: number, stem: stem, options: cleanOptions, answer: finalAnswer, kind: "choice"),
            warnings: warningList
        )
    }

    private static func extractOptions(_ text: String, restLineCount: Int) -> [ImportOption] {
        guard !text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return [] }
        let ns = text as NSString
        let matches = optionLabelRegex.matches(in: text, range: fullRange(text))
        let hits: [LabelHit] = matches.compactMap { match in
            guard let key = capture(match, group: 1, in: text)?.uppercased() else { return nil }
            return LabelHit(key: key, start: match.range(at: 1).location)
        }

        let firstA = hits.first { $0.key == "A" }
        let firstB = hits.first { $0.key == "B" }
        guard let start = firstA ?? firstB else {
            let lines = text.components(separatedBy: "\n")
                .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
                .filter { !$0.isEmpty }
            if (2...4).contains(lines.count) {
                return lines.enumerated().map { index, value in
                    ImportOption(key: ["A", "B", "C", "D"][index], value: value)
                }
            }
            return []
        }

        var chosen: [LabelHit] = []
        var virtualA: String?
        var expected: [String] = []
        if firstA == nil, let firstB {
            virtualA = ns.substring(to: firstB.start).trimmingCharacters(in: .whitespacesAndNewlines)
            if virtualA?.isEmpty == true { virtualA = nil }
            chosen.append(firstB)
            expected = ["C", "D"]
        } else {
            chosen.append(start)
            expected = ["B", "C", "D"]
        }

        for hit in hits where hit.start > chosen[chosen.count - 1].start {
            if hit.key == "A" { break }
            if hit.key == expected.first {
                chosen.append(hit)
                expected.removeFirst()
                if expected.isEmpty { break }
            }
        }

        var parts: [ImportOption] = []
        if let virtualA {
            parts.append(ImportOption(key: "A", value: virtualA))
        }
        for index in chosen.indices {
            let startPos = chosen[index].start
            let endPos = index + 1 < chosen.count ? chosen[index + 1].start : ns.length
            let raw = ns.substring(with: NSRange(location: startPos, length: endPos - startPos))
                .trimmingCharacters(in: .whitespacesAndNewlines)
            let key: String
            if virtualA != nil {
                key = ["B", "C", "D"][index]
            } else {
                key = chosen[index].key
            }
            let value = cleanOptionValue(raw, key: key)
            if !value.isEmpty {
                parts.append(ImportOption(key: key, value: value))
            }
        }

        if parts.count < 2 && restLineCount >= 2 {
            let lines = text.components(separatedBy: "\n")
                .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
                .filter { !$0.isEmpty }
            if (2...4).contains(lines.count) {
                return lines.enumerated().map { index, value in
                    ImportOption(key: ["A", "B", "C", "D"][index], value: value)
                }
            }
        }
        return parts
    }

    private static func cleanOptionValue(_ raw: String, key: String) -> String {
        guard let match = optionPrefixRegex.firstMatch(in: raw, range: fullRange(raw)),
              let hitKey = capture(match, group: 1, in: raw)?.uppercased(),
              hitKey == key,
              let value = capture(match, group: 2, in: raw)
        else { return raw.trimmingCharacters(in: .whitespacesAndNewlines) }
        return value.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private static func capture(_ match: NSTextCheckingResult, group: Int, in text: String) -> String? {
        guard match.numberOfRanges > group else { return nil }
        let range = match.range(at: group)
        guard range.location != NSNotFound else { return nil }
        return (text as NSString).substring(with: range)
    }

    private static func fullRange(_ text: String) -> NSRange {
        NSRange(location: 0, length: (text as NSString).length)
    }

    // MARK: - JSON

    private static func parseJSON(_ url: URL) throws -> ParsedBank {
        let data = try Data(contentsOf: url)
        let object = try JSONSerialization.jsonObject(with: data)
        let fallbackName = url.deletingPathExtension().lastPathComponent
        var name = fallbackName
        var items: [[String: Any]] = []

        if let array = object as? [[String: Any]] {
            items = array
        } else if let dictionary = object as? [String: Any] {
            if dictionary["banks"] != nil {
                throw ImportError.backupFile
            }
            if let value = dictionary["name"] as? String, !value.isEmpty {
                name = value
            }
            if let array = dictionary["questions"] as? [[String: Any]] {
                items = array
            } else {
                throw ImportError.noQuestions
            }
        } else {
            throw ImportError.noQuestions
        }

        guard !items.isEmpty else { throw ImportError.noQuestions }
        var questions: [ParsedQuestion] = []
        var warnings: [String] = []

        for (index, item) in items.enumerated() {
            let number: String
            if let value = item["num"] {
                number = String(describing: value)
            } else {
                number = "\(index + 1)"
            }
            let stem = (item["stem"] as? String)
                ?? (item["question"] as? String)
                ?? (item["title"] as? String)
                ?? ""
            var options: [[String]] = []
            if let value = item["options"] as? [[String]] {
                options = value.map { pair in
                    guard pair.count >= 2 else { return ["?", pair.first ?? ""] }
                    return [pair[0].uppercased(), pair[1]]
                }
            } else if let value = item["options"] as? [String] {
                options = value.map { raw in
                        let ns = raw as NSString
                        if let match = optionContentRegex.firstMatch(in: raw, range: NSRange(location: 0, length: ns.length)) {
                            return [ns.substring(with: match.range(at: 1)).uppercased(),
                                    ns.substring(with: match.range(at: 2)).trimmingCharacters(in: .whitespacesAndNewlines)]
                        }
                        return ["?", raw]
                    }
            }

            var answer: String?
            if let raw = item["answer"] as? String {
                let trimmed = raw.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
                if trimmed.count == 1, "ABCD".contains(trimmed) {
                    answer = trimmed
                } else if let match = answerRegex.firstMatch(in: raw, range: fullRange(raw)) {
                    answer = capture(match, group: 1, in: raw)?.uppercased()
                }
            }

            let kind = (item["kind"] as? String) ?? (item["type"] as? String) ?? "choice"
            if options.count < 2 && kind != "fill" {
                warnings.append("第 \(number) 题有 \(options.count) 个选项")
            }
            questions.append(ParsedQuestion(num: number, stem: stem, options: options, answer: answer, kind: kind))
        }

        return ParsedBank(name: name, fileName: url.lastPathComponent, questions: questions, warnings: warnings)
    }
}


private final class DocxXMLTextParser: NSObject, XMLParserDelegate {
    var text = ""
    private var inText = false
    private var currentParagraph = ""

    func parser(_ parser: XMLParser,
                didStartElement elementName: String,
                namespaceURI: String?,
                qualifiedName qName: String?,
                attributes attributeDict: [String: String] = [:]) {
        let name = localName(elementName)
        switch name {
        case "t":
            inText = true
        case "tab":
            currentParagraph += "\t"
        case "br", "cr":
            currentParagraph += "\n"
        default:
            break
        }
    }

    func parser(_ parser: XMLParser, foundCharacters string: String) {
        guard inText else { return }
        currentParagraph += string
    }

    func parser(_ parser: XMLParser,
                didEndElement elementName: String,
                namespaceURI: String?,
                qualifiedName qName: String?) {
        let name = localName(elementName)
        if name == "t" {
            inText = false
        } else if name == "p" {
            text += currentParagraph + "\n"
            currentParagraph = ""
        }
    }

    private func localName(_ name: String) -> String {
        name.split(separator: ":").last.map(String.init) ?? name
    }
}
