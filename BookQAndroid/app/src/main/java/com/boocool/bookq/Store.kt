package com.boocool.bookq

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Calendar
import java.util.UUID

data class AppState(
    val banks: List<QuizBank> = emptyList(),
    val selectedTab: AppTab = AppTab.HOME,
    val wrongIds: List<String> = emptyList(),
    val progress: Map<String, QuestionProgress> = emptyMap(),
    val attempts: List<Attempt> = emptyList(),
    val recentBankId: String? = null,
    val defaultMode: QuizMode = QuizMode.PRACTICE,
    val defaultSize: Int = 10,
    val defaultOrder: QuizOrder = QuizOrder.SEQUENTIAL,
    val haptics: Boolean = true
)

data class QuizSession(
    val queue: List<Question>,
    val answers: List<String?>,
    val locked: List<Boolean>,
    val index: Int = 0,
    val mode: QuizMode,
    val order: QuizOrder,
    val scope: QuizScope,
    val size: Int?,
    val startTime: Long = System.currentTimeMillis()
)

data class ProgressSummary(
    val done: Int,
    val total: Int,
    val percent: Double,
    val rate: Int?
)

class BookQStore(private val context: Context) {

    var state by mutableStateOf(AppState())
        private set

    var screen by mutableStateOf(Screen.HOME)
        private set

    var session by mutableStateOf<QuizSession?>(null)
        private set

    var result by mutableStateOf<QuizResult?>(null)
        private set

    var toast by mutableStateOf<String?>(null)
        private set

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var toastJob: Job? = null
    private val stateFile = File(context.filesDir, "bookq-state.json")

    init {
        load()
    }

    // MARK: - 题库

    val allQuestions: List<Question> get() = state.banks.flatMap { it.questions }
    val hasBanks: Boolean get() = state.banks.isNotEmpty()
    val wrongQuestions: List<Question>
        get() {
            val map = allQuestions.associateBy { it.id }
            return state.wrongIds.mapNotNull { map[it] }
        }

    fun bank(id: String): QuizBank? = state.banks.firstOrNull { it.id == id }

    val lastBank: QuizBank?
        get() = state.recentBankId?.let(::bank) ?: state.banks.firstOrNull()

    fun questions(scope: QuizScope): List<Question> = when (scope) {
        is QuizScope.All -> allQuestions
        is QuizScope.Bank -> bank(scope.id)?.questions.orEmpty()
        is QuizScope.Wrong -> wrongQuestions
    }

    fun scopeLabel(scope: QuizScope): String = when (scope) {
        is QuizScope.All -> "全部题库"
        is QuizScope.Bank -> bank(scope.id)?.name ?: "题库"
        is QuizScope.Wrong -> "错题重练"
    }

    private fun uniqueName(proposed: String): String {
        val base = proposed.trim().ifBlank { "未命名题库" }
        if (state.banks.none { it.name == base }) return base
        var index = 2
        while (state.banks.any { it.name == "$base ($index)" }) index++
        return "$base ($index)"
    }

    fun addBank(name: String, fileName: String, parsed: List<ParsedQuestion>): QuizBank? {
        if (parsed.isEmpty()) {
            showToast("没有解析到题目")
            return null
        }
        val unique = uniqueName(name)
        val id = UUID.randomUUID().toString()
        val questions = parsed.mapIndexed { index, q ->
            Question(
                id = "$id-${index + 1}",
                bankId = id,
                chapter = unique,
                num = q.num,
                stem = q.stem,
                options = q.options,
                answer = q.answer
            )
        }
        val bank = QuizBank(id = id, name = unique, fileName = fileName, questions = questions)
        state = state.copy(
            banks = state.banks + bank,
            recentBankId = id
        )
        persist()
        return bank
    }

    fun renameBank(id: String, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        if (state.banks.any { it.id != id && it.name == trimmed }) {
            showToast("已经有同名题库")
            return
        }
        val old = bank(id) ?: return
        if (old.name == trimmed) return
        state = state.copy(
            banks = state.banks.map { item ->
                if (item.id != id) item
                else item.copy(
                    name = trimmed,
                    questions = item.questions.map { q -> q.copy(chapter = trimmed) }
                )
            },
            attempts = state.attempts.map { attempt ->
                if (attempt.scope == old.name) attempt.copy(scope = trimmed) else attempt
            }
        )
        persist()
    }

    fun deleteBank(id: String) {
        val target = bank(id) ?: return
        val questionIds = target.questions.map { it.id }.toSet()
        state = state.copy(
            banks = state.banks.filterNot { it.id == id },
            wrongIds = state.wrongIds.filterNot { it in questionIds },
            progress = state.progress.filterKeys { it !in questionIds },
            attempts = state.attempts.filterNot { it.scope == target.name },
            recentBankId = if (state.recentBankId == id) state.banks.firstOrNull { it.id != id }?.id else state.recentBankId
        )
        persist()
    }

    fun wrongCountIn(bank: QuizBank): Int {
        val ids = bank.questions.map { it.id }.toSet()
        return state.wrongIds.count { it in ids }
    }

    // MARK: - 进度

    fun bankProgress(bank: QuizBank): ProgressSummary {
        val done = bank.questions.count { state.progress.containsKey(it.id) }
        val attempts = bank.questions.sumOf { state.progress[it.id]?.attempts ?: 0 }
        val correct = bank.questions.sumOf { state.progress[it.id]?.correct ?: 0 }
        return ProgressSummary(
            done = done,
            total = bank.count,
            percent = if (bank.count == 0) 0.0 else done.toDouble() / bank.count,
            rate = if (attempts == 0) null else (correct.toDouble() / attempts * 100).toInt()
        )
    }

    fun scopeProgress(scope: QuizScope): ProgressSummary {
        val list = questions(scope)
        val done = list.count { state.progress.containsKey(it.id) }
        val attempts = list.sumOf { state.progress[it.id]?.attempts ?: 0 }
        val correct = list.sumOf { state.progress[it.id]?.correct ?: 0 }
        return ProgressSummary(
            done = done,
            total = list.size,
            percent = if (list.isEmpty()) 0.0 else done.toDouble() / list.size,
            rate = if (attempts == 0) null else (correct.toDouble() / attempts * 100).toInt()
        )
    }

    val totalAnswered: Int get() = allQuestions.count { state.progress.containsKey(it.id) }

    val overallRate: Int?
        get() {
            val attempts = state.progress.values.sumOf { it.attempts }
            val correct = state.progress.values.sumOf { it.correct }
            return if (attempts == 0) null else (correct.toDouble() / attempts * 100).toInt()
        }

    val weeklyStats: Triple<Int, Int?, Int>
        get() {
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val cutoff = today - 6L * 24 * 60 * 60 * 1000
            val recent = state.attempts.filter { it.date >= cutoff }
            val total = recent.sumOf { it.total }
            val correct = recent.sumOf { it.correct }
            val days = recent.map {
                Calendar.getInstance().apply { timeInMillis = it.date }.let { c ->
                    "${c.get(Calendar.YEAR)}-${c.get(Calendar.DAY_OF_YEAR)}"
                }
            }.distinct().size
            return Triple(total, if (total == 0) null else (correct.toDouble() / total * 100).toInt(), days)
        }

    val weakestBanks: List<QuizBank>
        get() = state.banks
            .filter { bank -> bank.questions.any { (state.progress[it.id]?.attempts ?: 0) > 0 } }
            .sortedBy { bankProgress(it).rate ?: 0 }
            .take(2)

    // MARK: - 练习

    fun startQuiz(scope: QuizScope, mode: QuizMode, size: Int?, order: QuizOrder) {
        val candidates = questions(scope)
        if (candidates.isEmpty()) {
            showToast(if (scope is QuizScope.Wrong) "错题本还是空的" else "这个题库还没有题目")
            return
        }
        var pool = candidates
        if (order == QuizOrder.RANDOM) pool = pool.shuffled()
        if (size != null && size > 0 && size < pool.size) pool = pool.take(size)

        session = QuizSession(
            queue = pool,
            answers = List(pool.size) { null },
            locked = List(pool.size) { false },
            mode = mode,
            order = order,
            scope = scope,
            size = size
        )
        if (scope is QuizScope.Bank) {
            state = state.copy(recentBankId = scope.id)
            persist()
        }
        result = null
        screen = Screen.QUIZ
    }

    fun startWrongPractice() {
        startQuiz(QuizScope.Wrong, QuizMode.PRACTICE, null, QuizOrder.RANDOM)
    }

    fun restart() {
        val s = session ?: return
        startQuiz(s.scope, s.mode, s.size, s.order)
    }

    fun choose(key: String) {
        val s = session ?: return
        if (screen != Screen.QUIZ || s.index !in s.queue.indices) return
        val question = s.queue[s.index]

        if (s.mode == QuizMode.PRACTICE && s.locked[s.index]) return

        if (question.options.isEmpty()) {
            val answers = s.answers.toMutableList()
            val locked = s.locked.toMutableList()
            if (s.mode == QuizMode.PRACTICE) {
                answers[s.index] = "NA"
                locked[s.index] = true
            } else {
                answers[s.index] = if (answers[s.index] == "NA") null else "NA"
            }
            session = s.copy(answers = answers, locked = locked)
            return
        }

        if (s.mode == QuizMode.EXAM) {
            val answers = s.answers.toMutableList()
            answers[s.index] = if (answers[s.index] == key) null else key
            session = s.copy(answers = answers)
            return
        }

        val answers = s.answers.toMutableList().apply { this[s.index] = key }
        val locked = s.locked.toMutableList().apply { this[s.index] = true }
        session = s.copy(answers = answers, locked = locked)

        if (!question.hasAnswer) return

        val isCorrect = key == question.answer
        val old = state.progress[question.id] ?: QuestionProgress()
        val progress = old.copy(
            attempts = old.attempts + 1,
            correct = old.correct + if (isCorrect) 1 else 0
        )
        var wrongIds = state.wrongIds
        if (isCorrect) {
            wrongIds = wrongIds.filterNot { it == question.id }
        } else if (question.id !in wrongIds) {
            wrongIds = listOf(question.id) + wrongIds
        }
        state = state.copy(progress = state.progress + (question.id to progress), wrongIds = wrongIds)
        persist()
    }

    fun jump(position: Int) {
        val s = session ?: return
        if (position in s.queue.indices) session = s.copy(index = position)
    }

    fun next() {
        val s = session ?: return
        if (s.index + 1 < s.queue.size) {
            session = s.copy(index = s.index + 1)
        }
    }

    fun previous() {
        val s = session ?: return
        if (s.index > 0) session = s.copy(index = s.index - 1)
    }

    val unansweredCount: Int
        get() {
            val s = session ?: return 0
            return s.queue.indices.count { i -> s.queue[i].hasAnswer && s.answers[i] == null }
        }

    val noAnswerCount: Int
        get() = session?.queue?.count { !it.hasAnswer } ?: 0

    fun finish() {
        val s = session ?: return
        if (s.queue.isEmpty()) return

        var progress = state.progress
        var wrongIds = state.wrongIds

        if (s.mode == QuizMode.EXAM) {
            s.queue.forEachIndexed { index, question ->
                if (!question.hasAnswer || s.answers[index] == null) return@forEachIndexed
                val correct = s.answers[index] == question.answer
                val old = progress[question.id] ?: QuestionProgress()
                progress = progress + (question.id to old.copy(
                    attempts = old.attempts + 1,
                    correct = old.correct + if (correct) 1 else 0
                ))
                if (correct) wrongIds = wrongIds.filterNot { it == question.id }
                else if (question.id !in wrongIds) wrongIds = listOf(question.id) + wrongIds
            }
        }

        val items = s.queue.mapIndexedNotNull { index, question ->
            if (!question.hasAnswer) return@mapIndexedNotNull null
            if (s.mode == QuizMode.PRACTICE && s.answers[index] == null) return@mapIndexedNotNull null
            ResultItem(question, s.answers[index])
        }

        if (s.mode == QuizMode.PRACTICE && items.isEmpty()) {
            showToast("本轮还没有可评分的作答")
            return
        }

        val total = items.size
        val correct = items.count { it.isCorrect }
        val unanswered = if (s.mode == QuizMode.EXAM) {
            s.queue.indices.count { i -> s.queue[i].hasAnswer && s.answers[i] == null }
        } else 0
        val noAnswerCount = s.queue.count { !it.hasAnswer }
        val duration = (System.currentTimeMillis() - s.startTime) / 1000

        val order = mutableListOf<String>()
        val stats = mutableMapOf<String, Pair<Int, Int>>()
        items.forEach { item ->
            val chapter = item.question.chapter
            if (chapter !in order) order.add(chapter)
            val (c, t) = stats[chapter] ?: (0 to 0)
            stats[chapter] = (c + if (item.isCorrect) 1 else 0) to (t + 1)
        }
        val chapterStats = order.map { name ->
            val (c, t) = stats[name] ?: (0 to 0)
            ChapterStat(name, c, t)
        }

        if (total > 0) {
            val attempts = (state.attempts + Attempt(
                total = total,
                correct = correct,
                scope = scopeLabel(s.scope)
            )).takeLast(100)
            state = state.copy(progress = progress, wrongIds = wrongIds, attempts = attempts)
        } else {
            state = state.copy(progress = progress, wrongIds = wrongIds)
        }
        persist()

        result = QuizResult(
            total = total,
            correct = correct,
            unanswered = unanswered,
            noAnswerCount = noAnswerCount,
            durationSeconds = duration,
            scopeLabel = scopeLabel(s.scope),
            mode = s.mode,
            items = items,
            chapterStats = chapterStats
        )
        screen = Screen.RESULT
    }

    fun leaveQuiz() {
        session = null
        screen = Screen.HOME
        state = state.copy(selectedTab = AppTab.HOME)
    }

    fun goHome() {
        screen = Screen.HOME
        state = state.copy(selectedTab = AppTab.HOME)
    }

    fun openWrongBook() {
        screen = Screen.WRONG
    }

    fun closeWrongBook() {
        screen = Screen.HOME
    }

    fun setTab(tab: AppTab) {
        state = state.copy(selectedTab = tab)
    }

    // MARK: - 设置

    fun setDefaultMode(mode: QuizMode) {
        state = state.copy(defaultMode = mode); persist()
    }

    fun setDefaultSize(size: Int) {
        state = state.copy(defaultSize = size); persist()
    }

    fun setDefaultOrder(order: QuizOrder) {
        state = state.copy(defaultOrder = order); persist()
    }

    fun setHaptics(value: Boolean) {
        state = state.copy(haptics = value); persist()
    }

    fun removeWrong(id: String) {
        state = state.copy(wrongIds = state.wrongIds.filterNot { it == id }); persist()
    }

    fun clearWrong() {
        state = state.copy(wrongIds = emptyList()); persist()
    }

    fun clearProgress() {
        state = state.copy(progress = emptyMap(), wrongIds = emptyList(), attempts = emptyList()); persist()
    }

    // MARK: - 备份

    fun backupJson(): String = JSONObject().apply {
        put("version", 1)
        put("exportedAt", System.currentTimeMillis())
        put("banks", JSONArray().also { arr ->
            state.banks.forEach { arr.put(bankJson(it)) }
        })
        put("progress", JSONObject().also { obj ->
            state.progress.forEach { (id, p) ->
                obj.put(id, JSONObject().put("attempts", p.attempts).put("correct", p.correct))
            }
        })
        put("wrong", JSONArray(state.wrongIds))
        put("attempts", JSONArray().also { arr ->
            state.attempts.forEach { a ->
                arr.put(JSONObject().apply {
                    put("id", a.id); put("total", a.total); put("correct", a.correct)
                    put("date", a.date); put("scope", a.scope)
                })
            }
        })
    }.toString(2)

    fun restoreBackup(text: String) {
        val root = JSONObject(text)
        val banksArray = root.optJSONArray("banks") ?: throw ImportException("备份里没有题库")
        if (banksArray.length() == 0) throw ImportException("备份里没有题库")
        val banks = (0 until banksArray.length()).mapNotNull { banksArray.optJSONObject(it)?.let(::bankFromJson) }
        val progressObj = root.optJSONObject("progress")
        val progress = mutableMapOf<String, QuestionProgress>()
        progressObj?.keys()?.forEach { id ->
            val obj = progressObj.optJSONObject(id) ?: return@forEach
            progress[id] = QuestionProgress(obj.optInt("attempts"), obj.optInt("correct"))
        }
        val wrongArray = root.optJSONArray("wrong")
        val wrong = (0 until (wrongArray?.length() ?: 0)).mapNotNull { wrongArray?.optString(it) }
        val attemptsArray = root.optJSONArray("attempts")
        val attempts = (0 until (attemptsArray?.length() ?: 0)).mapNotNull { i ->
            attemptsArray?.optJSONObject(i)?.let {
                Attempt(
                    id = it.optString("id", UUID.randomUUID().toString()),
                    total = it.optInt("total"),
                    correct = it.optInt("correct"),
                    date = it.optLong("date", System.currentTimeMillis()),
                    scope = it.optString("scope")
                )
            }
        }
        state = state.copy(
            banks = banks,
            progress = progress,
            wrongIds = wrong.filter { id -> banks.any { b -> b.questions.any { it.id == id } } },
            attempts = attempts,
            recentBankId = banks.firstOrNull()?.id
        )
        persist()
        showToast("备份已恢复")
    }

    // MARK: - 提示

    fun showToast(message: String) {
        toast = message
        toastJob?.cancel()
        toastJob = scope.launch {
            delay(2000)
            if (toast == message) toast = null
        }
    }

    // MARK: - 持久化

    private fun persist() {
        try {
            stateFile.writeText(stateJson().toString())
        } catch (_: Exception) {
        }
    }

    private fun load() {
        if (!stateFile.exists()) return
        try {
            val root = JSONObject(stateFile.readText())
            val banksArray = root.optJSONArray("banks") ?: return
            val banks = (0 until banksArray.length()).mapNotNull { banksArray.optJSONObject(it)?.let(::bankFromJson) }
            val progressObj = root.optJSONObject("progress")
            val progress = mutableMapOf<String, QuestionProgress>()
            progressObj?.keys()?.forEach { id ->
                progressObj.optJSONObject(id)?.let {
                    progress[id] = QuestionProgress(it.optInt("attempts"), it.optInt("correct"))
                }
            }
            val wrongArray = root.optJSONArray("wrong")
            val wrong = (0 until (wrongArray?.length() ?: 0)).mapNotNull { wrongArray?.optString(it) }
            val attemptsArray = root.optJSONArray("attempts")
            val attempts = (0 until (attemptsArray?.length() ?: 0)).mapNotNull { i ->
                attemptsArray?.optJSONObject(i)?.let {
                    Attempt(
                        id = it.optString("id", UUID.randomUUID().toString()),
                        total = it.optInt("total"),
                        correct = it.optInt("correct"),
                        date = it.optLong("date", System.currentTimeMillis()),
                        scope = it.optString("scope")
                    )
                }
            }
            state = AppState(
                banks = banks,
                wrongIds = wrong,
                progress = progress,
                attempts = attempts,
                recentBankId = root.optString("recentBankId").takeIf { it.isNotBlank() },
                defaultMode = runCatching { QuizMode.valueOf(root.optString("defaultMode")) }.getOrDefault(QuizMode.PRACTICE),
                defaultSize = root.optInt("defaultSize", 10),
                defaultOrder = runCatching { QuizOrder.valueOf(root.optString("defaultOrder")) }.getOrDefault(QuizOrder.SEQUENTIAL),
                haptics = root.optBoolean("haptics", true)
            )
        } catch (_: Exception) {
        }
    }

    private fun stateJson(): JSONObject = JSONObject().apply {
        put("version", 1)
        put("banks", JSONArray().also { arr -> state.banks.forEach { arr.put(bankJson(it)) } })
        put("wrong", JSONArray(state.wrongIds))
        put("progress", JSONObject().also { obj ->
            state.progress.forEach { (id, p) ->
                obj.put(id, JSONObject().put("attempts", p.attempts).put("correct", p.correct))
            }
        })
        put("attempts", JSONArray().also { arr ->
            state.attempts.forEach { a ->
                arr.put(JSONObject().apply {
                    put("id", a.id); put("total", a.total); put("correct", a.correct)
                    put("date", a.date); put("scope", a.scope)
                })
            }
        })
        put("recentBankId", state.recentBankId ?: "")
        put("defaultMode", state.defaultMode.name)
        put("defaultSize", state.defaultSize)
        put("defaultOrder", state.defaultOrder.name)
        put("haptics", state.haptics)
    }

    private fun bankJson(bank: QuizBank): JSONObject = JSONObject().apply {
        put("id", bank.id)
        put("name", bank.name)
        put("fileName", bank.fileName)
        put("importedAt", bank.importedAt)
        put("questions", JSONArray().also { arr ->
            bank.questions.forEach { q ->
                arr.put(JSONObject().apply {
                    put("id", q.id)
                    put("bankId", q.bankId)
                    put("chapter", q.chapter)
                    put("num", q.num)
                    put("stem", q.stem)
                    put("options", JSONArray().also { opts ->
                        q.options.forEach { opt ->
                            opts.put(JSONArray().also { pair ->
                                opt.forEach { value -> pair.put(value) }
                            })
                        }
                    })
                    if (q.answer == null) put("answer", JSONObject.NULL) else put("answer", q.answer)
                })
            }
        })
    }

    private fun bankFromJson(obj: JSONObject): QuizBank? {
        val id = obj.optString("id").takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
        val name = obj.optString("name").ifBlank { "未命名题库" }
        val fileName = obj.optString("fileName").ifBlank { "$name.docx" }
        val array = obj.optJSONArray("questions") ?: return null
        val questions = (0 until array.length()).mapNotNull { i ->
            val q = array.optJSONObject(i) ?: return@mapNotNull null
            val options = mutableListOf<List<String>>()
            q.optJSONArray("options")?.let { opts ->
                for (j in 0 until opts.length()) {
                    val pair = opts.optJSONArray(j) ?: continue
                    options.add((0 until pair.length()).map { pair.optString(it) })
                }
            }
            Question(
                id = q.optString("id").ifBlank { "$id-$i" },
                bankId = q.optString("bankId").ifBlank { id },
                chapter = q.optString("chapter").ifBlank { name },
                num = q.optString("num").ifBlank { "${i + 1}" },
                stem = q.optString("stem"),
                options = options,
                answer = if (q.isNull("answer")) null else q.optString("answer").takeIf { it.isNotBlank() }
            )
        }
        return QuizBank(
            id = id,
            name = name,
            fileName = fileName,
            importedAt = obj.optLong("importedAt", System.currentTimeMillis()),
            questions = questions
        )
    }
}
