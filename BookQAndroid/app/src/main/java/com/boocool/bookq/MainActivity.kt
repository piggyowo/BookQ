package com.boocool.bookq

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = BookQStore(applicationContext)
        setContent {
            BookQTheme {
                BookQApp(store)
            }
        }
    }
}

private sealed class LibrarySheet {
    data class Preview(val parsed: ParsedBank) : LibrarySheet()
    data class Detail(val bankId: String) : LibrarySheet()
}

@Composable
private fun BookQApp(store: BookQStore) {
    val context = LocalContext.current
    val coroutine = rememberCoroutineScope()
    val activity = context as? Activity
    SideEffect {
        activity?.let {
            WindowCompat.getInsetsController(it.window, it.window.decorView).isAppearanceLightStatusBars = !Palette.IsDark
        }
    }

    var showStart by remember { mutableStateOf(false) }
    var presetBankId by remember { mutableStateOf<String?>(null) }
    var librarySheet by remember { mutableStateOf<LibrarySheet?>(null) }
    var loading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val openDocument = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            loading = true
            coroutine.launch {
                val result = runCatching {
                    withContext(Dispatchers.IO) { importUri(context, uri) }
                }
                loading = false
                result.onSuccess { parsed -> librarySheet = LibrarySheet.Preview(parsed) }
                    .onFailure { errorMessage = it.message ?: "导入失败" }
            }
        }
    }

    val restoreDocument = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            coroutine.launch {
                val result = runCatching {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                            ?: throw ImportException("读取备份文件失败")
                    }
                }
                result.onSuccess { text ->
                    runCatching { store.restoreBackup(text) }
                        .onFailure { errorMessage = it.message ?: "恢复失败" }
                }.onFailure { errorMessage = it.message ?: "恢复失败" }
            }
        }
    }

    val createBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(store.backupJson()) }
                    ?: throw ImportException("写入备份文件失败")
            }.onSuccess { store.showToast("备份已导出") }
                .onFailure { errorMessage = it.message ?: "导出失败" }
        }
    }

    when (store.screen) {
        Screen.HOME -> {
            Scaffold(
                bottomBar = {
                    BottomTabBar(
                        selected = store.state.selectedTab,
                        bankCount = store.state.banks.size,
                        wrongCount = store.state.wrongIds.size,
                        onSelect = { store.setTab(it) }
                    )
                },
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets(0, 0, 0, 0)
            ) { padding ->
                Box(Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding())) {
                    PaperBackground()
                    when (store.state.selectedTab) {
                        AppTab.HOME -> HomeScreen(
                            store = store,
                            onStart = { bankId ->
                                presetBankId = bankId
                                showStart = true
                            },
                        onWrong = { store.openWrongBook() }
                        )
                        AppTab.LIBRARY -> LibraryScreen(
                            store = store,
                            onImport = {
                                openDocument.launch(
                                    arrayOf(
                                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                        "text/plain",
                                        "text/markdown",
                                        "application/json",
                                        "*/*"
                                    )
                                )
                            },
                            onOpenBank = { librarySheet = LibrarySheet.Detail(it) }
                        )
                        AppTab.SETTINGS -> SettingsScreen(
                            store = store,
                            onExportBackup = { createBackup.launch("BookQ备份.json") },
                            onRestoreBackup = { restoreDocument.launch(arrayOf("application/json", "*/*")) }
                        )
                    }
                }
            }
        }
        Screen.QUIZ -> {
            Box(Modifier.fillMaxSize()) {
                PaperBackground()
                QuizScreen(store) { }
            }
        }
        Screen.RESULT -> {
            Box(Modifier.fillMaxSize()) {
                PaperBackground()
                ResultScreen(store)
            }
        }
        Screen.WRONG -> {
            Box(Modifier.fillMaxSize()) {
                PaperBackground()
                WrongBookScreen(store)
            }
        }
    }

    if (showStart) {
        StartQuizSheet(store, presetBankId, onDismiss = { showStart = false })
    }

    when (val sheet = librarySheet) {
        is LibrarySheet.Preview -> {
            ImportPreviewSheet(
                parsed = sheet.parsed,
                onDismiss = { librarySheet = null },
                onSave = { name ->
                    val bank = store.addBank(name, sheet.parsed.fileName, sheet.parsed.questions)
                    librarySheet = null
                    if (bank != null) store.showToast("已导入《${bank.name}》· ${bank.count} 题")
                }
            )
        }
        is LibrarySheet.Detail -> {
            BankDetailSheet(
                store = store,
                bankId = sheet.bankId,
                onDismiss = { librarySheet = null }
            )
        }
        null -> Unit
    }

    if (loading) {
        Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                Modifier
                    .background(Palette.Card, RoundedCornerShape(16.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(color = Palette.Primary, modifier = Modifier.size(26.dp))
                Text("正在解析题库…", style = sans(13).copy(color = Palette.Ink2))
            }
        }
    }

    if (store.toast != null) {
        ToastBar(store.toast!!)
    }

    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text("导入失败", style = serif(20)) },
            text = { Text(errorMessage!!, style = sans(13)) },
            confirmButton = { TextButton(onClick = { errorMessage = null }) { Text("知道了") } }
        )
    }
}

private fun importUri(context: Context, uri: Uri): ParsedBank {
    val displayName = queryDisplayName(context, uri)
    val mime = context.contentResolver.getType(uri).orEmpty()
    val fallbackExt = when {
        mime.contains("wordprocessingml") -> "docx"
        mime == "text/plain" -> "txt"
        mime.contains("markdown") -> "md"
        mime.contains("json") -> "json"
        else -> "docx"
    }
    val fileName = displayName?.takeIf { it.contains('.') } ?: "题库-${System.currentTimeMillis()}.$fallbackExt"
    val dir = File(context.cacheDir, "BookQImport-${UUID.randomUUID()}")
    dir.mkdirs()
    return try {
        val file = File(dir, fileName)
        context.contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { input.copyTo(it) }
        } ?: throw ImportException("无法读取所选文件")
        ImportService.parse(file, fileName)
    } finally {
        dir.deleteRecursively()
    }
}

private fun queryDisplayName(context: Context, uri: Uri): String? {
    return context.contentResolver.query(
        uri,
        arrayOf(OpenableColumns.DISPLAY_NAME),
        null,
        null,
        null
    )?.use { cursor ->
        if (cursor.moveToFirst()) {
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0) cursor.getString(index) else null
        } else null
    }
}
