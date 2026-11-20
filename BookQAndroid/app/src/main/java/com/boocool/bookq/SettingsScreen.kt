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
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen(
    store: BookQStore,
    onExportBackup: () -> Unit,
    onRestoreBackup: () -> Unit
) {
    var confirmClear by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(top = 8.dp, bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("设置", style = serif(28))
                Text("练习习惯与本机数据", style = sans(11).copy(color = Palette.Muted))
            }
            BrandMark(38.dp)
        }

        SettingsGroup("练习偏好") {
            MenuRow(
                icon = Icons.Outlined.Edit,
                title = "默认模式",
                value = store.state.defaultMode.longTitle
            ) {
                QuizMode.entries.forEach { mode ->
                    DropdownMenuItem(text = { Text(mode.longTitle) }, onClick = { store.setDefaultMode(mode) })
                }
            }
            SettingsDivider()
            MenuRow(
                icon = Icons.Outlined.Inventory2,
                title = "默认题量",
                value = if (store.state.defaultSize == 0) "全部" else "${store.state.defaultSize} 题"
            ) {
                listOf(10, 20, 50, 0).forEach { size ->
                    DropdownMenuItem(
                        text = { Text(if (size == 0) "全部" else "$size 题") },
                        onClick = { store.setDefaultSize(size) }
                    )
                }
            }
            SettingsDivider()
            MenuRow(
                icon = Icons.Outlined.ExpandMore,
                title = "默认顺序",
                value = store.state.defaultOrder.title
            ) {
                QuizOrder.entries.forEach { order ->
                    DropdownMenuItem(text = { Text(order.title) }, onClick = { store.setDefaultOrder(order) })
                }
            }
        }

        SettingsGroup("题库数据") {
            ActionRow(
                icon = Icons.Outlined.FileUpload,
                title = "导出备份",
                detail = "题库、进度、错题本一起打包",
                color = Palette.Primary,
                onClick = onExportBackup
            )
            SettingsDivider()
            ActionRow(
                icon = Icons.Outlined.Download,
                title = "从备份恢复",
                detail = "会覆盖当前题库和答题记录",
                color = Palette.Primary,
                onClick = onRestoreBackup
            )
            SettingsDivider()
            ActionRow(
                icon = Icons.Outlined.DeleteOutline,
                title = "清空答题记录",
                detail = "保留题库，只清进度和错题",
                color = Palette.Red,
                destructive = true,
                onClick = { confirmClear = true }
            )
            HorizontalDivider(color = Palette.Line2, thickness = 1.dp, modifier = Modifier.padding(top = 2.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("${store.state.banks.size} 个题库", style = sans(10).copy(color = Palette.Muted))
                Spacer(Modifier.weight(1f))
                Text("${store.allQuestions.size} 题", style = sans(10).copy(color = Palette.Muted))
                Text("·", style = sans(10).copy(color = Palette.Muted))
                Text("${store.state.attempts.size} 次练习", style = sans(10).copy(color = Palette.Muted))
            }
        }

        SettingsGroup("体验") {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                Box(
                    Modifier
                        .size(26.dp)
                        .background(Palette.Primary.copy(alpha = 0.07f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.TouchApp, contentDescription = null, tint = Palette.Primary, modifier = Modifier.size(15.dp))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("触感反馈", style = sans(13, FontWeight.Medium))
                    Text("作答时轻轻震一下", style = sans(10).copy(color = Palette.Muted))
                }
                Switch(
                    checked = store.state.haptics,
                    onCheckedChange = store::setHaptics,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Palette.Primary,
                        uncheckedThumbColor = Palette.Muted,
                        uncheckedTrackColor = Palette.Track
                    )
                )
            }
            SettingsDivider()
            ActionRow(
                icon = Icons.Outlined.HelpOutline,
                title = "怎么导入题库",
                detail = "支持 DOCX / TXT / MD / JSON",
                color = Palette.Primary,
                onClick = { showHelp = true }
            )
        }

        SettingsGroup("关于") {
            InfoRow(Icons.Outlined.Inventory2, "版本", "0.1.4-android.1")
            SettingsDivider()
            InfoRow(Icons.Outlined.FileUpload, "题库", "全部由你导入")
            SettingsDivider()
            InfoRow(Icons.Outlined.Palette, "视觉", "织物靛蓝")
        }

        Text(
            "题库不内置在 App 里。\n导入、备份、恢复都在本机完成，不上传任何文档。",
            style = sans(10).copy(color = Palette.Muted, lineHeight = 15.sp),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
        )
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("清空答题记录？", style = serif(20)) },
            text = { Text("题库会保留，进度、错题本和练习历史会被清空。", style = sans(13)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    store.clearProgress()
                    store.showToast("答题记录已清空")
                }) { Text("清空", color = Palette.Red) }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("取消") } }
        )
    }

    if (showHelp) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            title = { Text("导入题库", style = serif(20)) },
            text = {
                Text(
                    "到「题库」页点右上角导入，选择 DOCX、TXT、MD 或 JSON。解析后会先让你确认题库名和题目样例，再保存。",
                    style = sans(13)
                )
            },
            confirmButton = { TextButton(onClick = { showHelp = false }) { Text("知道了") } }
        )
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            style = sans(11, FontWeight.SemiBold).copy(color = Palette.Ink2, letterSpacing = 0.5.sp),
            modifier = Modifier.padding(start = 4.dp)
        )
        CardSurface(radius = 16.dp, padding = 0.dp) {
            Column { content() }
        }
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        color = Palette.Line2,
        thickness = 1.dp,
        modifier = Modifier.padding(start = 50.dp)
    )
}

@Composable
private fun MenuRow(
    icon: ImageVector,
    title: String,
    value: String,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            SettingsIcon(icon, Palette.Primary)
            Text(title, style = sans(13, FontWeight.Medium), modifier = Modifier.weight(1f))
            Text(value, style = sans(11).copy(color = Palette.Muted))
            Icon(Icons.Outlined.ExpandMore, contentDescription = null, tint = Palette.Muted, modifier = Modifier.size(14.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Box {
                content()
            }
        }
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    title: String,
    detail: String,
    color: Color,
    destructive: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        SettingsIcon(icon, color)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = sans(13, FontWeight.Medium).copy(color = if (destructive) Palette.Red else Palette.Ink))
            Text(detail, style = sans(10).copy(color = Palette.Muted))
        }
        Icon(Icons.Outlined.ArrowForward, contentDescription = null, tint = Palette.Line, modifier = Modifier.size(10.dp))
    }
}

@Composable
private fun InfoRow(icon: ImageVector, title: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        SettingsIcon(icon, Palette.Gold)
        Text(title, style = sans(13, FontWeight.Medium), modifier = Modifier.weight(1f))
        Text(value, style = sans(11).copy(color = Palette.Muted))
    }
}

@Composable
private fun SettingsIcon(icon: ImageVector, color: Color) {
    Box(
        Modifier
            .size(26.dp)
            .background(color.copy(alpha = 0.07f), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(15.dp))
    }
}
