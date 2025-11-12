package com.example.cleanup.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cleanup.R
import com.example.cleanup.ui.theme.PrimaryGradient
import com.example.cleanup.ui.theme.SuccessGreen
import com.example.cleanup.ui.theme.WarningOrange

@Composable
fun CircularProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    strokeWidth: Dp = 12.dp,
    showPercentage: Boolean = true,
    showText: Boolean = true,
    text: String = "存储空间",
    subText: String = "已用空间"
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1000),
        label = "progress"
    )
    
    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val canvasSize = size.toPx()
            val center = Offset(canvasSize / 2f, canvasSize / 2f)
            val radius = (canvasSize - strokeWidth.toPx()) / 2f
            
            // 背景圆环
            drawCircle(
                color = colorScheme.surfaceVariant,
                radius = radius,
                center = center,
                style = Stroke(
                    width = strokeWidth.toPx(),
                    cap = StrokeCap.Round
                )
            )
            
            // 进度圆环
            val sweepAngle = 360f * animatedProgress
            val startAngle = -90f
            
            // 根据进度选择颜色
            val progressColor = when {
                animatedProgress < 0.7f -> SuccessGreen
                animatedProgress < 0.9f -> WarningOrange
                else -> colorScheme.error
            }
            
            drawArc(
                color = progressColor,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(
                    center.x - radius,
                    center.y - radius
                ),
                size = Size(radius * 2, radius * 2),
                style = Stroke(
                    width = strokeWidth.toPx(),
                    cap = StrokeCap.Round
                )
            )
            
            // 添加渐变效果
            if (animatedProgress > 0) {
                drawArc(
                    brush = Brush.linearGradient(
                        colors = PrimaryGradient,
                        start = Offset(center.x - radius, center.y),
                        end = Offset(center.x + radius, center.y)
                    ),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(
                        center.x - radius,
                        center.y - radius
                    ),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(
                        width = strokeWidth.toPx() * 0.3f,
                        cap = StrokeCap.Round
                    )
                )
            }
        }
        
        // 中心文本
        if (showText) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (showPercentage) {
                    Text(
                        text = "${(animatedProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (subText.isNotEmpty()) {
                    Text(
                        text = subText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun StorageProgress(
    usedSpace: Long,
    totalSpace: Long,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp
) {
    val animatedProgress by animateFloatAsState(
        targetValue = if (totalSpace > 0) usedSpace.toFloat() / totalSpace.toFloat() else 0f,
        animationSpec = tween(1000),
        label = "progress"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val canvasSize = size.toPx()
            val center = Offset(canvasSize / 2f, canvasSize / 2f)
            val radius = (canvasSize - 8.dp.toPx()) / 2f
            
            // 背景圆环（深蓝灰色）
            drawCircle(
                color = Color(0xFF374151), // 深蓝灰色
                radius = radius,
                center = center,
                style = Stroke(
                    width = 8.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )
            
            // 进度圆环（橙色）
            val sweepAngle = 360f * animatedProgress
            val startAngle = -90f
            
            drawArc(
                color = Color(0xFFFF6B35), // 橙色
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(
                    center.x - radius,
                    center.y - radius
                ),
                size = Size(radius * 2, radius * 2),
                style = Stroke(
                    width = 8.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )
        }
    }
}
