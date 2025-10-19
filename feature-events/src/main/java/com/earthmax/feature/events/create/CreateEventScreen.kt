package com.earthmax.feature.events.create

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.earthmax.core.ui.components.EcoButton
import com.earthmax.core.ui.components.EcoButtonType
import com.earthmax.core.ui.components.EcoTextField
import com.earthmax.core.ui.components.SmallLoadingIndicator
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

    // Show success message
    if (uiState.showSuccessMessage) {
        LaunchedEffect(Unit) {
            // Auto-dismiss success message after 3 seconds
            kotlinx.coroutines.delay(3000)
            viewModel.dismissSuccessMessage()
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
            Column {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    onClick = { imagePickerLauncher.launch("image/*") },
                    colors = CardDefaults.cardColors(
                        containerColor = if (uiState.imageError != null) 
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                        else 
                            MaterialTheme.colorScheme.surface
                    ),
                    border = if (uiState.imageError != null) 
                        androidx.compose.foundation.BorderStroke(
                            1.dp, 
                            MaterialTheme.colorScheme.error
                        ) 
                    else null
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
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (uiState.imageError != null) 
                                    MaterialTheme.colorScheme.error 
                                else 
                                    MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                
                // Display image validation error
                uiState.imageError?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
            }

            // Title
            EcoTextField(
                value = uiState.title,
                onValueChange = viewModel::updateTitle,
                label = "Event Title",
                placeholder = "Enter a compelling event title",
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = uiState.titleError != null,
                errorMessage = uiState.titleError
            )

            // Description
            EcoTextField(
                value = uiState.description,
                onValueChange = viewModel::updateDescription,
                label = "Description",
                placeholder = "Describe your event and its environmental impact",
                modifier = Modifier.fillMaxWidth(),
                maxLines = 5,
                singleLine = false,
                isError = uiState.descriptionError != null,
                errorMessage = uiState.descriptionError
            )

            // Location
            EcoTextField(
                value = uiState.location,
                onValueChange = viewModel::updateLocation,
                label = "Location",
                placeholder = "Where will this event take place?",
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = uiState.locationError != null,
                errorMessage = uiState.locationError
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
            EcoTextField(
                value = if (uiState.maxParticipants == 0) "" else uiState.maxParticipants.toString(),
                onValueChange = { value ->
                    if (value.isEmpty()) {
                        viewModel.updateMaxParticipants(0)
                    } else {
                        value.toIntOrNull()?.let { viewModel.updateMaxParticipants(it) }
                    }
                },
                label = "Max Participants",
                placeholder = "Enter number of participants",
                keyboardType = KeyboardType.Number,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = uiState.maxParticipantsError != null,
                errorMessage = uiState.maxParticipantsError
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

            // Show general error message
            uiState.error?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Show success message
            if (uiState.showSuccessMessage) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🎉",
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "Event created successfully! Your environmental impact event is now live.",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Create Event Button
            EcoButton(
                onClick = { viewModel.createEvent() },
                enabled = uiState.isFormValid && !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                type = EcoButtonType.PRIMARY
            ) {
                if (uiState.isLoading) {
                    SmallLoadingIndicator(
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(
                        text = "Create Event",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}