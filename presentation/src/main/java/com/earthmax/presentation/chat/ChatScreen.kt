package com.earthmax.presentation.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.earthmax.data.model.Message

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    eventId: String,
    eventTitle: String,
    userId: String,
    userName: String,
    userAvatarUrl: String?,
    onNavigateBack: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val keyboardController = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()
    
    // Initialize chat when screen loads
    LaunchedEffect(eventId, userId) {
        viewModel.initializeChat(eventId, userId, userName, userAvatarUrl)
    }
    
    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }
    
    // Show error snackbar
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // You can show a snackbar here if needed
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = eventTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    color = if (uiState.isConnected) Color(0xFF10B981) else Color.Gray,
                                    shape = RoundedCornerShape(50)
                                )
                        )
                        Text(
                            text = if (uiState.isConnected) "Connected" else "Connecting...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFF0D9488), // Teal-600
                titleContentColor = Color.White,
                navigationIconContentColor = Color.White
            )
        )
        
        // Messages List
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (uiState.isLoading && uiState.messages.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF0D9488)
                    )
                }
            } else if (uiState.messages.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "💬",
                            style = MaterialTheme.typography.displayMedium
                        )
                        Text(
                            text = "No messages yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Start the conversation!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = uiState.messages,
                        key = { it.id }
                    ) { message ->
                        MessageBubble(
                            message = message,
                            isCurrentUser = message.senderId == userId,
                            onMessageLongPress = { messageId ->
                                // Handle long press (e.g., show context menu)
                            }
                        )
                    }
                }
            }
        }
        
        // Message Input
        MessageInput(
            messageText = uiState.messageText,
            onMessageTextChange = viewModel::updateMessageText,
            onSendMessage = {
                viewModel.sendMessage()
                keyboardController?.hide()
            },
            enabled = uiState.isConnected && !uiState.isLoading
        )
    }
}

@Composable
private fun MessageInput(
    messageText: String,
    onMessageTextChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    enabled: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = messageText,
                onValueChange = onMessageTextChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        text = "Type a message...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                enabled = enabled,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF0D9488),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                maxLines = 4
            )
            
            FloatingActionButton(
                onClick = onSendMessage,
                modifier = Modifier.size(48.dp),
                containerColor = Color(0xFF0D9488),
                contentColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 8.dp
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send message"
                )
            }
        }
    }
}