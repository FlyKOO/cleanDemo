package com.example.cleanup.data

import android.content.Context
import android.content.SharedPreferences
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import com.example.cleanup.R
import com.example.cleanup.work.AutoCleanupWorker

class AutoCleanupManager(private val context: Context) {
    
    private val settingsManager = SettingsManager(context)
    private val storageManager = StorageManager(context)
    private val cleanupHistoryManager = CleanupHistoryManager(context)
    private val prefs: SharedPreferences = context.getSharedPreferences("auto_cleanup", Context.MODE_PRIVATE)
    
    // 设置键名
    private val KEY_LAST_CLEANUP = "last_cleanup_time"
    private val KEY_CLEANUP_FREQUENCY = "cleanup_frequency"
    private val KEY_STORAGE_THRESHOLD = "storage_threshold"
    private val KEY_CLEANUP_TYPES = "cleanup_types"
    
    // 默认值
    private val DEFAULT_CLEANUP_FREQUENCY = 24L // 24小时
    private val DEFAULT_STORAGE_THRESHOLD = 80 // 80%
    private val DEFAULT_CLEANUP_TYPES = setOf("cache", "junk") // 默认清理缓存和垃圾文件
    
    // 清理频率选项
    enum class CleanupFrequency(val hours: Long, val displayNameKey: String) {
        DAILY(24, "daily"),
        WEEKLY(168, "weekly"),
        MONTHLY(720, "monthly"),
        MANUAL(0, "manual")
    }
    
    // 清理类型
    enum class CleanupType(val displayName: String) {
        CACHE("app_cache"),
        JUNK("junk_files"),
        TEMP("temp_files"),
        LOGS("log_files")
    }
    
    /**
     * 检查是否应该执行自动清理
     */
    suspend fun shouldPerformAutoCleanup(): Boolean {
        return withContext(Dispatchers.IO) {
            // 检查自动清理是否启用
            if (!settingsManager.isAutoCleanupEnabled()) {
                println("自动清理未启用")
                return@withContext false
            }
            
            // 检查安全模式
            if (settingsManager.isSafeModeEnabled()) {
                println("安全模式已启用，跳过自动清理")
                return@withContext false
            }
            
            // 检查存储空间阈值
            if (!isStorageThresholdReached()) {
                println("存储空间未达到阈值")
                return@withContext false
            }
            
            // 检查清理频率
            if (!isCleanupTimeReached()) {
                println("未到清理时间")
                return@withContext false
            }
            
            println("满足自动清理条件")
            true
        }
    }
    
    /**
     * 执行自动清理
     */
    suspend fun performAutoCleanup(): AutoCleanupResult {
        return withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            var totalFreedSpace = 0L
            val cleanupItems = mutableListOf<CleanupItem>()
            var success = true
            
            try {
                println("开始执行自动清理...")
                
                val cleanupTypes = getEnabledCleanupTypes()
                
                // 清理应用缓存
                if (cleanupTypes.contains("cache")) {
                    val cacheSize = storageManager.getCacheSize()
                    if (cacheSize > 0) {
                        if (storageManager.clearCache()) {
                            totalFreedSpace += cacheSize
                            cleanupItems.add(CleanupItem(context.getString(R.string.app_cache), cacheSize, 1))
                            println("清理应用缓存成功: ${formatBytes(cacheSize)}")
                        } else {
                            success = false
                            println("清理应用缓存失败")
                        }
                    }
                }
                
                // 清理垃圾文件
                if (cleanupTypes.contains("junk")) {
                    val junkSize = storageManager.getJunkSize()
                    if (junkSize > 0) {
                        if (storageManager.clearJunk()) {
                            totalFreedSpace += junkSize
                            cleanupItems.add(CleanupItem(context.getString(R.string.junk_files), junkSize, 1))
                            println("清理垃圾文件成功: ${formatBytes(junkSize)}")
                        } else {
                            success = false
                            println("清理垃圾文件失败")
                        }
                    }
                }
                
                // 清理临时文件
                if (cleanupTypes.contains("temp")) {
                    val tempSize = getTempFilesSize()
                    if (tempSize > 0) {
                        if (clearTempFiles()) {
                            totalFreedSpace += tempSize
                            cleanupItems.add(CleanupItem(context.getString(R.string.temp_files), tempSize, 1))
                            println("清理临时文件成功: ${formatBytes(tempSize)}")
                        } else {
                            success = false
                            println("清理临时文件失败")
                        }
                    }
                }
                
                // 清理日志文件
                if (cleanupTypes.contains("logs")) {
                    val logsSize = getLogsSize()
                    if (logsSize > 0) {
                        if (clearLogs()) {
                            totalFreedSpace += logsSize
                            cleanupItems.add(CleanupItem(context.getString(R.string.log_files), logsSize, 1))
                            println("清理日志文件成功: ${formatBytes(logsSize)}")
                        } else {
                            success = false
                            println("清理日志文件失败")
                        }
                    }
                }
                
                val duration = System.currentTimeMillis() - startTime
                
                // 记录清理历史
                if (success && totalFreedSpace > 0) {
                    val record = cleanupHistoryManager.createCleanupRecord(
                        freedSpace = totalFreedSpace,
                        duration = duration,
                        items = cleanupItems,
                        type = CleanupHistoryType.AUTO
                    )
                    cleanupHistoryManager.addCleanupRecord(record)
                }
                
                // 更新最后清理时间
                updateLastCleanupTime()
                
                println("自动清理完成，释放空间: ${formatBytes(totalFreedSpace)}")
                
                AutoCleanupResult(
                    success = success,
                    freedSpace = totalFreedSpace,
                    duration = duration,
                    items = cleanupItems
                )
                
            } catch (e: Exception) {
                println("自动清理异常: ${e.message}")
                e.printStackTrace()
                AutoCleanupResult(
                    success = false,
                    freedSpace = 0L,
                    duration = System.currentTimeMillis() - startTime,
                    items = emptyList(),
                    error = e.message
                )
            }
        }
    }
    
    /**
     * 检查存储空间是否达到阈值
     */
    private fun isStorageThresholdReached(): Boolean {
        val storageInfo = storageManager.getStorageInfo()
        val usedPercentage = (storageInfo.usedSpace.toFloat() / storageInfo.totalSpace.toFloat() * 100).toInt()
        val threshold = getStorageThreshold()
        
        println("存储使用率: $usedPercentage%, 阈值: $threshold%")
        return usedPercentage >= threshold
    }
    
    /**
     * 检查是否到了清理时间
     */
    private fun isCleanupTimeReached(): Boolean {
        val lastCleanupTime = prefs.getLong(KEY_LAST_CLEANUP, 0L)
        val frequency = getCleanupFrequency()
        
        if (frequency == CleanupFrequency.MANUAL) {
            return false
        }
        
        val currentTime = System.currentTimeMillis()
        val timeSinceLastCleanup = currentTime - lastCleanupTime
        val frequencyMillis = TimeUnit.HOURS.toMillis(frequency.hours)
        
        println("距离上次清理: ${timeSinceLastCleanup / (1000 * 60 * 60)}小时, 频率: ${frequency.hours}小时")
        return timeSinceLastCleanup >= frequencyMillis
    }
    
    /**
     * 获取临时文件大小
     */
    private fun getTempFilesSize(): Long {
        return try {
            val tempDir = context.cacheDir
            var totalSize = 0L
            
            tempDir.listFiles()?.forEach { file ->
                if (file.name.startsWith("temp_") || file.name.endsWith(".tmp")) {
                    totalSize += file.length()
                }
            }
            
            totalSize
        } catch (e: Exception) {
            println("获取临时文件大小异常: ${e.message}")
            0L
        }
    }
    
    /**
     * 清理临时文件
     */
    private fun clearTempFiles(): Boolean {
        return try {
            val tempDir = context.cacheDir
            var success = true
            
            tempDir.listFiles()?.forEach { file ->
                if (file.name.startsWith("temp_") || file.name.endsWith(".tmp")) {
                    if (!file.delete()) {
                        success = false
                    }
                }
            }
            
            success
        } catch (e: Exception) {
            println("清理临时文件异常: ${e.message}")
            false
        }
    }
    
    /**
     * 获取日志文件大小
     */
    private fun getLogsSize(): Long {
        return try {
            val logsDir = context.filesDir.resolve("logs")
            if (logsDir.exists()) {
                var totalSize = 0L
                logsDir.listFiles()?.forEach { file ->
                    if (file.name.endsWith(".log")) {
                        totalSize += file.length()
                    }
                }
                totalSize
            } else {
                0L
            }
        } catch (e: Exception) {
            println("获取日志文件大小异常: ${e.message}")
            0L
        }
    }
    
    /**
     * 清理日志文件
     */
    private fun clearLogs(): Boolean {
        return try {
            val logsDir = context.filesDir.resolve("logs")
            if (logsDir.exists()) {
                var success = true
                logsDir.listFiles()?.forEach { file ->
                    if (file.name.endsWith(".log")) {
                        if (!file.delete()) {
                            success = false
                        }
                    }
                }
                success
            } else {
                true
            }
        } catch (e: Exception) {
            println("清理日志文件异常: ${e.message}")
            false
        }
    }
    
    /**
     * 更新最后清理时间
     */
    private fun updateLastCleanupTime() {
        prefs.edit().putLong(KEY_LAST_CLEANUP, System.currentTimeMillis()).apply()
    }
    
    /**
     * 获取清理频率
     */
    fun getCleanupFrequency(): CleanupFrequency {
        val frequencyHours = prefs.getLong(KEY_CLEANUP_FREQUENCY, DEFAULT_CLEANUP_FREQUENCY)
        return CleanupFrequency.values().find { it.hours == frequencyHours } ?: CleanupFrequency.DAILY
    }
    
    /**
     * 设置清理频率
     */
    fun setCleanupFrequency(frequency: CleanupFrequency) {
        prefs.edit().putLong(KEY_CLEANUP_FREQUENCY, frequency.hours).apply()
        scheduleAutoCleanupWork()
    }
    
    /**
     * 获取存储阈值
     */
    fun getStorageThreshold(): Int {
        return prefs.getInt(KEY_STORAGE_THRESHOLD, DEFAULT_STORAGE_THRESHOLD)
    }
    
    /**
     * 设置存储阈值
     */
    fun setStorageThreshold(threshold: Int) {
        prefs.edit().putInt(KEY_STORAGE_THRESHOLD, threshold).apply()
    }
    
    /**
     * 获取启用的清理类型
     */
    fun getEnabledCleanupTypes(): Set<String> {
        val types = prefs.getStringSet(KEY_CLEANUP_TYPES, DEFAULT_CLEANUP_TYPES)
        return types ?: DEFAULT_CLEANUP_TYPES
    }
    
    /**
     * 设置启用的清理类型
     */
    fun setEnabledCleanupTypes(types: Set<String>) {
        prefs.edit().putStringSet(KEY_CLEANUP_TYPES, types).apply()
    }
    
    /**
     * 格式化字节大小
     */
    private fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 * 1024 -> "%.1f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
            bytes >= 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> "%.1f KB".format(bytes / 1024.0)
            else -> "$bytes B"
        }
    }
    
    /**
     * 启动自动清理监控
     */
    /**
     * 使用 WorkManager 调度自动清理任务。
     */
    fun scheduleAutoCleanupWork() {
        if (!settingsManager.isAutoCleanupEnabled()) {
            cancelAutoCleanupWork()
            return
        }

        val frequency = getCleanupFrequency()
        if (frequency == CleanupFrequency.MANUAL) {
            cancelAutoCleanupWork()
            return
        }

        val intervalHours = frequency.hours
        val intervalMillis = TimeUnit.HOURS.toMillis(intervalHours)
        val lastCleanupTime = prefs.getLong(KEY_LAST_CLEANUP, 0L)
        val now = System.currentTimeMillis()
        val timeSinceLastCleanup = if (lastCleanupTime == 0L) intervalMillis else now - lastCleanupTime
        val initialDelay = intervalMillis - timeSinceLastCleanup
        val safeInitialDelay = if (initialDelay < 0L) 0L else initialDelay

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(true)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<AutoCleanupWorker>(intervalHours, TimeUnit.HOURS)
            .setInitialDelay(safeInitialDelay, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            AUTO_CLEANUP_WORK_NAME,
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            workRequest
        )
    }

    /**
     * 取消自动清理任务。
     */
    fun cancelAutoCleanupWork() {
        WorkManager.getInstance(context).cancelUniqueWork(AUTO_CLEANUP_WORK_NAME)
    }

    companion object {
        private const val AUTO_CLEANUP_WORK_NAME = "auto_cleanup_periodic_work"
    }
}

data class AutoCleanupResult(
    val success: Boolean,
    val freedSpace: Long,
    val duration: Long,
    val items: List<CleanupItem>,
    val error: String? = null
)




