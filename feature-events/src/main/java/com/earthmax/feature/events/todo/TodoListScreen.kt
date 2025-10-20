package com.earthmax.feature.events.todo

import androidx.compose.animation.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.earthmax.domain.model.DomainTodoItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoListScreen(
    eventId: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TodoListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(eventId) {
        viewModel.loadTodoItems(eventId)
    }
    
    // Environmental teal color scheme
    val tealPrimary = Color(0xFF00695C)
    val tealSecondary = Color(0xFF4DB6AC)
    val tealLight = Color(0xFFB2DFDB)
    val tealSurface = Color(0xFFE0F2F1)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Todo List",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = tealPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = tealPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = tealSurface
                )
            )
        },
        floatingActionButton = {
            if (!uiState.isCreatingTodo) {
                FloatingActionButton(
                    onClick = { viewModel.startCreatingTodo() },
                    containerColor = tealPrimary,
                    contentColor = Color.White
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Todo"
                    )
                }
            }
        },
        containerColor = tealSurface.copy(alpha = 0.3f)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Error message
            uiState.error?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.clearError() }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            // Create todo form
            AnimatedVisibility(
                visible = uiState.isCreatingTodo,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                CreateTodoForm(
                    title = uiState.newTodoTitle,
                    description = uiState.newTodoDescription,
                    isLoading = uiState.isCreatingTodo,
                    onTitleChanged = viewModel::onNewTodoTitleChanged,
                    onDescriptionChanged = viewModel::onNewTodoDescriptionChanged,
                    onCreateClick = { viewModel.createTodoItem(eventId) },
                    onCancelClick = viewModel::cancelCreatingTodo,
                    tealPrimary = tealPrimary,
                    tealSecondary = tealSecondary,
                    tealSurface = tealSurface
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Todo items list
            when {
                uiState.isLoading && uiState.todoItems.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = tealPrimary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Loading todo items...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = tealPrimary
                            )
                        }
                    }
                }
                uiState.todoItems.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = tealSecondary,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No todo items yet",
                                style = MaterialTheme.typography.headlineSmall,
                                color = tealPrimary,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tap the + button to create your first todo item",
                                style = MaterialTheme.typography.bodyMedium,
                                color = tealPrimary.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = uiState.todoItems,
                            key = { it.id }
                        ) { todoItem ->
                            TodoItemCard(
                                todoItem = todoItem,
                                currentUserId = uiState.currentUser?.id,
                                onToggleCompletion = { viewModel.toggleTodoCompletion(todoItem.id) },
                                onDelete = { viewModel.deleteTodoItem(todoItem.id) },
                                tealPrimary = tealPrimary,
                                tealSecondary = tealSecondary,
                                tealLight = tealLight
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateTodoForm(
    title: String,
    description: String,
    isCreating: Boolean,
    onTitleChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onCreateClick: () -> Unit,
    onCancelClick: () -> Unit,
    tealPrimary: Color,
    tealSecondary: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Create New Todo",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = tealPrimary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = title,
                onValueChange = onTitleChanged,
                label = { Text("Title", color = tealPrimary) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = tealPrimary,
                    focusedLabelColor = tealPrimary,
                    cursorColor = tealPrimary
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = description,
                onValueChange = onDescriptionChanged,
                label = { Text("Description (optional)", color = tealPrimary) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = tealPrimary,
                    focusedLabelColor = tealPrimary,
                    cursorColor = tealPrimary
                ),
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onCancelClick,
                    enabled = !isLoading
                ) {
                    Text("Cancel", color = tealPrimary)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onCreateClick,
                    enabled = !isLoading && title.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = tealPrimary,
                        contentColor = Color.White
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Create")
                    }
                }
            }
        }
    }
}

@Composable
private fun TodoItemCard(
    todoItem: DomainTodoItem,
    currentUserId: String?,
    onToggleCompletion: () -> Unit,
    onDelete: () -> Unit,
    tealPrimary: Color,
    tealSecondary: Color,
    tealLight: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (todoItem.isCompleted) tealLight.copy(alpha = 0.3f) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Completion checkbox
            IconButton(
                onClick = onToggleCompletion,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = if (todoItem.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = if (todoItem.isCompleted) "Mark as incomplete" else "Mark as complete",
                    tint = if (todoItem.isCompleted) tealSecondary else tealPrimary.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Todo content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = todoItem.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (todoItem.isCompleted) tealPrimary.copy(alpha = 0.6f) else tealPrimary,
                    textDecoration = if (todoItem.isCompleted) TextDecoration.LineThrough else null
                )

                todoItem.description?.let { description ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (todoItem.isCompleted) tealPrimary.copy(alpha = 0.4f) else tealPrimary.copy(alpha = 0.7f),
                        textDecoration = if (todoItem.isCompleted) TextDecoration.LineThrough else null
                    )
                }

                // Show completion status
                if (todoItem.isCompleted) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Done,
                            contentDescription = null,
                            tint = tealSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Completed",
                            style = MaterialTheme.typography.labelSmall,
                            color = tealSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Delete button (only show for items created by current user)
            if (currentUserId != null && todoItem.canBeDeletedBy(currentUserId)) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete todo",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}