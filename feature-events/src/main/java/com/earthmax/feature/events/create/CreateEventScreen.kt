package com.earthmax.feature.events.create

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.earthmax.core.models.EventCategory
import com.earthmax.feature.events.components.EventCategoryChip
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventScreen(
    onNavigateBack: () -> Unit,
    onEventCreated: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CreateEventViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        viewModel.updateSelectedImageUri(uri)
    }

    LaunchedEffect(uiState.isEventCreated) {
        if (uiState.isEventCreated) {
            onEventCreated()
        }
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
            title = { Text("Create Event") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Event Image
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                onClick = { imagePickerLauncher.launch("image/*") }
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.selectedImageUri != null) {
                        AsyncImage(
                            model = uiState.selectedImageUri,
                            contentDescription = "Event image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = "Tap to add event image",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            // Title
            OutlinedTextField(
                value = uiState.title,
                onValueChange = viewModel::updateTitle,
                label = { Text("Event Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Description
            OutlinedTextField(
                value = uiState.description,
                onValueChange = viewModel::updateDescription,
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )

            // Location
            OutlinedTextField(
                value = uiState.location,
                onValueChange = viewModel::updateLocation,
                label = { Text("Location") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Date and Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Date Picker
                OutlinedTextField(
                    value = uiState.selectedDate?.let { 
                        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(it) 
                    } ?: "",
                    onValueChange = { },
                    label = { Text("Date") },
                    readOnly = true,
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                // Note: In a real implementation, you'd show this picker
                                // For now, we'll set a default date
                                viewModel.updateSelectedDate(Date())
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Select date"
                            )
                        }
                    },
                    modifier = Modifier.weight(1f)
                )

                // Time Picker
                OutlinedTextField(
                    value = uiState.selectedTime?.let { 
                        SimpleDateFormat("HH:mm", Locale.getDefault()).format(it) 
                    } ?: "",
                    onValueChange = { },
                    label = { Text("Time") },
                    readOnly = true,
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                // Note: In a real implementation, you'd show this picker
                                // For now, we'll set a default time
                                val calendar = Calendar.getInstance()
                                calendar.set(Calendar.HOUR_OF_DAY, 12)
                                calendar.set(Calendar.MINUTE, 0)
                                viewModel.updateSelectedTime(calendar.time)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = "Select time"
                            )
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            // Max Participants
            OutlinedTextField(
                value = uiState.maxParticipants.toString(),
                onValueChange = { value ->
                    value.toIntOrNull()?.let { viewModel.updateMaxParticipants(it) }
                },
                label = { Text("Max Participants") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Category Selection
            Text(
                text = "Category",
                style = MaterialTheme.typography.titleMedium
            )
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(EventCategory.values()) { category ->
                    EventCategoryChip(
                        category = category,
                        isSelected = uiState.selectedCategory == category,
                        onClick = { viewModel.updateSelectedCategory(category) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Create Button
            Button(
                onClick = viewModel::createEvent,
                enabled = uiState.isFormValid && !uiState.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Create Event")
                }
            }
        }
    }
}

@Composable
private fun LazyRow(
    horizontalArrangement: Arrangement.Horizontal,
    contentPadding: PaddingValues,
    content: @Composable () -> Unit
) {
    // Simplified implementation - in real app, use androidx.compose.foundation.lazy.LazyRow
    Row(
        horizontalArrangement = horizontalArrangement,
        modifier = Modifier.padding(contentPadding)
    ) {
        content()
    }
}

@Composable
private fun items(
    items: Array<EventCategory>,
    itemContent: @Composable (EventCategory) -> Unit
) {
    items.forEach { item ->
        itemContent(item)
    }
}