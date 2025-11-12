package com.example.cleanup.ui.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.cleanup.ui.theme.SuccessGreen
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun ParticleEffect(
    isActive: Boolean,
    modifier: Modifier = Modifier,
    particleCount: Int = 30,
    duration: Int = 2000
) {
    var particles by remember { mutableStateOf(emptyList<Particle>()) }
    
    LaunchedEffect(isActive) {
        if (isActive) {
            particles = generateParticles(particleCount)
            delay(duration.toLong())
            particles = emptyList()
        }
    }
    
    if (particles.isNotEmpty()) {
        Box(modifier = modifier) {
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                particles.forEach { particle ->
                    drawParticle(particle)
                }
            }
        }
    }
}

@Composable
fun CleanupParticleEffect(
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    ParticleEffect(
        isActive = isActive,
        modifier = modifier,
        particleCount = 50,
        duration = 3000
    )
}

private fun generateParticles(count: Int): List<Particle> {
    return (0 until count).map { index ->
        Particle(
            id = index,
            x = Random.nextFloat() * 100f,
            y = Random.nextFloat() * 100f,
            size = Random.nextFloat() * 6f + 2f,
            alpha = Random.nextFloat() * 0.8f + 0.2f,
            velocityX = (Random.nextFloat() - 0.5f) * 4f,
            velocityY = (Random.nextFloat() - 0.5f) * 4f,
            color = SuccessGreen.copy(alpha = Random.nextFloat() * 0.8f + 0.2f)
        )
    }
}

private fun DrawScope.drawParticle(particle: Particle) {
    val center = Offset(
        particle.x * size.width / 100f,
        particle.y * size.height / 100f
    )
    
    drawCircle(
        color = particle.color,
        radius = particle.size,
        center = center,
        alpha = particle.alpha
    )
}

data class Particle(
    val id: Int,
    val x: Float,
    val y: Float,
    val size: Float,
    val alpha: Float,
    val velocityX: Float,
    val velocityY: Float,
    val color: Color
)

@Composable
fun AnimatedParticle(
    particle: Particle,
    modifier: Modifier = Modifier,
    size: Dp = 8.dp
) {
    val animatedAlpha = remember { Animatable(particle.alpha) }
    val animatedScale = remember { Animatable(1f) }
    
    LaunchedEffect(particle.id) {
        animatedAlpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(2000)
        )
        animatedScale.animateTo(
            targetValue = 0f,
            animationSpec = tween(2000)
        )
    }
    
    Canvas(
        modifier = modifier.size(size)
    ) {
        drawCircle(
            color = particle.color,
            radius = particle.size * animatedScale.value,
            center = center,
            alpha = animatedAlpha.value
        )
    }
}


