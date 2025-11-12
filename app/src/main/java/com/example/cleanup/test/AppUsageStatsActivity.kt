package com.example.cleanup.test

import android.app.AppOpsManager
import android.app.usage.StorageStats
import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.os.UserHandle
import android.os.storage.StorageManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DecimalFormat
import java.util.UUID

class AppUsageStatsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AppUsageScreen() }
    }
}

data class AppRow(
    val label: String,
    val packageName: String,
    val isSystem: Boolean,
    val appBytes: Long?,      // 代码+资源（可能为 null 表示无权限/统计失败）
    val dataBytes: Long?,     // 数据（含缓存）
    val cacheBytes: Long?,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppUsageScreen() {
    val ctx = LocalContext.current
    val vm = remember { AppsVm(ctx) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { vm.load(showSystem = false) }

    MaterialTheme(colorScheme = darkColorScheme()) {
        Scaffold(topBar = {
            TopAppBar(title = { Text("应用占用统计", fontWeight = FontWeight.Bold) })
        }) { padding ->
            Column(Modifier.padding(padding).padding(12.dp).fillMaxSize()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilterChip(
                        selected = !vm.showSystem,
                        onClick = { scope.launch { vm.load(showSystem = false) } },
                        label = { Text("仅用户应用") }
                    )
                    FilterChip(
                        selected = vm.showSystem,
                        onClick = { scope.launch { vm.load(showSystem = true) } },
                        label = { Text("包含系统应用") }
                    )
                    Spacer(Modifier.weight(1f))
                    if (!vm.usageGranted) {
                        OutlinedButton(onClick = { ctx.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }) {
                            Text("授予使用情况访问")
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // 汇总统计
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("已安装应用：${vm.visibleCount} 个")
                        Text("总占用：${fmtSize(vm.totalAppBytes + vm.totalDataBytes)} (代码≈${fmtSize(vm.totalAppBytes)}, 数据≈${fmtSize(vm.totalDataBytes)})")
                        Text("总缓存：${fmtSize(vm.totalCacheBytes)}")
                        if (!vm.usageGranted) Text("* 未授予使用情况访问，仅能显示基础信息与卸载/打开设置清理缓存", style = MaterialTheme.typography.bodySmall)
                    }
                }

                Spacer(Modifier.height(8.dp))

                if (vm.loading) LinearProgressIndicator(Modifier.fillMaxWidth())

                LazyColumn(Modifier.fillMaxSize()) {
                    items(vm.items, key = { it.packageName }) { app ->
                        AppRowCard(app = app, onUninstall = {
                            val i = Intent(Intent.ACTION_DELETE, Uri.parse("package:" + app.packageName))
                            ctx.startActivity(i)
                        }, onClearCache = {
                            // 公开 SDK 无法直接清理其他 App 缓存：跳转到“应用信息”页由用户执行
                            val i = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + app.packageName))
                            ctx.startActivity(i)
                        })
                    }
                }
            }
        }
    }
}

@Composable
private fun AppRowCard(app: AppRow, onUninstall: () -> Unit, onClearCache: () -> Unit) {
    val pm = LocalContext.current.packageManager
    val icon = remember(app.packageName) {
        try { pm.getApplicationIcon(app.packageName).toBitmap(96,96).asImageBitmap() } catch (_: Exception) { null }
    }

    ElevatedCard(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(40.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(app.label, fontWeight = FontWeight.SemiBold)
                Text(app.packageName, style = MaterialTheme.typography.bodySmall)
                val code = app.appBytes ?: -1
                val data = app.dataBytes ?: -1
                val cache = app.cacheBytes ?: -1
                Text("占用：${fmtSizeSafe(code + data)}   |   缓存：${fmtSizeSafe(cache)}")
                if (app.isSystem) Text("系统应用", style = MaterialTheme.typography.labelSmall)
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), horizontalAlignment = Alignment.End) {
                OutlinedButton(onClick = onClearCache) { Text("清理缓存") }
                Button(onClick = onUninstall, enabled = !app.isSystem) { Text("卸载") }
            }
        }
    }
}

private fun fmtSizeSafe(bytes: Long): String = if (bytes < 0) "—" else fmtSize(bytes)
private fun fmtSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B","KB","MB","GB","TB")
    val group = (kotlin.math.log10(bytes.toDouble()) / kotlin.math.log10(1024.0)).toInt().coerceIn(0, units.size-1)
    val v = bytes / Math.pow(1024.0, group.toDouble())
    return String.format("%.2f %s", v, units[group])
}

class AppsVm(private val ctx: Context) {
    var items by mutableStateOf(listOf<AppRow>())
        private set
    var showSystem by mutableStateOf(false)
        private set
    var loading by mutableStateOf(false)
        private set

    var usageGranted by mutableStateOf(false)
        private set

    var totalAppBytes by mutableStateOf(0L)
        private set
    var totalDataBytes by mutableStateOf(0L)
        private set
    var totalCacheBytes by mutableStateOf(0L)
        private set

    val visibleCount: Int get() = items.size

    suspend fun load(showSystem: Boolean) {
        this.showSystem = showSystem
        loading = true
        items = emptyList()
        totalAppBytes = 0
        totalDataBytes = 0
        totalCacheBytes = 0

        val pm = ctx.packageManager
        val list = withContext(Dispatchers.IO) {
            val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val filtered = if (showSystem) apps else apps.filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
            usageGranted = hasUsageAccess(ctx)
            val ssm = ctx.getSystemService(StorageStatsManager::class.java)
            val user: UserHandle = Process.myUserHandle()
            filtered.map { ai ->
                val (appB, dataB, cacheB) = if (Build.VERSION.SDK_INT >= 26 && usageGranted) {
                    try {
                        val uuid: UUID = ai.storageUuid ?: StorageManager.UUID_DEFAULT
                        val stats: StorageStats = ssm.queryStatsForPackage(uuid, ai.packageName, user)
                        Triple(stats.appBytes, stats.dataBytes, stats.cacheBytes)
                    } catch (e: Exception) {
                        Triple(null, null, null)
                    }
                } else Triple(null, null, null)

                AppRow(
                    label = ai.loadLabel(pm)?.toString() ?: ai.packageName,
                    packageName = ai.packageName,
                    isSystem = (ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                    appBytes = appB,
                    dataBytes = dataB,
                    cacheBytes = cacheB
                )
            }.sortedBy { it.label.lowercase() }
        }

        items = list

        // 汇总（仅统计非 null 的）
        totalAppBytes = list.mapNotNull { it.appBytes }.sum()
        totalDataBytes = list.mapNotNull { it.dataBytes }.sum()
        totalCacheBytes = list.mapNotNull { it.cacheBytes }.sum()

        loading = false
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
private fun hasUsageAccess(ctx: Context): Boolean {
    return try {
        val aom = ctx.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = aom.unsafeCheckOpNoThrow("android:get_usage_stats", Process.myUid(), ctx.packageName)
        mode == AppOpsManager.MODE_ALLOWED
    } catch (_: Exception) { false }
}
