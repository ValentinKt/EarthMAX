package com.earthmax.feature.events.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.earthmax.core.models.Event
import com.earthmax.core.ui.animation.bounceClickAnimation
import com.earthmax.core.ui.animation.fadeInAnimation
import com.earthmax.core.ui.animation.scaleInAnimation
import com.earthmax.core.utils.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventCard(
    event: Event,
    currentUserId: String?,
    onEventClick: (String) -> Unit,
    onJoinClick: (String) -> Unit,
    onLeaveClick: (String) -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = tween(150),
        label = "card_scale"
    )
    
    val cardElevation by animateFloatAsState(
        targetValue = if (isPressed) 2f else 6f,
        animationSpec = tween(150),
        label = "card_elevation"
    )
    
    // Determine card colors based on event status
    val isJoined = event.isJoined
    val isEventFull = event.currentParticipants >= event.maxParticipants
    
    val cardBackgroundColor by animateColorAsState(
        targetValue = when {
            isJoined -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
            isEventFull -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
            else -> MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(300),
        label = "card_background"
    )
    
    Card(
        onClick = { onEventClick(event.id) },
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .bounceClickAnimation(pressed = false)
            .scaleInAnimation(visible = true),
        elevation = CardDefaults.cardElevation(defaultElevation = cardElevation.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackgroundColor),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column {
            // Event Image with Gradient Overlay
            Box {
                event.imageUrl?.let { imageUrl ->
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Event image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
                        contentScale = ContentScale.Crop
                    )
                    
                    // Gradient overlay for better text readability
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.3f)
                                    )
                                )
                            )
                    )
                }
                
                // Status indicator overlay
                if (isJoined || isEventFull) {
                    Surface(
                        color = when {
                            isJoined -> MaterialTheme.colorScheme.primary
                            isEventFull -> MaterialTheme.colorScheme.error
                            else -> Color.Transparent
                        },
                        shape = RoundedCornerShape(bottomStart = 12.dp),
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Text(
                            text = when {
                                isJoined -> "Joined"
                                isEventFull -> "Full"
                                else -> ""
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
            
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fadeInAnimation(visible = true)
            ) {
                // Event Category with enhanced styling
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Text(
                        text = event.category.name.replace("_", " "),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                
                // Event Title with enhanced typography
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                // Event Description with improved spacing
                Text(
                    text = event.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Event Details with improved layout
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Date and Time
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(20.dp)
                                    .padding(4.dp)
                            )
                        }
                        Text(
                            text = DateUtils.formatDateTime(event.dateTime),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    // Location
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier
                                    .size(20.dp)
                                    .padding(4.dp)
                            )
                        }
                        Text(
                            text = event.location,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Bottom Row: Participants and Join Button with enhanced design
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Participants Count with progress indicator
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Group,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .padding(4.dp)
                                )
                            }
                            Text(
                                text = "${event.currentParticipants}/${event.maxParticipants}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        
                        // Progress bar for participants
                        val progress = event.currentParticipants.toFloat() / event.maxParticipants.toFloat()
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .width(80.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = when {
                                progress >= 1f -> MaterialTheme.colorScheme.error
                                progress >= 0.8f -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.primary
                            },
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                    }
                    
                    // Enhanced Join/Leave Button
                    when {
                        isJoined -> {
                            OutlinedButton(
                                onClick = { onLeaveClick(event.id) },
                                enabled = !isLoading,
                                modifier = Modifier
                                    .height(40.dp)
                                    .bounceClickAnimation(pressed = false),
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                border = ButtonDefaults.outlinedButtonBorder.copy(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.error,
                                            MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                                        )
                                    )
                                )
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                } else {
                                    Text(
                                        "Leave",
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                        isEventFull -> {
                            Button(
                                onClick = { },
                                enabled = false,
                                modifier = Modifier
                                    .height(40.dp),
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(
                                    disabledContainerColor = MaterialTheme.colorScheme.errorContainer,
                                    disabledContentColor = MaterialTheme.colorScheme.onErrorContainer
                                )
                            ) {
                                Text(
                                    "Full",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        else -> {
                            Button(
                                onClick = { onJoinClick(event.id) },
                                enabled = !isLoading,
                                modifier = Modifier
                                    .height(40.dp)
                                    .bounceClickAnimation(pressed = false),
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    Text(
                                        "Join",
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}