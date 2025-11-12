package com.example.cleanup.data

import android.content.Context
import android.os.StatFs
import java.io.File
import kotlin.math.roundToInt
import com.example.cleanup.R

class StorageManager(private val context: Context) {
    
    private val cleanupHistoryManager = CleanupHistoryManager(context)
    private val notificationManager = NotificationManager(context)
    private val settingsManager = SettingsManager(context)
    
    /**
     * 获取存储空间信息
     */
    fun getStorageInfo(): StorageInfo {
        val internalStorage = getInternalStorageInfo()
        val externalStorage = getExternalStorageInfo()
        
        return StorageInfo(
            totalSpace = internalStorage.totalSpace + externalStorage.totalSpace,
            usedSpace = internalStorage.usedSpace + externalStorage.usedSpace,
            availableSpace = internalStorage.availableSpace + externalStorage.availableSpace
        )
    }
    
    /**
     * 获取内部存储信息
     */
    private fun getInternalStorageInfo(): StorageInfo {
        val stat = StatFs(context.filesDir.absolutePath)
        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.blockCountLong
        val availableBlocks = stat.availableBlocksLong
        
        val totalSpace = totalBlocks * blockSize
        val availableSpace = availableBlocks * blockSize
        val usedSpace = totalSpace - availableSpace
        
        return StorageInfo(
            totalSpace = totalSpace,
            usedSpace = usedSpace,
            availableSpace = availableSpace
        )
    }
    
    /**
     * 获取外部存储信息
     */
    private fun getExternalStorageInfo(): StorageInfo {
        val externalFilesDir = context.getExternalFilesDir(null)
        return if (externalFilesDir != null) {
            val stat = StatFs(externalFilesDir.absolutePath)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availableBlocks = stat.availableBlocksLong
            
            val totalSpace = totalBlocks * blockSize
            val availableSpace = availableBlocks * blockSize
            val usedSpace = totalSpace - availableSpace
            
            StorageInfo(
                totalSpace = totalSpace,
                usedSpace = usedSpace,
                availableSpace = availableSpace
            )
        } else {
            StorageInfo(0, 0, 0)
        }
    }
    
    /**
     * 获取缓存大小
     */
    fun getCacheSize(): Long {
        return try {
            val cacheDir = context.cacheDir
            getFolderSize(cacheDir)
        } catch (e: Exception) {
            0L
        }
    }
    
    /**
     * 获取垃圾文件大小
     */
    fun getJunkSize(): Long {
        var totalSize = 0L
        
        try {
            // 临时文件
            val tempDir = File(context.cacheDir, "temp")
            if (tempDir.exists()) {
                totalSize += getFolderSize(tempDir)
            }
            
            // 日志文件
            val logDir = File(context.filesDir, "logs")
            if (logDir.exists()) {
                totalSize += getFolderSize(logDir)
            }
            
            // 下载缓存
            val downloadCacheDir = File(context.cacheDir, "download")
            if (downloadCacheDir.exists()) {
                totalSize += getFolderSize(downloadCacheDir)
            }
            
            // 检查其他垃圾文件
            val filesDir = context.filesDir
            if (filesDir.exists() && filesDir.isDirectory) {
                val files = filesDir.listFiles()
                if (files != null) {
                    for (file in files) {
                        if (file.name.startsWith("temp_") || 
                            file.name.endsWith(".log") || 
                            file.name.endsWith(".tmp")) {
                            totalSize += file.length()
                        }
                    }
                }
            }
            
        } catch (e: Exception) {
            // 忽略错误，返回0
        }
        
        return totalSize
    }
    
    /**
     * 创建测试垃圾文件（用于演示清理功能）
     */
    fun createTestJunkFiles(): Boolean {
        return try {
            // 创建临时目录
            val tempDir = File(context.cacheDir, "temp")
            if (!tempDir.exists()) {
                tempDir.mkdirs()
            }
            
            // 创建一些测试文件
            for (i in 1..5) {
                val testFile = File(tempDir, "test_temp_$i.txt")
                testFile.writeText("This is a test temporary file $i\n".repeat(1000)) // 约5KB
            }
            
            // 创建日志文件
            val logDir = File(context.filesDir, "logs")
            if (!logDir.exists()) {
                logDir.mkdirs()
            }
            
            val logFile = File(logDir, "app.log")
            logFile.writeText("Application log content\n".repeat(2000)) // 约40KB
            
            // 创建其他垃圾文件
            for (i in 1..3) {
                val junkFile = File(context.filesDir, "temp_junk_$i.tmp")
                junkFile.writeText("Junk file content $i\n".repeat(500)) // 约10KB
            }
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * 获取重复文件大小（真实数据）
     */
    fun getDuplicateSize(): Long {
        return try {
            var totalDuplicateSize = 0L
            
            // 扫描常见目录中的重复文件
            val directoriesToScan = listOf(
                context.getExternalFilesDir(null),
                context.filesDir,
                context.cacheDir,
                File(context.getExternalFilesDir(null), "Downloads"),
                File(context.getExternalFilesDir(null), "Pictures"),
                File(context.getExternalFilesDir(null), "Documents")
            )
            
            for (dir in directoriesToScan) {
                if (dir != null && dir.exists() && dir.isDirectory) {
                    totalDuplicateSize += findDuplicateFilesInDirectory(dir)
                }
            }
            
            totalDuplicateSize
        } catch (e: Exception) {
            println("获取重复文件大小异常: ${e.message}")
            0L
        }
    }
    
    /**
     * 在目录中查找重复文件
     */
    private fun findDuplicateFilesInDirectory(directory: File): Long {
        val fileMap = mutableMapOf<String, MutableList<File>>()
        var duplicateSize = 0L
        
        try {
            // 递归扫描目录中的所有文件
            scanDirectoryForFiles(directory, fileMap)
            
            // 查找重复文件
            for ((hash, files) in fileMap) {
                if (files.size > 1) {
                    // 保留第一个文件，计算其他重复文件的大小
                    for (i in 1 until files.size) {
                        duplicateSize += files[i].length()
                    }
                }
            }
        } catch (e: Exception) {
            println("扫描目录重复文件异常: ${e.message}")
        }
        
        return duplicateSize
    }
    
    /**
     * 递归扫描目录中的文件
     */
    private fun scanDirectoryForFiles(directory: File, fileMap: MutableMap<String, MutableList<File>>) {
        if (!directory.exists() || !directory.isDirectory) return
        
        try {
            val files = directory.listFiles()
            if (files != null) {
                for (file in files) {
                    if (file.isDirectory) {
                        // 递归扫描子目录
                        scanDirectoryForFiles(file, fileMap)
                    } else {
                        // 使用文件名和大小作为重复检测的键
                        val key = "${file.name}_${file.length()}"
                        if (!fileMap.containsKey(key)) {
                            fileMap[key] = mutableListOf()
                        }
                        fileMap[key]?.add(file)
                    }
                }
            }
        } catch (e: Exception) {
            println("扫描目录异常: ${e.message}")
        }
    }
    
    /**
     * 计算文件夹大小
     */
    private fun getFolderSize(folder: File): Long {
        var size = 0L
        
        if (folder.isDirectory) {
            val files = folder.listFiles()
            if (files != null) {
                for (file in files) {
                    size += if (file.isDirectory) {
                        getFolderSize(file)
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
     * 清理缓存
     */
    fun clearCache(): Boolean {
        // 检查安全模式
        if (settingsManager.isSafeModeEnabled()) {
            println("安全模式已启用，跳过缓存清理")
            return false
        }
        
        val startTime = System.currentTimeMillis()
        var freedSpace = 0L
        
        return try {
            val cacheDir = context.cacheDir
            if (cacheDir.exists() && cacheDir.isDirectory) {
                val files = cacheDir.listFiles()
                if (files != null) {
                    for (file in files) {
                        freedSpace += getFolderSize(file)
                        if (file.isDirectory) {
                            deleteFolder(file)
                        } else {
                            file.delete()
                        }
                    }
                }
            }
            
            // 记录清理历史
            val duration = System.currentTimeMillis() - startTime
            val record = cleanupHistoryManager.createCleanupRecord(
                freedSpace = freedSpace,
                duration = duration,
                    items = listOf(
                        CleanupItem(context.getString(R.string.app_cache), freedSpace, 1)
                    ),
                type = CleanupHistoryType.MANUAL
            )
            cleanupHistoryManager.addCleanupRecord(record)
            
            // 发送清理完成通知
            if (settingsManager.isNotificationsEnabled() && freedSpace > 0) {
                notificationManager.sendCleanupCompleteNotification(freedSpace, context.getString(R.string.app_cache))
            }
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * 清理垃圾文件
     */
    fun clearJunk(): Boolean {
        // 检查安全模式
        if (settingsManager.isSafeModeEnabled()) {
            println("安全模式已启用，跳过垃圾文件清理")
            return false
        }
        
        val startTime = System.currentTimeMillis()
        var freedSpace = 0L
        var success = true
        
        try {
            // 清理临时文件
            val tempDir = File(context.cacheDir, "temp")
            if (tempDir.exists()) {
                freedSpace += getFolderSize(tempDir)
                success = deleteFolder(tempDir) && success
            }
            
            // 清理日志文件
            val logDir = File(context.filesDir, "logs")
            if (logDir.exists()) {
                freedSpace += getFolderSize(logDir)
                success = deleteFolder(logDir) && success
            }
            
            // 清理下载缓存
            val downloadCacheDir = File(context.cacheDir, "download")
            if (downloadCacheDir.exists()) {
                freedSpace += getFolderSize(downloadCacheDir)
                success = deleteFolder(downloadCacheDir) && success
            }
            
            // 清理其他可能的垃圾文件
            val filesDir = context.filesDir
            if (filesDir.exists() && filesDir.isDirectory) {
                val files = filesDir.listFiles()
                if (files != null) {
                    for (file in files) {
                        // 清理临时文件和日志文件
                        if (file.name.startsWith("temp_") || 
                            file.name.endsWith(".log") || 
                            file.name.endsWith(".tmp")) {
                            freedSpace += file.length()
                            file.delete()
                        }
                    }
                }
            }
            
            // 记录清理历史
            if (success && freedSpace > 0) {
                val duration = System.currentTimeMillis() - startTime
                val record = cleanupHistoryManager.createCleanupRecord(
                    freedSpace = freedSpace,
                    duration = duration,
                    items = listOf(
                        CleanupItem(context.getString(R.string.junk_files), freedSpace, 1)
                    ),
                    type = CleanupHistoryType.MANUAL
                )
                cleanupHistoryManager.addCleanupRecord(record)
                
                // 发送清理完成通知
                if (settingsManager.isNotificationsEnabled()) {
                    notificationManager.sendCleanupCompleteNotification(freedSpace, context.getString(R.string.junk_files))
                }
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
            success = false
        }
        
        return success
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
 * 存储空间信息数据类
 */
data class StorageInfo(
    val totalSpace: Long,      // 总空间（字节）
    val usedSpace: Long,       // 已用空间（字节）
    val availableSpace: Long   // 可用空间（字节）
) {
    /**
     * 获取使用率百分比
     */
    fun getUsagePercentage(): Float {
        return if (totalSpace > 0) {
            (usedSpace.toFloat() / totalSpace.toFloat()) * 100f
        } else {
            0f
        }
    }
}
