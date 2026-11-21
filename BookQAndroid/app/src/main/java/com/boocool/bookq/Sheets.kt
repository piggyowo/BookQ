package com.boocool.bookq

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SheetGrabber() {
    Box(
        Modifier
            .padding(top = 9.dp, bottom = 2.dp)
            .size(42.dp, 5.dp)
            .background(Palette.Line, CircleShape)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartQuizSheet(
    store: BookQStore,
    presetBankId: String?,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var scope by remember(presetBankId) {
        mutableStateOf(
            presetBankId?.let { QuizScope.Bank(it) } ?: QuizScope.All
        )
    }
    var mode by remember { mutableStateOf(store.state.defaultMode) }
    var size by remember {
        mutableStateOf(store.state.defaultSize.takeIf { it > 0 })
    }
    var order by remember { mutableStateOf(store.state.defaultOrder) }

    val available = store.questions(scope).size
    val sequenceInfo = store.sequenceInfo(scope)
    val sequentialDetail = when {
        available == 0 -> "暂无题目"
        sequenceInfo.completed -> "上一轮已完成 · 从第 1 题开始"
        sequenceInfo.nextIndex != null && sequenceInfo.nextIndex!! > 0 ->
            "从第 ${sequenceInfo.nextIndex!! + 1} 题继续 · 还剩 ${sequenceInfo.remaining} 题"
        else -> "从第 1 题开始 · 共 $available 题"
    }
    val remainingForOrder = if (sequenceInfo.completed) available else sequenceInfo.remaining
    val planned = if (order == QuizOrder.SEQUENTIAL) {
        minOf(size ?: remainingForOrder, remainingForOrder)
    } else {
        minOf(size ?: available, available)
    }
    val countOptions = buildList<Int?> {
        listOf(10, 20, 50).filter { it <= available }.forEach { add(it) }
        if (available > 0) add(null)
        if (isEmpty()) add(null)
    }

    LaunchedEffect(available) {
        if (size != null && size!! > available) size = null
        if (available == 0) size = null
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Palette.Paper,
        dragHandle = { SheetGrabber() }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("开始练习", style = serif(26))
                    Text("范围、模式、题量、顺序都可以在这一步调整", style = sans(12).copy(color = Palette.Muted))
                }
                CloseCircle(onDismiss)
            }

            SheetSection("练习范围") {
                Column {
                    ScopeRow(
                        title = "全部题库",
                        detail = "${store.state.banks.size} 个 · ${store.allQuestions.size} 题",
                        selected = scope is QuizScope.All,
                        onClick = { scope = QuizScope.All }
                    )
                    store.state.banks.forEach { bank ->
                        SheetDivider()
                        ScopeRow(
                            title = bank.name,
                            detail = "${bank.count} 题",
                            selected = scope is QuizScope.Bank && (scope as QuizScope.Bank).id == bank.id,
                            onClick = { scope = QuizScope.Bank(bank.id) }
                        )
                    }
                }.let { content ->
                    CardSurface(radius = 16.dp, padding = 0.dp) { content }
                }
            }

            SheetSection("模式") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ModeCard(QuizMode.PRACTICE, mode, Modifier.weight(1f)) { mode = it }
                    ModeCard(QuizMode.EXAM, mode, Modifier.weight(1f)) { mode = it }
                }
            }

            SheetSection("题量") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    countOptions.forEach { value ->
                        ChipButton(
                            title = value?.let { "$it 题" } ?: "全部 $available",
                            selected = size == value
                        ) { size = value }
                    }
                }
            }

            SheetSection("顺序") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OrderCard(
                        QuizOrder.SEQUENTIAL,
                        order,
                        Modifier.weight(1f),
                        detail = sequentialDetail
                    ) { order = it }
                    OrderCard(QuizOrder.RANDOM, order, Modifier.weight(1f)) { order = it }
                }
                if (order == QuizOrder.SEQUENTIAL) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "顺序进度 ${sequenceInfo.done}/${sequenceInfo.total} · ${sequenceInfo.lastLabel}",
                            style = sans(11).copy(color = Palette.Muted),
                            modifier = Modifier.weight(1f)
                        )
                        if (sequenceInfo.total > 0 && (sequenceInfo.nextIndex != null || sequenceInfo.completed)) {
                            Text(
                                "从头开始",
                                style = sans(11, FontWeight.Medium).copy(color = Palette.Primary),
                                modifier = Modifier
                                    .clickable { store.resetSequenceCursor(scope) }
                                    .padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Column(Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "${store.scopeLabel(scope)} · ${mode.title}模式 · $planned 题 · ${order.title}",
                    style = sans(12).copy(color = Palette.Muted)
                )
                if (order == QuizOrder.SEQUENTIAL && size != null && size!! > remainingForOrder && remainingForOrder > 0) {
                    Text(
                        "顺序模式只剩 $remainingForOrder 题，本轮会先做完这些",
                        style = sans(11).copy(color = Palette.Accent)
                    )
                }
                val noAnswer = store.questions(scope).count { !it.hasAnswer }
                if (noAnswer > 0) {
                    Text("其中 $noAnswer 题题库未提供答案，会单独提示", style = sans(11).copy(color = Palette.Accent))
                }
            }

            PrimaryButton(
                text = if (mode == QuizMode.EXAM) "开始考试" else "开始练习",
                icon = Icons.Outlined.PlayArrow,
                modifier = Modifier.fillMaxWidth(),
                enabled = available > 0
            ) {
                store.startQuiz(scope, mode, size, order)
                onDismiss()
            }

            Text(
                "题库里没有答案的题会提示「题库中无答案」，不计分，也不会进入错题本。",
                style = sans(10).copy(color = Palette.Muted, lineHeight = 15.sp),
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
fun CloseCircle(onClick: () -> Unit) {
    Box(
        Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(Palette.Card)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Outlined.Close, contentDescription = "关闭", tint = Palette.Ink2, modifier = Modifier.size(14.dp))
    }
}

@Composable
private fun SheetSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Text(
            title,
            style = sans(12, FontWeight.SemiBold).copy(color = Palette.Ink2, letterSpacing = 0.6.sp)
        )
        content()
    }
}

@Composable
private fun SheetDivider() {
    Box(Modifier.fillMaxWidth().padding(start = 46.dp).height(1.dp).background(Palette.Line2))
}

@Composable
private fun ScopeRow(title: String, detail: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        Box(
            Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(if (selected) Palette.Primary else Color.Transparent)
                .then(if (!selected) Modifier.padding(1.dp) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            if (!selected) {
                Box(Modifier.size(20.dp).background(Color.Transparent, CircleShape).padding(1.dp))
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = sans(13, FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(detail, style = sans(10).copy(color = Palette.Muted))
        }
        if (selected) {
            Icon(Icons.Outlined.Check, contentDescription = null, tint = Palette.Primary, modifier = Modifier.size(15.dp))
        }
    }
}

@Composable
private fun ModeCard(value: QuizMode, selectedMode: QuizMode, modifier: Modifier = Modifier, onSelect: (QuizMode) -> Unit) {
    val selected = value == selectedMode
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) Palette.Primary.copy(alpha = 0.07f) else Palette.Card)
            .clickable { onSelect(value) }
            .padding(13.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row {
            Icon(
                if (value == QuizMode.PRACTICE) Icons.Outlined.Edit else Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = if (selected) Palette.Primary else Palette.Muted,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.weight(1f))
            if (selected) {
                Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Palette.Primary, modifier = Modifier.size(15.dp))
            }
        }
        Text(value.longTitle, style = sans(14, FontWeight.SemiBold))
        Text(value.subtitle, style = sans(10).copy(color = Palette.Muted))
    }
}

@Composable
private fun OrderCard(
    value: QuizOrder,
    selectedOrder: QuizOrder,
    modifier: Modifier = Modifier,
    detail: String? = null,
    onSelect: (QuizOrder) -> Unit
) {
    val selected = value == selectedOrder
    Row(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Palette.Primary.copy(alpha = 0.07f) else Palette.Card)
            .clickable { onSelect(value) }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Icon(
            if (value == QuizOrder.SEQUENTIAL) Icons.Outlined.FormatListNumbered else Icons.Outlined.Shuffle,
            contentDescription = null,
            tint = if (selected) Palette.Primary else Palette.Muted,
            modifier = Modifier.size(16.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(value.title, style = sans(13, FontWeight.SemiBold))
            Text(
                detail ?: value.subtitle,
                style = sans(10).copy(color = Palette.Muted),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportPreviewSheet(
    parsed: ParsedBank,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember(parsed) { mutableStateOf(parsed.name) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Palette.Paper,
        dragHandle = { SheetGrabber() }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("确认题库", style = serif(26))
                    Text("检查解析结果，再保存到题库", style = sans(12).copy(color = Palette.Muted))
                }
                CloseCircle(onDismiss)
            }

            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("题库名称", style = sans(11, FontWeight.SemiBold).copy(color = Palette.Ink2))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    textStyle = sans(15, FontWeight.Medium),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Text(parsed.fileName, style = sans(10).copy(color = Palette.Muted))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PreviewStat("${parsed.questionCount}", "题", Modifier.weight(1f))
                PreviewStat("${parsed.noAnswerCount}", "无答案", Modifier.weight(1f), accent = parsed.noAnswerCount > 0)
                PreviewStat("${parsed.warnings.size}", "待核对", Modifier.weight(1f), accent = parsed.warnings.isNotEmpty())
            }

            if (parsed.warnings.isNotEmpty()) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Palette.AmberBg)
                        .padding(13.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Text("解析提示", style = sans(12, FontWeight.SemiBold).copy(color = Palette.Accent))
                    parsed.warnings.take(5).forEach { warning ->
                        Text("· $warning", style = sans(11).copy(color = Palette.Ink2))
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("题目样例", style = sans(12, FontWeight.SemiBold).copy(color = Palette.Ink2))
                parsed.questions.take(3).forEach { PreviewQuestion(it) }
            }

            PrimaryButton(
                text = "保存到题库 · ${parsed.questionCount} 题",
                modifier = Modifier.fillMaxWidth(),
                enabled = parsed.questionCount > 0
            ) { onSave(name) }

            Text(
                "保存后可以到主页开始练习；导入按钮只在题库页。",
                style = sans(10).copy(color = Palette.Muted),
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
private fun PreviewStat(value: String, label: String, modifier: Modifier = Modifier, accent: Boolean = false) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Palette.Card)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(value, style = serif(22).copy(color = if (accent) Palette.Accent else Palette.Primary))
        Text(label, style = sans(10).copy(color = Palette.Muted))
    }
}

@Composable
private fun PreviewQuestion(question: ParsedQuestion) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Palette.Card)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("第 ${question.num} 题", style = sans(10, FontWeight.Medium).copy(color = Palette.Gold))
            if (question.answer == null) {
                Text(
                    "无答案",
                    style = sans(9).copy(color = Palette.Accent),
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Palette.AmberBg)
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                )
            }
        }
        Text(question.stem, style = sans(12, FontWeight.Medium), lineHeight = 18.sp)
        question.options.forEach { option ->
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(option.firstOrNull() ?: "?", style = sans(10, FontWeight.SemiBold).copy(color = Palette.Ink2))
                Text(option.getOrNull(1) ?: "", style = sans(10).copy(color = Palette.Ink2), lineHeight = 15.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankDetailSheet(
    store: BookQStore,
    bankId: String,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val bank = store.bank(bankId)
    var showRename by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var draftName by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Palette.Paper,
        dragHandle = { SheetGrabber() }
    ) {
        if (bank == null) {
            Text("题库已不在了", style = sans(14).copy(color = Palette.Muted), modifier = Modifier.padding(24.dp))
        } else {
            val progress = store.bankProgress(bank)
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("题库详情", style = serif(24))
                        Text(bank.fileName, style = sans(11).copy(color = Palette.Muted))
                    }
                    CloseCircle(onDismiss)
                }

                CardSurface(radius = 18.dp, padding = 16.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(
                                Modifier
                                    .size(52.dp, 58.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (bank.colorIndex == 0) Palette.Primary else Palette.Accent),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(if (bank.colorIndex == 0) "布" else "UV", style = serif(20).copy(color = Palette.Cream))
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(bank.name, style = serif(21), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${bank.count} 题 · 导入于 ${formatDate(bank.importedAt)}", style = sans(11).copy(color = Palette.Muted))
                            }
                        }
                        ProgressTrack(progress.percent, height = 6.dp)
                        Row {
                            Text("已作答 ${progress.done} / ${progress.total}", style = sans(10).copy(color = Palette.Muted))
                            Spacer(Modifier.weight(1f))
                            Text(progress.rate?.let { "正确率 $it%" } ?: "正确率待生成", style = sans(10).copy(color = Palette.Muted))
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DetailMetric("${bank.count}", "题目", Modifier.weight(1f))
                    DetailMetric("${store.wrongCountIn(bank)}", "错题", Modifier.weight(1f), accent = true)
                    DetailMetric("${progress.done}", "已作答", Modifier.weight(1f))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(11.dp))
                            .background(Palette.Primary.copy(alpha = 0.07f))
                            .clickable {
                                draftName = bank.name
                                showRename = true
                            }
                            .padding(vertical = 11.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Edit, contentDescription = null, tint = Palette.Primary, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("重命名", style = sans(12, FontWeight.Medium).copy(color = Palette.Primary))
                    }
                    Row(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(11.dp))
                            .background(Palette.RedBg)
                            .clickable { confirmDelete = true }
                            .padding(vertical = 11.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = null, tint = Palette.Red, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("删除", style = sans(12, FontWeight.Medium).copy(color = Palette.Red))
                    }
                }

                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Palette.Card)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("题目预览", style = sans(12, FontWeight.SemiBold).copy(color = Palette.Ink2))
                    bank.questions.take(5).forEach { question ->
                        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(question.num, style = sans(9, FontWeight.Medium).copy(color = Palette.Gold), modifier = Modifier.width(22.dp))
                            Text(question.stem, style = sans(11).copy(color = Palette.Ink2), maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            if (!question.hasAnswer) {
                                Text("无答案", style = sans(9).copy(color = Palette.Accent))
                            }
                        }
                    }
                }

                Text(
                    "这里只管理题库，不提供开始入口；练习请回主页开始。",
                    style = sans(10).copy(color = Palette.Muted),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }

    if (showRename && bank != null) {
        AlertDialog(
            onDismissRequest = { showRename = false },
            title = { Text("重命名题库", style = serif(20)) },
            text = {
                OutlinedTextField(
                    value = draftName,
                    onValueChange = { draftName = it },
                    singleLine = true,
                    textStyle = sans(14),
                    shape = RoundedCornerShape(10.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    store.renameBank(bank.id, draftName)
                    showRename = false
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { showRename = false }) { Text("取消") } }
        )
    }

    if (confirmDelete && bank != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("删除这个题库？", style = serif(20)) },
            text = { Text("题库和它的答题进度会一起移除，原文件不受影响。", style = sans(13)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    store.deleteBank(bank.id)
                    onDismiss()
                    store.showToast("题库已删除")
                }) { Text("删除", color = Palette.Red) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("取消") } }
        )
    }
}

@Composable
private fun DetailMetric(value: String, label: String, modifier: Modifier = Modifier, accent: Boolean = false) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Palette.Card)
            .padding(vertical = 11.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(value, style = serif(20).copy(color = if (accent) Palette.Accent else Palette.Primary))
        Text(label, style = sans(10).copy(color = Palette.Muted))
    }
}
