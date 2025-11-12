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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cleanup.R
import com.example.cleanup.data.AutoCleanupManager
import com.example.cleanup.ui.viewmodel.AutoCleanupViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoCleanupSettingsScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: AutoCleanupViewModel = viewModel()
    
    val isAutoCleanupRunning by viewModel.isAutoCleanupRunning.collectAsState()
    val lastCleanupResult by viewModel.lastCleanupResult.collectAsState()
    val cleanupFrequency by viewModel.cleanupFrequency.collectAsState()
    val storageThreshold by viewModel.storageThreshold.collectAsState()
    val enabledCleanupTypes by viewModel.enabledCleanupTypes.collectAsState()
    
    var showFrequencyDialog by remember { mutableStateOf(false) }
    var showThresholdDialog by remember { mutableStateOf(false) }
    var showTypesDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = context.getString(R.string.auto_cleanup_settings),
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
                // 状态卡片
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = context.getString(R.string.auto_cleanup_status),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = viewModel.getLastCleanupSummary(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // 手动触发按钮
                        if (isAutoCleanupRunning) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = context.getString(R.string.cleaning_in_progress),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        } else {
                            Text(
                                text = context.getString(R.string.click_to_trigger_cleanup),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                modifier = Modifier.clickable {
                                    viewModel.triggerAutoCleanup()
                                }
                            )
                        }
                    }
                }
                
                // 清理频率设置
                SettingsCard(
                    title = context.getString(R.string.cleanup_frequency),
                    subtitle = viewModel.getCleanupFrequencyDisplayName(),
                    icon = Icons.Default.Settings,
                    onClick = { showFrequencyDialog = true }
                )
                
                // 存储阈值设置
                SettingsCard(
                    title = context.getString(R.string.storage_threshold),
                    subtitle = viewModel.getStorageThresholdDisplayText(),
                    icon = Icons.Default.Settings,
                    onClick = { showThresholdDialog = true }
                )
                
                // 清理类型设置
                SettingsCard(
                    title = context.getString(R.string.cleanup_types),
                    subtitle = viewModel.getEnabledCleanupTypesDisplayText(),
                    icon = Icons.Default.Settings,
                    onClick = { showTypesDialog = true }
                )
                
                // 清理类型详细设置
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = context.getString(R.string.cleanup_types_details),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        val cleanupTypes = listOf(
                            "cache" to context.getString(R.string.app_cache),
                            "junk" to context.getString(R.string.junk_files),
                            "temp" to context.getString(R.string.temp_files),
                            "logs" to context.getString(R.string.log_files)
                        )
                        
                        cleanupTypes.forEach { (type, displayName) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                
                                Switch(
                                    checked = enabledCleanupTypes.contains(type),
                                    onCheckedChange = { viewModel.toggleCleanupType(type) }
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
    
    // 清理频率选择对话框
    if (showFrequencyDialog) {
        FrequencySelectionDialog(
            currentFrequency = cleanupFrequency,
            onFrequencySelected = { frequency ->
                viewModel.setCleanupFrequency(frequency)
                showFrequencyDialog = false
            },
            onDismiss = { showFrequencyDialog = false }
        )
    }
    
    // 存储阈值设置对话框
    if (showThresholdDialog) {
        ThresholdSettingDialog(
            currentThreshold = storageThreshold,
            onThresholdChanged = { threshold ->
                viewModel.setStorageThreshold(threshold)
                showThresholdDialog = false
            },
            onDismiss = { showThresholdDialog = false }
        )
    }
    
    // 清理类型选择对话框
    if (showTypesDialog) {
        CleanupTypesSelectionDialog(
            enabledTypes = enabledCleanupTypes,
            onTypesChanged = { types ->
                // 更新启用的清理类型
                types.forEach { type ->
                    if (!enabledCleanupTypes.contains(type)) {
                        viewModel.toggleCleanupType(type)
                    }
                }
                enabledCleanupTypes.forEach { type ->
                    if (!types.contains(type)) {
                        viewModel.toggleCleanupType(type)
                    }
                }
                showTypesDialog = false
            },
            onDismiss = { showTypesDialog = false }
        )
    }
}

@Composable
private fun FrequencySelectionDialog(
    currentFrequency: AutoCleanupManager.CleanupFrequency,
    onFrequencySelected: (AutoCleanupManager.CleanupFrequency) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = context.getString(R.string.cleanup_frequency))
        },
        text = {
            Column {
                AutoCleanupManager.CleanupFrequency.values().forEach { frequency ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onFrequencySelected(frequency) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.RadioButton(
                            selected = frequency == currentFrequency,
                            onClick = { onFrequencySelected(frequency) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (frequency) {
                                AutoCleanupManager.CleanupFrequency.DAILY -> context.getString(R.string.daily)
                                AutoCleanupManager.CleanupFrequency.WEEKLY -> context.getString(R.string.weekly)
                                AutoCleanupManager.CleanupFrequency.MONTHLY -> context.getString(R.string.monthly)
                                AutoCleanupManager.CleanupFrequency.MANUAL -> context.getString(R.string.manual)
                            },
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(context.getString(R.string.cancel))
            }
        }
    )
}

@Composable
private fun ThresholdSettingDialog(
    currentThreshold: Int,
    onThresholdChanged: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var threshold by remember { mutableStateOf(currentThreshold.toFloat()) }
    
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = context.getString(R.string.storage_threshold))
        },
        text = {
            Column {
                Text(
                    text = "${threshold.toInt()}%",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Slider(
                    value = threshold,
                    onValueChange = { threshold = it },
                    valueRange = 50f..95f,
                    steps = 8, // 50%, 55%, 60%, ..., 95%
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "50% - 95%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onThresholdChanged(threshold.toInt())
                }
            ) {
                Text(context.getString(R.string.save_changes))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(context.getString(R.string.cancel))
            }
        }
    )
}

@Composable
private fun CleanupTypesSelectionDialog(
    enabledTypes: Set<String>,
    onTypesChanged: (Set<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTypes by remember { mutableStateOf(enabledTypes) }
    
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = context.getString(R.string.cleanup_types))
        },
        text = {
            Column {
                val cleanupTypes = listOf(
                    "cache" to context.getString(R.string.app_cache),
                    "junk" to context.getString(R.string.junk_files),
                    "temp" to context.getString(R.string.temp_files),
                    "logs" to context.getString(R.string.log_files)
                )
                
                cleanupTypes.forEach { (type, displayName) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                selectedTypes = if (selectedTypes.contains(type)) {
                                    selectedTypes - type
                                } else {
                                    selectedTypes + type
                                }
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.Checkbox(
                            checked = selectedTypes.contains(type),
                            onCheckedChange = { checked ->
                                selectedTypes = if (checked) {
                                    selectedTypes + type
                                } else {
                                    selectedTypes - type
                                }
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onTypesChanged(selectedTypes)
                }
            ) {
                Text(context.getString(R.string.save_changes))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(context.getString(R.string.cancel))
            }
        }
    )
}
