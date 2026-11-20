package com.boocool.bookq

import java.util.UUID

data class Question(
    val id: String,
    val bankId: String,
    val chapter: String,
    val num: String,
    val stem: String,
    val options: List<List<String>>,
    val answer: String?
) {
    val keys: List<String> get() = options.mapNotNull { it.firstOrNull() }

    fun textFor(key: String): String =
        options.firstOrNull { it.firstOrNull() == key }?.getOrNull(1).orEmpty()

    val hasAnswer: Boolean
        get() = answer != null && answer.isNotEmpty() && keys.contains(answer)

    val answerText: String get() = answer?.let(::textFor).orEmpty()

    val answerDisplay: String
        get() {
            val letter = answer ?: return answerText
            val label = answerText.trim()
            val alreadyPrefixed = listOf("$letter.", "$letter．", "$letter、", "$letter ")
                .any { label.startsWith(it) }
            return if (alreadyPrefixed || label.isEmpty()) label.ifEmpty { letter } else "$letter. $label"
        }

    val metaLine: String
        get() {
            val optionText = if (options.isEmpty()) "无选项" else "${options.size} 个选项"
            return "题库第 $num 题 · $optionText" + if (hasAnswer) "" else " · 题库无答案"
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
    val isCorrect: Boolean get() = choice != null && choice == question.answer
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
    val chapterStats: List<ChapterStat>
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
    val answer: String?
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
            q.answer == null || q.options.none { it.firstOrNull() == q.answer }
        }
}
