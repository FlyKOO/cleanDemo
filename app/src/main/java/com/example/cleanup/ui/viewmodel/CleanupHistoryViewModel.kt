package com.example.cleanup.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cleanup.data.CleanupRecord
import com.example.cleanup.data.CleanupHistoryManager
import com.example.cleanup.data.CleanupStats
import com.example.cleanup.work.EVENT_14
import com.example.cleanup.work.EVENT_15
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CleanupHistoryViewModel(private val cleanupHistoryManager: CleanupHistoryManager) : ViewModel() {
    
    private val _cleanupHistory = MutableStateFlow<List<CleanupRecord>>(emptyList())
    val cleanupHistory: StateFlow<List<CleanupRecord>> = _cleanupHistory.asStateFlow()
    
    private val _selectedPeriod = MutableStateFlow("all")
    val selectedPeriod: StateFlow<String> = _selectedPeriod.asStateFlow()
    
    private val _stats = MutableStateFlow(CleanupStats(0, 0L, 0L, 0, 0, 0))
    val stats: StateFlow<CleanupStats> = _stats.asStateFlow()
    
    init {
        loadCleanupHistory()
    }
    
    /**
     * 加载清理历史
     */
    fun loadCleanupHistory() {
        viewModelScope.launch {
            val period = _selectedPeriod.value
            val history = cleanupHistoryManager.getCleanupHistoryByPeriod(period)
            _cleanupHistory.value = history
            
            val stats = cleanupHistoryManager.getCleanupStats(period)
            _stats.value = stats
        }
    }
    
    /**
     * 设置时间范围
     */
    fun setSelectedPeriod(period: String) {
        EVENT_14()
        _selectedPeriod.value = period
        loadCleanupHistory()
    }
    
    /**
     * 添加清理记录
     */
    fun addCleanupRecord(record: CleanupRecord) {
        viewModelScope.launch {
            cleanupHistoryManager.addCleanupRecord(record)
            loadCleanupHistory()
        }
    }
    
    /**
     * 删除清理记录
     */
    fun deleteCleanupRecord(recordId: String) {
        viewModelScope.launch {
            EVENT_15()
            cleanupHistoryManager.deleteCleanupRecord(recordId)
            loadCleanupHistory()
        }
    }
    
    /**
     * 清除所有历史记录
     */
    fun clearAllHistory() {
        viewModelScope.launch {
            cleanupHistoryManager.clearAllHistory()
            loadCleanupHistory()
        }
    }
    
    /**
     * 创建清理记录
     */
    fun createCleanupRecord(
        freedSpace: Long,
        duration: Long,
        items: List<com.example.cleanup.data.CleanupItem>,
        type: com.example.cleanup.data.CleanupHistoryType
    ): CleanupRecord {
        return cleanupHistoryManager.createCleanupRecord(freedSpace, duration, items, type)
    }
}

