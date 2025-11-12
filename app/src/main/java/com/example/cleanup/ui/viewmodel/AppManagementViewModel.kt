package com.example.cleanup.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cleanup.data.AppInfo
import com.example.cleanup.data.AppManager
import com.example.cleanup.data.SettingsManager
import com.example.cleanup.work.EVENT_9
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AppManagementViewModel(application: Application) : AndroidViewModel(application) {
    
    private val appManager = AppManager(application)
    private val settingsManager = SettingsManager(application)
    
    // 应用列表
    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()
    
    // 显示系统应用
    private val _showSystemApps = MutableStateFlow(false)
    val showSystemApps: StateFlow<Boolean> = _showSystemApps.asStateFlow()
    
    // 排序方式
    private val _sortBy = MutableStateFlow("size")
    val sortBy: StateFlow<String> = _sortBy.asStateFlow()
    
    // 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // 过滤后的应用列表
    private val _filteredApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val filteredApps: StateFlow<List<AppInfo>> = _filteredApps.asStateFlow()
    
    init {
        loadApps()
    }
    
    /**
     * 加载应用列表
     */
    fun loadApps() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val allApps = appManager.getAllInstalledApps()
                _apps.value = allApps
                updateFilteredApps()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 更新过滤后的应用列表
     */
    private fun updateFilteredApps() {
        val apps = _apps.value
        val showSystem = _showSystemApps.value
        val sort = _sortBy.value
        
        val filtered = apps.filter { app ->
            if (showSystem) true else !app.isSystemApp
        }.sortedWith { a, b ->
            when (sort) {
                "size" -> b.totalSize.compareTo(a.totalSize)
                "name" -> a.appName.compareTo(b.appName)
                "lastUsed" -> b.lastUsed.compareTo(a.lastUsed)
                else -> 0
            }
        }
        
        _filteredApps.value = filtered
    }
    
    /**
     * 切换显示系统应用
     */
    fun toggleShowSystemApps() {
        _showSystemApps.value = !_showSystemApps.value
        updateFilteredApps()
    }
    
    /**
     * 设置排序方式
     */
    fun setSortBy(sortType: String) {
        EVENT_9(extra = sortType)
        _sortBy.value = sortType
        updateFilteredApps()
    }
    
    
    /**
     * 清理应用缓存
     */
    fun clearAppCache(packageName: String) {
        // 检查安全模式
        if (settingsManager.isSafeModeEnabled()) {
            println("安全模式已启用，无法清理应用缓存")
            return
        }
        
        println("=== ViewModel.clearAppCache 被调用 ===")
        println("包名: $packageName")
        viewModelScope.launch {
            try {
                println("开始清理缓存...")
                val success = appManager.clearAppCache(packageName)
                println("清理结果: $success")
                if (success) {
                    println("清理成功，重新加载应用列表...")
                    // 重新加载应用列表
                    loadApps()
                } else {
                    println("清理失败")
                }
            } catch (e: Exception) {
                println("清理缓存时发生异常: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    /**
     * 卸载应用
     */
    fun uninstallApp(packageName: String) {
        println("=== ViewModel.uninstallApp 被调用 ===")
        println("包名: $packageName")
        viewModelScope.launch {
            try {
                println("开始卸载应用...")
                val success = appManager.uninstallApp(packageName)
                println("卸载结果: $success")
                if (success) {
                    println("卸载成功，重新加载应用列表...")
                    // 重新加载应用列表
                    loadApps()
                } else {
                    println("卸载失败")
                }
            } catch (e: Exception) {
                println("卸载应用时发生异常: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    /**
     * 获取统计信息
     */
    fun getStats(): AppStats {
        val apps = _apps.value
        return AppStats(
            totalApps = apps.size,
            totalSize = apps.sumOf { it.totalSize },
            totalCacheSize = apps.sumOf { it.cacheSize }
        )
    }
}

/**
 * 应用统计信息
 */
data class AppStats(
    val totalApps: Int,
    val totalSize: Long,
    val totalCacheSize: Long
)
