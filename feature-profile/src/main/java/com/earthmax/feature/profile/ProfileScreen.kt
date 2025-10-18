package com.earthmax.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.earthmax.core.ui.components.EcoButton
import com.earthmax.core.ui.components.EcoButtonType
import com.earthmax.core.ui.components.EcoCard
import com.earthmax.core.ui.components.ErrorState
import com.earthmax.core.ui.components.LoadingIndicator
import com.earthmax.feature.profile.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateToAuth: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToEditProfile: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showSignOutDialog by remember { mutableStateOf(false) }

    // Refresh profile data when screen is first composed or when returning from other screens
    LaunchedEffect(Unit) {
        viewModel.refreshProfile()
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Profile",
                        modifier = Modifier.semantics {
                            contentDescription = "Profile screen title"
                        }
                    ) 
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.semantics {
                            contentDescription = "Open settings"
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Settings icon"
                        )
                    }
                    IconButton(
                        onClick = { showSignOutDialog = true },
                        modifier = Modifier.semantics {
                            contentDescription = "Sign out of the application"
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ExitToApp,
                            contentDescription = "Sign out icon"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    LoadingIndicator(
                        message = "Loading profile...",
                        modifier = Modifier.semantics {
                            contentDescription = "Profile loading progress indicator"
                        }
                    )
                }
                
                uiState.user != null -> {
                    val currentUser = uiState.user
                    if (currentUser != null) {
                        ProfileContent(
                            user = currentUser,
                            onEditProfile = onNavigateToEditProfile,
                            onRefresh = viewModel::refreshProfile,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                
                uiState.error != null -> {
                    val errorMessage = uiState.error
                    if (errorMessage != null) {
                        ErrorState(
                            message = errorMessage,
                            onRetry = viewModel::refreshProfile,
                            modifier = Modifier.semantics {
                                contentDescription = "Error loading profile: $errorMessage"
                            }
                        )
                    }
                }
            }
        }
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { 
                Text(
                    "Sign Out",
                    modifier = Modifier.semantics {
                        contentDescription = "Sign out confirmation dialog title"
                    }
                ) 
            },
            text = { 
                Text(
                    "Are you sure you want to sign out?",
                    modifier = Modifier.semantics {
                        contentDescription = "Sign out confirmation message"
                    }
                ) 
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutDialog = false
                        viewModel.signOut()
                        onNavigateToAuth()
                    },
                    modifier = Modifier.semantics {
                        contentDescription = "Confirm sign out"
                    }
                ) {
                    Text("Sign Out")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSignOutDialog = false },
                    modifier = Modifier.semantics {
                        contentDescription = "Cancel sign out"
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ProfileContent(
    user: com.earthmax.core.models.User,
    onEditProfile: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Profile Header
        EcoCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profile Image
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(user.profileImageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Profile Image",
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                    fallback = androidx.compose.ui.res.painterResource(
                        android.R.drawable.ic_menu_gallery
                    ),
                    error = androidx.compose.ui.res.painterResource(
                        android.R.drawable.ic_menu_gallery
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // User Name
                Text(
                    text = user.displayName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                // User Email
                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Edit Profile Button
                EcoButton(
                    onClick = onEditProfile,
                    type = EcoButtonType.OUTLINED,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Edit Profile")
                    }
                }
            }
        }

        // Profile Stats
        EcoCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "Environmental Impact",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(
                        label = "Events Created",
                        value = user.environmentalImpact.eventsOrganized.toString()
                    )
                    StatItem(
                        label = "Events Joined",
                        value = user.environmentalImpact.eventsAttended.toString()
                    )
                    StatItem(
                        label = "Impact Score",
                        value = user.environmentalImpact.impactScore.toString()
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(
                        label = "CO₂ Saved",
                        value = "${user.environmentalImpact.totalCO2Saved.toInt()}kg"
                    )
                    StatItem(
                        label = "Trees Planted",
                        value = user.environmentalImpact.treesPlanted.toString()
                    )
                    StatItem(
                        label = "Waste Recycled",
                        value = "${user.environmentalImpact.wasteRecycled.toInt()}kg"
                    )
                }
            }
        }

        // Monthly Goals
        if (user.profileCustomization.showMonthlyGoals) {
            EcoCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Monthly Goals",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    val goals = user.environmentalImpact.monthlyGoals
                    
                    GoalProgressItem(
                        label = "Events Target",
                        current = user.environmentalImpact.eventsAttended,
                        target = goals.targetEvents,
                        unit = "events"
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    GoalProgressItem(
                        label = "CO₂ Reduction",
                        current = user.environmentalImpact.totalCO2Saved.toInt(),
                        target = goals.targetCO2Reduction.toInt(),
                        unit = "kg"
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    GoalProgressItem(
                        label = "Recycling Goal",
                        current = user.environmentalImpact.wasteRecycled.toInt(),
                        target = goals.targetRecycling.toInt(),
                        unit = "kg"
                    )
                }
            }
        }

        // Achievements
        if (user.environmentalImpact.achievements.isNotEmpty()) {
            EcoCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Recent Achievements",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    user.environmentalImpact.achievements.take(3).forEach { achievement ->
                        AchievementItem(achievement = achievement)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        // Bio Section
        user.bio?.let { bio ->
            EcoCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "About",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = bio,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun GoalProgressItem(
    label: String,
    current: Int,
    target: Int,
    unit: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "$current / $target $unit",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        LinearProgressIndicator(
            progress = { (current.toFloat() / target.toFloat()).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

@Composable
private fun AchievementItem(
    achievement: com.earthmax.core.models.Achievement,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Achievement Icon
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = when (achievement.category) {
                    com.earthmax.core.models.AchievementCategory.PARTICIPATION -> "🎯"
                    com.earthmax.core.models.AchievementCategory.ORGANIZATION -> "🏆"
                    com.earthmax.core.models.AchievementCategory.IMPACT -> "🌱"
                    com.earthmax.core.models.AchievementCategory.COMMUNITY -> "🤝"
                    com.earthmax.core.models.AchievementCategory.CONSISTENCY -> "⭐"
                },
                style = MaterialTheme.typography.headlineSmall
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = achievement.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = achievement.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        if (achievement.isCompleted) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Completed",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}



@Composable
private fun ProfileHeader(
    name: String,
    email: String,
    profileImageUrl: String?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Profile header card containing user photo, name, and email"
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(profileImageUrl)
                    .placeholder(androidx.core.R.drawable.ic_call_answer)
                    .error(androidx.core.R.drawable.ic_call_answer)
                    .build(),
                contentDescription = "Profile picture for $name",
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .semantics {
                        contentDescription = if (profileImageUrl != null) {
                            "Profile picture for $name"
                        } else {
                            "Default profile picture placeholder for $name"
                        }
                    },
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics {
                    contentDescription = "User name: $name"
                }
            )

            Text(
                text = email,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics {
                    contentDescription = "User email address: $email"
                }
            )
        }
    }
}

@Composable
private fun EnvironmentalImpactCard(
    carbonFootprint: Double,
    waterSaved: Double,
    energySaved: Double,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Environmental impact statistics showing carbon footprint, water saved, and energy saved"
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Environmental Impact",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics {
                    contentDescription = "Environmental Impact section title"
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ImpactItem(
                    icon = Icons.Default.CloudOff,
                    value = "${carbonFootprint}kg",
                    label = "CO₂ Reduced",
                    contentDescription = "Carbon dioxide reduced: ${carbonFootprint} kilograms"
                )
                ImpactItem(
                    icon = Icons.Default.WaterDrop,
                    value = "${waterSaved}L",
                    label = "Water Saved",
                    contentDescription = "Water saved: ${waterSaved} liters"
                )
                ImpactItem(
                    icon = Icons.Default.Bolt,
                    value = "${energySaved}kWh",
                    label = "Energy Saved",
                    contentDescription = "Energy saved: ${energySaved} kilowatt hours"
                )
            }
        }
    }
}

@Composable
private fun PersonalStatsCard(
    activeDays: Int,
    challengesCompleted: Int,
    badgesEarned: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Personal statistics showing active days, challenges completed, and badges earned"
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Personal Stats",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics {
                    contentDescription = "Personal Statistics section title"
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatsItem(
                    icon = Icons.Default.CalendarToday,
                    value = activeDays.toString(),
                    label = "Active Days",
                    contentDescription = "Active days: $activeDays days"
                )
                StatsItem(
                    icon = Icons.Default.EmojiEvents,
                    value = challengesCompleted.toString(),
                    label = "Challenges",
                    contentDescription = "Challenges completed: $challengesCompleted challenges"
                )
                StatsItem(
                    icon = Icons.Default.Stars,
                    value = badgesEarned.toString(),
                    label = "Badges",
                    contentDescription = "Badges earned: $badgesEarned badges"
                )
            }
        }
    }
}

@Composable
private fun ImpactItem(
    icon: ImageVector,
    value: String,
    label: String,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.semantics {
            this.contentDescription = contentDescription
        },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "$label icon",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StatsItem(
    icon: ImageVector,
    value: String,
    label: String,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.semantics {
            this.contentDescription = contentDescription
        },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "$label icon",
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}