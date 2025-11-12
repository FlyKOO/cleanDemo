package com.example.cleanup.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.StatFs
import android.os.Environment
import android.os.Process
import android.widget.Toast
import java.io.File

class AppManager(private val context: Context) {
    
    private val cleanupHistoryManager = CleanupHistoryManager(context)
    private val settingsManager = SettingsManager(context)
    
    private val packageManager = context.packageManager
    
    
    /**
     * 格式化字节大小
     */
    private fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 * 1024 -> {
                "%.1f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
            }
            bytes >= 1024 * 1024 -> {
                "%.1f MB".format(bytes / (1024.0 * 1024.0))
            }
            bytes >= 1024 -> {
                "%.1f KB".format(bytes / 1024.0)
            }
            else -> "$bytes B"
        }
    }
    
    /**
     * 测试血压监测应用的缓存
     */
    private fun testBloodPressureCache() {
        try {
            println("=== 测试血压监测应用缓存 ===")
            val packageInfo = packageManager.getPackageInfo("com.example.bpmonitor", 0)
            val appInfo = packageInfo.applicationInfo
            
            if (appInfo == null) {
                println("无法获取血压监测应用信息")
                return
            }
            
            println("血压监测应用数据目录: ${appInfo.dataDir}")
            
            val cacheDir = File(appInfo.dataDir, "cache")
            println("缓存目录路径: ${cacheDir.absolutePath}")
            println("缓存目录存在: ${cacheDir.exists()}")
            println("缓存目录可读: ${cacheDir.canRead()}")
            
            if (cacheDir.exists()) {
                val files = cacheDir.listFiles()
                println("缓存目录文件数量: ${files?.size ?: 0}")
                
                if (files != null) {
                    for (file in files) {
                        println("文件: ${file.name}, 大小: ${file.length()} bytes")
                    }
                }
                
                val totalSize = getFolderSize(cacheDir)
                println("缓存目录总大小: ${totalSize} bytes (${totalSize / 1024.0} KB)")
            }
            println("=== 测试完成 ===")
        } catch (e: Exception) {
            println("测试血压监测应用缓存异常: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * 获取所有已安装的应用信息
     */
    fun getAllInstalledApps(): List<AppInfo> {
        val apps = mutableListOf<AppInfo>()
        
        // 测试血压监测应用的缓存
        testBloodPressureCache()
        
        try {
            val installedPackages = packageManager.getInstalledPackages(0)
            
            for (packageInfo in installedPackages) {
                val appInfo = packageInfo.applicationInfo ?: continue
                
                // 跳过系统核心应用
                if (appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0 && 
                    appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP == 0) {
                    continue
                }
                
                val cacheSize = getAppCacheSize(appInfo)
                val dataSize = getAppDataSize(appInfo)
                val totalSize = cacheSize + dataSize
                
                // 调试信息：打印应用大小信息
                if (packageInfo.packageName.contains("wechat") || 
                    packageInfo.packageName.contains("douyin") ||
                    packageInfo.packageName.contains("alipay") ||
                    packageInfo.packageName.contains("blood") ||
                    packageInfo.packageName.contains("pressure") ||
                    packageInfo.packageName.contains("monitor") ||
                    packageInfo.packageName.contains("qrcode") ||
                    packageInfo.packageName.contains("scanning")) {
                    println("=== 应用大小调试信息 ===")
                    println("应用名称: ${getAppName(appInfo)}")
                    println("包名: ${packageInfo.packageName}")
                    println("缓存大小: ${formatBytes(cacheSize)}")
                    println("数据大小: ${formatBytes(dataSize)}")
                    println("总大小: ${formatBytes(totalSize)}")
                    println("========================")
                }
                
                val app = AppInfo(
                    packageName = packageInfo.packageName,
                    appName = getAppName(appInfo),
                    versionName = packageInfo.versionName ?: "未知",
                    cacheSize = cacheSize,
                    dataSize = dataSize,
                    isSystemApp = isSystemApp(appInfo),
                    isEnabled = appInfo.enabled,
                    lastUsed = getLastUsedTime(packageInfo.packageName)
                )
                
                apps.add(app)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return apps
    }
    
    /**
     * 获取应用名称
     */
    private fun getAppName(appInfo: ApplicationInfo): String {
        return try {
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            appInfo.packageName
        }
    }
    
    /**
     * 获取应用缓存大小（使用系统方法）
     */
    private fun getAppCacheSize(appInfo: ApplicationInfo): Long {
        return try {
            // 详细调试信息
            println("=== 开始检查缓存目录 ===")
            println("应用包名: ${appInfo.packageName}")
            println("应用数据目录: ${appInfo.dataDir}")
            
            var totalSize = 0L
            
            // 方法1: 检查外部存储缓存目录（通常有权限访问）
            val externalCacheDir = File(Environment.getExternalStorageDirectory(), 
                "Android/data/${appInfo.packageName}/cache")
            if (externalCacheDir.exists() && externalCacheDir.canRead()) {
                val size = getFolderSize(externalCacheDir)
                totalSize += size
                println("外部缓存目录: ${externalCacheDir.absolutePath}")
                println("外部缓存大小: ${size} bytes (${size / 1024.0} KB)")
            }
            
            // 方法2: 检查应用自己的缓存目录（如果是当前应用）
            if (appInfo.packageName == context.packageName) {
                val ownCacheDir = context.cacheDir
                if (ownCacheDir.exists() && ownCacheDir.canRead()) {
                    val size = getFolderSize(ownCacheDir)
                    totalSize += size
                    println("自身缓存目录: ${ownCacheDir.absolutePath}")
                    println("自身缓存大小: ${size} bytes (${size / 1024.0} KB)")
                }
            }
            
            // 方法3: 使用PackageManager获取应用大小信息
            try {
                val packageInfo = packageManager.getPackageInfo(appInfo.packageName, 0)
                // 这里可以获取更多应用信息，但缓存大小需要特殊权限
                println("应用版本: ${packageInfo.versionName}")
            } catch (e: Exception) {
                println("获取包信息失败: ${e.message}")
            }
            
            // 方法4: 尝试访问应用的数据目录中的缓存文件夹
            if (totalSize == 0L) {
                try {
                    val appDataDir = File(appInfo.dataDir)
                    if (appDataDir.exists() && appDataDir.canRead()) {
                        // 检查常见的缓存目录
                        val cacheDirs = listOf(
                            "cache",
                            "code_cache", 
                            "shared_prefs",
                            "files/cache",
                            "files/temp"
                        )
                        
                        for (cacheDirName in cacheDirs) {
                            val cacheDir = File(appDataDir, cacheDirName)
                            if (cacheDir.exists() && cacheDir.canRead()) {
                                val size = getFolderSize(cacheDir)
                                if (size > 0) {
                                    totalSize += size
                                    println("发现缓存目录: ${cacheDir.absolutePath}")
                                    println("缓存大小: ${size} bytes (${size / 1024.0} KB)")
                                }
                            }
                        }
                    } else {
                        println("应用数据目录不可访问: ${appDataDir.absolutePath}")
                    }
                } catch (e: Exception) {
                    println("检查应用数据目录异常: ${e.message}")
                }
            }
            
            // 如果仍然没有找到缓存，返回0（不返回模拟数据）
            if (totalSize == 0L) {
                println("未找到可访问的缓存目录，返回0")
            }
            
            println("总缓存大小: ${totalSize} bytes (${totalSize / 1024.0} KB)")
            println("=== 缓存检查完成 ===")
            
            totalSize
        } catch (e: Exception) {
            println("获取缓存大小异常: ${e.message}")
            e.printStackTrace()
            0L
        }
    }
    
    /**
     * 获取应用数据大小（只包含APK大小，不包含数据目录）
     */
    private fun getAppDataSize(appInfo: ApplicationInfo): Long {
        return try {
            // 只计算APK文件大小，不计算数据目录
            // 因为数据目录可能包含大量不应该计算的文件
            val apkSize = getApkSize(appInfo)
            
            // 只返回真实数据，如果无法获取APK大小就返回0
            apkSize
        } catch (e: Exception) {
            // 发生异常时返回0，不返回模拟数据
            if (appInfo.packageName.contains("blood") || 
                appInfo.packageName.contains("pressure") ||
                appInfo.packageName.contains("monitor") ||
                appInfo.packageName.contains("qrcode") ||
                appInfo.packageName.contains("scanning")) {
                println("获取APK大小异常: ${e.message}")
            }
            0L
        }
    }
    
    /**
     * 获取APK文件大小
     */
    private fun getApkSize(appInfo: ApplicationInfo): Long {
        return try {
            val apkFile = File(appInfo.sourceDir)
            if (apkFile.exists() && apkFile.canRead()) {
                val size = apkFile.length()
                // 调试信息
                if (appInfo.packageName.contains("blood") || 
                    appInfo.packageName.contains("pressure") ||
                    appInfo.packageName.contains("monitor")) {
                    println("APK文件路径: ${apkFile.absolutePath}")
                    println("APK文件大小: ${size / (1024 * 1024)} MB")
                }
                size
            } else {
                if (appInfo.packageName.contains("blood") || 
                    appInfo.packageName.contains("pressure") ||
                    appInfo.packageName.contains("monitor")) {
                    println("APK文件不存在或无法读取: ${apkFile.absolutePath}")
                }
                0L
            }
        } catch (e: Exception) {
            if (appInfo.packageName.contains("blood") || 
                appInfo.packageName.contains("pressure") ||
                appInfo.packageName.contains("monitor")) {
                println("获取APK大小异常: ${e.message}")
            }
            0L
        }
    }
    
    /**
     * 获取应用数据目录大小（排除缓存）
     */
    private fun getAppDataDirectorySize(appInfo: ApplicationInfo): Long {
        return try {
            val dataDir = File(appInfo.dataDir)
            if (dataDir.exists() && dataDir.canRead()) {
                // 排除缓存目录，避免重复计算
                getFolderSizeExcludingCache(dataDir)
            } else {
                0L
            }
        } catch (e: Exception) {
            0L
        }
    }
    
    /**
     * 计算文件夹大小，排除缓存目录
     */
    private fun getFolderSizeExcludingCache(folder: File): Long {
        var size = 0L
        
        if (folder.isDirectory) {
            val files = folder.listFiles()
            if (files != null) {
                for (file in files) {
                    // 跳过缓存目录
                    if (file.name == "cache") {
                        continue
                    }
                    
                    size += if (file.isDirectory) {
                        getFolderSizeExcludingCache(file)
                    } else {
                        file.length()
                    }
                }
            }
        } else {
            size = folder.length()
        }
        
        return size
    }
    
    /**
     * 判断是否为系统应用
     */
    private fun isSystemApp(appInfo: ApplicationInfo): Boolean {
        return (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
    }
    
    /**
     * 获取应用最后使用时间（模拟）
     */
    private fun getLastUsedTime(packageName: String): Long {
        // 这里可以集成UsageStatsManager来获取真实的使用时间
        // 目前返回模拟数据
        return System.currentTimeMillis() - (Math.random() * 7 * 24 * 60 * 60 * 1000).toLong()
    }
    
    /**
     * 计算文件夹大小
     */
    private fun getFolderSize(folder: File): Long {
        var size = 0L
        
        try {
            if (folder.isDirectory) {
                val files = folder.listFiles()
                if (files != null) {
                    for (file in files) {
                        try {
                            size += if (file.isDirectory) {
                                getFolderSize(file)
                            } else {
                                file.length()
                            }
                        } catch (e: Exception) {
                            // 忽略无法访问的文件
                            continue
                        }
                    }
                }
            } else {
                size = folder.length()
            }
        } catch (e: Exception) {
            // 如果无法访问文件夹，返回0
            return 0L
        }
        
        return size
    }
    
    /**
     * 清理应用缓存
     */
    fun clearAppCache(packageName: String): Boolean {
        // 检查安全模式
        if (settingsManager.isSafeModeEnabled()) {
            println("安全模式已启用，跳过应用缓存清理")
            return false
        }
        
        println("=== AppManager.clearAppCache 被调用 ===")
        println("包名: $packageName")
        val startTime = System.currentTimeMillis()
        return try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val appInfo = packageInfo.applicationInfo
            if (appInfo == null) {
                println("无法获取应用信息")
                return false
            }
            
            println("应用数据目录: ${appInfo.dataDir}")
            
            var success = false
            var totalCleared = 0L
            
            // 方法1: 清理外部存储缓存目录（通常有权限访问）
            val externalCacheDir = File(Environment.getExternalStorageDirectory(), 
                "Android/data/${appInfo.packageName}/cache")
            if (externalCacheDir.exists() && externalCacheDir.canRead() && externalCacheDir.canWrite()) {
                val sizeBefore = getFolderSize(externalCacheDir)
                println("外部缓存目录: ${externalCacheDir.absolutePath}")
                println("清理前大小: ${sizeBefore} bytes (${sizeBefore / 1024.0} KB)")
                
                val result = deleteFolder(externalCacheDir)
                if (result) {
                    totalCleared += sizeBefore
                    success = true
                    println("成功清理外部缓存: ${sizeBefore} bytes")
                } else {
                    println("清理外部缓存失败")
                }
            } else {
                println("外部缓存目录不可访问: ${externalCacheDir.absolutePath}")
            }
            
            // 方法2: 清理应用自己的缓存目录（如果是当前应用）
            if (appInfo.packageName == context.packageName) {
                val ownCacheDir = context.cacheDir
                if (ownCacheDir.exists() && ownCacheDir.canRead() && ownCacheDir.canWrite()) {
                    val sizeBefore = getFolderSize(ownCacheDir)
                    println("自身缓存目录: ${ownCacheDir.absolutePath}")
                    println("清理前大小: ${sizeBefore} bytes (${sizeBefore / 1024.0} KB)")
                    
                    val result = deleteFolder(ownCacheDir)
                    if (result) {
                        totalCleared += sizeBefore
                        success = true
                        println("成功清理自身缓存: ${sizeBefore} bytes")
                    } else {
                        println("清理自身缓存失败")
                    }
                } else {
                    println("自身缓存目录不可访问: ${ownCacheDir.absolutePath}")
                }
            }
            
            // 方法3: 尝试清理应用数据目录中的缓存文件夹
            if (!success) {
                try {
                    val appDataDir = File(appInfo.dataDir)
                    if (appDataDir.exists() && appDataDir.canRead() && appDataDir.canWrite()) {
                        // 检查常见的缓存目录
                        val cacheDirs = listOf(
                            "cache",
                            "code_cache", 
                            "shared_prefs",
                            "files/cache",
                            "files/temp"
                        )
                        
                        for (cacheDirName in cacheDirs) {
                            val cacheDir = File(appDataDir, cacheDirName)
                            if (cacheDir.exists() && cacheDir.canRead() && cacheDir.canWrite()) {
                                val sizeBefore = getFolderSize(cacheDir)
                                if (sizeBefore > 0) {
                                    println("发现可清理的缓存目录: ${cacheDir.absolutePath}")
                                    println("清理前大小: ${sizeBefore} bytes (${sizeBefore / 1024.0} KB)")
                                    
                                    val result = deleteFolder(cacheDir)
                                    if (result) {
                                        totalCleared += sizeBefore
                                        success = true
                                        println("成功清理缓存目录: ${sizeBefore} bytes")
                                    } else {
                                        println("清理缓存目录失败: ${cacheDir.absolutePath}")
                                    }
                                }
                            }
                        }
                    } else {
                        println("应用数据目录不可访问: ${appDataDir.absolutePath}")
                    }
                } catch (e: Exception) {
                    println("清理应用数据目录异常: ${e.message}")
                }
            }
            
            // 如果仍然无法清理，返回失败（不模拟成功）
            if (!success) {
                println("无法清理应用缓存，可能没有权限或缓存目录不存在")
                return false
            }
            
            if (success) {
                println("清理完成，总共释放: ${totalCleared} bytes (${totalCleared / 1024.0} KB)")
                
                // 记录清理历史
                val duration = System.currentTimeMillis() - startTime
                val appName = try {
                    packageManager.getApplicationLabel(appInfo).toString()
                } catch (e: Exception) {
                    packageName
                }
                
                val record = cleanupHistoryManager.createCleanupRecord(
                    freedSpace = totalCleared,
                    duration = duration,
                    items = listOf(
                        CleanupItem("$appName 缓存", totalCleared, 1)
                    ),
                    type = CleanupHistoryType.MANUAL
                )
                cleanupHistoryManager.addCleanupRecord(record)
            } else {
                println("清理失败")
            }
            
            success
        } catch (e: Exception) {
            println("清理缓存时发生异常: ${e.message}")
            e.printStackTrace()
            false
        }
    }
    
    /**
     * 卸载应用
     */
//    fun uninstallApp(packageName: String): Boolean {
//        println("=== AppManager.uninstallApp 被调用 ===")
//        println("包名: $packageName")
//        return try {
//            // 启动系统卸载界面
//            println("启动系统卸载界面...")
//
//            val intent = android.content.Intent(android.content.Intent.ACTION_DELETE)
//            intent.data = android.net.Uri.parse("package:$packageName")
//            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
//
//            try {
//                context.startActivity(intent)
//                println("系统卸载界面已启动")
//                true
//            } catch (e: Exception) {
//                println("启动卸载界面失败: ${e.message}")
//                // 如果无法启动系统界面，返回true表示操作已尝试
//                true
//            }
//        } catch (e: Exception) {
//            println("卸载应用时发生异常: ${e.message}")
//            e.printStackTrace()
//            false
//        }
//    }

    fun uninstallApp(packageName: String) {
        try {
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.parse("package:$packageName")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            // 处理异常，比如没有找到卸载界面
            Toast.makeText(context, "err:", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 删除文件夹
     */
    private fun deleteFolder(folder: File): Boolean {
        return try {
            if (folder.isDirectory) {
                val files = folder.listFiles()
                if (files != null) {
                    for (file in files) {
                        if (file.isDirectory) {
                            deleteFolder(file)
                        } else {
                            file.delete()
                        }
                    }
                }
            }
            folder.delete()
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * 应用信息数据类
 */
data class AppInfo(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val cacheSize: Long,
    val dataSize: Long,
    val isSystemApp: Boolean,
    val isEnabled: Boolean,
    val lastUsed: Long
) {
    val totalSize: Long
        get() = cacheSize + dataSize
}
