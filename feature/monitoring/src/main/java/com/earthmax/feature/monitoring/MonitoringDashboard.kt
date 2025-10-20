package com.earthmax.feature.monitoring

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.earthmax.core.monitoring.PerformanceMetricsCollector
import com.earthmax.core.utils.Logger
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitoringDashboard(
    viewModel: MonitoringViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val timeRange by viewModel.timeRange.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    
    val tabs = listOf("Overview", "Performance", "Logs", "Health")
    
    LaunchedEffect(Unit) {
        viewModel.refreshData()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        TopAppBar(
            title = { 
                Text(
                    "Monitoring Dashboard",
                    fontWeight = FontWeight.Bold
                ) 
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = { viewModel.refreshData() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
                IconButton(onClick = { viewModel.exportPerformanceData() }) {
                    Icon(Icons.Default.FileDownload, contentDescription = "Export")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFF1976D2),
                titleContentColor = Color.White,
                navigationIconContentColor = Color.White,
                actionIconContentColor = Color.White
            )
        )
        
        // Tab Row
        TabRow(
            selectedTabIndex = selectedTab.ordinal,
            containerColor = Color(0xFF1976D2),
            contentColor = Color.White
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab.ordinal == index,
                    onClick = { 
                        viewModel.selectTab(MonitoringViewModel.MonitoringTab.values()[index])
                    },
                    text = { Text(title) }
                )
            }
        }
        
        // Content based on selected tab
        when (selectedTab) {
            MonitoringViewModel.MonitoringTab.OVERVIEW -> OverviewTab(uiState)
            MonitoringViewModel.MonitoringTab.PERFORMANCE -> PerformanceTab(uiState)
            MonitoringViewModel.MonitoringTab.LOGS -> LogsTab(uiState, viewModel)
            MonitoringViewModel.MonitoringTab.HEALTH -> HealthTab(uiState)
        }
    }
}

@Composable
private fun OverviewTab(
    uiState: MonitoringViewModel.MonitoringUiState
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // System Health Card
        uiState.systemHealth?.let { health ->
            item {
                HealthStatusCard(health)
            }
        }
        
        // Quick Stats Row
        uiState.realtimeData?.let { metrics ->
            item {
                QuickStatsRow(metrics)
            }
        }
        
        // Recent Logs
        if (uiState.recentLogs.isNotEmpty()) {
            item {
                RecentLogsCard(uiState.recentLogs.take(5))
            }
        }
        
        // Performance Recommendations
        uiState.systemHealth?.let { health ->
            item {
                SystemRecommendationsCard(health, uiState)
            }
        }
    }
}

@Composable
private fun PerformanceTab(
    uiState: MonitoringViewModel.MonitoringUiState
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Performance Metrics
        uiState.performanceMetrics?.let { metrics ->
            item {
                PerformanceMetricsCard(metrics)
            }
        }
        
        // Slow Operations
        if (uiState.slowOperations.isNotEmpty()) {
            item {
                SlowOperationsCard(uiState.slowOperations)
            }
        }
        
        // Memory Usage
        uiState.systemHealth?.let { health ->
            item {
                MemoryUsageCard(health)
            }
        }
    }
}

@Composable
private fun LogsTab(
    uiState: MonitoringViewModel.MonitoringUiState,
    viewModel: MonitoringViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Log Level Filter
        LogLevelFilter(
            selectedLevel = uiState.selectedLogLevel,
            onLevelSelected = { level -> 
                level?.let { viewModel.filterLogsByLevel(it) }
            }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Logs List
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.filteredLogs) { log ->
                LogEntryCard(log)
            }
        }
    }
}

@Composable
private fun HealthTab(
    uiState: MonitoringViewModel.MonitoringUiState
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // System Health Overview
        uiState.systemHealth?.let { health ->
            item {
                SystemHealthOverviewCard(health)
            }
        }
        
        // Health Metrics
        uiState.realtimeMetrics?.let { metrics ->
            item {
                HealthMetricsCard(metrics)
            }
        }
        
        // Health Alerts
        if (uiState.healthAlerts.isNotEmpty()) {
            item {
                HealthAlertsCard(uiState.healthAlerts)
            }
        }
    }
}

@Composable
private fun HealthStatusCard(
    systemHealth: PerformanceMetricsCollector.SystemHealth
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                systemHealth.errorRate > 10.0 -> Color(0xFFFFEBEE)
                systemHealth.errorRate > 5.0 -> Color(0xFFFFF3E0)
                else -> Color(0xFFE8F5E8)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    when {
                        systemHealth.errorRate > 10.0 -> Icons.Default.Error
                        systemHealth.errorRate > 5.0 -> Icons.Default.Warning
                        else -> Icons.Default.CheckCircle
                    },
                    contentDescription = null,
                    tint = when {
                        systemHealth.errorRate > 10.0 -> Color(0xFFD32F2F)
                        systemHealth.errorRate > 5.0 -> Color(0xFFF57C00)
                        else -> Color(0xFF388E3C)
                    },
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "System Health",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Error Rate",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "${String.format("%.1f", systemHealth.errorRate)}%",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column {
                    Text(
                        "Avg Response",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "${systemHealth.averageResponseTime}ms",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column {
                    Text(
                        "Memory Usage",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "${systemHealth.memoryUsagePercent}%",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickStatsRow(
    metrics: MonitoringViewModel.RealtimeData
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            title = "Active Users",
            value = metrics.metrics.activeUsers.toString(),
            icon = Icons.Default.People,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            title = "Requests/min",
            value = metrics.metrics.requestsPerMinute.toString(),
            icon = Icons.Default.TrendingUp,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            title = "Errors",
            value = metrics.metrics.networkErrors.toString(),
            icon = Icons.Default.Error,
            modifier = Modifier.weight(1f),
            isWarning = metrics.metrics.networkErrors > 10
        )
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    isWarning: Boolean = false
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isWarning) Color(0xFFFFEBEE) else Color(0xFFF5F5F5)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (isWarning) Color(0xFFD32F2F) else Color(0xFF1976D2),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isWarning) Color(0xFFD32F2F) else Color.Black
            )
            Text(
                title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RecentLogsCard(
    logs: List<Logger.LogEntry>
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.List,
                    contentDescription = null,
                    tint = Color(0xFF1976D2),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Recent Logs",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            logs.forEach { log ->
                LogEntryItem(log)
                if (log != logs.last()) {
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
    }
}

@Composable
private fun LogEntryItem(
    log: Logger.LogEntry
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Icon(
            when (log.level) {
                Logger.Level.ERROR -> Icons.Default.Error
                Logger.Level.WARNING -> Icons.Default.Warning
                Logger.Level.INFO -> Icons.Default.Info
                Logger.Level.DEBUG -> Icons.Default.Circle
            },
            contentDescription = null,
            tint = when (log.level) {
                Logger.Level.ERROR -> Color(0xFFD32F2F)
                Logger.Level.WARNING -> Color(0xFFF57C00)
                Logger.Level.INFO -> Color(0xFF1976D2)
                Logger.Level.DEBUG -> Color.Gray
            },
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                log.message,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(log.timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PerformanceMetricsCard(
    metrics: PerformanceMetricsCollector.AggregatedMetrics
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Speed,
                    contentDescription = null,
                    tint = Color(0xFF1976D2),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Performance Metrics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricRow("Total Logs", "${metrics.totalLogs}")
                MetricRow("Error Count", "${metrics.errorCount}")
                MetricRow("Network Requests", "${metrics.networkRequests}")
                MetricRow("Avg Response Time", "${metrics.averageResponseTime.toInt()}ms")
            }
        }
    }
}

@Composable
private fun MetricRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SlowOperationsCard(
    operations: List<MonitoringViewModel.SlowOperation>
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.HourglassEmpty,
                    contentDescription = null,
                    tint = Color(0xFFF57C00),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Slow Operations",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            operations.forEach { operation ->
                SlowOperationItem(operation)
                if (operation != operations.last()) {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
    }
}

@Composable
fun SlowOperationItem(
    operation: MonitoringViewModel.SlowOperation
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                operation.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Text(
                "${operation.duration}ms",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF57C00)
            )
        }
        if (operation.details.isNotEmpty()) {
            Text(
                operation.details,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun HealthMetricRow(
    label: String,
    value: String,
    isWarning: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium
        )
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isWarning) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFF57C00),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (isWarning) Color(0xFFF57C00) else Color.Black
            )
        }
    }
}

@Composable
fun SystemRecommendationsCard(
    systemHealth: PerformanceMetricsCollector.SystemHealth,
    uiState: MonitoringViewModel.MonitoringUiState
) {
    val recommendations = mutableListOf<String>()
    
    if (systemHealth.errorRate > 5.0) {
        recommendations.add("Consider investigating frequent error patterns")
    }
    if (systemHealth.averageResponseTime > 3000) {
        recommendations.add("Optimize slow network operations")
    }
    if (uiState.realtimeMetrics?.networkErrors ?: 0 > 10) {
        recommendations.add("Check network connectivity and API endpoints")
    }
    if (systemHealth.slowOperationsCount > 10) {
        recommendations.add("Profile and optimize slow operations")
    }
    
    if (recommendations.isEmpty()) {
        recommendations.add("System is performing well!")
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE3F2FD)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = Color(0xFF1976D2),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Recommendations",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1976D2)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            recommendations.forEach { recommendation ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        "• ",
                        color = Color(0xFF1976D2),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        recommendation,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}@Composable
fun HealthAlertsCard(alerts: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFEBEE)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFD32F2F),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Health Alerts",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD32F2F)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            alerts.forEach { alert ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        "• ",
                        color = Color(0xFFD32F2F),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        alert,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}@Composable
fun HealthMetricsCard(
    metrics: PerformanceMetricsCollector.AggregatedMetrics,
    systemHealth: PerformanceMetricsCollector.SystemHealth? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE8F5E8)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.MonitorHeart,
                    contentDescription = null,
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Health Metrics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Total Logs",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Text(
                        "${metrics.totalLogs}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column {
                    Text(
                        "Errors",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Text(
                        "${metrics.errorCount}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (metrics.errorCount > 0) Color.Red else Color.Green
                    )
                }
                Column {
                    Text(
                        "Warnings",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Text(
                        "${metrics.warningCount}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (metrics.warningCount > 0) Color(0xFFFF9800) else Color.Green
                    )
                }
            }
        }
    }
}@Composable
fun LogEntryCard(log: Logger.LogEntry) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (log.level) {
                Logger.Level.ERROR -> MaterialTheme.colorScheme.errorContainer
                Logger.Level.WARNING -> MaterialTheme.colorScheme.tertiaryContainer
                Logger.Level.INFO -> MaterialTheme.colorScheme.primaryContainer
                Logger.Level.DEBUG -> MaterialTheme.colorScheme.secondaryContainer
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = log.level.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = when (log.level) {
                        Logger.Level.ERROR -> MaterialTheme.colorScheme.onErrorContainer
                        Logger.Level.WARNING -> MaterialTheme.colorScheme.onTertiaryContainer
                        Logger.Level.INFO -> MaterialTheme.colorScheme.onPrimaryContainer
                        Logger.Level.DEBUG -> MaterialTheme.colorScheme.onSecondaryContainer
                    },
                    modifier = Modifier
                        .background(
                            color = when (log.level) {
                                Logger.Level.ERROR -> MaterialTheme.colorScheme.error
                                Logger.Level.WARNING -> MaterialTheme.colorScheme.tertiary
                                Logger.Level.INFO -> MaterialTheme.colorScheme.primary
                                Logger.Level.DEBUG -> MaterialTheme.colorScheme.secondary
                            },
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
                
                Text(
                    text = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = log.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            if (log.tag.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tag: ${log.tag}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}@Composable
fun SystemHealthOverviewCard(systemHealth: PerformanceMetricsCollector.SystemHealth) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (systemHealth.status) {
                PerformanceMetricsCollector.HealthStatus.HEALTHY -> MaterialTheme.colorScheme.primaryContainer
                PerformanceMetricsCollector.HealthStatus.WARNING -> MaterialTheme.colorScheme.tertiaryContainer
                PerformanceMetricsCollector.HealthStatus.CRITICAL -> MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "System Health",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = systemHealth.status.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = when (systemHealth.status) {
                        PerformanceMetricsCollector.HealthStatus.HEALTHY -> MaterialTheme.colorScheme.onPrimaryContainer
                        PerformanceMetricsCollector.HealthStatus.WARNING -> MaterialTheme.colorScheme.onTertiaryContainer
                        PerformanceMetricsCollector.HealthStatus.CRITICAL -> MaterialTheme.colorScheme.onErrorContainer
                    },
                    modifier = Modifier
                        .background(
                            color = when (systemHealth.status) {
                                PerformanceMetricsCollector.HealthStatus.HEALTHY -> MaterialTheme.colorScheme.primary
                                PerformanceMetricsCollector.HealthStatus.WARNING -> MaterialTheme.colorScheme.tertiary
                                PerformanceMetricsCollector.HealthStatus.CRITICAL -> MaterialTheme.colorScheme.error
                            },
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Memory Usage
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Memory Usage",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${systemHealth.memoryUsagePercent.toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            LinearProgressIndicator(
                progress = (systemHealth.memoryUsagePercent / 100).toFloat(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                color = when {
                    systemHealth.memoryUsagePercent > 80 -> MaterialTheme.colorScheme.error
                    systemHealth.memoryUsagePercent > 60 -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.primary
                }
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // CPU Usage
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "CPU Usage",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${systemHealth.cpuUsage.toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            LinearProgressIndicator(
                progress = (systemHealth.cpuUsage / 100).toFloat(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                color = when {
                    systemHealth.cpuUsage > 80 -> MaterialTheme.colorScheme.error
                    systemHealth.cpuUsage > 60 -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.primary
                }
            )
        }
    }
}
@Composable
fun MemoryUsageCard(systemHealth: PerformanceMetricsCollector.SystemHealth) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Memory Usage",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Used Memory",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${systemHealth.memoryUsagePercent.toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            LinearProgressIndicator(
                progress = (systemHealth.memoryUsagePercent / 100).toFloat(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                color = when {
                    systemHealth.memoryUsagePercent > 80 -> MaterialTheme.colorScheme.error
                    systemHealth.memoryUsagePercent > 60 -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.primary
                }
            )
            
            Text(
                text = "Memory: ${String.format("%.1f", systemHealth.memoryUsage)} MB",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun LogLevelFilter(
    selectedLevel: Logger.Level?,
    onLevelSelected: (Logger.Level?) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Filter by Log Level",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        onClick = { onLevelSelected(null) },
                        label = { Text("All") },
                        selected = selectedLevel == null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
                
                items(Logger.Level.values()) { level ->
                    FilterChip(
                        onClick = { onLevelSelected(level) },
                        label = { Text(level.name) },
                        selected = selectedLevel == level,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = when (level) {
                                Logger.Level.ERROR -> MaterialTheme.colorScheme.error
                                Logger.Level.WARNING -> MaterialTheme.colorScheme.tertiary
                                Logger.Level.INFO -> MaterialTheme.colorScheme.primary
                                Logger.Level.DEBUG -> MaterialTheme.colorScheme.secondary
                            },
                            selectedLabelColor = when (level) {
                                Logger.Level.ERROR -> MaterialTheme.colorScheme.onError
                                Logger.Level.WARNING -> MaterialTheme.colorScheme.onTertiary
                                Logger.Level.INFO -> MaterialTheme.colorScheme.onPrimary
                                Logger.Level.DEBUG -> MaterialTheme.colorScheme.onSecondary
                            }
                        )
                    )
                }
            }
        }
    }
}