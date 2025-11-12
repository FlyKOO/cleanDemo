package com.example.cleanup.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cleanup.R
import com.example.cleanup.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {},
    onNavigateToScheduledCleanup: () -> Unit = {},
    onNavigateToPermissions: () -> Unit = {},
    onNavigateToLanguageSelection: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onNavigateToAutoCleanupSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: SettingsViewModel = viewModel()
    
    // 从ViewModel获取设置状态
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val autoCleanupEnabled by viewModel.autoCleanupEnabled.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val safeModeEnabled by viewModel.safeModeEnabled.collectAsState()
    val appVersionInfo by viewModel.appVersionInfo.collectAsState()
    
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = context.getString(R.string.settings),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = context.getString(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 语言设置
                SettingsCard(
                    title = context.getString(R.string.language),
                    subtitle = viewModel.getCurrentLanguageDisplayName(),
                    icon = Icons.Default.Settings,
                    onClick = onNavigateToLanguageSelection
                )
                
                // 自动清理设置
                SettingsCard(
                    title = context.getString(R.string.auto_cleanup),
                    subtitle = if (autoCleanupEnabled) context.getString(R.string.enabled) else context.getString(R.string.disabled),
                    icon = Icons.Default.Settings,
                    onClick = onNavigateToAutoCleanupSettings,
                    trailing = {
                        Switch(
                            checked = autoCleanupEnabled,
                            onCheckedChange = { viewModel.setAutoCleanupEnabled(it) }
                        )
                    }
                )
                
                // 定时清理设置
                SettingsCard(
                    title = context.getString(R.string.scheduled_cleanup),
                    subtitle = context.getString(R.string.scheduled_cleanup_desc),
                    icon = Icons.Default.Settings,
                    onClick = onNavigateToScheduledCleanup
                )
                
                // 权限设置
                SettingsCard(
                    title = context.getString(R.string.app_management_permissions),
                    subtitle = context.getString(R.string.app_management_permissions_desc),
                    icon = Icons.Default.Settings,
                    onClick = onNavigateToPermissions
                )
                
                // 通知设置
                SettingsCard(
                    title = context.getString(R.string.notifications),
                    subtitle = if (notificationsEnabled) context.getString(R.string.enabled) else context.getString(R.string.disabled),
                    icon = Icons.Default.Settings,
                    trailing = {
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = { viewModel.setNotificationsEnabled(it) }
                        )
                    }
                )
                
                // 安全模式
                SettingsCard(
                    title = context.getString(R.string.safe_mode),
                    subtitle = if (safeModeEnabled) context.getString(R.string.enabled) else context.getString(R.string.disabled),
                    icon = Icons.Default.Settings,
                    trailing = {
                        Switch(
                            checked = safeModeEnabled,
                            onCheckedChange = { viewModel.setSafeModeEnabled(it) }
                        )
                    }
                )
                
                // 关于应用
                SettingsCard(
                    title = context.getString(R.string.about),
                    subtitle = "${context.getString(R.string.version)} ${appVersionInfo.versionName}",
                    icon = Icons.Default.Info,
                    onClick = onNavigateToAbout
                )
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun SettingsCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .let { cardModifier ->
                if (onClick != null) {
                    cardModifier.clickable { onClick() }
                } else {
                    cardModifier
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            trailing?.invoke()
        }
    }
}
