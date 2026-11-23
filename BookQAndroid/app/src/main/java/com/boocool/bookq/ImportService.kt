package com.boocool.bookq

import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipInputStream

class ImportException(message: String) : Exception(message)

object ImportService {
    private const val ANSWER_GROUP_PREFIX = "\u0001GROUP:"

    fun parse(file: File, displayName: String = file.name): ParsedBank {
        val ext = file.extension.lowercase()
        val baseName = displayName.substringBeforeLast('.', displayName)
        return when (ext) {
            "docx" -> parseText(parseDocx(file), baseName, displayName)
            "txt", "md", "markdown" -> {
                val text = try {
                    file.readText(Charsets.UTF_8)
                } catch (e: Exception) {
                    throw ImportException("文本文件读取失败，请确认是 UTF-8 编码")
                }
                parseText(text, baseName, displayName)
            }
            "json" -> parseJson(file, baseName, displayName)
            else -> throw ImportException("暂时不支持 .$ext 文件，请用 DOCX、TXT、MD 或 JSON")
        }
    }

    // MARK: DOCX

    private fun parseDocx(file: File): String {
        try {
            ZipInputStream(BufferedInputStream(FileInputStream(file))).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (entry.name == "word/document.xml") {
                        val bytes = zip.readBytes()
                        val text = parseDocxXml(bytes)
                        if (text.isNotBlank()) return text
                        throw ImportException("Word 文档里没有可读取的文字")
                    }
                }
            }
        } catch (e: ImportException) {
            throw e
        } catch (e: Exception) {
            throw ImportException("Word 文档解析失败：${e.message ?: "文件可能已损坏"}")
        }
        throw ImportException("DOCX 里没有 word/document.xml，可能不是有效的 Word 文档")
    }

    private fun parseDocxXml(bytes: ByteArray): String {
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(ByteArrayInputStream(bytes), "UTF-8")
        val out = StringBuilder()
        val paragraph = StringBuilder()
        var event = parser.eventType

        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    when (parser.name?.substringAfter(':') ?: "") {
                        "t" -> paragraph.append(parser.nextText())
                        "tab" -> paragraph.append('\t')
                        "br", "cr" -> paragraph.append('\n')
                    }
                }
                XmlPullParser.END_TAG -> {
                    if ((parser.name?.substringAfter(':') ?: "") == "p") {
                        out.append(paragraph).append('\n')
                        paragraph.setLength(0)
                    }
                }
            }
            event = parser.next()
        }
        return out.toString().replace('\u00a0', ' ')
    }

    // MARK: Text

    private val answerRegex = Regex("[（(]\\s*([A-Da-d])\\s*[）)]")
    private val answerTailRegex = Regex("[（(]\\s*([A-Da-d])\\s*$")
    private val emptyParenRegex = Regex("[（(]\\s*[）)]")
    private val optionLabelRegex = Regex("(?:^|[\\s　])([A-D])\\s*(?:[、.．,，:：])?\\s*")
    private val optionPrefixRegex = Regex("^([A-D])\\s*(?:[、.．,，:：])?\\s*(.*)$", RegexOption.DOT_MATCHES_ALL)
    private val optionContentRegex = Regex("^([A-D])\\s*(?:[、.．,，:：])?\\s*(.*)$", RegexOption.DOT_MATCHES_ALL)
    private val qnumRegex = Regex("^\\s*(\\d+)\\s*[.、:：]?\\s*")
    private val sectionBreakRegex = Regex("^\\s*[一二三四五六七八九十]+\\s*[、.．]?\\s*(填空题|判断题|简答题|问答题|论述题)")

    private data class OptionPart(val key: String, val value: String)
    private data class ParsedBlock(val question: ParsedQuestion, val warnings: List<String>)

    private val sectionHeadingRegex = Regex(
        "^\\s*(?:[一二三四五六七八九十]+\\s*[、.．]?\\s*)?(选择题|单项选择题|多项选择题|判断题|填空题|简答题|问答题|论述题|参考答案|答案速查|标准答案|正确答案|答案)(?:\\s*(?:及|与|和)?\\s*解析)?(?:\\s*[（(][^）)]*[）)])?(?:\\s|$|[：:、.．])"
    )
    private val answerGroupHeadingRegex = Regex(
        "^\\s*(?:[一二三四五六七八九十]+\\s*[、.．]?\\s*)?(选择题|单项选择题|多项选择题|判断题|填空题|简答题|问答题|论述题)(?:\\s|$|[（(：:、.．])"
    )
    private val judgmentAnswerRegex = Regex("[（(]\\s*(对|错|正确|错误|√|×|T|F|true|false)\\s*[）)]", RegexOption.IGNORE_CASE)
    private val answerKeyRangeRegex = Regex(
        "(\\d{1,3})\\s*[-—~至－]\\s*(\\d{1,3})\\s*[：:、.．]?\\s*([A-Da-dＡ-Ｄａ-ｄ][A-Da-dＡ-Ｄａ-ｄ、,，/／|；;\\s]*)"
    )
    private val answerKeyPairRegex = Regex(
        "(\\d{1,3})\\s*(?:[.、:：．,，]|[-—－]|\\s|[（(])\\s*([A-Da-dＡ-Ｄａ-ｄ]|对|错|正确|错误|√|×|T|F)\\s*[）)]?",
        RegexOption.IGNORE_CASE
    )
    private val answerKeyLineRegex = Regex("^\\s*(\\d{1,3})\\s*([.、:：．]|\\s)\\s*(.+)$", RegexOption.DOT_MATCHES_ALL)

    private data class RawSection(val kind: String, val lines: MutableList<String>)
    private data class FillAnswer(val text: String, val stem: String)

    private fun parseText(text: String, name: String, fileName: String): ParsedBank {
        val rawLines = text
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .split('\n')
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (rawLines.isEmpty()) throw ImportException("这个文件里没有可读取的题目")

        val sections = splitSections(rawLines)
        val questions = mutableListOf<ParsedQuestion>()
        val warnings = mutableListOf<String>()
        var answerLines: MutableList<String>? = null

        for (section in sections) {
            when (section.kind) {
                "choice", "judgment" -> {
                    val lines = mutableListOf<String>()
                    section.lines.forEach { line ->
                        if (section.kind == "choice" || section.kind == "judgment") {
                            lines.addAll(splitEmbeddedQuestions(line))
                        }
                    }
                    val blocks = buildBlocks(lines)
                    blocks.forEachIndexed { index, block ->
                        val parsed = if (section.kind == "judgment") {
                            parseJudgmentBlock(block, questions.size + 1)
                        } else {
                            parseBlock(block, questions.size + 1)
                        }
                        questions.add(parsed.question)
                        warnings.addAll(parsed.warnings)
                    }
                }
                "fill" -> {
                    val blocks = buildFillBlocks(section.lines)
                    blocks.forEach { block ->
                        questions.add(parseFillBlock(block, questions.size + 1))
                    }
                }
                "answers" -> {
                    if (answerLines == null) answerLines = mutableListOf()
                    answerLines.addAll(section.lines)
                }
            }
        }

        if (questions.isEmpty()) throw ImportException("没有解析到题目，请检查文档格式")

        if (answerLines != null) {
            applyAnswerKey(questions, answerLines!!)
        }

        return ParsedBank(
            name = name,
            fileName = fileName,
            questions = questions,
            warnings = warnings.distinct().sorted()
        )
    }

    private fun splitSections(lines: List<String>): List<RawSection> {
        val sections = mutableListOf<RawSection>()
        var current = RawSection("choice", mutableListOf())
        var hasHeading = false

        fun flush() {
            if (current.lines.isNotEmpty()) sections.add(current)
        }

        fun sectionKind(title: String): String = when (title) {
            "选择题", "单项选择题", "多项选择题" -> "choice"
            "判断题" -> "judgment"
            "填空题" -> "fill"
            "参考答案", "答案速查", "标准答案", "正确答案", "答案" -> "answers"
            else -> "ignore"
        }

        fun addAnswerContent(raw: String) {
            val t = raw.trim().trimStart('：', ':', '、', '.', '．').trim()
            if (t.isEmpty()) return
            val group = answerGroupHeadingRegex.find(t)
            if (group != null && qnumRegex.find(t) == null) {
                current.lines.add(ANSWER_GROUP_PREFIX + sectionKind(group.groupValues[1]))
                val rest = t.substring(group.range.last + 1).trim()
                if (rest.isNotEmpty()) current.lines.add(rest)
            } else {
                current.lines.add(t)
            }
        }

        for (line in lines) {
            val trimmed = line.trim()

            // 已经在文末答案区：一、选择题 / 二、填空题 只是答案分组标题，
            // 不能当成新的题目章节把后面的答案吞掉。
            if (current.kind == "answers") {
                val group = answerGroupHeadingRegex.find(trimmed)
                if (group != null && qnumRegex.find(trimmed) == null) {
                    current.lines.add(ANSWER_GROUP_PREFIX + sectionKind(group.groupValues[1]))
                    val rest = trimmed.substring(group.range.last + 1).trim()
                    if (rest.isNotEmpty()) current.lines.add(rest)
                    continue
                }
                val heading = sectionHeadingRegex.find(trimmed)
                if (heading != null && qnumRegex.find(trimmed) == null) {
                    if (sectionKind(heading.groupValues[1]) == "answers") {
                        addAnswerContent(trimmed.substring(heading.range.last + 1))
                    }
                    continue
                }
                current.lines.add(trimmed)
                continue
            }

            val heading = sectionHeadingRegex.find(trimmed)
            if (heading != null && qnumRegex.find(trimmed) == null) {
                flush()
                hasHeading = true
                val kind = sectionKind(heading.groupValues[1])
                current = RawSection(kind, mutableListOf())
                if (kind == "answers") {
                    addAnswerContent(trimmed.substring(heading.range.last + 1))
                }
                continue
            }
            current.lines.add(trimmed)
        }
        flush()

        if (!hasHeading) {
            return listOf(RawSection("choice", lines.toMutableList()))
        }
        return sections
    }

    private fun buildBlocks(lines: List<String>): List<List<String>> {
        val blocks = mutableListOf<List<String>>()
        var current: MutableList<String>? = null
        var currentHasAnswer = false

        fun flush() {
            current?.takeIf { it.isNotEmpty() }?.let { blocks.add(it.toList()) }
            current = null
            currentHasAnswer = false
        }

        for (line in lines) {
            val hasAnswer = answerRegex.containsMatchIn(line) ||
                answerTailRegex.containsMatchIn(line) ||
                emptyParenRegex.containsMatchIn(line) ||
                judgmentAnswerRegex.containsMatchIn(line)
            val startsWithOption = startsWithOptionLabel(line)
            val startsWithNumber = qnumRegex.containsMatchIn(line)

            if (current == null) {
                if (startsWithNumber || hasAnswer || startsWithOption) {
                    current = mutableListOf(line)
                    currentHasAnswer = hasAnswer
                }
                continue
            }

            when {
                startsWithNumber -> {
                    flush()
                    current = mutableListOf(line)
                    currentHasAnswer = hasAnswer
                }
                hasAnswer && !startsWithOption && !currentHasAnswer -> {
                    current!!.add(line)
                    currentHasAnswer = true
                }
                hasAnswer && !startsWithOption -> {
                    flush()
                    current = mutableListOf(line)
                    currentHasAnswer = true
                }
                else -> current!!.add(line)
            }
        }
        flush()
        return blocks
    }

    private fun buildFillBlocks(lines: List<String>): List<List<String>> {
        val blocks = mutableListOf<List<String>>()
        var current: MutableList<String>? = null
        for (line in lines) {
            if (qnumRegex.containsMatchIn(line)) {
                current?.let { blocks.add(it.toList()) }
                current = mutableListOf(line)
            } else if (current != null) {
                current!!.add(line)
            }
        }
        current?.let { blocks.add(it.toList()) }
        return blocks
    }

    private fun parseJudgmentBlock(block: List<String>, fallbackNumber: Int): ParsedBlock {
        val first = block.first()
        val rest = block.drop(1)
        val all = block.joinToString(" ")
        val number = qnumRegex.find(first)?.groupValues?.get(1) ?: fallbackNumber.toString()
        var stem = all.replaceFirst(qnumRegex, "").trim()

        val answerToken = judgmentAnswerRegex.find(stem)?.groupValues?.get(1)
            ?: answerRegex.find(stem)?.groupValues?.get(1)
        val answer = when (answerToken?.lowercase()) {
            "a", "对", "正确", "√", "t", "true" -> "A"
            "b", "错", "错误", "×", "f", "false" -> "B"
            else -> null
        }
        stem = stem.replace(judgmentAnswerRegex, "（　）")
        stem = stem.replace(answerRegex, "（　）")
        stem = stem.replace(answerTailRegex, "（　）")
        stem = stem.replace(emptyParenRegex, "（　）")
        stem = stem.replace(Regex("\\s+"), " ").trim()
        if (stem.endsWith("，") || stem.endsWith(",")) stem = stem.dropLast(1)

        return ParsedBlock(
            ParsedQuestion(
                num = number,
                stem = stem,
                options = listOf(listOf("A", "正确"), listOf("B", "错误")),
                answer = answer,
                kind = "judgment"
            ),
            emptyList()
        )
    }

    private fun parseFillBlock(block: List<String>, fallbackNumber: Int): ParsedQuestion {
        val first = block.first()
        val rest = block.drop(1).joinToString(" ").trim()
        val number = qnumRegex.find(first)?.groupValues?.get(1) ?: fallbackNumber.toString()
        var stem = first.replaceFirst(qnumRegex, "").trim()
        if (rest.isNotBlank()) stem = "$stem $rest"

        val answers = mutableListOf<String>()
        val paren = Regex("[（(]\\s*([^）)]{1,80})\\s*[）)]")
        stem = paren.replace(stem) { match ->
            val value = match.groupValues[1].trim()
            if (value.isNotEmpty()) answers.add(value)
            "（　）"
        }
        stem = stem.replace(Regex("\\s+"), " ").trim()
        if (stem.endsWith("，") || stem.endsWith(",")) stem = stem.dropLast(1)

        // 有些填空题没有括号，答案直接写在“有……”后面，例如“带磁性的材料有素磁片”。
        if (answers.isEmpty()) {
            val marker = stem.lastIndexOf("有")
            if (marker >= 0 && marker < stem.length - 1) {
                val tail = stem.substring(marker + 1).trim()
                if (tail.length in 1..40) {
                    answers.add(tail)
                    stem = stem.substring(0, marker + 1) + "（　）"
                }
            }
        }

        return ParsedQuestion(
            num = number,
            stem = stem,
            options = emptyList(),
            answer = answers.joinToString("、").ifBlank { null },
            kind = "fill"
        )
    }

    private data class AnswerKeyGroup(val kind: String?, val lines: MutableList<String>)

    private fun applyAnswerKey(questions: MutableList<ParsedQuestion>, lines: List<String>) {
        val groups = mutableListOf<AnswerKeyGroup>()
        var currentGroup: AnswerKeyGroup? = null

        lines.forEach { line ->
            if (line.startsWith(ANSWER_GROUP_PREFIX)) {
                currentGroup?.let { groups.add(it) }
                currentGroup = AnswerKeyGroup(
                    line.removePrefix(ANSWER_GROUP_PREFIX).trim(),
                    mutableListOf()
                )
            } else {
                if (currentGroup == null) currentGroup = AnswerKeyGroup(null, mutableListOf())
                currentGroup!!.lines.add(line)
            }
        }
        currentGroup?.let { groups.add(it) }

        val global = mutableMapOf<String, String>()
        val byKind = mutableMapOf<String, MutableMap<String, String>>()
        groups.forEach { group ->
            val parsed = buildAnswerMap(group.lines)
            if (group.kind == null) {
                parsed.forEach { (num, token) -> global.putIfAbsent(num, token) }
            } else if (group.kind != "ignore") {
                val target = byKind.getOrPut(group.kind) { mutableMapOf() }
                parsed.forEach { (num, token) -> target.putIfAbsent(num, token) }
            }
        }

        val ordinals = mutableMapOf<String, Int>()
        questions.forEachIndexed { index, question ->
            val ordinal = (ordinals[question.kind] ?: 0) + 1
            ordinals[question.kind] = ordinal

            val groupMap = byKind[question.kind]
            val fromGroup = groupMap?.get(question.num) ?: groupMap?.get(ordinal.toString())
            // 没有分组标题时，先按题号匹配；题号对不上再按整卷顺序。
            // 这样选择题 1-127 + 填空题 1-20 的文末连续答案也能对上。
            val directGlobal = global[question.num]
            val sequentialGlobal = global[(index + 1).toString()]
            val token = fromGroup ?: if (question.kind == "fill") {
                directGlobal?.takeIf { it.trim().length > 1 }
                    ?: sequentialGlobal?.takeIf { it.trim().length > 1 }
            } else {
                directGlobal ?: sequentialGlobal
            }
            if (token == null) return@forEachIndexed

            if (question.kind == "fill") {
                // 没有明确「填空题」答案分组时，文末的 A/B/C/D 更可能是选择题答案，
                // 不要把它们误填进填空题。
                if (fromGroup == null && token.trim().length <= 1) return@forEachIndexed
                if (question.answer.isNullOrBlank()) {
                    questions[index] = question.copy(answer = token.trim())
                }
            } else if (question.kind == "judgment") {
                if (question.answer == null) {
                    val mapped = when (token.trim().lowercase()) {
                        "a", "对", "正确", "√", "t", "true" -> "A"
                        "b", "错", "错误", "×", "f", "false" -> "B"
                        else -> null
                    }
                    if (mapped != null) questions[index] = question.copy(answer = mapped)
                }
            } else if (question.answer == null) {
                val mapped = token.trim().firstOrNull()?.uppercase()?.toString()
                if (mapped != null && mapped in listOf("A", "B", "C", "D")) {
                    questions[index] = question.copy(answer = mapped)
                }
            }
        }
    }

    private fun buildAnswerMap(lines: List<String>): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val joined = lines.joinToString("\n")

        answerKeyRangeRegex.findAll(joined).forEach { match ->
            val start = match.groupValues[1].toIntOrNull() ?: return@forEach
            val end = match.groupValues[2].toIntOrNull() ?: return@forEach
            val letters = match.groupValues[3].mapNotNull { normalizeAnswerLetter(it) }
            if (end - start + 1 == letters.size) {
                letters.forEachIndexed { offset, ch ->
                    map.putIfAbsent((start + offset).toString(), ch.toString())
                }
            }
        }

        // 有些 Word 表格会把题号和答案拆成上下两行：1 / A / 2 / B。
        lines.forEachIndexed { index, line ->
            val number = Regex("^\\s*(\\d{1,3})\\s*$").matchEntire(line)
                ?.groupValues?.get(1) ?: return@forEachIndexed
            val next = lines.getOrNull(index + 1)?.trim().orEmpty()
            if (next.isNotEmpty() &&
                !Regex("^\\d{1,3}$").matches(next) &&
                next.length <= 80
            ) {
                map.putIfAbsent(number, next)
            }
        }

        lines.forEach { line ->
            answerKeyPairRegex.findAll(line).forEach { match ->
                map.putIfAbsent(match.groupValues[1], match.groupValues[2])
            }
            val full = answerKeyLineRegex.matchEntire(line)
            if (full != null) {
                val rest = full.groupValues[3].trim()
                if (rest.isNotEmpty() && !Regex("\\s\\d{1,3}[.、:：]").containsMatchIn(rest)) {
                    map.putIfAbsent(full.groupValues[1], rest)
                }
            }
        }
        return map
    }

    private fun normalizeAnswerLetter(ch: Char): Char? {
        val c = when (ch) {
            in 'Ａ'..'Ｄ', in 'ａ'..'ｄ' -> (ch.code - 0xFEE0).toChar()
            else -> ch
        }
        val upper = c.uppercaseChar()
        return if (upper in 'A'..'D') upper else null
    }

    private fun startsWithOptionLabel(line: String): Boolean {
        val t = line.trimStart()
        if (t.isEmpty() || t[0] !in 'A'..'D') return false
        if (t.length == 1) return true
        val next = t[1]
        return next.isWhitespace() || next in "、.．,，:："
    }

    /**
     * 有些 Word 文档会把下一题直接塞进上一题的 D 选项后面，例如
     * `D、43.下一题（B）`。这里在数字题号前切开，避免整道题被吞掉。
     */
    private fun splitEmbeddedQuestions(line: String): List<String> {
        val marker = Regex("(?<!\\d)(\\d{1,3})[.、]\\s*(?=[^\\d])")
        val pieces = mutableListOf<String>()
        var start = 0
        for (match in marker.findAll(line)) {
            val pos = match.range.first
            if (pos == 0) continue
            if (!answerRegex.containsMatchIn(line.substring(pos))) continue
            val before = line.substring(start, pos).trim()
            if (before.isEmpty()) continue
            pieces.add(before)
            start = pos
        }
        if (start == 0) return listOf(line)
        pieces.add(line.substring(start).trim())
        return pieces.filter { it.isNotEmpty() }
    }

    private fun parseBlock(block: List<String>, fallbackNumber: Int, kind: String = "choice"): ParsedBlock {
        val first = block.first()
        val warningList = mutableListOf<String>()

        val answerMatch = answerRegex.find(first) ?: answerTailRegex.find(first)
        val answerEnd = answerMatch?.range?.let { it.last + 1 } ?: 0
        val afterAnswer = if (answerMatch != null) first.substring(answerEnd) else first

        val inlineLabel = optionLabelRegex.find(afterAnswer)
        val inlineStart = inlineLabel?.groups?.get(1)?.range?.first
        val hasInlineOptions = inlineLabel != null && inlineStart != null
        val inlineKey = inlineLabel?.groupValues?.get(1)?.uppercase()

        var stemFirst = when {
            !hasInlineOptions -> first
            inlineKey == "A" -> first.substring(0, answerEnd + inlineStart!!)
            else -> first.substring(0, answerEnd)
        }

        var stemExtra = ""
        val rest = block.drop(1)
        val restText = rest.joinToString("\n")

        val optionSource: String
        if (hasInlineOptions) {
            optionSource = buildString {
                append(afterAnswer.trim())
                if (restText.isNotBlank()) append('\n').append(restText)
            }
        } else if (restText.isNotBlank()) {
            val firstRestLabel = optionLabelRegex.find(restText)
            if (firstRestLabel == null) {
                if (rest.size >= 2) {
                    optionSource = restText
                } else {
                    stemExtra = restText
                    optionSource = ""
                }
            } else {
                val key = firstRestLabel.groupValues[1].uppercase()
                val prefix = restText.substring(0, firstRestLabel.groups[1]!!.range.first).trim()
                if (key == "A" && prefix.isNotEmpty()) {
                    stemExtra = prefix
                    optionSource = restText.substring(firstRestLabel.groups[1]!!.range.first)
                } else {
                    optionSource = restText
                }
            }
        } else {
            optionSource = ""
        }

        val options = extractOptions(optionSource, rest.size)
        val answer = answerMatch?.groupValues?.get(1)?.uppercase()

        var number = fallbackNumber.toString()
        val numMatch = qnumRegex.find(first)
        if (numMatch != null) {
            number = numMatch.groupValues[1]
            stemFirst = stemFirst.replaceFirst(qnumRegex, "")
        }

        var stem = stemFirst + if (stemExtra.isNotBlank()) " $stemExtra" else ""
        stem = stem.replace(answerRegex, "（　）")
        stem = stem.replace(answerTailRegex, "（　）")
        stem = stem.replace(emptyParenRegex, "（　）")
        stem = stem.replace(Regex("\\s+"), " ").trim()
        if (stem.endsWith("，") || stem.endsWith(",")) stem = stem.dropLast(1)

        var finalAnswer = answer
        if (finalAnswer == null) {
            val rawParen = Regex("[（(]\\s*([^）)]{1,8})\\s*[）)]").find(first)
            val token = rawParen?.groupValues?.get(1)?.trim()
            if (!token.isNullOrEmpty()) {
                val hit = options.indexOfFirst { it.value.startsWith(token) }
                if (hit >= 0) finalAnswer = options[hit].key
            }
        }

        if (options.size < 2) {
            warningList.add("第 $number 题识别到 ${options.size} 个选项")
        }
        if (finalAnswer != null && options.none { it.key == finalAnswer }) {
            warningList.add("第 $number 题的答案 $finalAnswer 不在选项里")
        }

        return ParsedBlock(
            ParsedQuestion(number, stem, options.map { listOf(it.key, it.value) }, finalAnswer, kind),
            warningList
        )
    }

    private fun extractOptions(text: String, restLineCount: Int): List<OptionPart> {
        if (text.isBlank()) return emptyList()

        val matches = optionLabelRegex.findAll(text).map { m ->
            Triple(
                m.groupValues[1].uppercase(),
                m.groups[1]!!.range.first,
                m.groups[1]!!.range.last + 1
            )
        }.toList()

        val firstA = matches.firstOrNull { it.first == "A" }
        val firstB = matches.firstOrNull { it.first == "B" }
        val start = firstA ?: firstB

        if (start == null) {
            val lines = text.split('\n').map { it.trim() }.filter { it.isNotEmpty() }
            if (lines.size in 2..4) {
                return lines.mapIndexed { i, value -> OptionPart(('A' + i).toString(), value) }
            }
            return emptyList()
        }

        val chosen = mutableListOf<Pair<String, Triple<String, Int, Int>>>()
        var virtualA: String? = null
        val expected = mutableListOf<String>()
        if (firstA == null && firstB != null) {
            virtualA = text.substring(0, firstB.second).trim().takeIf { it.isNotEmpty() }
            chosen.add("B" to firstB)
            expected.add("C")
            expected.add("D")
        } else {
            chosen.add("A" to start)
            expected.add("B")
            expected.add("C")
            expected.add("D")
        }
        for (m in matches) {
            if (m.second <= start.second) continue
            if (m.first == "A") break
            if (m.first == expected.firstOrNull()) {
                chosen.add(m.first to m)
                expected.removeAt(0)
                if (expected.isEmpty()) break
            }
        }

        val parts = mutableListOf<OptionPart>()
        if (virtualA != null) parts.add(OptionPart("A", virtualA))
        chosen.forEachIndexed { i, pair ->
            val startPos = pair.second.second
            val endPos = chosen.getOrNull(i + 1)?.second?.second ?: text.length
            val raw = text.substring(startPos, endPos).trim()
            val key = if (virtualA != null) ('B' + i).toString() else pair.first
            val value = cleanOptionValue(raw, pair.first)
            if (value.isNotEmpty()) parts.add(OptionPart(key, value))
        }

        if (parts.size < 2 && restLineCount >= 2) {
            val lines = text.split('\n').map { it.trim() }.filter { it.isNotEmpty() }
            if (lines.size in 2..4) {
                return lines.mapIndexed { i, value -> OptionPart(('A' + i).toString(), value) }
            }
        }
        return parts
    }

    private fun cleanOptionValue(raw: String, key: String): String {
        val m = optionPrefixRegex.matchEntire(raw) ?: return raw.trim()
        if (m.groupValues[1].uppercase() != key) return raw.trim()
        return m.groupValues[2].trim()
    }

    // MARK: JSON

    private fun parseJson(file: File, fallbackName: String, fileName: String): ParsedBank {
        val root = try {
            JSONTokener(file.readText(Charsets.UTF_8)).nextValue()
        } catch (e: Exception) {
            throw ImportException("JSON 解析失败：${e.message ?: "格式不正确"}")
        }

        var name = fallbackName
        val items: List<JSONObject>

        when (root) {
            is JSONArray -> {
                items = (0 until root.length()).mapNotNull { root.optJSONObject(it) }
            }
            is JSONObject -> {
                if (root.has("banks")) throw ImportException("这是 BookQ 备份文件，请到「设置 → 从备份恢复」导入")
                root.optString("name").takeIf { it.isNotBlank() }?.let { name = it }
                val array = root.optJSONArray("questions")
                    ?: throw ImportException("JSON 里没有 questions 数组")
                items = (0 until array.length()).mapNotNull { array.optJSONObject(it) }
            }
            else -> throw ImportException("JSON 格式不支持")
        }

        if (items.isEmpty()) throw ImportException("JSON 里没有题目")

        val questions = mutableListOf<ParsedQuestion>()
        val warnings = mutableListOf<String>()

        items.forEachIndexed { index, item ->
            val number = item.opt("num")?.toString()?.takeIf { it.isNotBlank() } ?: "${index + 1}"
            val stem = item.optString("stem").ifBlank {
                item.optString("question").ifBlank { item.optString("title") }
            }

            val options = mutableListOf<List<String>>()
            val rawOptions = item.optJSONArray("options")
            if (rawOptions != null) {
                for (i in 0 until rawOptions.length()) {
                    val entry = rawOptions.opt(i)
                    when (entry) {
                        is JSONArray -> {
                            options.add(
                                if (entry.length() >= 2) listOf(entry.optString(0).uppercase(), entry.optString(1))
                                else listOf("?", entry.optString(0))
                            )
                        }
                        is String -> {
                            val m = optionContentRegex.matchEntire(entry)
                            if (m != null) options.add(listOf(m.groupValues[1].uppercase(), m.groupValues[2].trim()))
                            else options.add(listOf("?", entry))
                        }
                    }
                }
            }

            var answer: String? = null
            val rawAnswer = item.optString("answer")
            if (rawAnswer.isNotBlank()) {
                val trimmed = rawAnswer.trim().uppercase()
                answer = when {
                    trimmed.length == 1 && "ABCD".contains(trimmed) -> trimmed
                    else -> answerRegex.find(rawAnswer)?.groupValues?.get(1)?.uppercase()
                }
            }

            if (options.size != 4) warnings.add("第 $number 题有 ${options.size} 个选项")
            questions.add(ParsedQuestion(number, stem, options, answer))
        }

        return ParsedBank(name, fileName, questions, warnings.distinct().sorted())
    }
}
