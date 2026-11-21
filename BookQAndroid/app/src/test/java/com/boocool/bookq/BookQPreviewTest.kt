package com.boocool.bookq

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import org.junit.Rule
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class BookQPreviewTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        theme = "android:Theme.Material.Light.NoActionBar",
        showSystemUi = false
    )

    @Test
    fun home() {
        val store = store()
        paparazzi.snapshot {
            AppFrame(store, AppTab.HOME) {
                HomeScreen(store, onStart = {}, onWrong = {})
            }
        }
    }

    @Test
    fun homeDark() {
        val store = store()
        store.setTheme(AppTheme.RUST)
        store.setAppearance(AppAppearance.DARK)
        paparazzi.snapshot {
            AppFrame(store, AppTab.HOME) {
                HomeScreen(store, onStart = {}, onWrong = {})
            }
        }
    }

    @Test
    fun quizDark() {
        val store = store()
        store.setTheme(AppTheme.INDIGO)
        store.setAppearance(AppAppearance.DARK)
        val bank = store.state.banks.first()
        val queue = bank.questions.take(10)
        val session = QuizSession(
            queue = queue,
            answers = List(10) { if (it == 0) "B" else null },
            locked = List(10) { it == 0 },
            index = 0,
            mode = QuizMode.PRACTICE,
            order = QuizOrder.SEQUENTIAL,
            scope = QuizScope.Bank(bank.id),
            size = 10
        )
        store.previewField("session", session)
        paparazzi.snapshot {
            BookQTheme {
                Box(Modifier.fillMaxSize()) {
                    PaperBackground()
                    QuizScreen(store) {}
                }
            }
        }
    }

    @Test
    fun library() {
        val store = store()
        paparazzi.snapshot {
            AppFrame(store, AppTab.LIBRARY) {
                LibraryScreen(store, onImport = {}, onOpenBank = {})
            }
        }
    }

    @Test
    fun settings() {
        val store = store()
        paparazzi.snapshot {
            AppFrame(store, AppTab.SETTINGS) {
                SettingsScreen(store, onExportBackup = {}, onRestoreBackup = {})
            }
        }
    }

    @Test
    fun settingsPractice() {
        val store = store()
        paparazzi.snapshot {
            AppFrame(store, AppTab.SETTINGS) {
                SettingsScreen(store, onExportBackup = {}, onRestoreBackup = {}, initialPage = SettingsPage.PRACTICE)
            }
        }
    }

    @Test
    fun settingsAppearance() {
        val store = store()
        store.setTheme(AppTheme.INDIGO)
        store.setAppearance(AppAppearance.LIGHT)
        paparazzi.snapshot {
            AppFrame(store, AppTab.SETTINGS) {
                SettingsScreen(store, onExportBackup = {}, onRestoreBackup = {}, initialPage = SettingsPage.APPEARANCE)
            }
        }
    }

    @Test
    fun settingsAppearanceDark() {
        val store = store()
        store.setTheme(AppTheme.RUST)
        store.setAppearance(AppAppearance.DARK)
        paparazzi.snapshot {
            AppFrame(store, AppTab.SETTINGS) {
                SettingsScreen(store, onExportBackup = {}, onRestoreBackup = {}, initialPage = SettingsPage.APPEARANCE)
            }
        }
    }

    @Test
    fun sequenceCursorRemembersPosition() {
        val store = store()
        val bank = store.state.banks.first()
        val scope = QuizScope.Bank(bank.id)
        store.startQuiz(scope, QuizMode.PRACTICE, 10, QuizOrder.SEQUENTIAL)
        assertEquals(bank.questions.first().id, store.session?.queue?.first()?.id)
        val answer = store.session!!.queue.first().answer ?: "A"
        store.choose(answer)
        store.restart()
        assertEquals(bank.questions[1].id, store.session?.queue?.first()?.id)
    }

    @Test
    fun themePreviewLight() {
        val store = store()
        store.setTheme(AppTheme.INDIGO)
        store.setAppearance(AppAppearance.LIGHT)
        paparazzi.snapshot {
            BookQTheme {
                Box(Modifier.fillMaxSize().background(BookQPalettes.IndigoLight.paper)) {
                    ThemePreviewPanel(
                        theme = AppTheme.RUST,
                        appearance = AppAppearance.LIGHT,
                        currentTheme = AppTheme.INDIGO,
                        currentAppearance = AppAppearance.LIGHT,
                        onDismiss = {},
                        onApply = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(640.dp)
                            .align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }

    @Test
    fun themePreviewDark() {
        val store = store()
        store.setTheme(AppTheme.INDIGO)
        store.setAppearance(AppAppearance.LIGHT)
        paparazzi.snapshot {
            BookQTheme {
                Box(Modifier.fillMaxSize().background(BookQPalettes.IndigoLight.paper)) {
                    ThemePreviewPanel(
                        theme = AppTheme.MOSS,
                        appearance = AppAppearance.DARK,
                        currentTheme = AppTheme.INDIGO,
                        currentAppearance = AppAppearance.LIGHT,
                        onDismiss = {},
                        onApply = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(640.dp)
                            .align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }

    @Test
    fun settingsData() {
        val store = store()
        paparazzi.snapshot {
            AppFrame(store, AppTab.SETTINGS) {
                SettingsScreen(store, onExportBackup = {}, onRestoreBackup = {}, initialPage = SettingsPage.DATA)
            }
        }
    }

    @Test
    fun settingsExperience() {
        val store = store()
        paparazzi.snapshot {
            AppFrame(store, AppTab.SETTINGS) {
                SettingsScreen(store, onExportBackup = {}, onRestoreBackup = {}, initialPage = SettingsPage.EXPERIENCE)
            }
        }
    }

    @Test
    fun settingsAbout() {
        val store = store()
        paparazzi.snapshot {
            AppFrame(store, AppTab.SETTINGS) {
                SettingsScreen(store, onExportBackup = {}, onRestoreBackup = {}, initialPage = SettingsPage.ABOUT)
            }
        }
    }

    @Test
    fun settingsImportHelp() {
        val store = store()
        paparazzi.snapshot {
            AppFrame(store, AppTab.SETTINGS) {
                SettingsScreen(store, onExportBackup = {}, onRestoreBackup = {}, initialPage = SettingsPage.IMPORT_HELP)
            }
        }
    }

    @Test
    fun quiz() {
        val store = store()
        val bank = store.state.banks.first()
        val queue = bank.questions.take(10)
        val session = QuizSession(
            queue = queue,
            answers = List(10) { if (it == 0) "B" else null },
            locked = List(10) { it == 0 },
            index = 0,
            mode = QuizMode.PRACTICE,
            order = QuizOrder.SEQUENTIAL,
            scope = QuizScope.Bank(bank.id),
            size = 10
        )
        store.previewField("session", session)
        paparazzi.snapshot {
            BookQTheme {
                Box(Modifier.fillMaxSize()) {
                    PaperBackground()
                    QuizScreen(store) {}
                }
            }
        }
    }

    @Test
    fun noAnswer() {
        val store = store()
        val bank = QuizBank(
            id = "uv",
            name = "UV题目及答案",
            fileName = "UV题目及答案.docx",
            questions = listOf(
                Question(
                    id = "uv-25",
                    bankId = "uv",
                    chapter = "UV题目及答案",
                    num = "25",
                    stem = "UV喷绘打印前，对承印材料进行表面处理的主要目的是什么？",
                    options = listOf(
                        listOf("A", "提高材料的柔韧性"),
                        listOf("B", "增强墨水与材料的附着力"),
                        listOf("C", "降低材料的气味"),
                        listOf("D", "改变材料的颜色")
                    ),
                    answer = null
                )
            )
        )
        val session = QuizSession(
            queue = bank.questions,
            answers = listOf("A"),
            locked = listOf(true),
            index = 0,
            mode = QuizMode.PRACTICE,
            order = QuizOrder.SEQUENTIAL,
            scope = QuizScope.Bank(bank.id),
            size = 1
        )
        store.previewField("session", session)
        paparazzi.snapshot {
            BookQTheme {
                Box(Modifier.fillMaxSize()) {
                    PaperBackground()
                    QuizScreen(store) {}
                }
            }
        }
    }

    @Test
    fun result() {
        val store = store()
        val bank = store.state.banks.first()
        val items = bank.questions.take(10).mapIndexed { index, question ->
            ResultItem(
                question = question,
                choice = if (index == 9) null else question.answer
            )
        }
        val result = QuizResult(
            total = 10,
            correct = 9,
            unanswered = 1,
            noAnswerCount = 0,
            durationSeconds = 412,
            scopeLabel = bank.name,
            mode = QuizMode.PRACTICE,
            items = items,
            chapterStats = listOf(
                ChapterStat("布艺题目及答案", 9, 10)
            )
        )
        store.previewField("result", result)
        paparazzi.snapshot {
            BookQTheme {
                Box(Modifier.fillMaxSize()) {
                    PaperBackground()
                    ResultScreen(store)
                }
            }
        }
    }

    @Composable
    private fun AppFrame(store: BookQStore, tab: AppTab, content: @Composable () -> Unit) {
        BookQTheme {
            Scaffold(
                bottomBar = {
                    BottomTabBar(
                        selected = tab,
                        bankCount = store.state.banks.size,
                        wrongCount = store.state.wrongIds.size,
                        onSelect = {}
                    )
                },
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets(0, 0, 0, 0)
            ) { padding ->
                Box(Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding())) {
                    PaperBackground()
                    content()
                }
            }
        }
    }

    private fun store(): BookQStore {
        val store = BookQStore(paparazzi.context)
        val fabric = previewBank(
            id = "fabric",
            name = "布艺题目及答案",
            count = 100,
            start = 1
        )
        val uv = previewBank(
            id = "uv",
            name = "UV题目及答案",
            count = 107,
            start = 1
        )
        val all = fabric.questions + uv.questions
        val wrongIds = all.take(7).map { it.id }
        val attempts = listOf(
            Attempt(total = 20, correct = 16, scope = "布艺题目及答案", date = todayAt(9, 20)),
            Attempt(total = 10, correct = 8, scope = "UV题目及答案", date = todayAt(15, 10)),
            Attempt(total = 15, correct = 11, scope = "布艺题目及答案", date = yesterdayAt(20, 5))
        )
        val progress = all.take(24).mapIndexed { index, q ->
            q.id to QuestionProgress(attempts = 2, correct = if (index % 4 == 0) 1 else 2)
        }.toMap()
        val state = AppState(
            banks = listOf(fabric, uv),
            selectedTab = AppTab.HOME,
            wrongIds = wrongIds,
            progress = progress,
            attempts = attempts,
            recentBankIds = listOf(fabric.id, uv.id),
            defaultMode = QuizMode.PRACTICE,
            defaultSize = 10,
            defaultOrder = QuizOrder.SEQUENTIAL,
            haptics = true
        )
        store.previewField("state", state)
        Palette.apply(state.theme, state.appearance)
        return store
    }

    private fun previewBank(id: String, name: String, count: Int, start: Int): QuizBank {
        val questions = (start until start + count).map { num ->
            val qid = "$id-$num"
            val optionCount = if (num % 9 == 0) 3 else 4
            val keys = listOf("A", "B", "C", "D").take(optionCount)
            val answer = when (num % 4) {
                0 -> "A"
                1 -> "B"
                2 -> "C"
                else -> "D"
            }
            Question(
                id = qid,
                bankId = id,
                chapter = name,
                num = num.toString(),
                stem = previewStem(num, name),
                options = keys.map { key ->
                    listOf(key, previewOption(key, num))
                },
                answer = answer
            )
        }
        return QuizBank(
            id = id,
            name = name,
            fileName = "$name.docx",
            importedAt = System.currentTimeMillis(),
            questions = questions
        )
    }

    private fun previewStem(num: Int, bank: String): String {
        val fabric = listOf(
            "快幕秀画面布常用的材质是哪种？",
            "600D 牛津布中的 600D 指的是什么？",
            "经编布与纬编布的主要区别是什么？",
            "PVC 灯箱布相比网布更适合哪种场景？",
            "张拉膜结构常用的膜材是哪种？",
            "织物喷绘前进行涂层处理的主要作用是？"
        )
        val uv = listOf(
            "UV 喷绘使用的墨水固化方式是哪种？",
            "UV 打印前对材料进行表面处理的主要目的是？",
            "以下哪种材料最适合户外长期展示？",
            "UV 灯箱布打印时常见的色彩模式是？",
            "UV 墨水相比溶剂墨水的主要优势是？",
            "白墨在 UV 打印中的主要作用是？"
        )
        val pool = if (bank.startsWith("UV")) uv else fabric
        return "第 $num 题 · " + pool[(num - 1) % pool.size]
    }

    private fun previewOption(key: String, num: Int): String {
        val pool = listOf(
            "涤纶经编布",
            "600D 牛津布",
            "PVC 灯箱布",
            "张拉膜专用膜材",
            "环保可回收材料",
            "高密度聚乙烯"
        )
        return "$key. " + pool[(num + key.first().code) % pool.size]
    }

    private fun todayAt(hour: Int, minute: Int): Long =
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun yesterdayAt(hour: Int, minute: Int): Long =
        Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
}

@Suppress("UNCHECKED_CAST")
private fun BookQStore.previewField(name: String, value: Any?) {
    val field = BookQStore::class.java.declaredFields.first {
        it.name == "$name\$delegate" || it.name == name
    }
    field.isAccessible = true
    (field.get(this) as MutableState<Any?>).value = value
}
