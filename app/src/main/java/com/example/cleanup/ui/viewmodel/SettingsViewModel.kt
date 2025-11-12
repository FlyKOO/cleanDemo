package com.example.cleanup.ui.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cleanup.MainActivity
import com.example.cleanup.data.SettingsManager
import com.example.cleanup.data.LanguageOption
import com.example.cleanup.data.AppVersionInfo
import com.example.cleanup.data.NotificationManager
import com.example.cleanup.data.AutoCleanupManager
import com.example.cleanup.work.EVENT_17
import com.example.cleanup.work.EVENT_22
import com.example.cleanup.work.EVENT_23
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    
    private val settingsManager = SettingsManager(application)
    private val autoCleanupManager = AutoCleanupManager(application)
    private val notificationManager = NotificationManager(application)
    
    // 语言设置
    private val _currentLanguage = MutableStateFlow(settingsManager.getLanguage())
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()
    
    private val _supportedLanguages = MutableStateFlow(settingsManager.getSupportedLanguages())
    val supportedLanguages: StateFlow<List<LanguageOption>> = _supportedLanguages.asStateFlow()
    
    // 自动清理设置
    private val _autoCleanupEnabled = MutableStateFlow(settingsManager.isAutoCleanupEnabled())
    val autoCleanupEnabled: StateFlow<Boolean> = _autoCleanupEnabled.asStateFlow()
    
    // 通知设置
    private val _notificationsEnabled = MutableStateFlow(settingsManager.isNotificationsEnabled())
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()
    
    // 安全模式设置
    private val _safeModeEnabled = MutableStateFlow(settingsManager.isSafeModeEnabled())
    val safeModeEnabled: StateFlow<Boolean> = _safeModeEnabled.asStateFlow()
    
    // 应用版本信息
    private val _appVersionInfo = MutableStateFlow(settingsManager.getAppVersion())
    val appVersionInfo: StateFlow<AppVersionInfo> = _appVersionInfo.asStateFlow()
    
    /**
     * 设置语言
     */
    fun setLanguage(language: String) {
        viewModelScope.launch {
            settingsManager.setLanguage(language)
            _currentLanguage.value = language
            
            // 更新语言列表以反映新的本地名称
            _supportedLanguages.value = settingsManager.getSupportedLanguages()
            
            // 重启Activity以应用新语言
            restartActivity()
        }
    }
    
    /**
     * 重启Activity以应用语言更改
     */
    private fun restartActivity() {
        val intent = Intent(getApplication(), MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        getApplication<Application>().startActivity(intent)
    }
    
    /**
     * 设置自动清理
     */
    fun setAutoCleanupEnabled(enabled: Boolean) {
        viewModelScope.launch {
            EVENT_17(extra =  enabled.toString())
            settingsManager.setAutoCleanupEnabled(enabled)
            _autoCleanupEnabled.value = enabled
            if (enabled) {
                autoCleanupManager.scheduleAutoCleanupWork()
            } else {
                autoCleanupManager.cancelAutoCleanupWork()
            }
        }
    }
    
    /**
     * 设置通知
     */
    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            EVENT_22(extra = enabled.toString())
            settingsManager.setNotificationsEnabled(enabled)
            _notificationsEnabled.value = enabled
        }
    }
    
    /**
     * 设置安全模式
     */
    fun setSafeModeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            EVENT_23(extra = enabled.toString())
            settingsManager.setSafeModeEnabled(enabled)
            _safeModeEnabled.value = enabled
        }
    }
    
    /**
     * 获取当前语言的显示名称
     */
    fun getCurrentLanguageDisplayName(): String {
        return settingsManager.getLanguageDisplayName(_currentLanguage.value)
    }
    
    /**
     * 重置所有设置
     */
    fun resetAllSettings() {
        viewModelScope.launch {
            settingsManager.resetAllSettings()
            // 重新加载所有设置
            _currentLanguage.value = settingsManager.getLanguage()
            _autoCleanupEnabled.value = settingsManager.isAutoCleanupEnabled()
            _notificationsEnabled.value = settingsManager.isNotificationsEnabled()
            _safeModeEnabled.value = settingsManager.isSafeModeEnabled()
        }
    }
    
}
