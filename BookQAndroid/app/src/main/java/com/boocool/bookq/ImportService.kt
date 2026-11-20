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
    private val optionStrictRegex = Regex("(?:^|[\\s　])([A-D])\\s?[:：.，,、]\\s?")
    private val optionLooseRegex = Regex("([A-D])\\s?[:：.，,、]\\s?")
    private val optionAtStartRegex = Regex("^\\s*([A-D])\\s?[:：.，,、]")
    private val optionContentRegex = Regex("^([A-D])\\s?[:：.，,、]\\s?(.*)$", RegexOption.DOT_MATCHES_ALL)
    private val qnumRegex = Regex("^\\s*(\\d+)\\s*[.、:：]?\\s*")

    private fun parseText(text: String, name: String, fileName: String): ParsedBank {
        val lines = text
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .split('\n')
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (lines.isEmpty()) throw ImportException("这个文件里没有可读取的题目")

        val blocks = mutableListOf<List<String>>()
        var current: MutableList<String>? = null

        fun flush() {
            current?.takeIf { it.isNotEmpty() }?.let { blocks.add(it.toList()) }
            current = null
        }

        lines.forEachIndexed { index, line ->
            val next = lines.getOrNull(index + 1)
            if (looksLikeChapter(line, next)) {
                flush()
                return@forEachIndexed
            }

            val hasAnswer = answerRegex.containsMatchIn(line) ||
                answerTailRegex.containsMatchIn(line) ||
                emptyParenRegex.containsMatchIn(line)
            val startsWithOption = optionAtStartRegex.containsMatchIn(line)
            val startsWithNumber = qnumRegex.containsMatchIn(line)
            val hasOptionMark = optionStrictRegex.containsMatchIn(line) ||
                optionLooseRegex.containsMatchIn(line)

            if ((startsWithNumber && (hasAnswer || hasOptionMark)) || (hasAnswer && !startsWithOption)) {
                flush()
                current = mutableListOf(line)
            } else if (current != null) {
                current!!.add(line)
            } else if (startsWithNumber) {
                current = mutableListOf(line)
            }
        }
        flush()

        if (blocks.isEmpty()) throw ImportException("没有解析到题目，请检查文档格式")

        val questions = mutableListOf<ParsedQuestion>()
        val warnings = mutableListOf<String>()

        blocks.forEachIndexed { index, block ->
            val first = block.first()
            val raw = block.joinToString(" ")

            var answer: String? = null
            var body = raw
            val match = answerRegex.find(first)
                ?: answerTailRegex.find(first)
                ?: answerRegex.find(raw)
            if (match != null) {
                answer = match.groupValues[1].uppercase()
                if (match.range.first < first.length) {
                    val replacedFirst = first.replaceRange(match.range, "（　）")
                    body = replacedFirst + raw.substring(first.length)
                }
            }

            val (questionTextRaw, optionTexts) = splitOptions(body)
            var questionText = questionTextRaw
            val number: String
            val numMatch = qnumRegex.find(questionText)
            if (numMatch != null) {
                number = numMatch.groupValues[1]
                questionText = questionText.replaceFirst(qnumRegex, "")
            } else {
                number = "${index + 1}"
            }

            var stem = questionText
            stem = stem.replace(answerRegex, "（　）")
            stem = stem.replace(answerTailRegex, "（　）")
            stem = stem.replace(emptyParenRegex, "（　）")
            stem = stem.replace(Regex("\\s+"), " ").trim()
            if (stem.endsWith("，") || stem.endsWith(",")) stem = stem.dropLast(1)

            val options = optionTexts.map { option ->
                val m = optionContentRegex.matchEntire(option)
                if (m != null) {
                    listOf(m.groupValues[1].uppercase(), m.groupValues[2].trim())
                } else {
                    listOf("?", option)
                }
            }

            if (options.size != 4) warnings.add("第 $number 题识别到 ${options.size} 个选项")
            if (answer != null && options.none { it.firstOrNull() == answer }) {
                warnings.add("第 $number 题的答案 $answer 不在选项里")
            }

            questions.add(ParsedQuestion(number, stem, options, answer))
        }

        return ParsedBank(
            name = name,
            fileName = fileName,
            questions = questions,
            warnings = warnings.distinct().sorted()
        )
    }

    private fun looksLikeChapter(line: String, next: String?): Boolean {
        if (line.isEmpty() || line.length > 24 || next == null) return false
        if (line.any { it.isDigit() }) return false
        if (answerRegex.containsMatchIn(line)) return false
        if (optionStrictRegex.containsMatchIn(line)) return false
        if (line.contains('？') || line.contains('?')) return false
        return qnumRegex.containsMatchIn(next)
    }

    private fun splitOptions(text: String): Pair<String, List<String>> {
        for (regex in listOf(optionStrictRegex, optionLooseRegex)) {
            val matches = regex.findAll(text).toList()
            val firstA = matches.firstOrNull { it.groupValues[1].uppercase() == "A" } ?: continue
            val chosen = mutableListOf(firstA)
            val needed = mutableListOf("B", "C", "D")

            for (m in matches) {
                if (m.range.first <= firstA.range.first) continue
                val letter = m.groupValues[1].uppercase()
                if (letter == needed.firstOrNull()) {
                    chosen.add(m)
                    needed.removeAt(0)
                    if (needed.isEmpty()) break
                }
            }
            if (chosen.size != 4) continue

            val starts = chosen.map { it.range.first }
            val stem = text.substring(0, starts[0]).trim()
            val options = (0..3).map { i ->
                text.substring(starts[i], if (i == 3) text.length else starts[i + 1]).trim()
            }
            if (options.all { it.isNotEmpty() }) return stem to options
        }
        return text.trim() to emptyList()
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
