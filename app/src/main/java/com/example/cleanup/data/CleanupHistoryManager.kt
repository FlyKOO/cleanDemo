package com.example.cleanup.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

data class CleanupRecord(
    val id: String,
    val timestamp: Long,
    val freedSpace: Long,
    val duration: Long, // 毫秒
    val items: List<CleanupItem>,
    val type: CleanupHistoryType
)

data class CleanupItem(
    val name: String,
    val size: Long,
    val count: Int
)

enum class CleanupHistoryType {
    MANUAL, AUTO, SCHEDULED
}

class CleanupHistoryManager(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("cleanup_history", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val historyKey = "cleanup_records"
    
    /**
     * 添加清理记录
     */
    fun addCleanupRecord(record: CleanupRecord) {
        val existingRecords = getCleanupHistory()
        val updatedRecords = existingRecords + record
        
        // 按时间戳排序，最新的在前面
        val sortedRecords = updatedRecords.sortedByDescending { it.timestamp }
        
        // 只保留最近100条记录
        val limitedRecords = sortedRecords.take(100)
        
        val json = gson.toJson(limitedRecords)
        prefs.edit().putString(historyKey, json).apply()
        
        println("添加清理记录: ${record.id}, 释放空间: ${formatBytes(record.freedSpace)}")
    }
    
    /**
     * 获取清理历史
     */
    fun getCleanupHistory(): List<CleanupRecord> {
        val json = prefs.getString(historyKey, null)
        return if (json != null) {
            try {
                val type = object : TypeToken<List<CleanupRecord>>() {}.type
                gson.fromJson(json, type) ?: emptyList()
            } catch (e: Exception) {
                println("解析清理历史失败: ${e.message}")
                emptyList()
            }
        } else {
            emptyList()
        }
    }
    
    /**
     * 根据时间范围获取清理历史
     */
    fun getCleanupHistoryByPeriod(period: String): List<CleanupRecord> {
        val allRecords = getCleanupHistory()
        val currentTime = System.currentTimeMillis()
        
        return when (period) {
            "week" -> {
                val weekAgo = currentTime - (7 * 24 * 60 * 60 * 1000L)
                allRecords.filter { it.timestamp >= weekAgo }
            }
            "month" -> {
                val monthAgo = currentTime - (30 * 24 * 60 * 60 * 1000L)
                allRecords.filter { it.timestamp >= monthAgo }
            }
            "all" -> allRecords
            else -> allRecords
        }
    }
    
    /**
     * 获取清理统计信息
     */
    fun getCleanupStats(period: String = "all"): CleanupStats {
        val records = getCleanupHistoryByPeriod(period)
        
        val totalFreedSpace = records.sumOf { it.freedSpace }
        val totalCleanupCount = records.size
        val averageFreedSpace = if (records.isNotEmpty()) {
            totalFreedSpace / records.size
        } else 0L
        
        // 按类型统计
        val manualCount = records.count { it.type == CleanupHistoryType.MANUAL }
        val autoCount = records.count { it.type == CleanupHistoryType.AUTO }
        val scheduledCount = records.count { it.type == CleanupHistoryType.SCHEDULED }
        
        return CleanupStats(
            totalCleanupCount = totalCleanupCount,
            totalFreedSpace = totalFreedSpace,
            averageFreedSpace = averageFreedSpace,
            manualCount = manualCount,
            autoCount = autoCount,
            scheduledCount = scheduledCount
        )
    }
    
    /**
     * 清除所有历史记录
     */
    fun clearAllHistory() {
        prefs.edit().remove(historyKey).apply()
        println("清除所有清理历史记录")
    }
    
    /**
     * 删除指定的清理记录
     */
    fun deleteCleanupRecord(recordId: String) {
        val existingRecords = getCleanupHistory()
        val updatedRecords = existingRecords.filter { it.id != recordId }
        
        val json = gson.toJson(updatedRecords)
        prefs.edit().putString(historyKey, json).apply()
        
        println("删除清理记录: $recordId")
    }
    
    /**
     * 创建清理记录
     */
    fun createCleanupRecord(
        freedSpace: Long,
        duration: Long,
        items: List<CleanupItem>,
        type: CleanupHistoryType
    ): CleanupRecord {
        return CleanupRecord(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            freedSpace = freedSpace,
            duration = duration,
            items = items,
            type = type
        )
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
        val formatter = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
        return formatter.format(date)
    }
}

data class CleanupStats(
    val totalCleanupCount: Int,
    val totalFreedSpace: Long,
    val averageFreedSpace: Long,
    val manualCount: Int,
    val autoCount: Int,
    val scheduledCount: Int
)

