package com.boocool.bookq

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class SettingsPage {
    PRACTICE, APPEARANCE, DATA, EXPERIENCE, ABOUT, IMPORT_HELP
}

@Composable
fun SettingsScreen(
    store: BookQStore,
    onExportBackup: () -> Unit,
    onRestoreBackup: () -> Unit,
    initialPage: SettingsPage? = null
) {
    var page by remember { mutableStateOf(initialPage) }
    var confirmClear by remember { mutableStateOf(false) }
    var previewTheme by remember { mutableStateOf<AppTheme?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        PaperPushHost(
            page = page,
            onDismiss = { page = null },
            root = {
                SettingsHome(store, onOpen = { page = it })
            },
            detail = { current ->
                when (current) {
                    SettingsPage.APPEARANCE -> SettingsDetail(
                        title = "外观",
                        subtitle = "主题决定布料颜色，明暗决定光线",
                        onBack = { page = null }
                    ) {
                        AppearanceSettings(store, onPreview = { previewTheme = it })
                    }
                    SettingsPage.PRACTICE -> SettingsDetail(
                        title = "练习偏好",
                        subtitle = "默认只是起点，每次开始仍可临时调整",
                        onBack = { page = null }
                    ) {
                        PracticeSettings(store)
                    }
                    SettingsPage.DATA -> SettingsDetail(
                        title = "题库数据",
                        subtitle = "备份、恢复和清理都只在本机完成",
                        onBack = { page = null }
                    ) {
                        DataSettings(
                            store = store,
                            onExportBackup = onExportBackup,
                            onRestoreBackup = onRestoreBackup,
                            onClear = { confirmClear = true }
                        )
                    }
                    SettingsPage.EXPERIENCE -> SettingsDetail(
                        title = "体验与帮助",
                        subtitle = "作答反馈和题库导入说明",
                        onBack = { page = null }
                    ) {
                        ExperienceSettings(store, onOpenHelp = { page = SettingsPage.IMPORT_HELP })
                    }
                    SettingsPage.ABOUT -> SettingsDetail(
                        title = "关于 BookQ",
                        subtitle = "版本、主题与数据说明",
                        onBack = { page = null }
                    ) {
                        AboutSettings(store)
                    }
                    SettingsPage.IMPORT_HELP -> SettingsDetail(
                        title = "怎么导入题库",
                        subtitle = "导入按钮只在「题库」页出现",
                        onBack = { page = null }
                    ) {
                        ImportHelp()
                    }
                }
            }
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

    previewTheme?.let { theme ->
        ThemePreviewSheet(
            theme = theme,
            appearance = store.state.appearance,
            currentTheme = store.state.theme,
            currentAppearance = store.state.appearance,
            onDismiss = { previewTheme = null },
            onApply = {
                store.setTheme(theme)
                previewTheme = null
            }
        )
    }
}

@Composable
private fun SettingsHome(store: BookQStore, onOpen: (SettingsPage) -> Unit) {
    val sizeLabel = if (store.state.defaultSize == 0) "全部" else "${store.state.defaultSize} 题"
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(top = 8.dp, bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        SettingsHeader("设置", "分组设置，点进去再改") { BrandMark(38.dp) }

        SettingsGroup("练习") {
            SettingsEntry(
                icon = Icons.Outlined.Edit,
                title = "练习偏好",
                detail = "默认模式、题量、顺序",
                value = "${store.state.defaultMode.title} · $sizeLabel · ${store.state.defaultOrder.title}",
                onClick = { onOpen(SettingsPage.PRACTICE) }
            )
        }

        SettingsGroup("个性化") {
            SettingsEntry(
                icon = Icons.Outlined.Contrast,
                title = "外观",
                detail = "主题与浅色 / 深色模式",
                value = "${store.state.theme.title} · ${store.state.appearance.title}",
                onClick = { onOpen(SettingsPage.APPEARANCE) }
            )
        }

        SettingsGroup("数据") {
            SettingsEntry(
                icon = Icons.Outlined.Inventory2,
                title = "题库数据",
                detail = "导出、恢复、清空答题记录",
                value = "${store.state.banks.size} 个题库 · ${store.allQuestions.size} 题",
                onClick = { onOpen(SettingsPage.DATA) }
            )
        }

        SettingsGroup("更多") {
            SettingsEntry(
                icon = Icons.Outlined.TouchApp,
                title = "体验与帮助",
                detail = "触感反馈、题库导入说明",
                value = if (store.state.haptics) "触感开启" else "触感关闭",
                onClick = { onOpen(SettingsPage.EXPERIENCE) }
            )
            SettingsDivider()
            SettingsEntry(
                icon = Icons.Outlined.Palette,
                title = "关于 BookQ",
                detail = "版本、主题与数据说明",
                value = "0.1.8",
                onClick = { onOpen(SettingsPage.ABOUT) }
            )
        }

        Text(
            "题库不内置在 App 里。\n导入、备份、恢复都在本机完成，不上传任何文档。",
            style = sans(10).copy(color = Palette.Muted, lineHeight = 15.sp),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun PracticeSettings(store: BookQStore) {
    SettingsGroup("默认模式") {
        QuizMode.entries.forEachIndexed { index, mode ->
            ChoiceRow(
                title = mode.longTitle,
                detail = mode.subtitle,
                selected = store.state.defaultMode == mode,
                onClick = { store.setDefaultMode(mode) }
            )
            if (index != QuizMode.entries.lastIndex) SettingsDivider()
        }
    }

    SettingsGroup("默认题量") {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(10, 20, 50, 0).forEach { size ->
                    SizeOption(
                        label = if (size == 0) "全部" else "$size",
                        selected = store.state.defaultSize == size,
                        modifier = Modifier.weight(1f),
                        onClick = { store.setDefaultSize(size) }
                    )
                }
            }
            Text(
                "开始练习时还可以再改，这里只决定默认值。",
                style = sans(10).copy(color = Palette.Muted)
            )
        }
    }

    SettingsGroup("默认顺序") {
        QuizOrder.entries.forEachIndexed { index, order ->
            ChoiceRow(
                title = order.title,
                detail = order.subtitle,
                selected = store.state.defaultOrder == order,
                onClick = { store.setDefaultOrder(order) }
            )
            if (index != QuizOrder.entries.lastIndex) SettingsDivider()
        }
    }
}

@Composable
private fun AppearanceSettings(store: BookQStore, onPreview: (AppTheme) -> Unit) {
    SettingsGroup("明暗模式") {
        AppAppearance.entries.forEachIndexed { index, appearance ->
            ChoiceRow(
                title = appearance.title,
                detail = appearance.subtitle,
                selected = store.state.appearance == appearance,
                onClick = { store.setAppearance(appearance) }
            )
            if (index != AppAppearance.entries.lastIndex) SettingsDivider()
        }
    }

    SettingsGroup("主题") {
        AppTheme.entries.forEachIndexed { index, theme ->
            ThemeRow(
                theme = theme,
                appearance = store.state.appearance,
                selected = store.state.theme == theme,
                onClick = { onPreview(theme) }
            )
            if (index != AppTheme.entries.lastIndex) SettingsDivider()
        }
    }

    Text(
        "明暗会立即应用；主题先预览，按「使用这个主题」才会切换。题库、进度和错题本不受影响。",
        style = sans(10).copy(color = Palette.Muted, lineHeight = 15.sp),
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

@Composable
private fun ThemeRow(
    theme: AppTheme,
    appearance: AppAppearance,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = BookQPalettes.of(theme, appearance)
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.size(width = 30.dp, height = 30.dp).clip(RoundedCornerShape(8.dp)) ) {
            Box(Modifier.weight(1f).fillMaxSize().background(colors.paper))
            Box(Modifier.weight(1f).fillMaxSize().background(colors.primary))
            Box(Modifier.weight(1f).fillMaxSize().background(colors.accent))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(theme.title, style = sans(13, FontWeight.Medium))
            Text(theme.subtitle, style = sans(10).copy(color = Palette.Muted))
        }
        if (selected) {
            Box(
                Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Palette.Primary)
                    .border(1.dp, Palette.Primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
        } else {
            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = Palette.Line,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun DataSettings(
    store: BookQStore,
    onExportBackup: () -> Unit,
    onRestoreBackup: () -> Unit,
    onClear: () -> Unit
) {
    CardSurface(radius = 16.dp, padding = 0.dp) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DataStat("${store.state.banks.size}", "题库", Modifier.weight(1f))
            DataDivider()
            DataStat("${store.allQuestions.size}", "题目", Modifier.weight(1f))
            DataDivider()
            DataStat("${store.state.attempts.size}", "次练习", Modifier.weight(1f))
        }
    }

    SettingsGroup("备份与恢复") {
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
    }

    SettingsGroup("清理") {
        ActionRow(
            icon = Icons.Outlined.DeleteOutline,
            title = "清空答题记录",
            detail = "保留题库，只清进度和错题",
            color = Palette.Red,
            destructive = true,
            onClick = onClear
        )
    }

    Text(
        "备份文件是 JSON，由你自己保存。恢复前会覆盖当前数据。",
        style = sans(10).copy(color = Palette.Muted, lineHeight = 15.sp),
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

@Composable
private fun ExperienceSettings(store: BookQStore, onOpenHelp: () -> Unit) {
    SettingsGroup("作答反馈") {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            SettingsIcon(Icons.Outlined.TouchApp, Palette.Primary)
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
    }

    SettingsGroup("题库导入") {
        SettingsEntry(
            icon = Icons.AutoMirrored.Outlined.HelpOutline,
            title = "怎么导入题库",
            detail = "支持 DOCX / TXT / MD / JSON",
            value = "查看",
            onClick = onOpenHelp
        )
    }
}

@Composable
private fun AboutSettings(store: BookQStore) {
    CardSurface(radius = 18.dp, padding = 20.dp, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            BrandMark(54.dp)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("BookQ", style = serif(27))
                Text("不内置题库的离线刷题 App", style = sans(11).copy(color = Palette.Muted))
            }
        }
    }

    SettingsGroup("应用") {
        InfoRow(Icons.Outlined.Inventory2, "版本", "0.1.8")
        SettingsDivider()
        InfoRow(Icons.Outlined.FileUpload, "题库", "全部由你导入")
        SettingsDivider()
        InfoRow(Icons.Outlined.Palette, "视觉", "${store.state.theme.title} · ${store.state.appearance.title}")
    }

    Text(
        "题库、答题记录和错题本只保存在本机。\n不登录、不联网、不上传任何文档。",
        style = sans(10).copy(color = Palette.Muted, lineHeight = 15.sp),
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

@Composable
private fun ImportHelp() {
    CardSurface(radius = 16.dp, padding = 18.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            StepRow("1", "打开「题库」页", "导入按钮只在题库页右上角出现。")
            StepRow("2", "点「导入」", "选择手机里的 DOCX、TXT、MD 或 JSON 文件。")
            StepRow("3", "确认后保存", "先预览题库名、题数和少量样题，再决定导入。")
        }
    }

    SettingsGroup("支持格式") {
        Row(
            Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("DOCX", "TXT", "MD", "JSON").forEach { format ->
                Text(
                    format,
                    style = sans(11, FontWeight.SemiBold).copy(color = Palette.Primary),
                    modifier = Modifier
                        .clip(RoundedCornerShape(9.dp))
                        .background(Palette.Track)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }

    Text(
        "一份文件就是一个题库，分类名默认取文件名。\n文档里的小标题只作分隔，不额外分类。",
        style = sans(10).copy(color = Palette.Muted, lineHeight = 15.sp),
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

@Composable
private fun SettingsHeader(title: String, subtitle: String, trailing: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = serif(28))
            Text(subtitle, style = sans(11).copy(color = Palette.Muted))
        }
        trailing()
    }
}

@Composable
private fun SettingsDetail(title: String, subtitle: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(top = 8.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Palette.Card)
                    .border(1.dp, Palette.Line, CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回", tint = Palette.Primary, modifier = Modifier.size(18.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = serif(25))
                Text(subtitle, style = sans(11).copy(color = Palette.Muted))
            }
        }
        content()
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
private fun SettingsEntry(
    icon: ImageVector,
    title: String,
    detail: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        SettingsIcon(icon, Palette.Primary)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = sans(14, FontWeight.Medium))
            Text(detail, style = sans(10).copy(color = Palette.Muted))
        }
        Text(
            value,
            style = sans(11).copy(color = Palette.Muted),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, tint = Palette.Line, modifier = Modifier.size(11.dp))
    }
}

@Composable
private fun ChoiceRow(title: String, detail: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = sans(13, FontWeight.Medium))
            Text(detail, style = sans(10).copy(color = Palette.Muted))
        }
        Box(
            Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (selected) Palette.Primary else Color.Transparent)
                .border(1.dp, if (selected) Palette.Primary else Palette.Line, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Icon(Icons.Outlined.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
private fun SizeOption(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier
            .height(46.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Palette.Primary else Palette.ChipBg)
            .border(1.dp, if (selected) Palette.Primary else Palette.Line, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            if (label == "全部") label else "$label 题",
            style = sans(13, FontWeight.SemiBold).copy(color = if (selected) Color.White else Palette.Ink2)
        )
    }
}

@Composable
private fun DataStat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(value, style = serif(21).copy(color = Palette.Primary))
        Text(label, style = sans(10).copy(color = Palette.Muted))
    }
}

@Composable
private fun DataDivider() {
    Box(Modifier.width(1.dp).height(34.dp).background(Palette.Line2))
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
        Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, tint = Palette.Line, modifier = Modifier.size(10.dp))
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
private fun StepRow(number: String, title: String, detail: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Box(
            Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(Palette.Primary.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Text(number, style = serif(13).copy(color = Palette.Primary))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = sans(13, FontWeight.Medium))
            Text(detail, style = sans(10).copy(color = Palette.Muted, lineHeight = 15.sp))
        }
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

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        color = Palette.Line2,
        thickness = 1.dp,
        modifier = Modifier.padding(start = 50.dp)
    )
}

// MARK: - 主题预览

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemePreviewSheet(
    theme: AppTheme,
    appearance: AppAppearance,
    currentTheme: AppTheme,
    currentAppearance: AppAppearance,
    onDismiss: () -> Unit,
    onApply: () -> Unit
) {
    val colors = BookQPalettes.of(theme, appearance)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.paper,
        dragHandle = { SheetGrabber() }
    ) {
        ThemePreviewPanel(
            theme = theme,
            appearance = appearance,
            currentTheme = currentTheme,
            currentAppearance = currentAppearance,
            onDismiss = onDismiss,
            onApply = onApply,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.72f)
                .navigationBarsPadding()
        )
    }
}

@Composable
internal fun ThemePreviewPanel(
    theme: AppTheme,
    appearance: AppAppearance,
    currentTheme: AppTheme,
    currentAppearance: AppAppearance,
    onDismiss: () -> Unit,
    onApply: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = BookQPalettes.of(theme, appearance)
    Column(
        modifier
            .background(colors.paper)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    "主题预览 · ${appearance.title}",
                    style = sans(10, FontWeight.SemiBold).copy(
                        color = colors.gold,
                        letterSpacing = 1.2.sp
                    )
                )
                Text(theme.title, style = serif(28).copy(color = colors.ink))
                Text(theme.subtitle, style = sans(11).copy(color = colors.muted))
            }
            PreviewCloseCircle(colors, onDismiss)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "答题页",
                style = sans(11, FontWeight.SemiBold).copy(color = colors.ink2, letterSpacing = 0.5.sp)
            )
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.gold.copy(alpha = 0.12f))
                    .border(1.dp, colors.gold.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Text(
                    "预览",
                    style = sans(9, FontWeight.SemiBold).copy(color = colors.gold, letterSpacing = 0.8.sp)
                )
            }
        }

        ThemeSampleCard(colors)

        Spacer(Modifier.weight(1f))

        Text(
            "当前仍是 ${currentTheme.title} · ${currentAppearance.title}。按「使用这个主题」才会切换；题库和进度不受影响。",
            style = sans(10).copy(color = colors.muted, lineHeight = 15.sp)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.card)
                    .border(1.dp, colors.line, RoundedCornerShape(12.dp))
                    .clickable(onClick = onDismiss)
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("取消", style = sans(15, FontWeight.Medium).copy(color = colors.ink2))
            }
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.primary)
                    .clickable(onClick = onApply)
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("使用这个主题", style = sans(15, FontWeight.SemiBold).copy(color = Color.White))
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ThemeSampleCard(colors: BookQColors) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.card)
            .border(1.dp, colors.line, RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "BookQ",
                style = sans(10, FontWeight.SemiBold).copy(color = colors.muted, letterSpacing = 0.8.sp)
            )
            Spacer(Modifier.width(8.dp))
            Box(Modifier.width(22.dp).height(2.dp).background(colors.gold.copy(alpha = 0.7f)))
            Spacer(Modifier.weight(1f))
            Text("03 / 10", style = sans(10).copy(color = colors.muted))
        }

        Box(
            Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .height(2.dp)
                .background(colors.track)
        ) {
            Box(Modifier.fillMaxWidth(0.3f).height(2.dp).background(colors.primary))
        }

        Text(
            "梭织物的三原组织不包括以下哪一项？",
            style = serif(19).copy(color = colors.ink, lineHeight = 26.sp),
            modifier = Modifier.padding(top = 18.dp)
        )

        Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            ThemeSampleOption(colors, "A", "平纹", false)
            ThemeSampleOption(colors, "B", "斜纹", false)
            ThemeSampleOption(colors, "C", "缎纹", false)
            ThemeSampleOption(colors, "D", "提花", true)
        }

        HorizontalDivider(
            color = colors.line2,
            thickness = 1.dp,
            modifier = Modifier.padding(top = 12.dp)
        )
        Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("单选 · 题库原文", style = sans(9).copy(color = colors.muted))
            Spacer(Modifier.weight(1f))
            Text("下一题 ›", style = sans(9, FontWeight.Medium).copy(color = colors.gold))
        }
    }
}

@Composable
private fun ThemeSampleOption(colors: BookQColors, label: String, text: String, selected: Boolean) {
    val shape = RoundedCornerShape(9.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) colors.primary.copy(alpha = 0.08f) else colors.paper.copy(alpha = if (colors.isDark) 0.35f else 0.55f))
            .border(1.dp, if (selected) colors.primary else colors.line, shape)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (selected) colors.primary else colors.paper)
                .border(1.dp, if (selected) colors.primary else colors.line, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                label,
                style = sans(10, FontWeight.SemiBold).copy(color = if (selected) Color.White else colors.ink2)
            )
        }
        Text(text, style = sans(12).copy(color = if (selected) colors.ink else colors.ink2))
        Spacer(Modifier.weight(1f))
        if (selected) {
            Box(
                Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(colors.accent),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
            }
        }
    }
}

@Composable
private fun PreviewCloseCircle(colors: BookQColors, onClick: () -> Unit) {
    Box(
        Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(colors.card)
            .border(1.dp, colors.line, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Outlined.Close, contentDescription = "关闭", tint = colors.ink2, modifier = Modifier.size(14.dp))
    }
}
