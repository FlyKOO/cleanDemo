package com.example.cleanup.data

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class LargeFileInfo(
    val name: String,
    val path: String,
    val size: Long,
    val type: FileType,
    val lastModified: Long,
    val isSelected: Boolean = false
)

enum class FileType {
    VIDEO, IMAGE, AUDIO, DOCUMENT, ARCHIVE, OTHER
}

class LargeFileManager(private val context: Context) {
    
    private val settingsManager = SettingsManager(context)
    
    // 最小文件大小阈值（100MB）
    private val minFileSize = 5L * 1024 * 1024
    
    // 支持的文件扩展名
    private val videoExtensions = setOf("mp4", "avi", "mkv", "mov", "wmv", "flv", "webm", "m4v", "3gp")
    private val imageExtensions = setOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "tiff", "svg")
    private val audioExtensions = setOf("mp3", "wav", "flac", "aac", "ogg", "wma", "m4a")
    private val documentExtensions = setOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "rtf")
    private val archiveExtensions = setOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz")
    
    /**
     * 扫描大文件
     */
    fun scanLargeFiles(): List<LargeFileInfo> {
        val largeFiles = mutableListOf<LargeFileInfo>()
        
        try {
            // 扫描外部存储
            val externalStorage = Environment.getExternalStorageDirectory()
            if (externalStorage.exists() && externalStorage.canRead()) {
                scanDirectory(externalStorage, largeFiles)
            }
            
            // 扫描下载目录
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (downloadDir.exists() && downloadDir.canRead()) {
                scanDirectory(downloadDir, largeFiles)
            }
            
            // 扫描图片目录
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            if (picturesDir.exists() && picturesDir.canRead()) {
                scanDirectory(picturesDir, largeFiles)
            }
            
            // 扫描视频目录
            val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
            if (moviesDir.exists() && moviesDir.canRead()) {
                scanDirectory(moviesDir, largeFiles)
            }
            
            // 扫描音乐目录
            val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
            if (musicDir.exists() && musicDir.canRead()) {
                scanDirectory(musicDir, largeFiles)
            }
            
            // 扫描文档目录
            val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            if (documentsDir.exists() && documentsDir.canRead()) {
                scanDirectory(documentsDir, largeFiles)
            }
            
        } catch (e: Exception) {
            println("扫描大文件时发生异常: ${e.message}")
            e.printStackTrace()
        }
        
        return largeFiles.sortedByDescending { it.size }
    }
    
    /**
     * 递归扫描目录
     */
    private fun scanDirectory(directory: File, largeFiles: MutableList<LargeFileInfo>) {
        try {
            val files = directory.listFiles()
            if (files != null) {
                for (file in files) {
                    if (file.isDirectory) {
                        // 跳过系统目录和隐藏目录
                        if (!file.name.startsWith(".") && 
                            !file.name.equals("Android", ignoreCase = true) &&
                            !file.name.equals("DCIM", ignoreCase = true)) {
                            scanDirectory(file, largeFiles)
                        }
                    } else {
                        // 检查文件大小
                        if (file.length() >= minFileSize) {
                            if(largeFiles.find { it.path == file.path } != null){
                                Log.e("scanDirectory","文件重复" + file.path)
                                continue
                            }
                            val fileType = getFileType(file.name)
                            largeFiles.add(
                                LargeFileInfo(
                                    name = file.name,
                                    path = file.absolutePath,
                                    size = file.length(),
                                    type = fileType,
                                    lastModified = file.lastModified()
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println("扫描目录 ${directory.absolutePath} 时发生异常: ${e.message}")
        }
    }
    
    /**
     * 根据文件扩展名确定文件类型
     */
    private fun getFileType(fileName: String): FileType {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        
        return when {
            videoExtensions.contains(extension) -> FileType.VIDEO
            imageExtensions.contains(extension) -> FileType.IMAGE
            audioExtensions.contains(extension) -> FileType.AUDIO
            documentExtensions.contains(extension) -> FileType.DOCUMENT
            archiveExtensions.contains(extension) -> FileType.ARCHIVE
            else -> FileType.OTHER
        }
    }
    
    /**
     * 删除文件
     */
    fun deleteFile(filePath: String): Boolean {
        // 检查安全模式
        if (settingsManager.isSafeModeEnabled()) {
            println("安全模式已启用，跳过文件删除")
            return false
        }
        
        return try {
            val file = File(filePath)
            if (file.exists() && file.canWrite()) {
                val result = file.delete()
                println("删除文件 ${filePath}: ${if (result) "成功" else "失败"}")
                result
            } else {
                println("文件不存在或无写权限: ${filePath}")
                false
            }
        } catch (e: Exception) {
            println("删除文件时发生异常: ${e.message}")
            e.printStackTrace()
            false
        }
    }
    
    /**
     * 批量删除文件
     */
    fun deleteFiles(filePaths: List<String>): List<String> {
        // 检查安全模式
        if (settingsManager.isSafeModeEnabled()) {
            println("安全模式已启用，跳过批量文件删除")
            return filePaths // 返回所有文件路径，表示都删除失败
        }
        
        val deletedFiles = mutableListOf<String>()
        val failedFiles = mutableListOf<String>()
        
        for (filePath in filePaths) {
            if (deleteFile(filePath)) {
                deletedFiles.add(filePath)
            } else {
                failedFiles.add(filePath)
            }
        }
        
        println("批量删除结果: 成功 ${deletedFiles.size} 个，失败 ${failedFiles.size} 个")
        return deletedFiles
    }
    
    /**
     * 格式化字节大小
     */
    fun formatBytes(bytes: Long): String {
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
     * 格式化日期
     */
    fun formatDate(timestamp: Long): String {
        val date = Date(timestamp)
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return formatter.format(date)
    }
}

