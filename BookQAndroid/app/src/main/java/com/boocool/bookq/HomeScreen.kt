package com.boocool.bookq

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    store: BookQStore,
    onStart: (String?) -> Unit,
    onWrong: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(top = 8.dp, bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        HomeTopBar(store, onWrong)

        if (!store.hasBanks) {
            EmptyHero(onGoLibrary = { store.setTab(AppTab.LIBRARY) })
        } else {
            PracticeHero(store, onStart, onWrong)
            MyBanks(store, onStart)
            WeeklyCard(store)
            WeakSection(store)
        }
    }
}

@Composable
private fun HomeTopBar(store: BookQStore, onWrong: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        BrandMark(42.dp)
        Spacer(Modifier.width(11.dp))
        Column {
            Text("BookQ", style = serif(20))
            Text(
                "BOOK Q · 随身练",
                style = sans(9, FontWeight.Medium).copy(
                    color = Palette.Muted,
                    letterSpacing = 1.4.sp
                )
            )
        }
        Spacer(Modifier.weight(1f))
        Row(
            Modifier
                .clip(CircleShape)
                .background(Palette.Card.copy(alpha = 0.9f))
                .clickable(onClick = onWrong)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(Icons.Outlined.BookmarkBorder, contentDescription = null, tint = Palette.Ink2, modifier = Modifier.size(14.dp))
            Text("错题本", style = sans(12, FontWeight.Medium).copy(color = Palette.Ink2))
            if (store.state.wrongIds.isNotEmpty()) {
                Text(
                    "${store.state.wrongIds.size}",
                    style = sans(10, FontWeight.SemiBold).copy(color = Color.White),
                    modifier = Modifier
                        .background(Palette.Accent, CircleShape)
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyHero(onGoLibrary: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        HeroSurface {
            Box(Modifier.fillMaxWidth().height(286.dp)) {
                PaperStackArt(
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 20.dp, y = 52.dp)
                        .rotate(4f)
                )
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .padding(end = 42.dp),
                    verticalArrangement = Arrangement.spacedBy(13.dp)
                ) {
                    Text(
                        "LIBRARY EMPTY",
                        style = sans(9, FontWeight.SemiBold).copy(color = Palette.Cream.copy(alpha = 0.6f), letterSpacing = 2.sp)
                    )
                    Text("你的题库，\n还在文件里", style = serif(30).copy(color = Palette.Cream, lineHeight = 36.sp))
                    Text(
                        "导入一份 Word 或文本题库，\n把题库装进手机。",
                        style = sans(13).copy(color = Palette.Cream.copy(alpha = 0.72f), lineHeight = 20.sp)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        listOf("DOCX", "TXT", "MD", "JSON").forEach { FormatChip(it) }
                    }
                }
            }
        }

        Column(Modifier.padding(top = 22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("导入之后", style = serif(17))
            FeatureRow(Icons.Outlined.Description, "解析后先确认", "题库名、题数、样例都能检查，再保存")
            FeatureRow(Icons.Outlined.Smartphone, "全部存在本机", "不联网也能刷，答题记录只留在这台设备")
            FeatureRow(Icons.Outlined.Info, "无答案也会提示", "题库里没写答案的题，不计分、不进错题本")
        }

        PrimaryButton(
            text = "去题库导入",
            icon = Icons.Outlined.Download,
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            onClick = onGoLibrary
        )
    }
}

@Composable
private fun FormatChip(text: String) {
    Text(
        text,
        style = sans(10, FontWeight.SemiBold).copy(color = Palette.Cream.copy(alpha = 0.85f)),
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(Color.Transparent)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

@Composable
private fun FeatureRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Box(
            Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(Palette.Primary.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Palette.Primary, modifier = Modifier.size(15.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = sans(13, FontWeight.SemiBold))
            Text(subtitle, style = sans(12).copy(color = Palette.Muted, lineHeight = 17.sp))
        }
    }
}

@Composable
private fun PracticeHero(store: BookQStore, onStart: (String?) -> Unit, onWrong: () -> Unit) {
    val bank = store.lastBank ?: return
    val progress = store.bankProgress(bank)
    val count = if (store.state.defaultSize == 0) "全部" else "${store.state.defaultSize} 道"

    HeroSurface {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(
                        "TODAY'S PRACTICE",
                        style = sans(9, FontWeight.SemiBold).copy(color = Palette.Cream.copy(alpha = 0.6f), letterSpacing = 2.sp)
                    )
                    Text("今天，练 $count", style = serif(29).copy(color = Palette.Cream, lineHeight = 36.sp))
                }
                Spacer(Modifier.width(10.dp))
                RingProgress(progress.percent, size = 72.dp, lineWidth = 7.dp)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Icon(Icons.Outlined.School, contentDescription = null, tint = Palette.Cream.copy(alpha = 0.85f), modifier = Modifier.size(11.dp))
                Text(
                    bank.name,
                    style = sans(11, FontWeight.Medium).copy(color = Palette.Cream.copy(alpha = 0.85f)),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text("· 共 ${bank.count} 题", style = sans(11).copy(color = Palette.Cream.copy(alpha = 0.55f)))
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CreamPillButton("开始练习", icon = Icons.Outlined.PlayArrow, onClick = { onStart(null) })
                if (store.state.wrongIds.isNotEmpty()) {
                    Text(
                        "${store.state.wrongIds.size} 道错题",
                        style = sans(11).copy(color = Palette.Cream.copy(alpha = 0.75f)),
                        modifier = Modifier.clickable(onClick = onWrong)
                    )
                }
            }
        }
    }
}

@Composable
private fun MyBanks(store: BookQStore, onStart: (String?) -> Unit) {
    val shown = store.homeBanks
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("我的题库", "管理", onTrailing = { store.setTab(AppTab.LIBRARY) })
        CardSurface(padding = 0.dp, radius = 18.dp) {
            Column {
                shown.forEachIndexed { index, bank ->
                    BankRow(store, bank, index = index, onClick = { onStart(bank.id) })
                    if (index != shown.lastIndex) {
                        HorizontalDivider(
                            color = Palette.Line2,
                            thickness = 1.dp,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
                if (shown.isNotEmpty()) {
                    DashedDivider()
                }
                AllBanksRow(store)
            }
        }
    }
}

@Composable
private fun BankRow(store: BookQStore, bank: QuizBank, index: Int, onClick: () -> Unit) {
    val progress = store.bankProgress(bank)
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(end = 13.dp)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .width(4.dp)
                .height(42.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (index % 2 == 0) Palette.Primary else Palette.Accent)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(bank.name, style = sans(14, FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${bank.count} 题 · ${rateLabel(progress.rate)}", style = sans(11).copy(color = Palette.Muted), maxLines = 1)
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("${(progress.percent * 100).roundToInt()}%", style = serif(15).copy(color = Palette.Primary))
            ProgressTrack(progress.percent, Modifier.width(54.dp), height = 4.dp)
        }
        Spacer(Modifier.width(8.dp))
        Icon(Icons.Outlined.ArrowForward, contentDescription = null, tint = Palette.Line, modifier = Modifier.size(10.dp))
    }
}

@Composable
private fun AllBanksRow(store: BookQStore) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { store.setTab(AppTab.LIBRARY) }
            .background(Palette.Primary.copy(alpha = 0.025f))
            .padding(start = 16.dp, end = 13.dp)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AllBanksShelf()
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("全部题库", style = sans(14, FontWeight.SemiBold))
            Text(
                if (store.state.banks.size > 3) "共 ${store.state.banks.size} 个 · 最近导入 3 个"
                else "共 ${store.state.banks.size} 个",
                style = sans(11).copy(color = Palette.Muted)
            )
        }
        Spacer(Modifier.width(8.dp))
        Text("查看全部", style = sans(11, FontWeight.Medium).copy(color = Palette.Primary))
        Spacer(Modifier.width(6.dp))
        Icon(Icons.Outlined.ArrowForward, contentDescription = null, tint = Palette.Line, modifier = Modifier.size(10.dp))
    }
}

@Composable
private fun AllBanksShelf() {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            ShelfBar(20.dp, Palette.Primary)
            ShelfBar(24.dp, Palette.Accent)
            ShelfBar(18.dp, Palette.Gold)
        }
        Box(Modifier.width(24.dp).height(1.dp).background(Palette.Line))
    }
}

@Composable
private fun ShelfBar(height: androidx.compose.ui.unit.Dp, color: Color) {
    Box(
        Modifier
            .width(4.dp)
            .height(height)
            .clip(RoundedCornerShape(1.dp))
            .background(color)
    )
}

@Composable
private fun DashedDivider() {
    Canvas(Modifier.fillMaxWidth().height(1.dp)) {
        drawLine(
            color = Palette.Line2,
            start = Offset(0f, 0.5f),
            end = Offset(size.width, 0.5f),
            strokeWidth = 1f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 3f), 0f)
        )
    }
}

@Composable
private fun WeeklyCard(store: BookQStore) {
    val (total, rate, days) = store.weeklyStats
    CardSurface(radius = 18.dp, padding = 18.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
            Row {
                Text("这周的手感", style = serif(18), modifier = Modifier.weight(1f))
                Text(if (days == 0) "还没开始" else "练了 $days 天", style = sans(11).copy(color = Palette.Muted))
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(Modifier.width(86.dp)) {
                    Text("$total", style = serif(36).copy(color = Palette.Primary))
                    Text("本周答题", style = sans(11).copy(color = Palette.Muted))
                }
                Box(Modifier.width(1.dp).height(42.dp).background(Palette.Line2))
                Column {
                    Text(rate?.let { "$it%" } ?: "—", style = serif(22).copy(color = Palette.Accent))
                    Text("正确率", style = sans(11).copy(color = Palette.Muted))
                }
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier
                        .size(46.dp)
                        .background(Palette.Primary.copy(alpha = 0.07f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (days >= 5) Icons.Outlined.School else Icons.Outlined.Info,
                        contentDescription = null,
                        tint = if (days >= 5) Palette.Accent else Palette.Primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun WeakSection(store: BookQStore) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("薄弱环节", style = serif(19))
        if (store.weakestBanks.isEmpty()) {
            Text(
                "完成一轮练习后，这里会按题库正确率给出排序。",
                style = sans(12).copy(color = Palette.Muted),
                modifier = Modifier.padding(vertical = 6.dp)
            )
        } else {
            CardSurface(radius = 16.dp, padding = 0.dp) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 15.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                    store.weakestBanks.forEach { bank ->
                        val progress = store.bankProgress(bank)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                bank.name,
                                style = sans(12, FontWeight.Medium).copy(color = Palette.Ink2),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.width(104.dp)
                            )
                            ProgressTrack(progress.percent, Modifier.weight(1f), height = 5.dp, low = true)
                            Text(
                                progress.rate?.let { "$it%" } ?: "未练",
                                style = sans(11, FontWeight.SemiBold).copy(color = Palette.Accent),
                                modifier = Modifier.width(38.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PaperStackArt(modifier: Modifier = Modifier) {
    Box(modifier.size(170.dp, 210.dp)) {
        Box(
            Modifier
                .size(150.dp, 178.dp)
                .offset(x = 10.dp, y = 12.dp)
                .rotate(-7f)
                .clip(RoundedCornerShape(16.dp))
                .background(Palette.Line)
        )
        Box(
            Modifier
                .size(150.dp, 178.dp)
                .offset(x = 28.dp, y = 4.dp)
                .rotate(4f)
                .clip(RoundedCornerShape(16.dp))
                .background(Palette.Line2)
        )
        Column(
            Modifier
                .size(150.dp, 178.dp)
                .offset(x = 18.dp, y = 12.dp)
                .rotate(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(Palette.Card)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Box(Modifier.size(24.dp).background(Palette.Primary, RoundedCornerShape(4.dp)))
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Box(Modifier.size(54.dp, 6.dp).background(Palette.Ink.copy(alpha = 0.75f), CircleShape))
                    Box(Modifier.size(38.dp, 4.dp).background(Palette.Line, CircleShape))
                }
            }
            repeat(4) { row ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(9.dp).border(1.dp, Palette.Line, CircleShape))
                    Box(Modifier.size(if (row == 2) 68.dp else 94.dp, 5.dp).background(Palette.Line2, CircleShape))
                }
            }
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                repeat(3) {
                    Box(Modifier.size(22.dp, 5.dp).background(Palette.Gold.copy(alpha = 0.35f), CircleShape))
                }
            }
        }
    }
}
