package com.earthmax.feature.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.earthmax.core.ui.components.EcoButton
import com.earthmax.core.ui.components.EcoButtonType
import com.earthmax.core.ui.components.EcoTextField
import com.earthmax.core.ui.components.LoadingIndicator
import com.earthmax.core.ui.components.SmallLoadingIndicator
import com.earthmax.core.models.ProfileTheme
import com.earthmax.core.models.ProfileVisibility
import com.earthmax.core.models.EventCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.updateProfileImage(it) }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.isUpdateSuccessful) {
        if (uiState.isUpdateSuccessful) {
            snackbarHostState.showSnackbar("Profile updated successfully!")
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                LoadingIndicator(message = "Updating profile...")
            } else {
                EditProfileContent(
                    uiState = uiState,
                    onDisplayNameChange = viewModel::updateDisplayName,
                    onBioChange = viewModel::updateBio,
                    onImageClick = { imagePickerLauncher.launch("image/*") },
                    onSaveClick = viewModel::updateProfile,
                    onThemeChange = viewModel::updateTheme,
                    onVisibilityChange = viewModel::updateVisibility,
                    onShowImpactStatsChange = viewModel::updateShowImpactStats,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun EditProfileContent(
    uiState: EditProfileUiState,
    onDisplayNameChange: (String) -> Unit,
    onBioChange: (String) -> Unit,
    onImageClick: () -> Unit,
    onSaveClick: () -> Unit,
    onThemeChange: (ProfileTheme) -> Unit,
    onVisibilityChange: (ProfileVisibility) -> Unit,
    onShowImpactStatsChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Profile Image Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            onClick = onImageClick
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    contentAlignment = Alignment.BottomEnd
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(uiState.profileImageUri ?: uiState.currentUser?.profileImageUrl)
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
                    
                    FloatingActionButton(
                        onClick = onImageClick,
                        modifier = Modifier.size(40.dp),
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Change photo",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Tap to change profile photo",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Form Fields
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Profile Information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Email (Read-only)
                OutlinedTextField(
                    value = uiState.currentUser?.email ?: "",
                    onValueChange = { },
                    label = { Text("Email") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    enabled = false
                )

                // Display Name
                EcoTextField(
                    value = uiState.displayName,
                    onValueChange = onDisplayNameChange,
                    label = "Display Name",
                    placeholder = "Enter your display name",
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Bio
                EcoTextField(
                    value = uiState.bio,
                    onValueChange = onBioChange,
                    label = "Bio",
                    placeholder = "Tell others about yourself and your environmental interests",
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 5,
                    singleLine = false
                )
            }
        }

        // Profile Customization Section
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Profile Customization",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Profile Theme Selection
                Text(
                    text = "Profile Theme",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Column(
                    modifier = Modifier.selectableGroup(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProfileTheme.values().forEach { theme ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = (uiState.selectedTheme == theme),
                                    onClick = { onThemeChange(theme) },
                                    role = Role.RadioButton
                                )
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (uiState.selectedTheme == theme),
                                onClick = null
                            )
                            Text(
                                text = when (theme) {
                                    ProfileTheme.FOREST -> "Forest Green"
                                    ProfileTheme.OCEAN -> "Ocean Blue"
                                    ProfileTheme.MOUNTAIN -> "Mountain Gray"
                                    ProfileTheme.DESERT -> "Desert Sand"
                                    ProfileTheme.ARCTIC -> "Arctic White"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    }
                }

                Divider()

                // Profile Visibility
                Text(
                    text = "Profile Visibility",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Column(
                    modifier = Modifier.selectableGroup(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProfileVisibility.values().forEach { visibility ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = (uiState.selectedVisibility == visibility),
                                    onClick = { onVisibilityChange(visibility) },
                                    role = Role.RadioButton
                                )
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (uiState.selectedVisibility == visibility),
                                onClick = null
                            )
                            Column(
                                modifier = Modifier.padding(start = 16.dp)
                            ) {
                                Text(
                                    text = when (visibility) {
                                        ProfileVisibility.PUBLIC -> "Public"
                                        ProfileVisibility.FRIENDS_ONLY -> "Friends Only"
                                        ProfileVisibility.PRIVATE -> "Private"
                                    },
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = when (visibility) {
                                        ProfileVisibility.PUBLIC -> "Anyone can see your profile"
                                        ProfileVisibility.FRIENDS_ONLY -> "Only your friends can see your profile"
                                        ProfileVisibility.PRIVATE -> "Only you can see your profile"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Divider()

                // Impact Stats Visibility
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Show Impact Stats",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Display your environmental impact statistics on your profile",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.showImpactStats,
                        onCheckedChange = onShowImpactStatsChange
                    )
                }
            }
        }

        // Save Button
        EcoButton(
            onClick = onSaveClick,
            type = EcoButtonType.PRIMARY,
            enabled = uiState.isFormValid && !uiState.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (uiState.isLoading) {
                SmallLoadingIndicator(
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Text(
                    text = "Save Changes",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}