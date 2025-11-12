package com.example.cleanup.ui.screen

import android.text.format.Formatter
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cleanup.R
import com.example.cleanup.ui.animation.CleanupParticleEffect
import com.example.cleanup.ui.animation.rememberCleanupAnimation
import com.example.cleanup.ui.viewmodel.StorageViewModel
import com.example.cleanup.ui.component.CleanupButton
import com.example.cleanup.ui.component.StorageProgress
import com.example.cleanup.ui.theme.PrimaryGradient
import com.example.cleanup.work.EVENT_3
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToAppManagement: () -> Unit = {},
    onNavigateToLargeFileScan: () -> Unit = {},
    onNavigateToCleanupHistory: () -> Unit = {}
) {
    val viewModel: StorageViewModel = viewModel()
    val context = LocalContext.current
    
    // 调试信息：检查当前语言环境
    LaunchedEffect(Unit) {
        println("=== DashboardScreen 语言调试 ===")
        println("当前Locale: ${context.resources.configuration.locales[0]}")
        println("系统默认Locale: ${java.util.Locale.getDefault()}")
        println("设置标题文本: ${context.getString(R.string.dashboard_title)}")
        println("高级功能文本: ${context.getString(R.string.advanced_features)}")
    }
    
    // 从ViewModel获取真实数据
    val storageInfo by viewModel.storageInfo.collectAsState()
    val cacheSizeBytes by viewModel.cacheSize.collectAsState()
    val junkSizeBytes by viewModel.junkSize.collectAsState()
    val duplicateSizeBytes by viewModel.duplicateSize.collectAsState()
    val isCleaning by viewModel.isCleaning.collectAsState()
    val cleaningProgress by viewModel.cleaningProgress.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    
    // 调试信息
    LaunchedEffect(isRefreshing) {
        println("DashboardScreen: isRefreshing = $isRefreshing")
    }
    
    LaunchedEffect(isCleaning) {
        println("DashboardScreen: isCleaning = $isCleaning")
    }
    
    LaunchedEffect(cleaningProgress) {
        println("DashboardScreen: cleaningProgress = $cleaningProgress")
    }
    
    LaunchedEffect(cacheSizeBytes, junkSizeBytes, duplicateSizeBytes) {
        println("DashboardScreen: cacheSizeBytes = $cacheSizeBytes, junkSizeBytes = $junkSizeBytes, duplicateSizeBytes = $duplicateSizeBytes")
        val totalSizeBytes = cacheSizeBytes + junkSizeBytes + duplicateSizeBytes
        println("DashboardScreen: totalSizeBytes = $totalSizeBytes")
        val isButtonEnabled = !isCleaning && totalSizeBytes > 0
        println("DashboardScreen: isButtonEnabled = $isButtonEnabled")
    }
    
    // 本地状态
    var showParticles by remember { mutableStateOf(false) }
    
    // 动画状态
    val cleanupAnimation = rememberCleanupAnimation(isActive = isCleaning)

    val totalCleanableBytes = cacheSizeBytes + junkSizeBytes + duplicateSizeBytes
    
    // 清理完成后的处理
    LaunchedEffect(isCleaning) {
        if (!isCleaning && showParticles) {
            // 清理完成，延迟关闭粒子效果
            delay(1000)
            showParticles = false
        }
    }
    
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = context.getString(R.string.dashboard_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refreshData() },
                        enabled = !isRefreshing
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = context.getString(R.string.refresh_data),
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                    IconButton(
                        onClick = onNavigateToSettings
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = context.getString(R.string.settings),
                            tint = MaterialTheme.colorScheme.onBackground
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
            // 粒子效果
            CleanupParticleEffect(
                isActive = showParticles,
                modifier = Modifier.fillMaxSize()
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // 存储空间概览
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1E3A8A) // 深蓝色背景
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 8.dp
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        // 标题
                        Text(
                            text = context.getString(R.string.storage_overview),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // 主要内容区域：圆圈 + 清理详情
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 存储空间圆圈（左侧）
                            StorageProgress(
                                usedSpace = storageInfo.usedSpace,
                                totalSpace = storageInfo.totalSpace,
                                modifier = Modifier.size(100.dp)
                            )
                            
                            // 清理详情（右侧）
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CleanupDetailItem(
                                    title = context.getString(R.string.cache),
                                    size = Formatter.formatFileSize(context, cacheSizeBytes),
                                    icon = Icons.Default.Delete
                                )

                                CleanupDetailItem(
                                    title = context.getString(R.string.junk),
                                    size = Formatter.formatFileSize(context, junkSizeBytes),
                                    icon = Icons.Default.Delete
                                )

                                CleanupDetailItem(
                                    title = context.getString(R.string.duplicate),
                                    size = Formatter.formatFileSize(context, duplicateSizeBytes),
                                    icon = Icons.Default.Delete
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // 底部统计信息
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = context.getString(
                                    R.string.available_space,
                                    Formatter.formatFileSize(context, storageInfo.availableSpace)
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = context.getString(
                                    R.string.used_space,
                                    Formatter.formatFileSize(context, storageInfo.usedSpace)
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${context.getString(R.string.usage_percentage)}: ${"%.1f".format(storageInfo.getUsagePercentage())}%",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF3B82F6), // 蓝色高亮
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                // 一键智能清理
                CleanupButton(
                    onClick = {
                        EVENT_3()
                        println("=== CleanupButton 被点击 ===")
                        println("isCleaning: $isCleaning")
                        println("cleaningProgress: $cleaningProgress")
                        if (!isCleaning) {
                            println("开始调用 viewModel.startCleanup()")
                            viewModel.startCleanup()
                            showParticles = true
                        } else {
                            println("正在清理中，跳过点击")
                        }
                    },
                    isEnabled = !isCleaning && totalCleanableBytes > 0,
                    isLoading = isCleaning,
                    progress = cleaningProgress,
                    text = if (isCleaning) {
                        context.getString(R.string.cleanup_in_progress)
                    } else {
                        context.getString(R.string.one_click_cleanup)
                    },
                    subText = if (isCleaning) {
                        context.getString(R.string.progress_percentage, (cleaningProgress * 100).toInt())
                    } else {
                        context.getString(
                            R.string.estimated_free_space,
                            Formatter.formatFileSize(context, totalCleanableBytes)
                        )
                    }
                )
                
                
                // 第二行：高级功能
                Text(
                    text = context.getString(R.string.advanced_features),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CleanupCard(
                        title = context.getString(R.string.app_management),
                        icon = Icons.Default.Settings,
                        onClick = onNavigateToAppManagement,
                        modifier = Modifier.weight(1f),
                        isEnabled = true
                    )
                    
                    CleanupCard(
                        title = context.getString(R.string.large_file_scan),
                        icon = Icons.Default.Settings,
                        onClick = onNavigateToLargeFileScan,
                        modifier = Modifier.weight(1f),
                        isEnabled = true
                    )
                    
                    CleanupCard(
                        title = context.getString(R.string.cleanup_history),
                        icon = Icons.Default.Settings,
                        onClick = onNavigateToCleanupHistory,
                        modifier = Modifier.weight(1f),
                        isEnabled = true
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun CleanupDetailItem(
    title: String,
    size: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 图标
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(
                    color = Color(0xFF3B82F6).copy(alpha = 0.2f), // 浅蓝色背景
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF3B82F6), // 蓝色图标
                modifier = Modifier.size(10.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // 文本
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        
        // 存储空间
        Text(
            text = size,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF3B82F6), // 蓝色高亮
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun CleanupCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true
) {
    Card(
        modifier = modifier
            .height(120.dp) // 固定高度确保一致性
            .padding(4.dp)
            .clickable(enabled = isEnabled) { 
                if (isEnabled) onClick() 
            },
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 图标
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 标题
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}
