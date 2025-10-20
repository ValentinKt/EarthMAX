package com.earthmax.feature.events.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.earthmax.core.models.EventCategory
import com.earthmax.core.utils.DateUtils
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToMap: () -> Unit,
    onNavigateToChat: (String, String) -> Unit,
    onNavigateToTodoList: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EventDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshEvent()
    }

    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // Show snackbar or handle error
        }
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { Text("Event Details") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            },
            actions = {
                IconButton(onClick = onNavigateToMap) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "View on map"
                    )
                }
                // Chat button - only show if user is joined or is organizer
                uiState.event?.let { event ->
                    val currentUser = uiState.currentUser
                    val isOrganizer = currentUser?.id == event.organizerId
                    val isJoined = event.isJoined
                    
                    if (isOrganizer || isJoined) {
                        IconButton(
                            onClick = { 
                                onNavigateToChat(event.id, event.title)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Chat,
                                contentDescription = "Chat with participants"
                            )
                        }
                        
                        // Todo List button - only show if user is joined or is organizer
                        IconButton(
                            onClick = { 
                                onNavigateToTodoList(event.id)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "View todo list"
                            )
                        }
                    }
                }
                IconButton(onClick = { /* Share event */ }) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share event"
                    )
                }
            }
        )

        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            uiState.event?.let { event ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Event Image
                    event.imageUrl?.let { imageUrl ->
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = "Event image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Category Badge
                        AssistChip(
                            onClick = { },
                            label = {
                                Text(
                                    text = when (event.category) {
                                        EventCategory.CLEANUP -> "🧹 Cleanup"
                                        EventCategory.TREE_PLANTING -> "🌳 Tree Planting"
                                        EventCategory.RECYCLING -> "♻️ Recycling"
                                        EventCategory.EDUCATION -> "📚 Education"
                                        EventCategory.CONSERVATION -> "🦋 Conservation"
                                        EventCategory.OTHER -> "🌍 Other"
                                    }
                                )
                            }
                        )

                        // Title
                        Text(
                            text = event.title,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )

                        // Description
                        Text(
                            text = event.description,
                            style = MaterialTheme.typography.bodyLarge
                        )

                        // Event Details Card
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Date and Time
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault())
                                                .format(event.dateTime),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = SimpleDateFormat("HH:mm", Locale.getDefault())
                                                .format(event.dateTime),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // Location
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = event.location,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }

                                // Participants
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.People,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "${event.currentParticipants}/${event.maxParticipants} participants",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }

                                // Organizer
                                uiState.organizer?.let { organizer ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "Organized by",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = organizer.displayName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Action Button
                        val currentUser = uiState.currentUser
                        val isOrganizer = currentUser?.id == event.organizerId
                        val isJoined = event.isJoined
                        val isFull = event.currentParticipants >= event.maxParticipants

                        when {
                            isOrganizer -> {
                                OutlinedButton(
                                    onClick = { /* Navigate to edit event */ },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Edit Event")
                                }
                            }
                            isJoined -> {
                                OutlinedButton(
                                    onClick = viewModel::leaveEvent,
                                    enabled = !uiState.isLeaving,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (uiState.isLeaving) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.ExitToApp,
                                            contentDescription = null
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Leave Event")
                                }
                            }
                            isFull -> {
                                Button(
                                    onClick = { },
                                    enabled = false,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Event Full")
                                }
                            }
                            else -> {
                                Button(
                                    onClick = viewModel::joinEvent,
                                    enabled = !uiState.isJoining,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (uiState.isJoining) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Join Event")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}