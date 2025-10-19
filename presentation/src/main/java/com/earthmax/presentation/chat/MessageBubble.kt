package com.earthmax.presentation.chat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.earthmax.data.model.Message
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: Message,
    isCurrentUser: Boolean,
    onMessageLongPress: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val bubbleColor = if (isCurrentUser) {
        Color(0xFF0D9488) // Teal-600
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    
    val textColor = if (isCurrentUser) {
        Color.White
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    val alignment = if (isCurrentUser) {
        Alignment.CenterEnd
    } else {
        Alignment.CenterStart
    }
    
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Column(
            horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Sender name (only for other users)
            if (!isCurrentUser) {
                Text(
                    text = message.senderName,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF0D9488),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            
            // Message bubble
            Box(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = if (isCurrentUser) 16.dp else 4.dp,
                            topEnd = if (isCurrentUser) 4.dp else 16.dp,
                            bottomStart = 16.dp,
                            bottomEnd = 16.dp
                        )
                    )
                    .background(bubbleColor)
                    .combinedClickable(
                        onClick = { },
                        onLongClick = { onMessageLongPress(message.id) }
                    )
                    .padding(12.dp)
            ) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor
                )
            }
            
            // Timestamp and read status
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Text(
                    text = formatTimestamp(message.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                
                // Read indicator for current user's messages
                if (isCurrentUser) {
                    if (message.isRead) {
                        Text(
                            text = "✓✓",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF10B981) // Green for read
                        )
                    } else {
                        Text(
                            text = "✓",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60_000 -> "Just now" // Less than 1 minute
        diff < 3600_000 -> "${diff / 60_000}m ago" // Less than 1 hour
        diff < 86400_000 -> { // Less than 24 hours
            val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
            formatter.format(Date(timestamp))
        }
        diff < 604800_000 -> { // Less than 7 days
            val formatter = SimpleDateFormat("EEE HH:mm", Locale.getDefault())
            formatter.format(Date(timestamp))
        }
        else -> {
            val formatter = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
            formatter.format(Date(timestamp))
        }
    }
}