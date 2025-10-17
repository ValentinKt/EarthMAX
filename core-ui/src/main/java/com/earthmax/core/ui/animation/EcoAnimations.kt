package com.earthmax.core.ui.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

object EcoAnimations {
    
    // Standard animation specs for consistency
    val fastSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessHigh
    )
    
    val mediumSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMedium
    )
    
    val slowSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow
    )
    
    val fastTween = tween<Float>(durationMillis = 150)
    val mediumTween = tween<Float>(durationMillis = 300)
    val slowTween = tween<Float>(durationMillis = 500)
    
    // Press animation scale values
    const val PRESS_SCALE = 0.96f
    const val HOVER_SCALE = 1.02f
    const val NORMAL_SCALE = 1f
    
    // Fade animation alpha values
    const val VISIBLE_ALPHA = 1f
    const val HIDDEN_ALPHA = 0f
    const val DISABLED_ALPHA = 0.6f
}

@Composable
fun Modifier.fadeInAnimation(
    visible: Boolean,
    animationSpec: AnimationSpec<Float> = EcoAnimations.mediumTween
): Modifier {
    val alpha = remember { Animatable(if (visible) 0f else 1f) }
    
    LaunchedEffect(visible) {
        alpha.animateTo(
            targetValue = if (visible) EcoAnimations.VISIBLE_ALPHA else EcoAnimations.HIDDEN_ALPHA,
            animationSpec = animationSpec
        )
    }
    
    return this.alpha(alpha.value)
}

@Composable
fun Modifier.slideInFromBottom(
    visible: Boolean,
    animationSpec: AnimationSpec<Float> = EcoAnimations.mediumSpring
): Modifier {
    val offsetY = remember { Animatable(if (visible) 100f else 0f) }
    val alpha = remember { Animatable(if (visible) 0f else 1f) }
    
    LaunchedEffect(visible) {
        if (visible) {
            // Animate both offset and alpha simultaneously
            offsetY.animateTo(0f, animationSpec)
            alpha.animateTo(EcoAnimations.VISIBLE_ALPHA, animationSpec)
        } else {
            offsetY.animateTo(100f, animationSpec)
            alpha.animateTo(EcoAnimations.HIDDEN_ALPHA, animationSpec)
        }
    }
    
    return this
        .offset { IntOffset(0, offsetY.value.roundToInt()) }
        .alpha(alpha.value)
}

@Composable
fun Modifier.slideInFromLeft(
    visible: Boolean,
    animationSpec: AnimationSpec<Float> = EcoAnimations.mediumSpring
): Modifier {
    val offsetX = remember { Animatable(if (visible) -100f else 0f) }
    val alpha = remember { Animatable(if (visible) 0f else 1f) }
    
    LaunchedEffect(visible) {
        if (visible) {
            offsetX.animateTo(0f, animationSpec)
            alpha.animateTo(EcoAnimations.VISIBLE_ALPHA, animationSpec)
        } else {
            offsetX.animateTo(-100f, animationSpec)
            alpha.animateTo(EcoAnimations.HIDDEN_ALPHA, animationSpec)
        }
    }
    
    return this
        .offset { IntOffset(offsetX.value.roundToInt(), 0) }
        .alpha(alpha.value)
}

@Composable
fun Modifier.scaleInAnimation(
    visible: Boolean,
    animationSpec: AnimationSpec<Float> = EcoAnimations.mediumSpring
): Modifier {
    val scale = remember { Animatable(if (visible) 0.8f else 1f) }
    val alpha = remember { Animatable(if (visible) 0f else 1f) }
    
    LaunchedEffect(visible) {
        if (visible) {
            scale.animateTo(EcoAnimations.NORMAL_SCALE, animationSpec)
            alpha.animateTo(EcoAnimations.VISIBLE_ALPHA, animationSpec)
        } else {
            scale.animateTo(0.8f, animationSpec)
            alpha.animateTo(EcoAnimations.HIDDEN_ALPHA, animationSpec)
        }
    }
    
    return this
        .scale(scale.value)
        .alpha(alpha.value)
}

@Composable
fun Modifier.bounceClickAnimation(
    pressed: Boolean,
    animationSpec: AnimationSpec<Float> = EcoAnimations.fastSpring
): Modifier {
    val scale = remember { Animatable(EcoAnimations.NORMAL_SCALE) }
    
    LaunchedEffect(pressed) {
        scale.animateTo(
            targetValue = if (pressed) EcoAnimations.PRESS_SCALE else EcoAnimations.NORMAL_SCALE,
            animationSpec = animationSpec
        )
    }
    
    return this.scale(scale.value)
}

@Composable
fun Modifier.pulseAnimation(
    enabled: Boolean,
    minScale: Float = 0.95f,
    maxScale: Float = 1.05f,
    durationMillis: Int = 1000
): Modifier {
    val scale = remember { Animatable(minScale) }
    
    LaunchedEffect(enabled) {
        if (enabled) {
            while (true) {
                scale.animateTo(
                    targetValue = maxScale,
                    animationSpec = tween(durationMillis / 2)
                )
                scale.animateTo(
                    targetValue = minScale,
                    animationSpec = tween(durationMillis / 2)
                )
            }
        } else {
            scale.animateTo(EcoAnimations.NORMAL_SCALE)
        }
    }
    
    return this.scale(scale.value)
}