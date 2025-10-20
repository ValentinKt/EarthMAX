package com.earthmax.ui.performance

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun PerformanceMetricsWidget(
    modifier: Modifier = Modifier,
    onNavigateToDashboard: () -> Unit = {},
    viewModel: PerformanceDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.startMonitoring()
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onNavigateToDashboard() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Performance",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "View Details",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Performance Score Circle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PerformanceScoreCircle(
                    score = uiState.overallScore,
                    size = 80.dp
                )

                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    QuickMetric(
                        icon = Icons.Default.Memory,
                        value = "${uiState.memoryUsage}%",
                        label = "Memory",
                        color = getMetricColor(uiState.memoryUsage)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    QuickMetric(
                        icon = Icons.Default.Speed,
                        value = "${uiState.averageFps.toInt()}",
                        label = "FPS",
                        color = getFrameRateColor(uiState.averageFps)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    QuickMetric(
                        icon = Icons.Default.Battery6Bar,
                        value = "${uiState.batteryLevel}%",
                        label = "Battery",
                        color = getBatteryColor(uiState.batteryLevel)
                    )
                }
            }

            // Warning indicators
            if (uiState.memoryLeaks.isNotEmpty() || uiState.averageFps < 30f || uiState.memoryUsage > 80) {
                Spacer(modifier = Modifier.height(8.dp))
                WarningIndicators(
                    memoryLeaks = uiState.memoryLeaks.size,
                    lowFps = uiState.averageFps < 30f,
                    highMemory = uiState.memoryUsage > 80
                )
            }
        }
    }
}

@Composable
private fun PerformanceScoreCircle(
    score: Int,
    size: dp,
    modifier: Modifier = Modifier
) {
    val animatedScore by animateIntAsState(
        targetValue = score,
        animationSpec = tween(durationMillis = 1000, easing = EaseOutCubic),
        label = "score_animation"
    )

    val color = when {
        score >= 80 -> Color(0xFF4CAF50)
        score >= 60 -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            drawPerformanceCircle(
                score = animatedScore,
                color = color,
                size = this.size
            )
        }
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$animatedScore",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = "Score",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun DrawScope.drawPerformanceCircle(
    score: Int,
    color: Color,
    size: Size
) {
    val strokeWidth = 8.dp.toPx()
    val radius = (size.minDimension - strokeWidth) / 2
    val center = Offset(size.width / 2, size.height / 2)
    
    // Background circle
    drawCircle(
        color = color.copy(alpha = 0.2f),
        radius = radius,
        center = center,
        style = Stroke(width = strokeWidth)
    )
    
    // Progress arc
    val sweepAngle = (score / 100f) * 360f
    drawArc(
        color = color,
        startAngle = -90f,
        sweepAngle = sweepAngle,
        useCenter = false,
        topLeft = Offset(
            center.x - radius,
            center.y - radius
        ),
        size = Size(radius * 2, radius * 2),
        style = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round
        )
    )
}

@Composable
private fun QuickMetric(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = color
        )
        Spacer(modifier = Modifier.width(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun WarningIndicators(
    memoryLeaks: Int,
    lowFps: Boolean,
    highMemory: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (memoryLeaks > 0) {
            WarningChip(
                text = "$memoryLeaks Memory Leaks",
                color = Color(0xFFF44336)
            )
        }
        
        if (lowFps) {
            WarningChip(
                text = "Low FPS",
                color = Color(0xFFFF9800)
            )
        }
        
        if (highMemory) {
            WarningChip(
                text = "High Memory",
                color = Color(0xFFFF9800)
            )
        }
    }
}

@Composable
private fun WarningChip(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = color,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// Compact version for navigation bars or status areas
@Composable
fun CompactPerformanceIndicator(
    modifier: Modifier = Modifier,
    viewModel: PerformanceDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.startMonitoring()
    }

    val color = when {
        uiState.overallScore >= 80 -> Color(0xFF4CAF50)
        uiState.overallScore >= 60 -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }

    Row(
        modifier = modifier
            .background(
                color = color.copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color = color, shape = CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "${uiState.overallScore}",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}

private fun getMetricColor(value: Int): Color {
    return when {
        value <= 60 -> Color(0xFF4CAF50)
        value <= 80 -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }
}

private fun getFrameRateColor(fps: Float): Color {
    return when {
        fps >= 55f -> Color(0xFF4CAF50)
        fps >= 30f -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }
}

private fun getBatteryColor(level: Int): Color {
    return when {
        level >= 50 -> Color(0xFF4CAF50)
        level >= 20 -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }
}