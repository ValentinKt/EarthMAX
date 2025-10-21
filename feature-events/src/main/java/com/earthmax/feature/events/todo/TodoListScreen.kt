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
import com.earthmax.domain.model.canBeDeletedBy

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
                    containerColor = tealSurface,
                    titleContentColor = tealPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Error banner
            AnimatedVisibility(visible = uiState.error != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = uiState.error ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.weight(1f))
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
                    isCreating = uiState.isCreatingTodo,
                    onTitleChanged = viewModel::onNewTodoTitleChanged,
                    onDescriptionChanged = viewModel::onNewTodoDescriptionChanged,
                    onCreateClick = { viewModel.createTodoItem(eventId) },
                    onCancelClick = viewModel::cancelCreatingTodo,
                    tealPrimary = tealPrimary,
                    tealSecondary = tealSecondary
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
                                imageVector = Icons.Default.Inbox,
                                contentDescription = null,
                                tint = tealSecondary,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No todo items yet",
                                style = MaterialTheme.typography.bodyLarge,
                                color = tealPrimary
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.todoItems) { todoItem ->
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
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(16.dp)
    ) {
        Text(
            text = "Create New Todo",
            style = MaterialTheme.typography.titleMedium,
            color = tealPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = title,
            onValueChange = onTitleChanged,
            label = { Text("Title") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = tealPrimary,
                unfocusedBorderColor = tealSecondary,
                focusedLabelColor = tealPrimary,
                unfocusedLabelColor = tealSecondary
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChanged,
            label = { Text("Description") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = tealPrimary,
                unfocusedBorderColor = tealSecondary,
                focusedLabelColor = tealPrimary,
                unfocusedLabelColor = tealSecondary
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onCreateClick,
                enabled = !isCreating && title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = tealPrimary,
                    contentColor = Color.White
                )
            ) {
                if (isCreating) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Create")
            }
            OutlinedButton(
                onClick = onCancelClick,
                enabled = !isCreating,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = tealPrimary
                )
            ) {
                Text("Cancel")
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
            containerColor = tealLight.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onToggleCompletion) {
                    Icon(
                        imageVector = if (todoItem.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (todoItem.isCompleted) tealPrimary else tealSecondary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = todoItem.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            textDecoration = if (todoItem.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        color = tealPrimary
                    )
                    todoItem.description?.let { description ->
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = tealSecondary
                        )
                    }
                }
                if (currentUserId?.let { todoItem.canBeDeletedBy(it) } == true) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}