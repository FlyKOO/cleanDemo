package com.example.cleanup.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cleanup.R
import com.example.cleanup.data.AutoCleanupManager
import com.example.cleanup.data.AutoCleanupResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AutoCleanupViewModel(application: Application) : AndroidViewModel(application) {
    
    private val autoCleanupManager = AutoCleanupManager(application)
    
    // 自动清理状态
    private val _isAutoCleanupRunning = MutableStateFlow(false)
    val isAutoCleanupRunning: StateFlow<Boolean> = _isAutoCleanupRunning.asStateFlow()
    
    // 最后清理结果
    private val _lastCleanupResult = MutableStateFlow<AutoCleanupResult?>(null)
    val lastCleanupResult: StateFlow<AutoCleanupResult?> = _lastCleanupResult.asStateFlow()
    
    // 清理频率
    private val _cleanupFrequency = MutableStateFlow(autoCleanupManager.getCleanupFrequency())
    val cleanupFrequency: StateFlow<AutoCleanupManager.CleanupFrequency> = _cleanupFrequency.asStateFlow()
    
    // 存储阈值
    private val _storageThreshold = MutableStateFlow(autoCleanupManager.getStorageThreshold())
    val storageThreshold: StateFlow<Int> = _storageThreshold.asStateFlow()
    
    // 启用的清理类型
    private val _enabledCleanupTypes = MutableStateFlow(autoCleanupManager.getEnabledCleanupTypes())
    val enabledCleanupTypes: StateFlow<Set<String>> = _enabledCleanupTypes.asStateFlow()
    
    init {
        autoCleanupManager.scheduleAutoCleanupWork()
    }
    
    /**
     * 手动触发自动清理
     */
    fun triggerAutoCleanup() {
        viewModelScope.launch {
            _isAutoCleanupRunning.value = true
            try {
                val result = autoCleanupManager.performAutoCleanup()
                _lastCleanupResult.value = result
            } finally {
                _isAutoCleanupRunning.value = false
            }
        }
    }
    
    /**
     * 检查是否应该执行自动清理
     */
    fun checkAutoCleanupConditions() {
        viewModelScope.launch {
            val shouldCleanup = autoCleanupManager.shouldPerformAutoCleanup()
            if (shouldCleanup) {
                triggerAutoCleanup()
            }
        }
    }
    
    /**
     * 设置清理频率
     */
    fun setCleanupFrequency(frequency: AutoCleanupManager.CleanupFrequency) {
        autoCleanupManager.setCleanupFrequency(frequency)
        _cleanupFrequency.value = frequency
    }
    
    /**
     * 设置存储阈值
     */
    fun setStorageThreshold(threshold: Int) {
        autoCleanupManager.setStorageThreshold(threshold)
        _storageThreshold.value = threshold
    }
    
    /**
     * 设置启用的清理类型
     */
    fun setEnabledCleanupTypes(types: Set<String>) {
        autoCleanupManager.setEnabledCleanupTypes(types)
        _enabledCleanupTypes.value = types
    }
    
    /**
     * 切换清理类型
     */
    fun toggleCleanupType(type: String) {
        val currentTypes = _enabledCleanupTypes.value.toMutableSet()
        if (currentTypes.contains(type)) {
            currentTypes.remove(type)
        } else {
            currentTypes.add(type)
        }
        setEnabledCleanupTypes(currentTypes)
    }
    
    /**
     * 获取清理频率显示名称
     */
    fun getCleanupFrequencyDisplayName(): String {
        val context = getApplication<android.app.Application>()
        return when (_cleanupFrequency.value) {
            AutoCleanupManager.CleanupFrequency.DAILY -> context.getString(R.string.daily)
            AutoCleanupManager.CleanupFrequency.WEEKLY -> context.getString(R.string.weekly)
            AutoCleanupManager.CleanupFrequency.MONTHLY -> context.getString(R.string.monthly)
            AutoCleanupManager.CleanupFrequency.MANUAL -> context.getString(R.string.manual)
        }
    }
    
    /**
     * 获取存储阈值显示文本
     */
    fun getStorageThresholdDisplayText(): String {
        val context = getApplication<android.app.Application>()
        return context.getString(R.string.auto_cleanup_when_storage_reaches, _storageThreshold.value)
    }
    
    /**
     * 获取启用的清理类型显示文本
     */
    fun getEnabledCleanupTypesDisplayText(): String {
        val context = getApplication<android.app.Application>()
        val types = _enabledCleanupTypes.value
        val localizedTypes = types.map { type ->
            when (type) {
                "cache" -> context.getString(R.string.app_cache)
                "junk" -> context.getString(R.string.junk_files)
                "temp" -> context.getString(R.string.temp_files)
                "logs" -> context.getString(R.string.log_files)
                else -> type
            }
        }
        
        return when {
            localizedTypes.isEmpty() -> context.getString(R.string.none)
            localizedTypes.size == 1 -> localizedTypes.first()
            localizedTypes.size == 2 -> localizedTypes.joinToString(", ")
            else -> "${localizedTypes.take(2).joinToString(", ")}${context.getString(R.string.and_others_count, localizedTypes.size)}"
        }
    }
    
    /**
     * 获取最后清理结果摘要
     */
    fun getLastCleanupSummary(): String {
        val context = getApplication<android.app.Application>()
        val result = _lastCleanupResult.value
        return when {
            result == null -> context.getString(R.string.no_auto_cleanup_executed)
            result.success -> context.getString(R.string.last_cleanup_freed, formatBytes(result.freedSpace))
            else -> context.getString(R.string.last_cleanup_failed, result.error ?: context.getString(R.string.unknown_error))
        }
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
}




