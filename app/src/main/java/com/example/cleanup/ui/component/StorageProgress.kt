package com.example.cleanup.ui.component

import android.text.format.Formatter
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cleanup.R
import kotlin.math.min

@Composable
fun StorageProgress(
    usedSpace: Long,
    totalSpace: Long,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val progress = if (totalSpace > 0) {
        min(usedSpace.toFloat() / totalSpace.toFloat(), 1f)
    } else {
        0f
    }
    
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 圆形进度条
        val progressColor = when {
            progress > 0.9f -> Color(0xFFE53E3E) // 红色 - 空间不足
            progress > 0.7f -> Color(0xFFED8936) // 橙色 - 空间紧张
            else -> Color(0xFF38A169) // 绿色 - 空间充足
        }
        val backgroundColor = MaterialTheme.colorScheme.surfaceVariant
        Canvas(
            modifier = Modifier.size(120.dp)
        ) {
            drawStorageProgress(
                progress = progress,
                size = size,
                backgroundColor = backgroundColor,
                progressColor = progressColor
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 存储信息
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StorageInfoItem(
                label = context.getString(R.string.used),
                value = Formatter.formatFileSize(context, usedSpace),
                color = MaterialTheme.colorScheme.primary
            )

            StorageInfoItem(
                label = context.getString(R.string.available),
                value = Formatter.formatFileSize(context, totalSpace - usedSpace),
                color = MaterialTheme.colorScheme.outline
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "${(progress * 100).toInt()}% ${context.getString(R.string.used)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StorageInfoItem(
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

private fun DrawScope.drawStorageProgress(
    progress: Float,
    size: Size,
    backgroundColor: Color,
    progressColor: Color
) {
    val strokeWidth = 12.dp.toPx()
    val radius = (min(size.width, size.height) - strokeWidth) / 2
    val center = Offset(size.width / 2, size.height / 2)
    
    // 背景圆环
    drawCircle(
        color = backgroundColor,
        radius = radius,
        center = center,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    )
    
    // 进度圆环
    val sweepAngle = progress * 360f
    drawArc(
        color = progressColor,
        startAngle = -90f,
        sweepAngle = sweepAngle,
        useCenter = false,
        topLeft = Offset(
            center.x - radius,
            center.y - radius
        ),
        size = Size(radius * 2, radius * 2),
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    )
}
