package com.earthmax.ui.performance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.earthmax.performance.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerformanceDashboard(
    viewModel: PerformanceDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.startMonitoring()
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopMonitoring()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Performance Dashboard",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            // Overall Performance Score
            PerformanceScoreCard(
                score = uiState.overallScore,
                modifier = Modifier.size(80.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Memory Performance
            item {
                PerformanceMetricCard(
                    title = "Memory Usage",
                    icon = Icons.Default.Memory,
                    value = "${uiState.memoryUsage}%",
                    status = getPerformanceStatus(uiState.memoryUsage),
                    details = listOf(
                        "Used: ${uiState.memoryUsed} MB",
                        "Available: ${uiState.memoryAvailable} MB",
                        "Max: ${uiState.memoryMax} MB"
                    ),
                    recommendations = uiState.memoryRecommendations
                )
            }

            // Frame Rate Performance
            item {
                PerformanceMetricCard(
                    title = "Frame Rate",
                    icon = Icons.Default.Speed,
                    value = "${uiState.averageFps} FPS",
                    status = getFrameRateStatus(uiState.averageFps),
                    details = listOf(
                        "Dropped frames: ${uiState.droppedFrames}",
                        "Jank frames: ${uiState.jankFrames}",
                        "Frame consistency: ${uiState.frameConsistency}%"
                    ),
                    recommendations = uiState.frameRateRecommendations
                )
            }

            // Network Performance
            item {
                PerformanceMetricCard(
                    title = "Network",
                    icon = Icons.Default.NetworkCheck,
                    value = "${uiState.averageResponseTime}ms",
                    status = getNetworkStatus(uiState.averageResponseTime),
                    details = listOf(
                        "Success rate: ${uiState.networkSuccessRate}%",
                        "Data usage: ${uiState.dataUsage} MB",
                        "Active requests: ${uiState.activeRequests}"
                    ),
                    recommendations = uiState.networkRecommendations
                )
            }

            // Battery Performance
            item {
                PerformanceMetricCard(
                    title = "Battery",
                    icon = Icons.Default.Battery6Bar,
                    value = "${uiState.batteryLevel}%",
                    status = getBatteryStatus(uiState.batteryLevel),
                    details = listOf(
                        "Temperature: ${uiState.batteryTemperature}°C",
                        "Power usage: ${uiState.powerUsage}mW",
                        "Time remaining: ${uiState.timeRemaining}"
                    ),
                    recommendations = uiState.batteryRecommendations
                )
            }

            // Database Performance
            item {
                PerformanceMetricCard(
                    title = "Database",
                    icon = Icons.Default.Storage,
                    value = "${uiState.dbPerformanceScore}%",
                    status = getPerformanceStatus(uiState.dbPerformanceScore),
                    details = listOf(
                        "Avg query time: ${uiState.avgQueryTime}ms",
                        "Slow queries: ${uiState.slowQueries}",
                        "Cache hit rate: ${uiState.cacheHitRate}%"
                    ),
                    recommendations = uiState.databaseRecommendations
                )
            }

            // UI Performance
            item {
                PerformanceMetricCard(
                    title = "UI Performance",
                    icon = Icons.Default.Visibility,
                    value = "${uiState.uiPerformanceScore}%",
                    status = getPerformanceStatus(uiState.uiPerformanceScore),
                    details = listOf(
                        "Overdraw level: ${uiState.overdrawLevel}",
                        "Layout depth: ${uiState.layoutDepth}",
                        "View count: ${uiState.viewCount}"
                    ),
                    recommendations = uiState.uiRecommendations
                )
            }

            // Memory Leaks
            if (uiState.memoryLeaks.isNotEmpty()) {
                item {
                    MemoryLeaksCard(
                        leaks = uiState.memoryLeaks,
                        onFixLeak = { leak -> viewModel.fixMemoryLeak(leak) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PerformanceScoreCard(
    score: Int,
    modifier: Modifier = Modifier
) {
    val color = when {
        score >= 80 -> Color(0xFF4CAF50) // Green
        score >= 60 -> Color(0xFFFF9800) // Orange
        else -> Color(0xFFF44336) // Red
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "$score",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = "Score",
                    style = MaterialTheme.typography.bodySmall,
                    color = color
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PerformanceMetricCard(
    title: String,
    icon: ImageVector,
    value: String,
    status: PerformanceStatus,
    details: List<String>,
    recommendations: List<String>,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = { expanded = !expanded },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = status.color,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = status.color
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand"
                    )
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                
                // Details
                details.forEach { detail ->
                    Text(
                        text = "• $detail",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }

                // Recommendations
                if (recommendations.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Recommendations:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    recommendations.forEach { recommendation ->
                        Text(
                            text = "→ $recommendation",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 1.dp, horizontal = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MemoryLeaksCard(
    leaks: List<MemoryLeak>,
    onFixLeak: (MemoryLeak) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF44336).copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Memory Leaks",
                    tint = Color(0xFFF44336),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Memory Leaks Detected (${leaks.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFF44336)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            leaks.forEach { leak ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = leak.objectName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Size: ${leak.size} bytes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Cause: ${leak.possibleCause}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Button(
                            onClick = { onFixLeak(leak) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Apply Fix")
                        }
                    }
                }
            }
        }
    }
}

private fun getPerformanceStatus(value: Int): PerformanceStatus {
    return when {
        value >= 80 -> PerformanceStatus.EXCELLENT
        value >= 60 -> PerformanceStatus.GOOD
        value >= 40 -> PerformanceStatus.FAIR
        else -> PerformanceStatus.POOR
    }
}

private fun getFrameRateStatus(fps: Float): PerformanceStatus {
    return when {
        fps >= 55f -> PerformanceStatus.EXCELLENT
        fps >= 45f -> PerformanceStatus.GOOD
        fps >= 30f -> PerformanceStatus.FAIR
        else -> PerformanceStatus.POOR
    }
}

private fun getNetworkStatus(responseTime: Long): PerformanceStatus {
    return when {
        responseTime <= 200 -> PerformanceStatus.EXCELLENT
        responseTime <= 500 -> PerformanceStatus.GOOD
        responseTime <= 1000 -> PerformanceStatus.FAIR
        else -> PerformanceStatus.POOR
    }
}

private fun getBatteryStatus(level: Int): PerformanceStatus {
    return when {
        level >= 80 -> PerformanceStatus.EXCELLENT
        level >= 50 -> PerformanceStatus.GOOD
        level >= 20 -> PerformanceStatus.FAIR
        else -> PerformanceStatus.POOR
    }
}

enum class PerformanceStatus(val color: Color) {
    EXCELLENT(Color(0xFF4CAF50)),
    GOOD(Color(0xFF8BC34A)),
    FAIR(Color(0xFFFF9800)),
    POOR(Color(0xFFF44336))
}

data class MemoryLeak(
    val objectName: String,
    val size: Long,
    val possibleCause: String,
    val recommendation: String
)