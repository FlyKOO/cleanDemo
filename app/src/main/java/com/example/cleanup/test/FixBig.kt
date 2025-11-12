package com.example.cleanup.test

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

class FixBig : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { App() }
    }
}
data class FileItem(
    val path: String,
    val name: String,
    val size: Long,
    val lastModified: Long,
    var checked: Boolean = false,
) {
    // 使 checked 变为可观察状态（修复“无法选择/全选无效”）
    var checkedState by mutableStateOf(checked)
}

private fun humanSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val u = arrayOf("KB","MB","GB","TB","PB")
    var v = bytes.toDouble(); var i = 0
    while (v >= 1024 && i < u.lastIndex) { v /= 1024; i++ }
    return String.format(Locale.getDefault(), "%.2f %s", v, u[i])
}

private fun formatTime(millis: Long): String {
    val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return fmt.format(Date(millis))
}

private fun isAllFilesGranted(): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Environment.isExternalStorageManager() else true

private fun openAllFilesGrant(activity: Activity) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val uri = Uri.parse("package:${activity.packageName}")
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri)
            activity.startActivity(intent)
        }
    } catch (_: Exception) {
        try { activity.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)) }
        catch (_: ActivityNotFoundException) {}
    }
}

/** 递归扫描（IO 线程），可被协程取消 */
private suspend fun scanLargeFiles(
    rootPath: String,
    minBytes: Long,
    onProgress: (scanned: Long, found: Int) -> Unit
): List<FileItem> = withContext(Dispatchers.IO) {
    val result = ArrayList<FileItem>(256)
    var scanned = 0L

    fun walk(dir: File) {
        if (!isActive) return
        val children = try { dir.listFiles() } catch (_: SecurityException) { null } ?: return
        for (f in children) {
            if (!isActive) return
            if (f.isDirectory) {
                if (f.name.equals("Android", ignoreCase = true)) continue // 跳过受保护目录
                walk(f)
            } else if (f.isFile) {
                scanned++
                if (f.length() >= minBytes) {
                    result.add(FileItem(
                        path = f.absolutePath,
                        name = f.name,
                        size = f.length(),
                        lastModified = f.lastModified()
                    ))
                }
                if (scanned % 200 == 0L) onProgress(scanned, result.size)
            }
        }
    }

    val root = File(rootPath)
    if (root.exists() && root.isDirectory && root.canRead()) walk(root)
    onProgress(scanned, result.size)
    result.sortByDescending { it.size }
    result
}

/** 删除文件（IO 线程） */
private suspend fun deleteFiles(
    items: List<FileItem>,
    onEach: (ok: Boolean, item: FileItem) -> Unit
): Pair<Int, Int> = withContext(Dispatchers.IO) {
    var ok = 0; var fail = 0
    for (i in items) {
        if (!isActive) break
        val r = try { File(i.path).delete() } catch (_: SecurityException) { false }
        if (r) ok++ else fail++
        onEach(r, i)
    }
    ok to fail
}

/** ====== Compose UI ====== */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun App() {
    MaterialTheme {
        val ctx = LocalContext.current
        val activity = ctx as Activity
        val scope = rememberCoroutineScope()

        // 默认目录 -> /sdcard
        var dirText by remember { mutableStateOf(TextFieldValue("/sdcard")) }
        // 最小体积阈值（MB）
        var minMbText by remember { mutableStateOf(TextFieldValue("50")) }

        // 列表与状态
        val files = remember { mutableStateListOf<FileItem>() }
        var scanning by remember { mutableStateOf(false) }
        var scannedCount by remember { mutableStateOf(0L) }
        var foundCount by remember { mutableStateOf(0) }
        var job by remember { mutableStateOf<Job?>(null) }

        // 权限（<= Android 10）
        val needsLegacyPerm = remember { Build.VERSION.SDK_INT < Build.VERSION_CODES.R }
        val permLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { /* 用户可再次点击触发 */ }

        fun ensurePermissionOrGuide(): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (!isAllFilesGranted()) {
                    Toast.makeText(ctx, "请授予“所有文件访问”后再扫描/删除", Toast.LENGTH_SHORT).show()
                    openAllFilesGrant(activity)
                    false
                } else true
            } else {
                permLauncher.launch(arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ))
                true
            }
        }

        fun cancelScanIfRunning() {
            job?.cancel(); job = null; scanning = false
        }

        fun startScan() {
            if (!ensurePermissionOrGuide()) return
            val path = dirText.text.trim()
            val minMb = minMbText.text.trim().toLongOrNull() ?: 50L
            val minBytes = max(1L, minMb) * 1024L * 1024L

            val f = File(path)
            if (!(f.exists() && f.isDirectory)) {
                Toast.makeText(ctx, "目录不存在或不可读：$path", Toast.LENGTH_SHORT).show()
                return
            }

            cancelScanIfRunning()
            files.clear()
            scannedCount = 0; foundCount = 0; scanning = true

            job = scope.launch(Dispatchers.Default) {
                val list = scanLargeFiles(path, minBytes) { scanned, found ->
                    scannedCount = scanned; foundCount = found
                }
                withContext(Dispatchers.Main) {
                    files.clear(); files.addAll(list)
                    scanning = false
                    Toast.makeText(ctx, "扫描完成，找到 $foundCount 个大文件", Toast.LENGTH_SHORT).show()
                }
            }
        }

        fun selectAll(v: Boolean) {
            files.forEach { it.checkedState = v } // 可观察状态 => 立即刷新
        }

        val selected = files.filter { it.checkedState }
        val selectedSize = selected.sumOf { it.size }

        suspend fun doDelete() {
            if (selected.isEmpty()) {
                Toast.makeText(ctx, "请先选择要删除的文件", Toast.LENGTH_SHORT).show()
                return
            }
            val (ok, fail) = deleteFiles(selected) { _, _ -> }
            // 从 UI 列表移除已不存在的
            files.removeAll(selected.filter { !File(it.path).exists() }.toSet())
            Toast.makeText(ctx, "删除完成：成功 $ok，失败 $fail", Toast.LENGTH_LONG).show()
        }

        Scaffold(
            topBar = { TopAppBar(title = { Text("大文件扫描清理（/sdcard）") }) },
            bottomBar = {
                Surface(tonalElevation = 2.dp) {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("已选：${selected.size} 个，${humanSize(selectedSize)}")
                            Spacer(Modifier.weight(1f))
                            AssistChip(onClick = { selectAll(true) }, label = { Text("全选") })
                            Spacer(Modifier.width(8.dp))
                            AssistChip(onClick = { files.forEach { it.checkedState = !it.checkedState } }, label = { Text("反选") })
                            Spacer(Modifier.width(8.dp))
                            Button(
                                enabled = selected.isNotEmpty(),
                                onClick = { scope.launch { if (ensurePermissionOrGuide()) doDelete() } }
                            ) { Text("删除所选") }
                        }
                        if (scanning) {
                            LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 8.dp))
                            Text(
                                "扫描中… 已遍历 $scannedCount 个文件，发现 $foundCount 个大文件",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }
                }
            }
        ) { inner ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner)
                    .padding(12.dp)
            ) {
                // 目录输入与快捷目录
                OutlinedTextField(
                    value = dirText,
                    onValueChange = { dirText = it },
                    label = { Text("扫描目录路径") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                )
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    val chips = listOf(
                        "Root" to "/sdcard",
                        "Download" to (Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)?.absolutePath ?: "/sdcard/Download"),
                        "Movies" to (Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)?.absolutePath ?: "/sdcard/Movies"),
                        "Pictures" to (Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)?.absolutePath ?: "/sdcard/Pictures"),
                        "Music" to (Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)?.absolutePath ?: "/sdcard/Music"),
                        "DCIM" to (Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)?.absolutePath ?: "/sdcard/DCIM")
                    )
                    chips.forEach { (name, path) ->
                        AssistChip(
                            onClick = { dirText = TextFieldValue(path) },
                            label = { Text(name) },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // 阈值设置（MB）
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = minMbText,
                        onValueChange = { tv ->
                            val digits = tv.text.filter { it.isDigit() }
                            minMbText = TextFieldValue(digits.ifEmpty { "1" })
                        },
                        label = { Text("最小体积(MB)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        modifier = Modifier.width(160.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Button(onClick = { startScan() }, enabled = !scanning) { Text("开始扫描") }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = { cancelScanIfRunning() }, enabled = scanning) { Text("取消扫描") }
                    Spacer(Modifier.weight(1f))
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val granted = isAllFilesGranted()
                        AssistChip(
                            onClick = { openAllFilesGrant(activity) },
                            label = { Text(if (granted) "已获所有文件访问" else "授予所有文件访问") },
                            leadingIcon = { if (granted) Icon(Icons.Filled.Check, contentDescription = null) }
                        )
                    } else if (needsLegacyPerm) {
                        AssistChip(
                            onClick = {
                                permLauncher.launch(arrayOf(
                                    Manifest.permission.READ_EXTERNAL_STORAGE,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                                ))
                            },
                            label = { Text("申请读写权限") }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // 列表区域 —— 关键：weight(1f) + contentPadding 防止被底部栏遮挡
                if (files.isEmpty() && !scanning) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("没有找到符合条件的大文件")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(bottom = 88.dp)
                    ) {
                        itemsIndexed(files, key = { _, it -> it.path }) { index, item ->
                            FileRow(
                                item = item,
                                onChecked = { v -> item.checkedState = v },
                                onClick = { item.checkedState = !item.checkedState },
                                onLongClick = { Toast.makeText(activity, item.path, Toast.LENGTH_SHORT).show() }
                            )
                            if (index < files.lastIndex) Divider()
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileRow(
    item: FileItem,
    onChecked: (Boolean) -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = item.checkedState, onCheckedChange = onChecked)
        Column(Modifier.weight(1f)) {
            Text(item.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
            Text(item.path, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            Text("${humanSize(item.size)} · ${formatTime(item.lastModified)}", style = MaterialTheme.typography.bodySmall)
        }
    }
}
