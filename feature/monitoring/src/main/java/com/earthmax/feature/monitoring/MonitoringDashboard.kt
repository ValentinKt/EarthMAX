package com.earthmax.feature.monitoring

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.earthmax.core.monitoring.LogFilterManager
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
                containerColor = Color(0xFF008080) // Teal
            )
        )
        
        // Tab Row
        TabRow(
            selectedTabIndex = selectedTab.ordinal,
            containerColor = Color(0xFF20B2AA), // Light Sea Green
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
        uiState.realtimeMetrics?.let { metrics ->
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickStatCard(
                        title = "Total Logs",
                        value = uiState.historicalLogs.size.toString(),
                        icon = Icons.Default.List,
                        modifier = Modifier.weight(1f)
                    )
                    QuickStatCard(
                        title = "Errors",
                        value = metrics.errorCount.toString(),
                        icon = Icons.Default.Error,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickStatCard(
                        title = "Network Requests",
                        value = metrics.networkRequests.toString(),
                        icon = Icons.Default.NetworkCheck,
                        modifier = Modifier.weight(1f)
                    )
                    QuickStatCard(
                        title = "Avg Response",
                        value = "${metrics.averageResponseTime.toInt()}ms",
                        icon = Icons.Default.Speed,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        
        // Recent Critical Issues
        uiState.systemHealth?.let { health ->
            if (health.criticalIssues.isNotEmpty()) {
                item {
                    CriticalIssuesCard(health.criticalIssues)
                }
            }
        }
        
        // Performance Chart Placeholder
        uiState.realtimeMetrics?.let { metrics ->
            item {
                PerformanceChartCard(metrics.hourlyMetrics)
            }
        }
    }
}

@Composable
private fun PerformanceTab(uiState: MonitoringViewModel.MonitoringUiState) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Performance Metrics Summary
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFE0F2F1) // Very light teal
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "Performance Summary",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF004D40) // Dark teal
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    uiState.realtimeMetrics?.let { metrics ->
                        PerformanceMetricRow(
                            "Average Response Time",
                            "${metrics.averageResponseTime.toInt()}ms"
                        )
                        PerformanceMetricRow(
                            "Total Network Requests",
                            metrics.networkRequests.toString()
                        )
                        PerformanceMetricRow(
                            "Network Errors",
                            metrics.networkErrors.toString()
                        )
                        PerformanceMetricRow(
                            "User Actions",
                            metrics.userActions.toString()
                        )
                        PerformanceMetricRow(
                            "Business Events",
                            metrics.businessEvents.toString()
                        )
                    }
                }
            }
        }
        
        // Slowest Operations
        uiState.realtimeMetrics?.let { metrics ->
            if (metrics.slowestOperations.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFF3E0) // Light orange
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            "Slowest Operations",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE65100) // Dark orange
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        metrics.slowestOperations.take(5).forEach { metric ->
                            SlowOperationItem(metric)
                        }
                    }
                }
            }
        }
        
        // Performance by Tag
        if (metrics.performanceByTag.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF3E5F5) // Light purple
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            "Performance by Tag",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4A148C) // Dark purple
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        metrics.performanceByTag.entries
                            .sortedByDescending { it.value }
                            .take(10)
                            .forEach { (tag, avgTime) ->
                                PerformanceMetricRow(tag, "${avgTime.toInt()}ms")
                            }
                    }
                }
            }
        }
    }
}

@Composable
private fun LogsTab(
    uiState: MonitoringViewModel.MonitoringUiState,
    viewModel: MonitoringViewModel
) {
    var showFilters by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Filter Controls
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFE0F2F1) // Very light teal
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
                        "Logs (${uiState.filterStats?.filteredLogs ?: 0}/${uiState.filterStats?.totalLogs ?: 0})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Row {
                        IconButton(onClick = { showFilters = !showFilters }) {
                            Icon(
                                if (showFilters) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Toggle Filters"
                            )
                        }
                        IconButton(onClick = { viewModel.exportLogs() }) {
                            Icon(Icons.Default.Download, contentDescription = "Export")
                        }
                    }
                }
                
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { 
                        searchQuery = it
                        viewModel.setSearchQuery(it)
                    },
                    label = { Text("Search logs...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // Filter Presets
                AnimatedVisibility(
                    visible = showFilters,
                    enter = fadeIn(animationSpec = tween(300)),
                    exit = fadeOut(animationSpec = tween(300))
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            "Quick Filters",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            items(listOf(
                                "errors_only" to "Errors Only",
                                "warnings_and_errors" to "Warnings & Errors",
                                "network_logs" to "Network",
                                "user_actions" to "User Actions",
                                "performance_issues" to "Performance",
                                "last_hour" to "Last Hour"
                            )) { (preset, label) ->
                                FilterChip(
                                    onClick = { viewModel.applyFilterPreset(preset) },
                                    label = { Text(label) },
                                    selected = false
                                )
                            }
                        }
                        
                        // Level Filters
                        Text(
                            "Log Levels",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            items(Logger.Level.values()) { level ->
                                val isSelected = viewModel.isLevelSelected(level)
                                FilterChip(
                                    onClick = { viewModel.toggleLogLevel(level) },
                                    label = { Text(level.name) },
                                    selected = isSelected,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = when (level) {
                                            Logger.Level.ERROR -> Color(0xFFFFEBEE)
                                        Logger.Level.WARNING -> Color(0xFFFFF3E0)
                                        Logger.Level.INFO -> Color(0xFFE3F2FD)
                                        Logger.Level.DEBUG -> Color(0xFFF3E5F5)
                                        }
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Logs List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.filteredLogs) { logEntry ->
                LogEntryCard(logEntry)
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
                HealthStatusCard(health)
            }
        }
        
        // Health Metrics
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFE8F5E8) // Light green
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "Health Metrics",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32) // Dark green
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    HealthMetricRow(
                        "Error Rate",
                        "${String.format("%.2f", health.errorRate)}%",
                        health.errorRate > 5.0
                    )
                    HealthMetricRow(
                        "Average Response Time",
                        "${health.averageResponseTime.toInt()}ms",
                        health.averageResponseTime > 3000
                    )
                    HealthMetricRow(
                        "Slow Operations",
                        health.slowOperationsCount.toString(),
                        health.slowOperationsCount > 10
                    )
                }
            }
        }
        
        // Critical Issues
        if (health.criticalIssues.isNotEmpty()) {
            item {
                CriticalIssuesCard(health.criticalIssues)
            }
        }
        
        // System Recommendations
        item {
            SystemRecommendationsCard(health, uiState)
        }
    }
}

@Composable
private fun HealthStatusCard(systemHealth: PerformanceMetricsCollector.SystemHealth) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (systemHealth.status) {
                PerformanceMetricsCollector.HealthStatus.HEALTHY -> Color(0xFFE8F5E8)
                PerformanceMetricsCollector.HealthStatus.WARNING -> Color(0xFFFFF3E0)
                PerformanceMetricsCollector.HealthStatus.CRITICAL -> Color(0xFFFFEBEE)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (systemHealth.status) {
                    PerformanceMetricsCollector.HealthStatus.HEALTHY -> Icons.Default.CheckCircle
                    PerformanceMetricsCollector.HealthStatus.WARNING -> Icons.Default.Warning
                    PerformanceMetricsCollector.HealthStatus.CRITICAL -> Icons.Default.Error
                },
                contentDescription = null,
                tint = when (systemHealth.status) {
                    PerformanceMetricsCollector.HealthStatus.HEALTHY -> Color(0xFF4CAF50)
                    PerformanceMetricsCollector.HealthStatus.WARNING -> Color(0xFFFF9800)
                    PerformanceMetricsCollector.HealthStatus.CRITICAL -> Color(0xFFF44336)
                },
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    "System Health: ${systemHealth.status.name}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Last updated: ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun QuickStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color = Color(0xFF008080),
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE0F2F1)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = color
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
private fun CriticalIssuesCard(issues: List<String>) {
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
                    tint = Color(0xFFF44336),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Critical Issues",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF44336)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            issues.forEach { issue ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        "• ",
                        color = Color(0xFFF44336),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        issue,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun PerformanceChartCard(hourlyMetrics: Map<String, Long>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE0F2F1)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "24-Hour Activity",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF004D40)
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            // Simple bar chart representation
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(hourlyMetrics.entries.toList()) { (hour, count) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val maxCount = hourlyMetrics.values.maxOrNull() ?: 1
                        val height = ((count.toFloat() / maxCount.toFloat()) * 60).coerceAtLeast(4f)
                        
                        Box(
                            modifier = Modifier
                                .width(16.dp)
                                .height(height.dp)
                                .background(
                                    Color(0xFF008080),
                                    RoundedCornerShape(2.dp)
                                )
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            hour,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 8.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LogEntryCard(logEntry: Logger.LogEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (logEntry.level) {
                Logger.Level.ERROR -> Color(0xFFFFEBEE)
                Logger.Level.WARNING -> Color(0xFFFFF3E0)
                Logger.Level.INFO -> Color(0xFFE3F2FD)
                Logger.Level.DEBUG -> Color(0xFFF3E5F5)
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
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (logEntry.level) {
                            Logger.Level.ERROR -> Icons.Default.Error
                            Logger.Level.WARNING -> Icons.Default.Warning
                            Logger.Level.INFO -> Icons.Default.Info
                            Logger.Level.DEBUG -> Icons.Default.BugReport
                        },
                        contentDescription = null,
                        tint = when (logEntry.level) {
                            Logger.Level.ERROR -> Color(0xFFF44336)
                            Logger.Level.WARNING -> Color(0xFFFF9800)
                            Logger.Level.INFO -> Color(0xFF2196F3)
                            Logger.Level.DEBUG -> Color(0xFF9C27B0)
                        },
                        modifier = Modifier.size(16.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        logEntry.tag,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Text(
                    SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(logEntry.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                logEntry.message,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            
            if (logEntry.exception != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Exception: ${logEntry.exception}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFF44336),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            if (logEntry.metadata.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Metadata: ${logEntry.metadata.entries.joinToString(", ") { "${it.key}=${it.value}" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun PerformanceMetricRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
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
private fun SlowOperationItem(metric: Logger.PerformanceMetric) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                metric.operation,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                metric.tag,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Text(
            "${metric.duration.toInt()}ms",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFE65100)
        )
    }
}

@Composable
private fun HealthMetricRow(label: String, value: String, isWarning: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
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
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (isWarning) Color(0xFFFF9800) else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun SystemRecommendationsCard(
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
    if (uiState.aggregatedMetrics.networkErrors > 10) {
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
}