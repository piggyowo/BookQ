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
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.Canvas
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max

@Composable
fun ResultScreen(store: BookQStore) {
    val result = store.result
    if (result == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("还没有本轮结果", style = serif(24))
                PrimaryButton("返回首页") { store.goHome() }
            }
        }
        return
    }

    var showAll by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    val restartTitle = run {
        val session = store.session
        if (session?.order == QuizOrder.SEQUENTIAL) {
            val info = store.sequenceInfo(session.scope)
            if (info.nextIndex != null && info.remaining > 0) "继续下一批" else "重新一轮"
        } else {
            "再来一轮"
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 48.dp)
    ) {
        Row(Modifier.padding(top = 12.dp, bottom = 16.dp)) {
            GhostPillButton("返回首页") { store.goHome() }
        }

        ScoreCard(result)

        Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                StatCard("正确", "${result.correct}", "题", Modifier.weight(1f))
                StatCard("错误", "${max(0, result.total - result.correct)}", "题", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                StatCard("正确率", "${result.percent}", "%", Modifier.weight(1f))
                StatCard("错题本累计", "${store.state.wrongIds.size}", "题", Modifier.weight(1f))
            }
        }

        if (result.fillCount > 0) {
            Text(
                "另有 ${result.fillCount} 道填空题仅查看答案，未计入对错与正确率",
                style = sans(11).copy(color = Palette.Muted),
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        if (result.chapterStats.isNotEmpty()) {
            Column(Modifier.padding(top = 34.dp, bottom = 14.dp)) {
                SectionHeader("分类掌握度", "本轮作答")
            }
            CardSurface(radius = 20.dp, padding = 22.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    result.chapterStats.forEach { stat ->
                        ChapterRow(stat)
                    }
                }
            }
        }

        ReviewSection(
            result = result,
            showAll = showAll,
            onShowAll = { showAll = it },
            expanded = expanded,
            onExpanded = { expanded = it }
        )

        Column(Modifier.padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PrimaryButton(
                    restartTitle,
                    icon = Icons.Outlined.Refresh,
                    modifier = Modifier.weight(1f)
                ) { store.restart() }
                GhostPillButton("去错题本", Modifier.weight(1f)) { store.openWrongBook() }
            }
            GhostPillButton("返回首页", Modifier.fillMaxWidth()) { store.goHome() }
        }

        Text(
            "成绩已记录在本机 · 共 ${store.state.attempts.size} 次作答记录",
            style = sans(11).copy(color = Palette.Muted),
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun ScoreCard(result: QuizResult) {
    val scoreColor = when {
        result.percent >= 90 -> Palette.Green
        result.percent >= 80 -> Palette.Primary2
        result.percent >= 60 -> Palette.Gold
        else -> Palette.Accent
    }
    HeroSurface {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            ScoreDonut(result.percent, scoreColor)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    result.grade,
                    style = sans(12, FontWeight.Medium).copy(color = Palette.Cream.copy(alpha = 0.9f), letterSpacing = 1.6.sp),
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                        .padding(horizontal = 13.dp, vertical = 5.dp)
                )
                Text(
                    "${result.total} 题答对 ${result.correct} 题",
                    style = serif(23).copy(color = Palette.Cream, lineHeight = 30.sp)
                )
                Text(
                    resultSubtitle(result),
                    style = sans(12).copy(color = Palette.Cream.copy(alpha = 0.66f), lineHeight = 19.sp)
                )
            }
        }
    }
}

@Composable
private fun ScoreDonut(percent: Int, color: Color) {
    Box(Modifier.size(132.dp), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            drawCircle(Color.White.copy(alpha = 0.12f), style = Stroke(13.dp.toPx()))
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * percent / 100f,
                useCenter = false,
                style = Stroke(13.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$percent", style = serif(42).copy(color = Color.White))
            Text("正确率", style = sans(10).copy(color = Palette.Cream.copy(alpha = 0.6f), letterSpacing = 2.sp))
        }
    }
}

private fun resultSubtitle(result: QuizResult): String {
    val parts = mutableListOf(result.scopeLabel, result.mode.title, "用时 ${formatDuration(result.durationSeconds)}")
    if (result.unanswered > 0) parts.add("未作答 ${result.unanswered} 题")
    if (result.noAnswerCount > 0) parts.add("题库无答案 ${result.noAnswerCount} 题（未计入）")
    return parts.joinToString(" · ")
}

@Composable
private fun StatCard(label: String, value: String, unit: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Palette.Card)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(label, style = sans(11).copy(color = Palette.Muted, letterSpacing = 1.1.sp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = serif(24))
            Text(unit, style = sans(11).copy(color = Palette.Muted), modifier = Modifier.padding(start = 3.dp, bottom = 2.dp))
        }
    }
}

@Composable
private fun ChapterRow(stat: ChapterStat) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            stat.name,
            style = sans(12).copy(color = Palette.Ink),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(76.dp)
        )
        ProgressTrack(
            value = if (stat.total == 0) 0.0 else stat.correct.toDouble() / stat.total,
            height = 9.dp,
            low = stat.rate < 60,
            modifier = Modifier.weight(1f)
        )
        Text(
            "${stat.correct}/${stat.total} · ${stat.rate}%",
            style = sans(13, FontWeight.Medium).copy(color = Palette.Ink2),
            maxLines = 1,
            modifier = Modifier.width(92.dp)
        )
    }
}

@Composable
private fun ReviewSection(
    result: QuizResult,
    showAll: Boolean,
    onShowAll: (Boolean) -> Unit,
    expanded: Boolean,
    onExpanded: (Boolean) -> Unit
) {
    val wrongCount = result.items.count { !it.isCorrect }
    val items = if (showAll) result.items else result.items.filter { !it.isCorrect }
    val shown = if (expanded) items else items.take(8)

    Column(Modifier.padding(top = 34.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("答题回顾", style = serif(19), modifier = Modifier.weight(1f))
            Row(
                Modifier
                    .clip(CircleShape)
                    .background(Palette.Track)
                    .padding(3.dp)
            ) {
                ReviewTab("只看错题", !showAll) { onShowAll(false) }
                ReviewTab("全部 ${result.items.size} 题", showAll) { onShowAll(true) }
            }
        }
        Spacer(Modifier.height(14.dp))
        if (items.isEmpty()) {
            Text(
                if (wrongCount == 0) "本轮没有错题，保持这个状态。" else "暂无内容",
                style = sans(13).copy(color = Palette.Muted),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Palette.Card)
                    .padding(vertical = 34.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                shown.forEach { ReviewRow(it) }
            }
            if (items.size > 8) {
                GhostPillButton(
                    if (expanded) "收起" else "展开全部 ${items.size} 条",
                    Modifier.fillMaxWidth().padding(top = 14.dp)
                ) { onExpanded(!expanded) }
            }
        }
    }
}

@Composable
private fun ReviewTab(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text,
        style = sans(12, if (selected) FontWeight.SemiBold else FontWeight.Normal).copy(
            color = if (selected) Palette.Primary else Palette.Ink2
        ),
        modifier = Modifier
            .clip(CircleShape)
            .background(if (selected) Color.White else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 5.dp)
    )
}

@Composable
private fun ReviewRow(item: ResultItem) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Palette.SoftPaper)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                if (item.isCorrect) "正确" else "错误",
                style = sans(11, FontWeight.SemiBold).copy(color = if (item.isCorrect) Palette.Green else Palette.Red),
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (item.isCorrect) Palette.GreenBg else Palette.RedBg)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )
            Text(
                "${item.question.chapter} · 第 ${item.question.num} 题",
                style = sans(11).copy(color = Palette.Muted),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(item.question.stem, style = sans(14, FontWeight.Medium).copy(lineHeight = 22.sp))
        Text(
            "正确答案：${item.question.answerDisplay}",
            style = sans(12).copy(color = Palette.Green, lineHeight = 18.sp)
        )
        if (item.choice != null && item.choice != item.question.answer) {
            Text("你的选择：${item.choice}", style = sans(12).copy(color = Palette.Red, textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough))
        } else if (item.choice == null) {
            Text("未作答", style = sans(12).copy(color = Palette.Muted))
        }
    }
}

@Composable
fun WrongBookScreen(store: BookQStore) {
    var confirmClear by remember { mutableStateOf(false) }
    val questions = store.wrongQuestions

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 48.dp)
    ) {
        Row(
            Modifier.padding(top = 12.dp, bottom = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GhostPillButton("返回首页") { store.goHome() }
            Spacer(Modifier.weight(1f))
            if (questions.isNotEmpty()) {
                GhostPillButton("清空") { confirmClear = true }
            }
        }

        Column(Modifier.padding(bottom = 22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("错题本", style = serif(28))
            Text("共 ${questions.size} 题 · 答对后会自动移出", style = sans(13).copy(color = Palette.Muted))
            if (questions.isNotEmpty()) {
                PrimaryButton(
                    "开始重练",
                    icon = Icons.Outlined.PlayArrow,
                    modifier = Modifier.padding(top = 10.dp)
                ) { store.startWrongPractice() }
            }
        }

        if (questions.isEmpty()) {
            Column(
                Modifier.fillMaxWidth().padding(vertical = 70.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Outlined.BookmarkBorder, contentDescription = null, tint = Palette.Muted.copy(alpha = 0.6f), modifier = Modifier.size(40.dp))
                Text("错题本是空的", style = serif(22))
                Text("练习中答错的题会自动收集到这里。", style = sans(13).copy(color = Palette.Muted))
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                questions.forEach { question ->
                    WrongCard(store, question)
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("清空错题本？", style = serif(20)) },
            text = { Text("此操作不可撤销。", style = sans(13)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    store.clearWrong()
                }) { Text("清空", color = Palette.Red) }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("取消") } }
        )
    }
}

@Composable
private fun WrongCard(store: BookQStore, question: Question) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Palette.Card)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PillLabel(question.chapter)
            Text("第 ${question.num} 题", style = sans(11).copy(color = Palette.Muted))
            Spacer(Modifier.weight(1f))
            Text(
                "移除",
                style = sans(12).copy(color = Palette.Muted),
                modifier = Modifier.clickable { store.removeWrong(question.id) }
            )
        }
        Text(question.stem, style = sans(15, FontWeight.Medium).copy(lineHeight = 23.sp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Palette.Green, modifier = Modifier.size(16.dp))
            Text(
                "正确答案：${question.answerDisplay}",
                style = sans(13, FontWeight.SemiBold).copy(color = Palette.Green, lineHeight = 19.sp)
            )
        }
    }
}
