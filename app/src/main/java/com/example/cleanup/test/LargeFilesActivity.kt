package com.example.cleanup.test

import android.app.Activity
import android.content.ContentUris
import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.database.getLongOrNull
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class LargeFileItem(
    val uri: Uri,
    val displayName: String,
    val sizeBytes: Long,
    val mimeType: String?,
    val dateAddedSec: Long?,
    val source: Source
) {
    enum class Source { MediaStore, SAF }
}

fun formatSize(bytes: Long): String {
    val kb = 1024.0
    val mb = kb * 1024
    val gb = mb * 1024
    return when {
        bytes >= gb -> String.format(Locale.US, "%.2f GB", bytes / gb)
        bytes >= mb -> String.format(Locale.US, "%.2f MB", bytes / mb)
        bytes >= kb -> String.format(Locale.US, "%.2f KB", bytes / kb)
        else -> "$bytes B"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
class LargeFilesActivity : androidx.activity.ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                BigFileCleanerScreen()
            }
        }
    }
}

@Composable
fun BigFileCleanerScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 权限请求（根据系统版本动态）
    val neededPermissions = remember {
        if (Build.VERSION.SDK_INT >= 33) {
            arrayOf(
                android.Manifest.permission.READ_MEDIA_IMAGES,
                android.Manifest.permission.READ_MEDIA_VIDEO,
                android.Manifest.permission.READ_MEDIA_AUDIO
            )
        } else {
            arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }
    var hasPermissions by remember { mutableStateOf(false) }
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasPermissions = result.values.all { it }
    }

    // SAF：选择文件夹
    var pickedTreeUri by remember { mutableStateOf<Uri?>(null) }
    val treePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            // 持久化权限
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            pickedTreeUri = uri
        }
    }

    // 批量删除（MediaStore 删除需要系统确认）
    var pendingDeleteUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    val deleteSender = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { /* result -> 可根据需要提示成功/取消 */ }

    // 状态
    var thresholdMBText by remember { mutableStateOf("50") } // 默认阈值 50MB
    val thresholdMB = thresholdMBText.toLongOrNull()?.coerceAtLeast(1) ?: 50L
    var isScanning by remember { mutableStateOf(false) }
    var scanProgress by remember { mutableStateOf(0f) }
    var items by remember { mutableStateOf(listOf<LargeFileItem>()) }
    var selected by remember { mutableStateOf(setOf<Uri>()) }
    var scanSource by remember { mutableStateOf(LargeFileItem.Source.MediaStore) }

    val totalSizeSelected = remember(selected, items) {
        items.filter { selected.contains(it.uri) }.sumOf { it.sizeBytes }
    }

    fun toggle(uri: Uri) {
        selected = if (selected.contains(uri)) selected - uri else selected + uri
    }

    // 执行 MediaStore 扫描
    suspend fun scanMediaStore(minSizeBytes: Long): List<LargeFileItem> {
        val out = mutableListOf<LargeFileItem>()
        val resolver = context.contentResolver
        // 查询 Files 表，覆盖 images/video/audio/downloads 等
        val external = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.DATE_ADDED
        )
        val selection = "${MediaStore.Files.FileColumns.SIZE} >= ?"
        val args = arrayOf(minSizeBytes.toString())

        resolver.query(
            external,
            projection,
            selection,
            args,
            "${MediaStore.Files.FileColumns.SIZE} DESC"
        )?.use { c ->
            val total = c.count.takeIf { it > 0 } ?: 1
            var idx = 0
            while (c.moveToNext()) {
                val id = c.getLong(0)
                val name = c.getString(1) ?: "(unknown)"
                val size = c.getLong(2)
                val mime = c.getString(3)
                val date = c.getLongOrNull(4)
                val uri = ContentUris.withAppendedId(external, id)
                out += LargeFileItem(
                    uri,
                    name,
                    size,
                    mime,
                    date,
                    LargeFileItem.Source.MediaStore
                )
                idx++
                if (idx % 20 == 0) {
                    // 简单进度反馈
                    scanProgress = idx.toFloat() / total
                    delay(1)
                }
            }
        }
        scanProgress = 1f
        return out
    }

    // 递归扫描 SAF 目录
    suspend fun scanTree(minSizeBytes: Long, tree: Uri): List<LargeFileItem> {
        val out = mutableListOf<LargeFileItem>()
        fun walk(doc: DocumentFile) {
            if (doc.isDirectory) {
                doc.listFiles().forEach { walk(it) }
            } else if (doc.isFile) {
                val size = doc.length()
                if (size >= minSizeBytes) {
                    out += LargeFileItem(
                        uri = doc.uri,
                        displayName = doc.name ?: "(unknown)",
                        sizeBytes = size,
                        mimeType = doc.type,
                        dateAddedSec = null,
                        source = LargeFileItem.Source.SAF
                    )
                }
            }
        }
        val root = DocumentFile.fromTreeUri(context, tree)
        root?.let { walk(it) }
        return out.sortedByDescending { it.sizeBytes }
    }

    // 点击扫描
    fun onScan(source: LargeFileItem.Source) {
        scanSource = source
        if (source == LargeFileItem.Source.MediaStore && !hasPermissions) {
            permLauncher.launch(neededPermissions)
            return
        }
        if (source == LargeFileItem.Source.SAF && pickedTreeUri == null) {
            treePicker.launch(null)
            return
        }
        scope.launch(Dispatchers.IO) {
            isScanning = true
            scanProgress = 0f
            selected = emptySet()
            val minBytes = thresholdMB * 1024L * 1024L
            val result = if (source == LargeFileItem.Source.MediaStore) {
                scanMediaStore(minBytes)
            } else {
                scanTree(minBytes, pickedTreeUri!!)
            }
            items = result
            isScanning = false
        }
    }

    // 删除（MediaStore: 系统确认；SAF: 直接删除）
    fun onDeleteSelected() {
        val toDelete = items.filter { selected.contains(it.uri) }
        if (toDelete.isEmpty()) return
        when (scanSource) {
            LargeFileItem.Source.MediaStore -> {
                val uris = toDelete.map { it.uri }
                pendingDeleteUris = uris
                val intent = if (Build.VERSION.SDK_INT >= 30) {
                    MediaStore.createDeleteRequest(context.contentResolver, uris)
                } else {
                    // Android 10 也支持 createDeleteRequest
                    MediaStore.createDeleteRequest(context.contentResolver, uris)
                }
                deleteSender.launch(
                    IntentSenderRequest.Builder(intent.intentSender).build()
                )
            }
            LargeFileItem.Source.SAF -> {
                scope.launch(Dispatchers.IO) {
                    var deleted = 0
                    toDelete.forEach { item ->
                        val df = DocumentFile.fromSingleUri(context, item.uri)
                        if (df != null && df.exists()) {
                            if (df.delete()) deleted++
                        }
                    }
                    // 刷新列表
                    items = items.filterNot { selected.contains(it.uri) }
                    selected = emptySet()
                }
            }
        }
    }

    // UI
    Scaffold(
        topBar = {

        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(12.dp)
                .fillMaxSize()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = thresholdMBText,
                    onValueChange = { thresholdMBText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("大小阈值 (MB)") },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Text(text = "当前：${thresholdMB}MB", modifier = Modifier.widthIn(min = 100.dp))
            }

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onScan(LargeFileItem.Source.MediaStore) }) {
                    Text("扫描媒体库 (MediaStore)")
                }
                Button(onClick = { onScan(LargeFileItem.Source.SAF) }) {
                    Text("选择文件夹并扫描 (SAF)")
                }
            }

            if (pickedTreeUri != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "已选文件夹: ${pickedTreeUri}",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(8.dp))

            if (isScanning) {
                LinearProgressIndicator(progress = scanProgress , modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Text("正在扫描中… ${"%.0f".format(scanProgress * 100)}%")
            }

            val totalCount = items.size
            val totalSize = items.sumOf { it.sizeBytes }
            val selectedCount = selected.size

            Row(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("发现：$totalCount 个大文件（合计 ${formatSize(totalSize)}）")
                Text("已选：$selectedCount 个（${formatSize(totalSizeSelected)}）")
            }

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    selected = items.map { it.uri }.toSet()
                }, enabled = items.isNotEmpty()) {
                    Text("全选")
                }
                OutlinedButton(onClick = {
                    selected = emptySet()
                }, enabled = selected.isNotEmpty()) {
                    Text("全不选")
                }
                Button(
                    onClick = { onDeleteSelected() },
                    enabled = selected.isNotEmpty()
                ) {
                    Text("删除所选")
                }
            }

            Spacer(Modifier.height(8.dp))

            LazyColumn(Modifier.fillMaxSize()) {
                items(items, key = { it.uri }) { f ->
                    FileRow(
                        item = f,
                        checked = selected.contains(f.uri),
                        onToggle = { toggle(f.uri) }
                    )
                }
            }
        }
    }
}

@Composable
fun FileRow(item: LargeFileItem, checked: Boolean, onToggle: () -> Unit) {
    val dateStr = remember(item.dateAddedSec) {
        item.dateAddedSec?.let {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            sdf.format(Date(it * 1000))
        } ?: "-"
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
        Column(Modifier.weight(1f)) {
            Text(item.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${formatSize(item.sizeBytes)} • ${item.mimeType ?: "unknown"} • ${item.source} • $dateStr",
                style = MaterialTheme.typography.bodySmall
            )
            Text(item.uri.toString(), style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
