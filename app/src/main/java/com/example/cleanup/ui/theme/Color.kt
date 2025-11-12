package com.example.cleanup.ui.theme

import androidx.compose.ui.graphics.Color

// 主色调 - 深蓝渐变
val PrimaryBlue = Color(0xFF1E3A8A)
val PrimaryBlueLight = Color(0xFF3B82F6)
val PrimaryGradient = listOf(PrimaryBlue, PrimaryBlueLight)

// 辅助色
val SuccessGreen = Color(0xFF10B981)  // 清理成功状态
val WarningOrange = Color(0xFFF59E0B) // 需要清理的项目
val ErrorRed = Color(0xFFEF4444)      // 错误状态

// 背景色 - 深色主题
val BackgroundDark = Color(0xFF0F172A)
val SurfaceDark = Color(0xFF1E293B)
val SurfaceVariantDark = Color(0xFF334155)

// 文本色
val OnBackgroundDark = Color(0xFFF8FAFC)
val OnSurfaceDark = Color(0xFFE2E8F0)
val OnSurfaceVariantDark = Color(0xFFCBD5E1)

// 透明度变体
val PrimaryBlueAlpha = PrimaryBlue.copy(alpha = 0.1f)
val SuccessGreenAlpha = SuccessGreen.copy(alpha = 0.1f)
val WarningOrangeAlpha = WarningOrange.copy(alpha = 0.1f)