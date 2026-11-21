package com.boocool.bookq

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private enum class OptionVisual { NORMAL, SELECTED, CORRECT, WRONG, DIM, NO_ANSWER }

@Composable
fun QuizScreen(store: BookQStore, onFinished: () -> Unit) {
    val session = store.session ?: return
    val question = session.queue.getOrNull(session.index) ?: return
    val answer = session.answers.getOrNull(session.index)
    val locked = session.mode == QuizMode.PRACTICE && session.locked.getOrNull(session.index) == true

    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    var showSheet by remember { mutableStateOf(false) }
    var confirmExit by remember { mutableStateOf(false) }
    var confirmSubmit by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(session.startTime) {
        while (true) {
            delay(1000)
            now = System.currentTimeMillis()
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 40.dp)
        ) {
            QuizTopBar(
                store = store,
                session = session,
                elapsed = (now - session.startTime) / 1000,
                onBack = {
                    if (session.answers.any { it != null }) confirmExit = true else store.leaveQuiz()
                },
                onOpenSheet = { showSheet = true }
            )
            ProgressTrack(
                value = if (session.queue.isEmpty()) 0.0 else (session.index + 1).toDouble() / session.queue.size,
                height = 4.dp,
                low = true,
                modifier = Modifier.padding(top = 13.dp)
            )
            QuestionCard(
                question = question,
                answer = answer,
                mode = session.mode,
                locked = locked,
                onChoose = { key ->
                    store.choose(key)
                    if (store.state.haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            )
            QuizBottomBar(
                session = session,
                onPrevious = { store.previous() },
                onNext = {
                    if (session.index + 1 < session.queue.size) {
                        store.next()
                    } else if (session.mode == QuizMode.EXAM && store.unansweredCount > 0) {
                        confirmSubmit = true
                    } else {
                        store.finish()
                        onFinished()
                    }
                }
            )
        }
    }

    if (showSheet) {
        AnswerSheet(
            store = store,
            onDismiss = { showSheet = false },
            onSubmit = {
                showSheet = false
                if (session.mode == QuizMode.EXAM && store.unansweredCount > 0) {
                    confirmSubmit = true
                } else {
                    store.finish()
                    onFinished()
                }
            }
        )
    }

    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text("结束本轮？", style = serif(20)) },
            text = { Text("已作答的记录会保留，可以回主页重新开始。", style = sans(13)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmExit = false
                    store.leaveQuiz()
                }) { Text("结束并返回主页", color = Palette.Red) }
            },
            dismissButton = { TextButton(onClick = { confirmExit = false }) { Text("继续答题") } }
        )
    }

    if (confirmSubmit) {
        AlertDialog(
            onDismissRequest = { confirmSubmit = false },
            title = { Text("还有 ${store.unansweredCount} 题未作答", style = serif(20)) },
            text = { Text("未作答将计为错误。", style = sans(13)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmSubmit = false
                    store.finish()
                    onFinished()
                }) { Text("交卷") }
            },
            dismissButton = { TextButton(onClick = { confirmSubmit = false }) { Text("继续答题") } }
        )
    }
}

@Composable
private fun QuizTopBar(
    store: BookQStore,
    session: QuizSession,
    elapsed: Long,
    onBack: () -> Unit,
    onOpenSheet: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Palette.Card)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.ArrowBack, contentDescription = "返回", tint = Palette.Ink, modifier = Modifier.size(18.dp))
        }
        Row(
            Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PillLabel(store.scopeLabel(session.scope), modifier = Modifier.weight(1f, fill = false))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Icon(Icons.Outlined.Timer, contentDescription = null, tint = Palette.Muted, modifier = Modifier.size(13.dp))
                Text(formatDuration(elapsed), style = sans(13).copy(color = Palette.Ink2))
            }
        }
        Text(
            "${minOf(session.index + 1, session.queue.size)}/${session.queue.size}",
            style = serif(19)
        )
        Box(
            Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Palette.Card)
                .clickable(onClick = onOpenSheet),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.GridView, contentDescription = "答题卡", tint = Palette.Primary, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun QuestionCard(
    question: Question,
    answer: String?,
    mode: QuizMode,
    locked: Boolean,
    onChoose: (String) -> Unit
) {
    val shape = RoundedCornerShape(22.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .padding(top = 22.dp)
            .shadow(10.dp, shape)
            .clip(shape)
            .background(Palette.Card)
    ) {
        Column(Modifier.padding(20.dp)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(
                        Brush.linearGradient(listOf(Palette.Primary, Palette.Primary2, Palette.Gold))
                    )
            )
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    question.num,
                    style = serif(44).copy(color = Palette.Number),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    PillLabel(question.chapter)
                    Text(question.metaLine, style = sans(11).copy(color = Palette.Muted), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Text(
                question.stem,
                style = sans(18, FontWeight.SemiBold).copy(lineHeight = 28.sp),
                modifier = Modifier.padding(top = 18.dp, bottom = 24.dp)
            )

            if (question.options.isEmpty()) {
                NoOptionBlock(answered = answer != null, onConfirm = { onChoose("NA") })
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                    question.options.forEach { option ->
                        val key = option.firstOrNull() ?: ""
                        OptionRow(
                            key = key,
                            text = option.getOrNull(1).orEmpty(),
                            visual = visualFor(key, question, answer, mode, locked),
                            enabled = mode == QuizMode.EXAM || !locked,
                            onClick = { onChoose(key) }
                        )
                    }
                }
            }

            NoteBar(question, answer, mode, locked)
        }
    }
}

@Composable
private fun NoOptionBlock(answered: Boolean, onConfirm: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(Palette.AmberBg)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("本题题库未提供选项与答案。", style = sans(13).copy(color = Palette.Amber), textAlign = TextAlign.Center)
        if (answered) {
            Text("已确认：题库中无答案", style = sans(13, FontWeight.SemiBold).copy(color = Palette.Amber))
        } else {
            GhostPillButton("确认作答（题库无答案）", onClick = onConfirm)
        }
    }
}

@Composable
private fun OptionRow(
    key: String,
    text: String,
    visual: OptionVisual,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val background = when (visual) {
        OptionVisual.SELECTED -> Palette.SelectedBg
        OptionVisual.CORRECT -> Palette.GreenBg
        OptionVisual.WRONG -> Palette.RedBg
        OptionVisual.NO_ANSWER -> Palette.AmberBg
        else -> Palette.SoftPaper
    }
    val border = when (visual) {
        OptionVisual.SELECTED -> Palette.Primary
        OptionVisual.CORRECT -> Palette.Green
        OptionVisual.WRONG -> Palette.Red
        OptionVisual.NO_ANSWER -> Palette.AmberLine
        else -> Palette.Line
    }
    val keyBackground = when (visual) {
        OptionVisual.SELECTED -> Palette.Primary
        OptionVisual.CORRECT -> Palette.Green
        OptionVisual.WRONG -> Palette.Red
        OptionVisual.NO_ANSWER -> Palette.Gold
        else -> Palette.Track
    }
    val keyColor = if (visual == OptionVisual.NORMAL || visual == OptionVisual.DIM) Palette.InkSoft else Color.White
    val mark = when (visual) {
        OptionVisual.CORRECT -> Icons.Outlined.Check
        OptionVisual.WRONG -> Icons.Outlined.Close
        else -> null
    }

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(background)
            .then(Modifier.clickable(enabled = enabled, onClick = onClick))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(keyBackground),
            contentAlignment = Alignment.Center
        ) {
            Text(key, style = sans(14, FontWeight.Bold).copy(color = keyColor))
        }
        Text(text, style = sans(15), modifier = Modifier.weight(1f))
        if (mark != null) {
            Icon(
                mark,
                contentDescription = null,
                tint = if (visual == OptionVisual.CORRECT) Palette.Green else Palette.Red,
                modifier = Modifier.size(18.dp)
            )
        }
    }
    // 1dp 边框单独绘制，避免 clickable 改变背景顺序
    Box(
        Modifier
            .fillMaxWidth()
            .height(0.dp)
            .padding(horizontal = 0.dp)
    )
}

private fun visualFor(
    key: String,
    question: Question,
    answer: String?,
    mode: QuizMode,
    locked: Boolean
): OptionVisual {
    if (!question.hasAnswer) return if (answer == key) OptionVisual.NO_ANSWER else OptionVisual.NORMAL
    if (mode == QuizMode.EXAM) return if (answer == key) OptionVisual.SELECTED else OptionVisual.NORMAL
    if (!locked) return if (answer == key) OptionVisual.SELECTED else OptionVisual.NORMAL
    if (key == question.answer) return OptionVisual.CORRECT
    if (key == answer) return OptionVisual.WRONG
    return OptionVisual.DIM
}

@Composable
private fun NoteBar(question: Question, answer: String?, mode: QuizMode, locked: Boolean) {
    if (mode == QuizMode.PRACTICE && locked) {
        val (color, content) = when {
            !question.hasAnswer -> Palette.Amber to "题库中无答案，本题不计分"
            answer == question.answer -> Palette.Green to "回答正确，答案 ${question.answer ?: ""}"
            else -> Palette.Red to "回答错误，正确答案是 ${question.answerDisplay}"
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                if (color == Palette.Green) Icons.Outlined.Check else if (color == Palette.Red) Icons.Outlined.Close else Icons.Outlined.Info,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Text(content, style = sans(13).copy(color = color), lineHeight = 19.sp)
        }
    } else if (mode == QuizMode.EXAM) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Outlined.Info, contentDescription = null, tint = Palette.Muted, modifier = Modifier.size(18.dp))
            Text(
                if (question.hasAnswer) "考试模式：选择后可修改，交卷后统一评分" else "本题题库中无答案，选择后不计分",
                style = sans(13).copy(color = Palette.Muted),
                lineHeight = 19.sp
            )
        }
    }
}

@Composable
private fun QuizBottomBar(session: QuizSession, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GhostPillButton(
            "上一题",
            modifier = Modifier.then(if (session.index == 0) Modifier.padding(0.dp) else Modifier)
        ) { if (session.index > 0) onPrevious() }
        Spacer(Modifier.weight(1f))
        Row(
            Modifier
                .clip(CircleShape)
                .background(Palette.Primary)
                .clickable(onClick = onNext)
                .padding(horizontal = 22.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                if (session.index + 1 < session.queue.size) "下一题" else if (session.mode == QuizMode.EXAM) "交卷" else "查看结果",
                style = sans(15, FontWeight.SemiBold).copy(color = Color.White)
            )
            Icon(Icons.Outlined.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnswerSheet(
    store: BookQStore,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit
) {
    val session = store.session ?: return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val answered = session.answers.count { it != null }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Palette.Card,
        dragHandle = { SheetGrabber() }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("答题卡", style = serif(18), modifier = Modifier.weight(1f))
                CloseCircle(onDismiss)
            }
            Text(
                "共 ${session.queue.size} 题，已作答 $answered 题" +
                    if (store.noAnswerCount > 0) " · ${store.noAnswerCount} 题无答案" else "",
                style = sans(12).copy(color = Palette.Muted),
                modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
            )

            session.queue.indices.chunked(5).forEach { row ->
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 9.dp),
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    row.forEach { position -> SheetCell(store, session, position, Modifier.weight(1f)) }
                    repeat(5 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }

            Column(Modifier.padding(top = 9.dp, bottom = 22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    Legend(Palette.ChipBg, "未作答")
                    Legend(Palette.AnsweredBg, "已作答")
                    Legend(Palette.GreenBg, "答对")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    Legend(Palette.RedBg, "答错")
                    Legend(Palette.AmberBg, "题库无答案")
                }
            }

            PrimaryButton(
                text = if (session.mode == QuizMode.EXAM) "交卷并查看结果" else "结束本轮",
                modifier = Modifier.fillMaxWidth()
            ) { onSubmit() }
        }
    }
}

@Composable
private fun SheetCell(store: BookQStore, session: QuizSession, position: Int, modifier: Modifier = Modifier) {
    val question = session.queue[position]
    val answer = session.answers[position]
    val locked = session.mode == QuizMode.PRACTICE && session.locked[position]
    val current = position == session.index

    val fill = when {
        !question.hasAnswer && answer != null -> Palette.AmberBg
        session.mode == QuizMode.PRACTICE && locked && answer == question.answer -> Palette.GreenBg
        session.mode == QuizMode.PRACTICE && locked -> Palette.RedBg
        answer != null -> Palette.AnsweredBg
        else -> Palette.ChipBg
    }
    val textColor = when {
        !question.hasAnswer && answer != null -> Palette.Amber
        session.mode == QuizMode.PRACTICE && locked && answer == question.answer -> Palette.Green
        session.mode == QuizMode.PRACTICE && locked -> Palette.Red
        answer != null -> Palette.Primary
        else -> Palette.Ink2
    }

    Box(
        modifier
            .height(42.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(fill)
            .clickable { store.jump(position) },
        contentAlignment = Alignment.Center
    ) {
        Text("${position + 1}", style = sans(13, FontWeight.SemiBold).copy(color = textColor))
    }
}

@Composable
private fun Legend(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Box(
            Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color)
        )
        Text(text, style = sans(11).copy(color = Palette.Muted))
    }
}
