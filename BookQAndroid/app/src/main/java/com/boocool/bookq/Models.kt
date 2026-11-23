package com.boocool.bookq

import java.util.UUID


enum class AppTheme(val title: String, val subtitle: String) {
    INDIGO("织物靛蓝", "墨蓝布卷 · 黄铜微光"),
    RUST("茜草暖砂", "暖砂纸 · 茜草红"),
    MOSS("苔绿青铜", "苔绿 · 青铜刻度")
}

enum class AppAppearance(val title: String, val subtitle: String) {
    LIGHT("浅色", "纸张质感最完整"),
    DARK("深色", "夜间低亮更护眼")
}

data class Question(
    val id: String,
    val bankId: String,
    val chapter: String,
    val num: String,
    val stem: String,
    val options: List<List<String>>,
    val answer: String?,
    val kind: String = "choice"
) {
    val keys: List<String> get() = options.mapNotNull { it.firstOrNull() }

    val isFill: Boolean get() = kind == "fill"
    val isShort: Boolean get() = kind == "short"
    val isMulti: Boolean get() = kind == "multi"
    val isJudgment: Boolean get() = kind == "judgment"
    /** 填空/简答只展示答案，不参与对错、正确率和错题本。 */
    val isFillLike: Boolean get() = isFill || isShort
    val isGraded: Boolean get() = !isFillLike && hasAnswer

    val answerKeys: Set<String>
        get() = (answer ?: "").filter { it in 'A'..'D' }.map { it.toString() }.toSet()

    fun textFor(key: String): String =
        options.firstOrNull { it.firstOrNull() == key }?.getOrNull(1).orEmpty()

    val hasAnswer: Boolean
        get() = when {
            isFillLike -> !answer.isNullOrBlank()
            isMulti -> answerKeys.isNotEmpty() && answerKeys.all { keys.contains(it) }
            else -> answer != null && answer.isNotEmpty() && keys.contains(answer)
        }

    fun isCorrect(choice: String?): Boolean {
        if (choice == null) return false
        if (isMulti) {
            return choice.filter { it in 'A'..'D' }.map { it.toString() }.toSet() == answerKeys
        }
        return choice == answer
    }

    val answerText: String
        get() = when {
            isFillLike -> answer.orEmpty()
            isMulti -> answerKeys.sorted().joinToString("、") { textFor(it) }
            else -> answer?.let(::textFor).orEmpty()
        }

    val answerDisplay: String
        get() {
            if (isFillLike) return answerText
            if (isMulti) {
                return answerKeys.sorted().joinToString("、") { key ->
                    val label = textFor(key).trim()
                    if (label.isEmpty()) key else "$key. $label"
                }
            }
            val letter = answer ?: return answerText
            val label = answerText.trim()
            val alreadyPrefixed = listOf("$letter.", "$letter．", "$letter、", "$letter ")
                .any { label.startsWith(it) }
            return if (alreadyPrefixed || label.isEmpty()) label.ifEmpty { letter } else "$letter. $label"
        }

    val metaLine: String
        get() {
            if (isFillLike) {
                return "题库第 $num 题 · ${if (isShort) "简答题" else "填空题"} · 点击查看答案"
            }
            val typeText = when {
                isJudgment -> "判断题"
                isMulti -> "多选题 · ${options.size} 个选项"
                else -> "${options.size} 个选项"
            }
            return "题库第 $num 题 · $typeText" + if (hasAnswer) "" else " · 题库无答案"
        }
}

data class QuizBank(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val fileName: String,
    val importedAt: Long = System.currentTimeMillis(),
    val questions: List<Question>
) {
    val count: Int get() = questions.size

    val colorIndex: Int
        get() = id.sumOf { it.code } % 2
}

enum class QuizMode(val title: String, val longTitle: String, val subtitle: String) {
    PRACTICE("练习", "练习模式", "答完立即看对错"),
    EXAM("考试", "考试模式", "交卷后统一评分")
}

enum class QuizOrder(val title: String, val subtitle: String) {
    RANDOM("随机", "所有题打乱后抽取"),
    SEQUENTIAL("顺序", "从第一题往后做")
}

enum class AppTab(val title: String) {
    HOME("主页"), LIBRARY("题库"), SETTINGS("设置")
}

enum class Screen { HOME, QUIZ, RESULT, WRONG }

sealed class QuizScope {
    data object All : QuizScope()
    data class Bank(val id: String) : QuizScope()
    data object Wrong : QuizScope()
}

data class QuestionProgress(val attempts: Int = 0, val correct: Int = 0) {
    val rate: Int get() = if (attempts == 0) 0 else ((correct.toDouble() / attempts) * 100).toInt()
}

data class Attempt(
    val id: String = UUID.randomUUID().toString(),
    val total: Int,
    val correct: Int,
    val date: Long = System.currentTimeMillis(),
    val scope: String
) {
    val percent: Int get() = if (total == 0) 0 else ((correct.toDouble() / total) * 100).toInt()
}

data class ResultItem(val question: Question, val choice: String?) {
    val isCorrect: Boolean get() = question.isCorrect(choice)
}

data class ChapterStat(val name: String, val correct: Int, val total: Int) {
    val rate: Int get() = if (total == 0) 0 else ((correct.toDouble() / total) * 100).toInt()
}

data class QuizResult(
    val total: Int,
    val correct: Int,
    val unanswered: Int,
    val noAnswerCount: Int,
    val durationSeconds: Long,
    val scopeLabel: String,
    val mode: QuizMode,
    val items: List<ResultItem>,
    val chapterStats: List<ChapterStat>,
    val fillCount: Int = 0
) {
    val percent: Int get() = if (total == 0) 0 else ((correct.toDouble() / total) * 100).toInt()

    val grade: String
        get() = when {
            total == 0 -> "暂无评分"
            percent >= 90 -> "熟练"
            percent >= 80 -> "掌握良好"
            percent >= 60 -> "仍需巩固"
            else -> "建议重练"
        }
}

data class ParsedQuestion(
    val num: String,
    val stem: String,
    val options: List<List<String>>,
    val answer: String?,
    val kind: String = "choice"
)

data class ParsedBank(
    val name: String,
    val fileName: String,
    val questions: List<ParsedQuestion>,
    val warnings: List<String>
) {
    val questionCount: Int get() = questions.size
    val noAnswerCount: Int
        get() = questions.count { q ->
            when {
                q.kind == "fill" || q.kind == "short" -> false
                q.answer == null -> true
                q.kind == "multi" -> q.answer.any { ch -> q.options.none { it.firstOrNull() == ch.toString() } }
                else -> q.options.none { it.firstOrNull() == q.answer }
            }
        }
    val fillCount: Int get() = questions.count { it.kind == "fill" }
    val shortCount: Int get() = questions.count { it.kind == "short" }
    val multiCount: Int get() = questions.count { it.kind == "multi" }
    val judgmentCount: Int get() = questions.count { it.kind == "judgment" }
}

// MARK: - 顺序做题

data class SequenceCursor(
    val nextQuestionId: String? = null,
    val completed: Boolean = false
)

data class SequenceInfo(
    val total: Int,
    val nextIndex: Int?,
    val remaining: Int,
    val done: Int,
    val completed: Boolean,
    val lastNumber: String?
) {
    val nextNumber: Int? get() = nextIndex?.plus(1)

    val lastLabel: String
        get() = when {
            completed -> "上一轮已完成"
            lastNumber != null -> "上次停在 第 $lastNumber 题"
            else -> "还没开始"
        }
}
