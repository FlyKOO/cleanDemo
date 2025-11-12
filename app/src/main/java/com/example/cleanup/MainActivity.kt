package com.example.cleanup

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.cleanup.data.SettingsManager
import com.example.cleanup.ui.navigation.CleanupNavigation
import com.example.cleanup.ui.screen.DashboardScreen
import com.example.cleanup.ui.theme.CleanupTheme
import java.util.Locale

class MainActivity : ComponentActivity() {
    
    override fun attachBaseContext(newBase: Context) {
        println("=== MainActivity.attachBaseContext ===")
        val settingsManager = SettingsManager(newBase)
        val language = settingsManager.getLanguage()
        println("MainActivity获取语言: $language")
        
        val locale = when (language) {
            "zh" -> Locale("zh", "CN")
            "en" -> Locale.ENGLISH
            "pl" -> Locale("pl", "PL")
            "th" -> Locale("th", "TH")
            else -> Locale("zh", "CN")
        }
        
        println("MainActivity设置Locale: $locale")
        Locale.setDefault(locale)
        
        val config = Configuration(newBase.resources.configuration)
        val context = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale)
            newBase.createConfigurationContext(config)
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
            @Suppress("DEPRECATION")
            newBase.resources.updateConfiguration(config, newBase.resources.displayMetrics)
            newBase
        }
        
        super.attachBaseContext(context)
    }
    private fun isAllFilesGranted(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Environment.isExternalStorageManager() else true
    private fun openAllFilesGrant(activity: Activity) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val uri = Uri.parse("package:${activity.packageName}")
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri)
                activity.startActivity(intent)
            }
        } catch (_: Exception) {
            try { activity.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)) }
            catch (_: ActivityNotFoundException) {}
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        println("=== MainActivity.onCreate ===")
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            CleanupTheme {
                // 权限（<= Android 10）
                val needsLegacyPerm = remember { Build.VERSION.SDK_INT < Build.VERSION_CODES.R }
                val permLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { /* 用户可再次点击触发 */ }

                fun ensurePermissionOrGuide(): Boolean {
                    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        if (!isAllFilesGranted()) {
                            Toast.makeText(this, "请授予“所有文件访问”后再扫描/删除", Toast.LENGTH_SHORT).show()
                            openAllFilesGrant(this)
                            false
                        } else true
                    } else {
                        permLauncher.launch(arrayOf(
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        ))
                        true
                    }
                }
                LaunchedEffect(Unit) {
                    ensurePermissionOrGuide()
                }
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CleanupNavigation()
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardPreview() {
    CleanupTheme {
        DashboardScreen()
    }
}