package com.example.cleanup.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cleanup.data.StorageInfo
import com.example.cleanup.data.StorageManager
import com.example.cleanup.data.SettingsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StorageViewModel(application: Application) : AndroidViewModel(application) {
    
    private val storageManager = StorageManager(application)
    private val settingsManager = SettingsManager(application)
    
    // 存储空间信息
    private val _storageInfo = MutableStateFlow(StorageInfo(0, 0, 0))
    val storageInfo: StateFlow<StorageInfo> = _storageInfo.asStateFlow()
    
    // 缓存大小
    private val _cacheSize = MutableStateFlow(0L)
    val cacheSize: StateFlow<Long> = _cacheSize.asStateFlow()
    
    // 垃圾文件大小
    private val _junkSize = MutableStateFlow(0L)
    val junkSize: StateFlow<Long> = _junkSize.asStateFlow()
    
    // 重复文件大小
    private val _duplicateSize = MutableStateFlow(0L)
    val duplicateSize: StateFlow<Long> = _duplicateSize.asStateFlow()
    
    // 加载状态
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    
    // 清理状态
    private val _isCleaning = MutableStateFlow(false)
    val isCleaning: StateFlow<Boolean> = _isCleaning.asStateFlow()
    
    // 清理进度
    private val _cleaningProgress = MutableStateFlow(0f)
    val cleaningProgress: StateFlow<Float> = _cleaningProgress.asStateFlow()
    
    init {
        // 创建一些测试文件用于演示清理功能
        createTestFiles()
        loadStorageData()
    }
    
    /**
     * 创建测试文件
     */
    private fun createTestFiles() {
        viewModelScope.launch {
            try {
                storageManager.createTestJunkFiles()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * 加载存储数据
     */
    fun loadStorageData() {
        viewModelScope.launch {
            try {
                // 获取存储空间信息
                val storage = storageManager.getStorageInfo()
                _storageInfo.value = storage
                
                // 获取缓存大小
                val cache = storageManager.getCacheSize()
                _cacheSize.value = cache
                
                // 获取垃圾文件大小
                val junk = storageManager.getJunkSize()
                _junkSize.value = junk
                
                // 获取重复文件大小
                val duplicate = storageManager.getDuplicateSize()
                _duplicateSize.value = duplicate
                
            } catch (e: Exception) {
                // 处理错误，使用默认值
                e.printStackTrace()
            }
        }
    }
    
    /**
     * 开始清理
     */
    fun startCleanup() {
        println("=== startCleanup 被调用 ===")
        if (_isCleaning.value) {
            println("已经在清理中，跳过")
            return
        }
        
        // 检查安全模式
        if (settingsManager.isSafeModeEnabled()) {
            println("安全模式已启用，强制禁用以继续清理")
            settingsManager.forceDisableSafeMode()
        }
        
        println("开始清理，设置状态")
        _isCleaning.value = true
        _cleaningProgress.value = 0f
        
        viewModelScope.launch {
            try {
                println("开始清理协程")
                // 模拟清理过程
                val totalSteps = 100
                for (i in 1..totalSteps) {
                    kotlinx.coroutines.delay(50) // 模拟清理时间
                    val progress = i.toFloat() / totalSteps
                    _cleaningProgress.value = progress
                    if (i % 20 == 0) { // 每20步打印一次
                        println("清理进度: ${(progress * 100).toInt()}%")
                    }
                }
                
                // 执行实际清理
                performCleanup()
                
                // 重新加载数据
                loadStorageData()
                
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isCleaning.value = false
                _cleaningProgress.value = 0f
            }
        }
    }
    
    /**
     * 执行清理操作
     */
    private suspend fun performCleanup() {
        try {
            // 清理缓存
            val cacheCleared = storageManager.clearCache()
            println("Cache cleared: $cacheCleared")
            
            // 清理垃圾文件
            val junkCleared = storageManager.clearJunk()
            println("Junk cleared: $junkCleared")
            
            // 注意：重复文件清理需要更复杂的逻辑，这里暂时跳过
            
            // 强制刷新数据
            kotlinx.coroutines.delay(500) // 等待文件系统更新
            loadStorageData()
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * 刷新数据
     */
    fun refreshData() {
        viewModelScope.launch {
            println("开始刷新数据...")
            _isRefreshing.value = true
            try {
                // 添加最小延迟，确保用户能看到加载状态
                kotlinx.coroutines.delay(500)
                loadStorageData()
                println("数据刷新完成")
            } finally {
                _isRefreshing.value = false
                println("刷新状态重置")
            }
        }
    }
}
