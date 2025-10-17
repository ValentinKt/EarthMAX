package com.earthmax.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.earthmax.core.ui.theme.EcoGreen
import com.earthmax.core.ui.theme.EcoGreenDark
import com.earthmax.core.ui.theme.EcoGreenLight
import com.earthmax.core.ui.theme.PrimaryGradientEnd
import com.earthmax.core.ui.theme.PrimaryGradientStart

enum class EcoButtonType {
    PRIMARY,
    SECONDARY,
    OUTLINED,
    TEXT
}

enum class EcoButtonVariant {
    Primary,
    Secondary,
    Outline
}

@Composable
fun EcoButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    type: EcoButtonType = EcoButtonType.PRIMARY,
    enabled: Boolean = true,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit
) {
    when (type) {
        EcoButtonType.PRIMARY -> {
            Button(
                onClick = onClick,
                modifier = modifier.height(48.dp),
                enabled = enabled,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                contentPadding = contentPadding,
                content = content
            )
        }
        EcoButtonType.SECONDARY -> {
            Button(
                onClick = onClick,
                modifier = modifier.height(48.dp),
                enabled = enabled,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                ),
                contentPadding = contentPadding,
                content = content
            )
        }
        EcoButtonType.OUTLINED -> {
            OutlinedButton(
                onClick = onClick,
                modifier = modifier.height(48.dp),
                enabled = enabled,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                contentPadding = contentPadding,
                content = content
            )
        }
        EcoButtonType.TEXT -> {
            TextButton(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                contentPadding = contentPadding,
                content = content
            )
        }
    }
}

@Composable
fun EcoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    type: EcoButtonType = EcoButtonType.PRIMARY,
    enabled: Boolean = true
) {
    EcoButton(
        onClick = onClick,
        modifier = modifier,
        type = type,
        enabled = enabled
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun EcoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    variant: EcoButtonVariant = EcoButtonVariant.Primary
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // Animated scale for press feedback
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = 1000f
        ),
        label = "button_scale"
    )
    
    // Animated colors based on variant and state
    val backgroundColor by animateColorAsState(
        targetValue = when {
            !enabled -> MaterialTheme.colorScheme.surfaceVariant
            isPressed -> when (variant) {
                EcoButtonVariant.Primary -> EcoGreenDark
                EcoButtonVariant.Secondary -> MaterialTheme.colorScheme.secondaryContainer
                EcoButtonVariant.Outline -> MaterialTheme.colorScheme.surfaceVariant
            }
            else -> when (variant) {
                EcoButtonVariant.Primary -> EcoGreen
                EcoButtonVariant.Secondary -> MaterialTheme.colorScheme.secondary
                EcoButtonVariant.Outline -> Color.Transparent
            }
        },
        animationSpec = tween(150),
        label = "button_background"
    )
    
    val textColor by animateColorAsState(
        targetValue = when {
            !enabled -> MaterialTheme.colorScheme.onSurfaceVariant
            else -> when (variant) {
                EcoButtonVariant.Primary -> Color.White
                EcoButtonVariant.Secondary -> MaterialTheme.colorScheme.onSecondary
                EcoButtonVariant.Outline -> MaterialTheme.colorScheme.primary
            }
        },
        animationSpec = tween(150),
        label = "button_text_color"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .then(
                if (variant == EcoButtonVariant.Primary && enabled) {
                    Modifier.background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                PrimaryGradientStart,
                                PrimaryGradientEnd
                            )
                        )
                    )
                } else {
                    Modifier.background(backgroundColor)
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(
                horizontal = 24.dp,
                vertical = 16.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelLarge
        )
    }
}