package com.earthmax.feature.events

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.earthmax.core.models.EventCategory
import com.earthmax.feature.events.components.EventCard
import com.earthmax.feature.events.components.EventCategoryChip
import com.earthmax.feature.events.components.SearchBar
import com.earthmax.feature.events.components.EmptyEventsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsHomeScreen(
    onNavigateToEventDetail: (String) -> Unit,
    onNavigateToCreateEvent: () -> Unit,
    onNavigateToMap: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EventsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val events = viewModel.events.collectAsLazyPagingItems()
    var showSearchBar by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (currentUser != null) {
                            "Hello, ${currentUser!!.displayName}!"
                        } else {
                            "EarthMAX Events"
                        }
                    )
                },
                actions = {
                    IconButton(
                        onClick = { showSearchBar = !showSearchBar }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search events"
                        )
                    }
                    IconButton(
                        onClick = onNavigateToMap
                    ) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = "Map view"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreateEvent
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create event"
                )
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar
            if (showSearchBar) {
                SearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = { viewModel.setSearchQuery(it) },
                    onSearchClose = { showSearchBar = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Category Filter
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = uiState.selectedCategory == null,
                        onClick = { viewModel.setSelectedCategory(null) },
                        label = { Text("All") }
                    )
                }
                items(EventCategory.values()) { category ->
                    EventCategoryChip(
                        category = category,
                        isSelected = uiState.selectedCategory == category,
                        onClick = { viewModel.setSelectedCategory(category) }
                    )
                }
            }

            // Events List
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (events.itemCount == 0) {
                EmptyEventsState(
                    onCreateEventClick = onNavigateToCreateEvent
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        count = events.itemCount,
                        key = { index -> events[index]?.id ?: index }
                    ) { index ->
                        val event = events[index]
                        event?.let { eventItem ->
                            EventCard(
                                event = eventItem,
                                currentUserId = currentUser?.id,
                                onEventClick = { onNavigateToEventDetail(eventItem.id) },
                                onJoinClick = { viewModel.joinEvent(eventItem.id) },
                                onLeaveClick = { viewModel.leaveEvent(eventItem.id) },
                                isLoading = uiState.isLoading
                            )
                        }
                    }
                }
            }

            // Error handling
            uiState.error?.let { error ->
                LaunchedEffect(error) {
                    // Show error message
                }
            }
        }
    }
}