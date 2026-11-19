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
    private static let optionStrictRegex = try! NSRegularExpression(pattern: "(?:^|[\\s　])([A-D])\\s?[:：.，,、]\\s?")
    private static let optionLooseRegex = try! NSRegularExpression(pattern: "([A-D])\\s?[:：.，,、]\\s?")
    private static let optionAtStartRegex = try! NSRegularExpression(pattern: "^\\s*([A-D])\\s?[:：.，,、]")
    private static let optionContentRegex = try! NSRegularExpression(pattern: "^([A-D])\\s?[:：.，,、]\\s?(.*)$", options: [.dotMatchesLineSeparators])
    private static let qnumRegex = try! NSRegularExpression(pattern: "^\\s*(\\d+)\\s*[.、:：]?\\s*")

    private static func parseText(_ text: String, name: String, fileName: String) throws -> ParsedBank {
        let lines = text
            .replacingOccurrences(of: "\r\n", with: "\n")
            .replacingOccurrences(of: "\r", with: "\n")
            .components(separatedBy: "\n")
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty }

        guard !lines.isEmpty else { throw ImportError.noQuestions }

        var blocks: [[String]] = []
        var current: [String]?

        func flush() {
            if let current, !current.isEmpty {
                blocks.append(current)
            }
            current = nil
        }

        for index in lines.indices {
            let line = lines[index]
            let next = index + 1 < lines.count ? lines[index + 1] : nil

            if looksLikeChapter(line, next: next) {
                flush()
                continue
            }

            let hasAnswer = answerRegex.firstMatch(in: line, range: fullRange(line)) != nil
                || answerTailRegex.firstMatch(in: line, range: fullRange(line)) != nil
                || emptyParenRegex.firstMatch(in: line, range: fullRange(line)) != nil
            let startsWithOption = optionAtStartRegex.firstMatch(in: line, range: fullRange(line)) != nil
            let startsWithNumber = qnumRegex.firstMatch(in: line, range: fullRange(line)) != nil
            let hasOptionMark = optionStrictRegex.firstMatch(in: line, range: fullRange(line)) != nil
                || optionLooseRegex.firstMatch(in: line, range: fullRange(line)) != nil

            if (startsWithNumber && (hasAnswer || hasOptionMark)) || (hasAnswer && !startsWithOption) {
                flush()
                current = [line]
            } else if current != nil {
                current?.append(line)
            } else if startsWithNumber {
                current = [line]
            }
        }
        flush()

        guard !blocks.isEmpty else { throw ImportError.noQuestions }

        var questions: [ParsedQuestion] = []
        var warnings: [String] = []

        for (index, block) in blocks.enumerated() {
            let first = block[0]
            let raw = block.joined(separator: " ")

            var answer: String?
            var body = raw
            if let match = answerRegex.firstMatch(in: first, range: fullRange(first))
                ?? answerTailRegex.firstMatch(in: first, range: fullRange(first))
                ?? answerRegex.firstMatch(in: raw, range: fullRange(raw)) {
                answer = capture(match, group: 1, in: match.range.location < first.utf16.count ? first : raw)?.uppercased()
                if match.range.location < first.utf16.count {
                    let mutable = NSMutableString(string: first)
                    mutable.replaceCharacters(in: match.range, with: "（　）")
                    let replacedFirst = mutable as String
                    body = replacedFirst + String(raw.dropFirst(first.count))
                }
            }

            var (questionText, optionTexts) = splitOptions(body)
            let number: String
            if let match = qnumRegex.firstMatch(in: questionText, range: fullRange(questionText)) {
                number = (questionText as NSString).substring(with: match.range(at: 1))
                questionText = qnumRegex.stringByReplacingMatches(
                    in: questionText,
                    range: fullRange(questionText),
                    withTemplate: ""
                )
            } else {
                number = "\(index + 1)"
            }

            var stem = questionText
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

            var cleanOptions: [[String]] = []
            for option in optionTexts {
                let ns = option as NSString
                if let match = optionContentRegex.firstMatch(in: option, range: NSRange(location: 0, length: ns.length)) {
                    let key = ns.substring(with: match.range(at: 1)).uppercased()
                    let value = ns.substring(with: match.range(at: 2)).trimmingCharacters(in: .whitespacesAndNewlines)
                    cleanOptions.append([key, value])
                } else {
                    cleanOptions.append(["?", option])
                }
            }

            if cleanOptions.count != 4 {
                warnings.append("第 \(number) 题识别到 \(cleanOptions.count) 个选项")
            }
            if let answer, !cleanOptions.contains(where: { $0.first == answer }) {
                warnings.append("第 \(number) 题的答案 \(answer) 不在选项里")
            }

            questions.append(ParsedQuestion(
                num: number,
                stem: stem,
                options: cleanOptions,
                answer: answer
            ))
        }

        return ParsedBank(
            name: name,
            fileName: fileName,
            questions: questions,
            warnings: Array(Set(warnings)).sorted()
        )
    }

    private static func looksLikeChapter(_ line: String, next: String?) -> Bool {
        let trimmed = line.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty, trimmed.count <= 24, let next else { return false }
        if trimmed.rangeOfCharacter(from: .decimalDigits) != nil { return false }
        if answerRegex.firstMatch(in: trimmed, range: fullRange(trimmed)) != nil { return false }
        if optionStrictRegex.firstMatch(in: trimmed, range: fullRange(trimmed)) != nil { return false }
        if trimmed.contains("？") || trimmed.contains("?") { return false }
        return qnumRegex.firstMatch(in: next, range: fullRange(next)) != nil
    }

    private static func splitOptions(_ text: String) -> (String, [String]) {
        let ns = text as NSString
        for regex in [optionStrictRegex, optionLooseRegex] {
            let matches = regex.matches(in: text, range: fullRange(text))
            guard let firstA = matches.first(where: { capture($0, group: 1, in: text)?.uppercased() == "A" }) else {
                continue
            }
            var chosen: [NSTextCheckingResult] = [firstA]
            var needed = ["B", "C", "D"]
            for match in matches where match.range.location > firstA.range.location {
                guard let letter = capture(match, group: 1, in: text)?.uppercased() else { continue }
                if letter == needed.first {
                    chosen.append(match)
                    needed.removeFirst()
                    if needed.isEmpty { break }
                }
            }
            guard chosen.count == 4 else { continue }

            let starts = chosen.map { $0.range(at: 1).location }
            let bounds = starts + [ns.length]
            var options: [String] = []
            for index in 0..<4 {
                let length = bounds[index + 1] - bounds[index]
                let value = ns.substring(with: NSRange(location: bounds[index], length: length))
                    .trimmingCharacters(in: .whitespacesAndNewlines)
                options.append(value)
            }
            guard options.allSatisfy({ !$0.isEmpty }) else { continue }
            let stem = ns.substring(to: starts[0]).trimmingCharacters(in: .whitespacesAndNewlines)
            return (stem, options)
        }
        return (text.trimmingCharacters(in: .whitespacesAndNewlines), [])
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

            if options.count != 4 {
                warnings.append("第 \(number) 题有 \(options.count) 个选项")
            }
            questions.append(ParsedQuestion(num: number, stem: stem, options: options, answer: answer))
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
