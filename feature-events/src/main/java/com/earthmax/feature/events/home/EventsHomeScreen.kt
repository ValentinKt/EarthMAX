package com.earthmax.feature.events.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.earthmax.core.models.Event
import com.earthmax.core.models.EventCategory
import com.earthmax.core.ui.animation.fadeInAnimation
import com.earthmax.core.ui.animation.slideInFromBottom
import com.earthmax.feature.events.EventsViewModel
import com.earthmax.feature.events.components.EventCard
import com.earthmax.feature.events.components.EventCategoryChip
import com.earthmax.feature.events.components.SearchBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsHomeScreen(
    onNavigateToEventDetail: (String) -> Unit,
    onNavigateToCreateEvent: () -> Unit,
    onNavigateToMap: () -> Unit,
    onNavigateToProfile: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EventsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val events = viewModel.events.collectAsLazyPagingItems()
    
    var showSearchBar by remember { mutableStateOf(false) }
    
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "EarthMAX",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        uiState.currentUser?.let { user ->
                            Text(
                                text = "Welcome back, ${user.displayName}!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showSearchBar = !showSearchBar }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search events"
                        )
                    }
                    IconButton(onClick = onNavigateToMap) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = "View map"
                        )
                    }
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreateEvent,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create event"
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar
            AnimatedVisibility(
                visible = showSearchBar,
                enter = slideInVertically(
                    initialOffsetY = { -it },
                    animationSpec = tween(300)
                ) + fadeIn(animationSpec = tween(300)),
                exit = slideOutVertically(
                    targetOffsetY = { -it },
                    animationSpec = tween(300)
                ) + fadeOut(animationSpec = tween(300))
            ) {
                SearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = viewModel::setSearchQuery,
                    onSearchClose = { showSearchBar = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            
            // Category Filter
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                items(EventCategory.values()) { category ->
                    EventCategoryChip(
                        category = category,
                        isSelected = uiState.selectedCategory == category,
                        onClick = { viewModel.setSelectedCategory(category) }
                    )
                }
            }
            
            // Events List
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (events.itemCount == 0 && !uiState.isLoading) {
                    item {
                        EmptyEventsState(
                            onCreateEvent = onNavigateToCreateEvent,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    items(
                        count = events.itemCount,
                        key = events.itemKey { it.id }
                    ) { index ->
                        val event = events[index]
                        event?.let {
                             EventCard(
                                 event = it,
                                 currentUserId = uiState.currentUser?.id,
                                 onEventClick = onNavigateToEventDetail,
                                 onJoinClick = viewModel::joinEvent,
                                 onLeaveClick = viewModel::leaveEvent,
                                 isLoading = uiState.isLoading,
                                 modifier = Modifier
                                     .fillMaxWidth()
                                     .fadeInAnimation(visible = true)
                             )
                        }
                    }
                }
            }
        }
    }
    
    // Error Snackbar
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // Show snackbar
            viewModel.clearError()
        }
    }
}

@Composable
private fun EmptyEventsState(
    onCreateEvent: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No events found",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Be the first to create an event in your area!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = onCreateEvent,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create Event")
        }
    }
}