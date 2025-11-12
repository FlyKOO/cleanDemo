package com.example.cleanup.data

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import java.util.Locale
import com.example.cleanup.R

class SettingsManager(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    
    // 设置键名
    private val KEY_LANGUAGE = "language"
    private val KEY_AUTO_CLEANUP = "auto_cleanup"
    private val KEY_NOTIFICATIONS = "notifications"
    private val KEY_SAFE_MODE = "safe_mode"
    private val KEY_FIRST_LAUNCH = "first_launch"
    
    // 默认值
    private val DEFAULT_LANGUAGE = "zh"
    private val DEFAULT_AUTO_CLEANUP = false
    private val DEFAULT_NOTIFICATIONS = true
    private val DEFAULT_SAFE_MODE = false
    private val DEFAULT_FIRST_LAUNCH = true
    
    /**
     * 获取当前语言
     */
    fun getLanguage(): String {
        return prefs.getString(KEY_LANGUAGE, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE
    }
    
    /**
     * 设置语言
     */
    fun setLanguage(language: String) {
        println("=== 语言切换调试 ===")
        println("设置语言: $language")
        prefs.edit().putString(KEY_LANGUAGE, language).apply()
        updateAppLanguage(language)
        println("语言设置完成")
    }
    
    /**
     * 更新应用语言环境
     */
    private fun updateAppLanguage(language: String) {
        println("开始更新语言环境: $language")
        val locale = when (language) {
            "zh" -> Locale("zh", "CN")
            "en" -> Locale.ENGLISH
            "pl" -> Locale("pl", "PL")
            "th" -> Locale("th", "TH")
            else -> Locale("zh", "CN")
        }
        
        println("设置Locale: $locale")
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale)
            context.createConfigurationContext(config)
            println("使用新API设置语言环境")
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
            println("使用旧API设置语言环境")
        }
        println("语言环境更新完成")
    }
    
    /**
     * 获取语言显示名称
     */
    fun getLanguageDisplayName(language: String): String {
        return when (language) {
            "zh" -> "中文"
            "en" -> "English"
            "pl" -> "Polski"
            "th" -> "ไทย"
            else -> "中文"
        }
    }
    
    /**
     * 获取所有支持的语言
     */
    fun getSupportedLanguages(): List<LanguageOption> {
        val currentLang = getLanguage()
        return getSupportedLanguages(currentLang)
    }
    
    /**
     * 获取所有支持的语言（指定语言环境）
     */
    fun getSupportedLanguages(language: String): List<LanguageOption> {
        return listOf(
            LanguageOption("zh", "中文", getLanguageNativeName("zh", language)),
            LanguageOption("en", "English", getLanguageNativeName("en", language)),
            LanguageOption("pl", "Polski", getLanguageNativeName("pl", language)),
            LanguageOption("th", "ไทย", getLanguageNativeName("th", language))
        )
    }
    
    /**
     * 获取语言的本地名称
     */
    private fun getLanguageNativeName(languageCode: String, currentLanguage: String = getLanguage()): String {
        // 临时设置语言环境以获取正确的翻译
        val originalLocale = Locale.getDefault()
        val targetLocale = when (currentLanguage) {
            "zh" -> Locale("zh", "CN")
            "en" -> Locale.ENGLISH
            "pl" -> Locale("pl", "PL")
            "th" -> Locale("th", "TH")
            else -> Locale("zh", "CN")
        }
        
        // 临时设置语言环境
        Locale.setDefault(targetLocale)
        val config = Configuration(context.resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(targetLocale)
        } else {
            @Suppress("DEPRECATION")
            config.locale = targetLocale
        }
        
        val contextWithLocale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.createConfigurationContext(config)
        } else {
            context
        }
        
        val result = when (languageCode) {
            "zh" -> contextWithLocale.getString(R.string.language_native_chinese)
            "en" -> contextWithLocale.getString(R.string.language_native_english)
            "pl" -> contextWithLocale.getString(R.string.language_native_polish)
            "th" -> contextWithLocale.getString(R.string.language_native_thai)
            else -> contextWithLocale.getString(R.string.language_native_chinese)
        }
        
        // 恢复原始语言环境
        Locale.setDefault(originalLocale)
        
        return result
    }
    
    /**
     * 是否启用自动清理
     */
    fun isAutoCleanupEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTO_CLEANUP, DEFAULT_AUTO_CLEANUP)
    }
    
    /**
     * 设置自动清理
     */
    fun setAutoCleanupEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_CLEANUP, enabled).apply()
    }
    
    /**
     * 是否启用通知
     */
    fun isNotificationsEnabled(): Boolean {
        return prefs.getBoolean(KEY_NOTIFICATIONS, DEFAULT_NOTIFICATIONS)
    }
    
    /**
     * 设置通知
     */
    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS, enabled).apply()
    }
    
    /**
     * 是否启用安全模式
     */
    fun isSafeModeEnabled(): Boolean {
        val isEnabled = prefs.getBoolean(KEY_SAFE_MODE, DEFAULT_SAFE_MODE)
        println("=== 安全模式检查 ===")
        println("安全模式状态: $isEnabled")
        println("默认值: $DEFAULT_SAFE_MODE")
        return isEnabled
    }
    
    /**
     * 设置安全模式
     */
    fun setSafeModeEnabled(enabled: Boolean) {
        println("=== 设置安全模式 ===")
        println("设置安全模式为: $enabled")
        prefs.edit().putBoolean(KEY_SAFE_MODE, enabled).apply()
        println("安全模式设置完成")
    }
    
    /**
     * 强制禁用安全模式（用于调试）
     */
    fun forceDisableSafeMode() {
        println("=== 强制禁用安全模式 ===")
        prefs.edit().putBoolean(KEY_SAFE_MODE, false).apply()
        println("安全模式已强制禁用")
    }
    
    /**
     * 是否首次启动
     */
    fun isFirstLaunch(): Boolean {
        return prefs.getBoolean(KEY_FIRST_LAUNCH, DEFAULT_FIRST_LAUNCH)
    }
    
    /**
     * 设置首次启动完成
     */
    fun setFirstLaunchCompleted() {
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
    }
    
    /**
     * 获取应用版本信息
     */
    fun getAppVersion(): AppVersionInfo {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        return AppVersionInfo(
            versionName = packageInfo.versionName ?: "1.0.0",
            versionCode = packageInfo.longVersionCode,
            packageName = context.packageName
        )
    }
    
    /**
     * 重置所有设置
     */
    fun resetAllSettings() {
        prefs.edit().clear().apply()
    }
}

data class LanguageOption(
    val code: String,
    val displayName: String,
    val nativeName: String
)

data class AppVersionInfo(
    val versionName: String,
    val versionCode: Long,
    val packageName: String
)




