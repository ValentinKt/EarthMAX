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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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

    LaunchedEffect(uiState.isUpdateSuccessful) {
        if (uiState.isUpdateSuccessful) {
            snackbarHostState.showSnackbar("Profile updated successfully!")
            onNavigateBack()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Edit Profile",
                        modifier = Modifier.semantics {
                            contentDescription = "Edit Profile screen title"
                        }
                    ) 
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.semantics {
                            contentDescription = "Navigate back to profile screen"
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back arrow icon"
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
                
                else -> {
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
            .padding(16.dp)
            .semantics {
                contentDescription = "Edit profile form with customization options"
            },
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Profile Image Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = "Profile image section. Tap to change profile photo"
                },
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
                        contentDescription = "Current profile image. Tap to change",
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
                        modifier = Modifier
                            .size(40.dp)
                            .semantics {
                                contentDescription = "Change profile photo button"
                            },
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Camera icon for changing photo",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Tap to change profile photo",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.semantics {
                        contentDescription = "Instructions to tap for changing profile photo"
                    }
                )
            }
        }

        // Form Fields
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = "Profile information form fields"
                }
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Profile Information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.semantics {
                        contentDescription = "Profile Information section header"
                    }
                )

                // Email (Read-only)
                OutlinedTextField(
                    value = uiState.currentUser?.email ?: "",
                    onValueChange = { },
                    label = { 
                        Text(
                            "Email",
                            modifier = Modifier.semantics {
                                contentDescription = "Email address field, read-only"
                            }
                        ) 
                    },
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = "Email address: ${uiState.currentUser?.email ?: "Not available"}. This field is read-only"
                        },
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = "Display name text field. Current value: ${uiState.displayName.ifEmpty { "Empty" }}"
                        },
                    singleLine = true
                )

                // Bio
                EcoTextField(
                    value = uiState.bio,
                    onValueChange = onBioChange,
                    label = "Bio",
                    placeholder = "Tell others about yourself and your environmental interests",
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = "Bio text field. Current value: ${uiState.bio.ifEmpty { "Empty" }}"
                        },
                    maxLines = 5,
                    singleLine = false
                )
            }
        }

        // Profile Customization Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = "Profile customization options including theme selection and privacy settings"
                }
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Profile Customization",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.semantics {
                        contentDescription = "Profile Customization section header"
                    }
                )

                // Profile Theme Selection
                Text(
                    text = "Profile Theme",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.semantics {
                        contentDescription = "Profile theme selection. Currently selected: ${
                            when (uiState.selectedTheme) {
                                ProfileTheme.FOREST -> "Forest Green"
                                ProfileTheme.OCEAN -> "Ocean Blue"
                                ProfileTheme.MOUNTAIN -> "Mountain Gray"
                                ProfileTheme.DESERT -> "Desert Sand"
                                ProfileTheme.ARCTIC -> "Arctic White"
                            }
                        }"
                    }
                )
                
                Column(
                    modifier = Modifier
                        .selectableGroup()
                        .semantics {
                            contentDescription = "Theme selection radio button group"
                        },
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProfileTheme.values().forEach { theme ->
                        val themeName = when (theme) {
                            ProfileTheme.FOREST -> "Forest Green"
                            ProfileTheme.OCEAN -> "Ocean Blue"
                            ProfileTheme.MOUNTAIN -> "Mountain Gray"
                            ProfileTheme.DESERT -> "Desert Sand"
                            ProfileTheme.ARCTIC -> "Arctic White"
                        }
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = (uiState.selectedTheme == theme),
                                    onClick = { onThemeChange(theme) },
                                    role = Role.RadioButton
                                )
                                .padding(horizontal = 16.dp)
                                .semantics {
                                    contentDescription = "$themeName theme option. ${if (uiState.selectedTheme == theme) "Selected" else "Not selected"}"
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (uiState.selectedTheme == theme),
                                onClick = null,
                                modifier = Modifier.semantics {
                                    contentDescription = "$themeName radio button"
                                }
                            )
                            Text(
                                text = themeName,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .padding(start = 16.dp)
                                    .semantics {
                                        contentDescription = "$themeName theme label"
                                    }
                            )
                        }
                    }
                }

                Divider(
                    modifier = Modifier.semantics {
                        contentDescription = "Section divider"
                    }
                )

                // Profile Visibility
                Text(
                    text = "Profile Visibility",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.semantics {
                        contentDescription = "Profile visibility settings. Currently selected: ${
                            when (uiState.selectedVisibility) {
                                ProfileVisibility.PUBLIC -> "Public - Anyone can see your profile"
                                ProfileVisibility.FRIENDS_ONLY -> "Friends Only - Only your friends can see your profile"
                                ProfileVisibility.PRIVATE -> "Private - Only you can see your profile"
                            }
                        }"
                    }
                )
                
                Column(
                    modifier = Modifier
                        .selectableGroup()
                        .semantics {
                            contentDescription = "Profile visibility radio button group"
                        },
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProfileVisibility.values().forEach { visibility ->
                        val visibilityName = when (visibility) {
                            ProfileVisibility.PUBLIC -> "Public"
                            ProfileVisibility.FRIENDS_ONLY -> "Friends Only"
                            ProfileVisibility.PRIVATE -> "Private"
                        }
                        val visibilityDescription = when (visibility) {
                            ProfileVisibility.PUBLIC -> "Anyone can see your profile"
                            ProfileVisibility.FRIENDS_ONLY -> "Only your friends can see your profile"
                            ProfileVisibility.PRIVATE -> "Only you can see your profile"
                        }
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = (uiState.selectedVisibility == visibility),
                                    onClick = { onVisibilityChange(visibility) },
                                    role = Role.RadioButton
                                )
                                .padding(horizontal = 16.dp)
                                .semantics {
                                    contentDescription = "$visibilityName visibility option. $visibilityDescription. ${if (uiState.selectedVisibility == visibility) "Selected" else "Not selected"}"
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (uiState.selectedVisibility == visibility),
                                onClick = null,
                                modifier = Modifier.semantics {
                                    contentDescription = "$visibilityName radio button"
                                }
                            )
                            Column(
                                modifier = Modifier.padding(start = 16.dp)
                            ) {
                                Text(
                                    text = visibilityName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.semantics {
                                        contentDescription = "$visibilityName visibility label"
                                    }
                                )
                                Text(
                                    text = visibilityDescription,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.semantics {
                                        contentDescription = "Visibility description: $visibilityDescription"
                                    }
                                )
                            }
                        }
                    }
                }

                Divider(
                    modifier = Modifier.semantics {
                        contentDescription = "Section divider"
                    }
                )

                // Impact Stats Visibility
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = "Impact statistics visibility setting. Currently ${if (uiState.showImpactStats) "enabled" else "disabled"}"
                        },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Show Impact Stats",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.semantics {
                                contentDescription = "Show Impact Stats toggle label"
                            }
                        )
                        Text(
                            text = "Display your environmental impact statistics on your profile",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.semantics {
                                contentDescription = "Impact stats description: Display your environmental impact statistics on your profile"
                            }
                        )
                    }
                    Switch(
                        checked = uiState.showImpactStats,
                        onCheckedChange = onShowImpactStatsChange,
                        modifier = Modifier.semantics {
                            contentDescription = "Impact statistics visibility toggle. Currently ${if (uiState.showImpactStats) "enabled" else "disabled"}. Tap to ${if (uiState.showImpactStats) "disable" else "enable"}"
                        }
                    )
                }
            }
        }

        // Save Button
        EcoButton(
            onClick = onSaveClick,
            type = EcoButtonType.PRIMARY,
            enabled = uiState.isFormValid && !uiState.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = if (uiState.isLoading) {
                        "Saving profile changes, please wait"
                    } else if (uiState.isFormValid) {
                        "Save Changes button. Tap to save your profile changes"
                    } else {
                        "Save Changes button. Currently disabled because form is not valid"
                    }
                }
        ) {
            if (uiState.isLoading) {
                SmallLoadingIndicator(
                    modifier = Modifier
                        .size(20.dp)
                        .semantics {
                            contentDescription = "Saving in progress"
                        }
                )
            } else {
                Text(
                    text = "Save Changes",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.semantics {
                        contentDescription = "Save Changes button text"
                    }
                )
            }
        }
    }
}