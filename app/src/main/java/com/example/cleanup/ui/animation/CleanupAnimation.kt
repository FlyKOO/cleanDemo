package com.example.cleanup.ui.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun rememberCleanupAnimation(
    isActive: Boolean = false
): CleanupAnimationState {
    val density = LocalDensity.current
    val infiniteTransition = rememberInfiniteTransition()
    
    // 旋转动画
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000)
        ),
    )
    
    // 缩放动画
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000)
        ),
    )
    
    // 脉冲动画
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(800)
        ),
    )
    
    return CleanupAnimationState(
        rotation = if (isActive) rotation else 0f,
        scale = if (isActive) scale else 1f,
        pulse = if (isActive) pulse else 1f
    )
}

data class CleanupAnimationState(
    val rotation: Float = 0f,
    val scale: Float = 1f,
    val pulse: Float = 1f
)

@Composable
fun rememberParticleAnimation(
    particleCount: Int = 20,
    isActive: Boolean = false
): List<ParticleState> {
    var particles by remember { mutableStateOf(emptyList<ParticleState>()) }
    
    LaunchedEffect(isActive) {
        if (isActive) {
            particles = generateParticles(particleCount)
            delay(2000) // 动画持续时间
            particles = emptyList()
        }
    }
    
    return particles
}

private fun generateParticles(count: Int): List<ParticleState> {
    return (0 until count).map { index ->
        ParticleState(
            id = index,
            x = (0..100).random().toFloat(),
            y = (0..100).random().toFloat(),
            size = (2..8).random().toFloat(),
            alpha = 1f,
            velocityX = (-2..2).random().toFloat(),
            velocityY = (-2..2).random().toFloat()
        )
    }
}

data class ParticleState(
    val id: Int,
    val x: Float,
    val y: Float,
    val size: Float,
    val alpha: Float,
    val velocityX: Float,
    val velocityY: Float
)

@Composable
fun rememberProgressAnimation(
    targetProgress: Float,
    duration: Int = 1000
): Float {
    val animatedProgress = remember { Animatable(0f) }
    
    LaunchedEffect(targetProgress) {
        animatedProgress.animateTo(
            targetValue = targetProgress,
            animationSpec = tween(duration)
        )
    }
    
    return animatedProgress.value
}

@Composable
fun rememberShakeAnimation(
    isShaking: Boolean = false
): Offset {
    val shakeOffset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    
    LaunchedEffect(isShaking) {
        if (isShaking) {
            repeat(3) {
                shakeOffset.animateTo(
                    targetValue = Offset(10f, 0f),
                    animationSpec = tween(50)
                )
                shakeOffset.animateTo(
                    targetValue = Offset(-10f, 0f),
                    animationSpec = tween(50)
                )
            }
            shakeOffset.animateTo(
                targetValue = Offset.Zero,
                animationSpec = tween(100)
            )
        }
    }
    
    return shakeOffset.value
}


