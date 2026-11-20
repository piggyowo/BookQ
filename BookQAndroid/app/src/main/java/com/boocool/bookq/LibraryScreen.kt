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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

private enum class LibraryFilter(val title: String) {
    ALL("全部"), UNSTARTED("还没练"), PRACTICED("练过的")
}

@Composable
fun LibraryScreen(
    store: BookQStore,
    onImport: () -> Unit,
    onOpenBank: (String) -> Unit
) {
    var search by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(LibraryFilter.ALL) }
    val all = store.state.banks.sortedByDescending { it.importedAt }
    val filtered = all.filter { bank ->
        val matchesSearch = search.isBlank() ||
            bank.name.contains(search, ignoreCase = true) ||
            bank.fileName.contains(search, ignoreCase = true)
        val matchesFilter = when (filter) {
            LibraryFilter.ALL -> true
            LibraryFilter.UNSTARTED -> store.bankProgress(bank).done == 0
            LibraryFilter.PRACTICED -> store.bankProgress(bank).done > 0
        }
        matchesSearch && matchesFilter
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(top = 8.dp, bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("题库", style = serif(28))
                Text("导入、查看和管理你的题库", style = sans(11).copy(color = Palette.Muted))
            }
            Row(
                Modifier
                    .clip(CircleShape)
                    .background(Palette.Primary)
                    .clickable(onClick = onImport)
                    .padding(horizontal = 15.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                Text("导入", style = sans(13, FontWeight.SemiBold).copy(color = Color.White))
            }
        }

        if (store.state.banks.isEmpty()) {
            EmptyLibrary(onImport)
        } else {
            SummaryStrip(store)
            SearchField(search, { search = it })
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LibraryFilter.entries.forEach { value ->
                    ChipButton(value.title, selected = filter == value) { filter = value }
                }
                Spacer(Modifier.weight(1f))
                Text("${filtered.size} 个", style = sans(10).copy(color = Palette.Muted))
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                filtered.forEach { bank ->
                    LibraryBankCard(store, bank, onClick = { onOpenBank(bank.id) })
                }
                if (filtered.isEmpty()) {
                    Text(
                        if (filter == LibraryFilter.ALL && search.isNotBlank()) "没有匹配的题库" else "这个筛选下还没有题库",
                        style = sans(12).copy(color = Palette.Muted),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 22.dp)
                    )
                }
            }

            ImportCard(onImport)
        }
    }
}

@Composable
private fun EmptyLibrary(onImport: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 18.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Palette.Card.copy(alpha = 0.7f))
            .dashedBorder(Palette.Line, 1.dp, 22.dp)
            .padding(horizontal = 22.dp, vertical = 38.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            Modifier
                .size(76.dp)
                .background(Palette.Primary.copy(alpha = 0.07f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.LibraryBooks, contentDescription = null, tint = Palette.Primary, modifier = Modifier.size(32.dp))
        }
        Text("还没有题库", style = serif(23))
        Text(
            "从「文件」里选一份题目文档，\n解析后先确认，再保存到题库。",
            style = sans(13).copy(color = Palette.Muted, lineHeight = 20.sp)
        )
        Row(
            Modifier
                .clip(CircleShape)
                .background(Palette.Primary)
                .clickable(onClick = onImport)
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text("导入第一份题库", style = sans(14, FontWeight.SemiBold).copy(color = Color.White))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            listOf("DOCX", "TXT", "MD", "JSON").forEach { text ->
                Text(
                    text,
                    style = sans(9, FontWeight.SemiBold).copy(color = Palette.Ink2),
                    modifier = Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .background(Palette.Card)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}

@Composable
private fun SummaryStrip(store: BookQStore) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Palette.Card)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        Box(
            Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(Palette.Primary),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.LibraryBooks, contentDescription = null, tint = Palette.Cream, modifier = Modifier.size(19.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("全部题库", style = sans(14, FontWeight.SemiBold))
            Text("${store.state.banks.size} 个 · ${store.allQuestions.size} 题", style = sans(11).copy(color = Palette.Muted))
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                if (store.totalAnswered == 0) "未开始" else "${store.totalAnswered} / ${store.allQuestions.size}",
                style = serif(16).copy(color = Palette.Primary)
            )
            Text("已作答", style = sans(10).copy(color = Palette.Muted))
        }
    }
}

@Composable
private fun SearchField(value: String, onChange: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Palette.Card)
            .padding(horizontal = 13.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Outlined.Search, contentDescription = null, tint = Palette.Muted, modifier = Modifier.size(15.dp))
        BasicTextField(
            value = value,
            onValueChange = onChange,
            textStyle = sans(13),
            singleLine = true,
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) {
                        Text("搜索题库名或文件名", style = sans(13).copy(color = Palette.Muted))
                    }
                    inner()
                }
            }
        )
        if (value.isNotEmpty()) {
            Icon(
                Icons.Outlined.Close,
                contentDescription = "清空",
                tint = Palette.Line,
                modifier = Modifier.size(15.dp).clickable { onChange("") }
            )
        }
    }
}

@Composable
private fun LibraryBankCard(store: BookQStore, bank: QuizBank, onClick: () -> Unit) {
    val progress = store.bankProgress(bank)
    val wrong = store.wrongCountIn(bank)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Palette.Card)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier
                    .width(4.dp)
                    .height(42.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (bank.colorIndex == 0) Palette.Primary else Palette.Accent)
            )
            Box(
                Modifier
                    .size(38.dp, 42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Palette.Primary.copy(alpha = 0.07f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Description, contentDescription = null, tint = Palette.Primary, modifier = Modifier.size(18.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(bank.name, style = sans(15, FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(bank.fileName, style = sans(10).copy(color = Palette.Muted), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(
                "${bank.count} 题",
                style = sans(10, FontWeight.Medium).copy(color = Palette.Ink2),
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Palette.Track)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Text("已作答 ${progress.done} / ${progress.total}", style = sans(11).copy(color = Palette.Muted))
            Text("·", style = sans(11).copy(color = Palette.Muted))
            Text(progress.rate?.let { "正确率 $it%" } ?: "正确率待生成", style = sans(11).copy(color = Palette.Muted))
            if (wrong > 0) {
                Text("·", style = sans(11).copy(color = Palette.Muted))
                Text("$wrong 错题", style = sans(11).copy(color = Palette.Accent))
            }
        }
        ProgressTrack(progress.percent, height = 5.dp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("导入于 ${formatDate(bank.importedAt)}", style = sans(10).copy(color = Palette.Muted))
            Spacer(Modifier.weight(1f))
            Text("详情", style = sans(11, FontWeight.Medium).copy(color = Palette.Primary))
            Icon(Icons.Outlined.ArrowForward, contentDescription = null, tint = Palette.Primary, modifier = Modifier.size(10.dp))
        }
    }
}

@Composable
private fun ImportCard(onImport: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Palette.Card.copy(alpha = 0.6f))
            .dashedBorder(Palette.Line, 1.dp, 16.dp)
            .clickable(onClick = onImport)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            Modifier
                .size(30.dp)
                .background(Palette.Primary.copy(alpha = 0.08f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Add, contentDescription = null, tint = Palette.Primary, modifier = Modifier.size(16.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("导入新题库", style = sans(13, FontWeight.SemiBold))
            Text("DOCX / TXT / MD / JSON", style = sans(10).copy(color = Palette.Muted))
        }
        Icon(Icons.Outlined.ArrowForward, contentDescription = null, tint = Palette.Line, modifier = Modifier.size(11.dp))
    }
}

private fun Modifier.dashedBorder(color: Color, width: androidx.compose.ui.unit.Dp, radius: androidx.compose.ui.unit.Dp): Modifier =
    this.then(
        Modifier.drawBehind {
            val stroke = width.toPx()
            val path = Path().apply {
                addRoundRect(
                    androidx.compose.ui.geometry.RoundRect(
                        0f, 0f, size.width, size.height,
                        androidx.compose.ui.geometry.CornerRadius(radius.toPx(), radius.toPx())
                    )
                )
            }
            drawPath(
                path,
                color,
                style = Stroke(
                    width = stroke,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                )
            )
        }
    )
