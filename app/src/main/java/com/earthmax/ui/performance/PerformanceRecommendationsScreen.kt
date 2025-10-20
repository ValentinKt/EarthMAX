package com.earthmax.ui.performance

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerformanceRecommendationsScreen(
    recommendations: List<PerformanceRecommendation>,
    onApplyRecommendation: (PerformanceRecommendation) -> Unit,
    onDismissRecommendation: (PerformanceRecommendation) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf<RecommendationCategory?>(null) }
    
    Column(
        modifier = modifier
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
                text = "Performance Recommendations",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00695C)
            )
            
            IconButton(onClick = onRefresh) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh recommendations",
                    tint = Color(0xFF00695C)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Summary card
        PerformanceSummaryCard(
            recommendations = recommendations,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Category filter
        CategoryFilterRow(
            categories = RecommendationCategory.values().toList(),
            selectedCategory = selectedCategory,
            onCategorySelected = { selectedCategory = it },
            recommendationCounts = recommendations.groupBy { it.category }
                .mapValues { it.value.size }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Recommendations list
        val filteredRecommendations = if (selectedCategory != null) {
            recommendations.filter { it.category == selectedCategory }
        } else {
            recommendations
        }

        if (filteredRecommendations.isEmpty()) {
            EmptyRecommendationsState(
                selectedCategory = selectedCategory,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredRecommendations) { recommendation ->
                    RecommendationCard(
                        recommendation = recommendation,
                        onApply = { onApplyRecommendation(recommendation) },
                        onDismiss = { onDismissRecommendation(recommendation) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PerformanceSummaryCard(
    recommendations: List<PerformanceRecommendation>,
    modifier: Modifier = Modifier
) {
    val criticalCount = recommendations.count { it.priority == RecommendationPriority.CRITICAL }
    val highCount = recommendations.count { it.priority == RecommendationPriority.HIGH }
    val mediumCount = recommendations.count { it.priority == RecommendationPriority.MEDIUM }
    val lowCount = recommendations.count { it.priority == RecommendationPriority.LOW }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF00695C).copy(alpha = 0.05f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = Color(0xFF00695C),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Performance Summary",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00695C)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                PriorityCountItem(
                    count = criticalCount,
                    label = "Critical",
                    color = Color(0xFFF44336)
                )
                PriorityCountItem(
                    count = highCount,
                    label = "High",
                    color = Color(0xFFFF9800)
                )
                PriorityCountItem(
                    count = mediumCount,
                    label = "Medium",
                    color = Color(0xFFFFEB3B)
                )
                PriorityCountItem(
                    count = lowCount,
                    label = "Low",
                    color = Color(0xFF4CAF50)
                )
            }
        }
    }
}

@Composable
private fun PriorityCountItem(
    count: Int,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CategoryFilterRow(
    categories: List<RecommendationCategory>,
    selectedCategory: RecommendationCategory?,
    onCategorySelected: (RecommendationCategory?) -> Unit,
    recommendationCounts: Map<RecommendationCategory, Int>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { onCategorySelected(null) },
                label = {
                    Text("All (${recommendationCounts.values.sum()})")
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }
        
        items(categories) { category ->
            val count = recommendationCounts[category] ?: 0
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onCategorySelected(category) },
                label = {
                    Text("${category.displayName} ($count)")
                },
                leadingIcon = {
                    Icon(
                        imageVector = category.icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }
    }
}

@Composable
private fun RecommendationCard(
    recommendation: PerformanceRecommendation,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = recommendation.priority.color.copy(alpha = 0.05f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = recommendation.category.icon,
                        contentDescription = recommendation.category.displayName,
                        tint = recommendation.priority.color,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = recommendation.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = recommendation.category.displayName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Priority badge
                Surface(
                    color = recommendation.priority.color.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = recommendation.priority.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = recommendation.priority.color,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Description
            Text(
                text = recommendation.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Expected improvement
            if (recommendation.expectedImprovement.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Expected improvement: ${recommendation.expectedImprovement}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Implementation steps
            if (recommendation.implementationSteps.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Implementation:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                recommendation.implementationSteps.forEachIndexed { index, step ->
                    Text(
                        text = "${index + 1}. $step",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (recommendation.canAutoImplement) {
                    Button(
                        onClick = onApply,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00695C)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoFixHigh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Apply")
                    }
                } else {
                    OutlinedButton(
                        onClick = onApply,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("View Guide")
                    }
                }
                
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Dismiss")
                }
            }
        }
    }
}

@Composable
private fun EmptyRecommendationsState(
    selectedCategory: RecommendationCategory?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Color(0xFF4CAF50),
            modifier = Modifier.size(64.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = if (selectedCategory != null) {
                "No ${selectedCategory.displayName.lowercase()} recommendations"
            } else {
                "No recommendations available"
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Your app is performing well in this area!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

data class PerformanceRecommendation(
    val id: String,
    val title: String,
    val description: String,
    val category: RecommendationCategory,
    val priority: RecommendationPriority,
    val expectedImprovement: String = "",
    val implementationSteps: List<String> = emptyList(),
    val canAutoImplement: Boolean = false,
    val estimatedEffort: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

enum class RecommendationCategory(
    val displayName: String,
    val icon: ImageVector
) {
    MEMORY("Memory", Icons.Default.Memory),
    PERFORMANCE("Performance", Icons.Default.Speed),
    BATTERY("Battery", Icons.Default.BatteryAlert),
    NETWORK("Network", Icons.Default.NetworkCheck),
    UI("User Interface", Icons.Default.Visibility),
    DATABASE("Database", Icons.Default.Storage)
}

enum class RecommendationPriority(
    val displayName: String,
    val color: Color
) {
    CRITICAL("Critical", Color(0xFFF44336)),
    HIGH("High", Color(0xFFFF9800)),
    MEDIUM("Medium", Color(0xFFFFEB3B)),
    LOW("Low", Color(0xFF4CAF50))
}