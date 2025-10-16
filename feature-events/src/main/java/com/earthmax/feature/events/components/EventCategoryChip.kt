package com.earthmax.feature.events.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.earthmax.core.models.EventCategory

@Composable
fun EventCategoryChip(
    category: EventCategory,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Text(
                text = when (category) {
                    EventCategory.CLEANUP -> "🧹 Cleanup"
                    EventCategory.TREE_PLANTING -> "🌳 Tree Planting"
                    EventCategory.RECYCLING -> "♻️ Recycling"
                    EventCategory.EDUCATION -> "📚 Education"
                    EventCategory.CONSERVATION -> "🦋 Conservation"
                    EventCategory.OTHER -> "🌍 Other"
                }
            )
        },
        modifier = modifier
    )
}