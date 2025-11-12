package com.example.cleanup.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cleanup.R
import com.example.cleanup.data.CleanupFrequency
import com.example.cleanup.data.ScheduledCleanup
import com.example.cleanup.data.ScheduledCleanupRepository
import com.example.cleanup.data.ScheduledCleanupType
import com.example.cleanup.ui.theme.PrimaryGradient
import com.example.cleanup.ui.theme.SuccessGreen
import com.example.cleanup.work.EVENT_20
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduledCleanupScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val repository = remember(context) { ScheduledCleanupRepository(context) }

    // 控制添加任务对话框显示
    var showAddTaskDialog by remember { mutableStateOf(false) }

    // 控制编辑任务对话框显示
    var showEditTaskDialog by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<ScheduledCleanup?>(null) }

    var scheduledCleanups by remember { mutableStateOf(emptyList<ScheduledCleanup>()) }

    LaunchedEffect(Unit) {
        scheduledCleanups = repository.getScheduledCleanups()
    }
    
    val enabledCount = scheduledCleanups.count { it.isEnabled }
    val totalCount = scheduledCleanups.size
    
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = context.getString(R.string.scheduled_cleanup),
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // 统计信息卡片
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
                        text = context.getString(R.string.scheduled_cleanup_stats),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatItem(
                            label = context.getString(R.string.total_tasks),
                            value = "$totalCount",
                            color = MaterialTheme.colorScheme.primary
                        )
                        StatItem(
                            label = context.getString(R.string.enabled),
                            value = "$enabledCount",
                            color = SuccessGreen
                        )
                        StatItem(
                            label = context.getString(R.string.disabled),
                            value = "${totalCount - enabledCount}",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 定时清理任务列表 - 使用LazyColumn支持滚动
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 任务列表项
                items(scheduledCleanups) { cleanup ->
                    ScheduledCleanupItem(
                        cleanup = cleanup,
                        onToggleEnabled = {
                            scheduledCleanups = repository.toggleScheduledCleanup(cleanup.id)
                        },
                        onEdit = { 
                            editingTask = cleanup
                            showEditTaskDialog = true
                        },
                        onDelete = {
                            scheduledCleanups = repository.deleteScheduledCleanup(cleanup.id)
                        }
                    )
                }
                
                // 添加新任务按钮
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showAddTaskDialog = true
                                EVENT_20()
                                       },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = context.getString(R.string.add_new_scheduled_task),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
    
    // 添加任务对话框
    if (showAddTaskDialog) {
        AddScheduledCleanupDialog(
            onAddTask = { newTask ->
                scheduledCleanups = repository.addScheduledCleanup(newTask)
                showAddTaskDialog = false
            },
            onDismiss = { showAddTaskDialog = false }
        )
    }
    
    // 编辑任务对话框
    if (showEditTaskDialog && editingTask != null) {
        EditScheduledCleanupDialog(
            task = editingTask!!,
            onUpdateTask = { updatedTask ->
                scheduledCleanups = repository.updateScheduledCleanup(updatedTask)
                showEditTaskDialog = false
                editingTask = null
            },
            onDismiss = { 
                showEditTaskDialog = false
                editingTask = null
            }
        )
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ScheduledCleanupItem(
    cleanup: ScheduledCleanup,
    onToggleEnabled: (String) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (cleanup.isEnabled) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 头部信息
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 频率图标
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                when (cleanup.frequency) {
                                    CleanupFrequency.DAILY -> MaterialTheme.colorScheme.primary
                                    CleanupFrequency.WEEKLY -> MaterialTheme.colorScheme.secondary
                                    CleanupFrequency.MONTHLY -> MaterialTheme.colorScheme.tertiary
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (cleanup.frequency) {
                                CleanupFrequency.DAILY -> Icons.Default.Settings
                                CleanupFrequency.WEEKLY -> Icons.Default.Settings
                                CleanupFrequency.MONTHLY -> Icons.Default.Settings
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column {
                        Text(
                            text = cleanup.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = when (cleanup.frequency) {
                                CleanupFrequency.DAILY -> "${context.getString(R.string.daily)} ${cleanup.time}"
                                CleanupFrequency.WEEKLY -> "${context.getString(R.string.weekly)} ${cleanup.time}"
                                CleanupFrequency.MONTHLY -> "${context.getString(R.string.monthly)} ${cleanup.time}"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Switch(
                    checked = cleanup.isEnabled,
                    onCheckedChange = { onToggleEnabled(cleanup.id) }
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 清理类型
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                cleanup.cleanupTypes.forEach { type ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(PrimaryGradient)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = when (type) {
                                ScheduledCleanupType.CACHE -> context.getString(R.string.cache)
                                ScheduledCleanupType.JUNK -> context.getString(R.string.junk)
                                ScheduledCleanupType.DUPLICATE -> context.getString(R.string.duplicate)
                                ScheduledCleanupType.LARGE_FILES -> context.getString(R.string.large_files)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 运行信息
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = context.getString(R.string.last_run),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = cleanup.lastRun?.let { formatDate(it) } ?: context.getString(R.string.never_run),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Column {
                    Text(
                        text = context.getString(R.string.next_run),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatDate(cleanup.nextRun),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = context.getString(R.string.edit),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = context.getString(R.string.delete),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val date = java.util.Date(timestamp)
    val formatter = java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault())
    return formatter.format(date)
}

@Composable
private fun AddScheduledCleanupDialog(
    onAddTask: (ScheduledCleanup) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var taskName by remember { mutableStateOf("") }
    var selectedFrequency by remember { mutableStateOf(CleanupFrequency.DAILY) }
    var selectedTime by remember { mutableStateOf("02:00") }
    var selectedTypes by remember { mutableStateOf(setOf(ScheduledCleanupType.CACHE)) }
    
    // 计算下次运行时间
    val calculateNextRun = { frequency: CleanupFrequency ->
        val now = System.currentTimeMillis()
        when (frequency) {
            CleanupFrequency.DAILY -> now + 24 * 3600000L
            CleanupFrequency.WEEKLY -> now + 7 * 24 * 3600000L
            CleanupFrequency.MONTHLY -> now + 30 * 24 * 3600000L
        }
    }
    
    // 对话框内容
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // 标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = context.getString(R.string.add_scheduled_cleanup_task),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = context.getString(R.string.close)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 任务名称
            OutlinedTextField(
                value = taskName,
                onValueChange = { taskName = it },
                label = { Text(context.getString(R.string.task_name)) },
                placeholder = { Text(context.getString(R.string.task_name_placeholder)) },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 执行频率
            Text(
                text = context.getString(R.string.execution_frequency),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Column(
                modifier = Modifier.selectableGroup()
            ) {
                CleanupFrequency.values().forEach { frequency ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedFrequency == frequency,
                                onClick = { selectedFrequency = frequency }
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedFrequency == frequency,
                            onClick = { selectedFrequency = frequency }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (frequency) {
                                CleanupFrequency.DAILY -> context.getString(R.string.daily)
                                CleanupFrequency.WEEKLY -> context.getString(R.string.weekly)
                                CleanupFrequency.MONTHLY -> context.getString(R.string.monthly)
                            },
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 执行时间
            Text(
                text = context.getString(R.string.execution_time),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = selectedTime,
                onValueChange = { selectedTime = it },
                label = { Text(context.getString(R.string.time_hh_mm)) },
                placeholder = { Text("02:00") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 清理类型
            Text(
                text = context.getString(R.string.cleanup_type),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                ScheduledCleanupType.values().forEach { type ->
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
                        Checkbox(
                            checked = selectedTypes.contains(type),
                            onCheckedChange = { checked ->
                                selectedTypes = if (checked) {
                                    selectedTypes + type
                                } else {
                                    selectedTypes - type
                                }
                            }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = when (type) {
                                ScheduledCleanupType.CACHE -> context.getString(R.string.app_cache)
                                ScheduledCleanupType.JUNK -> context.getString(R.string.junk_files)
                                ScheduledCleanupType.DUPLICATE -> context.getString(R.string.duplicate_files)
                                ScheduledCleanupType.LARGE_FILES -> context.getString(R.string.large_files)
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text(context.getString(R.string.cancel))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (taskName.isNotBlank() && selectedTypes.isNotEmpty()) {
                            val newTask = ScheduledCleanup(
                                id = UUID.randomUUID().toString(),
                                name = taskName,
                                isEnabled = true,
                                frequency = selectedFrequency,
                                time = selectedTime,
                                cleanupTypes = selectedTypes.toList(),
                                lastRun = null,
                                nextRun = calculateNextRun(selectedFrequency)
                            )
                            onAddTask(newTask)
                        }
                    },
                    enabled = taskName.isNotBlank() && selectedTypes.isNotEmpty()
                ) {
                    Text(context.getString(R.string.add_task))
                }
            }
        }
    }
}

@Composable
private fun EditScheduledCleanupDialog(
    task: ScheduledCleanup,
    onUpdateTask: (ScheduledCleanup) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var taskName by remember { mutableStateOf(task.name) }
    var selectedFrequency by remember { mutableStateOf(task.frequency) }
    var selectedTime by remember { mutableStateOf(task.time) }
    var selectedTypes by remember { mutableStateOf(task.cleanupTypes.toSet()) }
    
    // 计算下次运行时间
    val calculateNextRun = { frequency: CleanupFrequency ->
        val now = System.currentTimeMillis()
        when (frequency) {
            CleanupFrequency.DAILY -> now + 24 * 3600000L
            CleanupFrequency.WEEKLY -> now + 7 * 24 * 3600000L
            CleanupFrequency.MONTHLY -> now + 30 * 24 * 3600000L
        }
    }
    
    // 对话框内容
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // 标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = context.getString(R.string.edit_scheduled_cleanup_task),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭"
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 任务名称
            OutlinedTextField(
                value = taskName,
                onValueChange = { taskName = it },
                label = { Text(context.getString(R.string.task_name)) },
                placeholder = { Text(context.getString(R.string.task_name_placeholder)) },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 执行频率
            Text(
                text = context.getString(R.string.execution_frequency),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Column(
                modifier = Modifier.selectableGroup()
            ) {
                CleanupFrequency.values().forEach { frequency ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedFrequency == frequency,
                                onClick = { selectedFrequency = frequency }
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedFrequency == frequency,
                            onClick = { selectedFrequency = frequency }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (frequency) {
                                CleanupFrequency.DAILY -> context.getString(R.string.daily)
                                CleanupFrequency.WEEKLY -> context.getString(R.string.weekly)
                                CleanupFrequency.MONTHLY -> context.getString(R.string.monthly)
                            },
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 执行时间
            Text(
                text = context.getString(R.string.execution_time),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = selectedTime,
                onValueChange = { selectedTime = it },
                label = { Text(context.getString(R.string.time_hh_mm)) },
                placeholder = { Text("02:00") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 清理类型
            Text(
                text = context.getString(R.string.cleanup_type),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                ScheduledCleanupType.values().forEach { type ->
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
                        Checkbox(
                            checked = selectedTypes.contains(type),
                            onCheckedChange = { checked ->
                                selectedTypes = if (checked) {
                                    selectedTypes + type
                                } else {
                                    selectedTypes - type
                                }
                            }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = when (type) {
                                ScheduledCleanupType.CACHE -> context.getString(R.string.app_cache)
                                ScheduledCleanupType.JUNK -> context.getString(R.string.junk_files)
                                ScheduledCleanupType.DUPLICATE -> context.getString(R.string.duplicate_files)
                                ScheduledCleanupType.LARGE_FILES -> context.getString(R.string.large_files)
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text(context.getString(R.string.cancel))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (taskName.isNotBlank() && selectedTypes.isNotEmpty()) {
                            val updatedTask = task.copy(
                                name = taskName,
                                frequency = selectedFrequency,
                                time = selectedTime,
                                cleanupTypes = selectedTypes.toList(),
                                nextRun = calculateNextRun(selectedFrequency)
                            )
                            onUpdateTask(updatedTask)
                        }
                    },
                    enabled = taskName.isNotBlank() && selectedTypes.isNotEmpty()
                ) {
                    Text(context.getString(R.string.save_changes))
                }
            }
        }
    }
}
