package com.example.cleanup

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.cleanup.data.AutoCleanupManager
import com.example.cleanup.data.SettingsManager
import java.util.Locale

class CleanupApplication : Application(), DefaultLifecycleObserver {
    
    override fun onCreate() {
        super<Application>.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        println("=== CleanupApplication.onCreate ===")
        // 在应用启动时设置语言环境
        setAppLanguage()
        AutoCleanupManager(this).scheduleAutoCleanupWork()
    }
    
    override fun attachBaseContext(base: Context) {
        println("=== CleanupApplication.attachBaseContext ===")
        val newContext = updateBaseContextLanguage(base)
        super.attachBaseContext(newContext)
    }
    
    /**
     * 更新基础Context的语言环境
     */
    private fun updateBaseContextLanguage(context: Context): Context {
        println("=== updateBaseContextLanguage ===")
        val settingsManager = SettingsManager(context)
        val language = settingsManager.getLanguage()
        println("从设置中获取语言: $language")
        val locale = getLocaleFromLanguage(language)
        println("转换为Locale: $locale")
        
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale)
            val newContext = context.createConfigurationContext(config)
            println("使用新API创建新Context")
            newContext
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
            println("使用旧API更新配置")
            context
        }
    }
    
    /**
     * 设置应用语言环境
     */
    private fun setAppLanguage() {
        val settingsManager = SettingsManager(this)
        val language = settingsManager.getLanguage()
        val locale = getLocaleFromLanguage(language)
        
        Locale.setDefault(locale)
        val config = Configuration(resources.configuration)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale)
            createConfigurationContext(config)
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
            @Suppress("DEPRECATION")
            resources.updateConfiguration(config, resources.displayMetrics)
        }
    }
    
    /**
     * 根据语言代码获取Locale对象
     */
    private fun getLocaleFromLanguage(language: String): Locale {
        return when (language) {
            "zh" -> Locale("zh", "CN")
            "en" -> Locale.ENGLISH
            "pl" -> Locale("pl", "PL")
            "th" -> Locale("th", "TH")
            else -> Locale("zh", "CN")
        }
    }
}
