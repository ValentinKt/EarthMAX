package com.earthmax.feature.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.earthmax.core.ui.components.Avatar
import com.earthmax.core.ui.components.SmallAvatar
import com.earthmax.core.ui.components.LargeAvatar
import com.earthmax.core.ui.components.ExtraLargeAvatar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvatarTestScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Avatar Component Test") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Test with image URL
            TestSection(
                title = "Avatar with Image URL",
                description = "Testing with a sample profile image"
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SmallAvatar(
                        profileImageUrl = "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=150&h=150&fit=crop&crop=face",
                        displayName = "John Doe"
                    )
                    Avatar(
                        profileImageUrl = "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=150&h=150&fit=crop&crop=face",
                        displayName = "John Doe",
                        size = 64.dp
                    )
                    LargeAvatar(
                        profileImageUrl = "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=150&h=150&fit=crop&crop=face",
                        displayName = "John Doe"
                    )
                    ExtraLargeAvatar(
                        profileImageUrl = "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=150&h=150&fit=crop&crop=face",
                        displayName = "John Doe"
                    )
                }
            }

            // Test with initials fallback
            TestSection(
                title = "Avatar with Initials Fallback",
                description = "Testing fallback to initials when no image is provided"
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SmallAvatar(
                        profileImageUrl = null,
                        displayName = "Jane Smith"
                    )
                    Avatar(
                        profileImageUrl = null,
                        displayName = "Jane Smith",
                        size = 64.dp
                    )
                    LargeAvatar(
                        profileImageUrl = null,
                        displayName = "Jane Smith"
                    )
                    ExtraLargeAvatar(
                        profileImageUrl = null,
                        displayName = "Jane Smith"
                    )
                }
            }

            // Test with single name
            TestSection(
                title = "Avatar with Single Name",
                description = "Testing with a single word name"
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SmallAvatar(
                        profileImageUrl = null,
                        displayName = "Alice"
                    )
                    Avatar(
                        profileImageUrl = null,
                        displayName = "Alice",
                        size = 64.dp
                    )
                    LargeAvatar(
                        profileImageUrl = null,
                        displayName = "Alice"
                    )
                    ExtraLargeAvatar(
                        profileImageUrl = null,
                        displayName = "Alice"
                    )
                }
            }

            // Test with empty/null name
            TestSection(
                title = "Avatar with Empty Name",
                description = "Testing fallback when name is empty or null"
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SmallAvatar(
                        profileImageUrl = null,
                        displayName = null
                    )
                    Avatar(
                        profileImageUrl = null,
                        displayName = "",
                        size = 64.dp
                    )
                    LargeAvatar(
                        profileImageUrl = null,
                        displayName = "   "
                    )
                    ExtraLargeAvatar(
                        profileImageUrl = null,
                        displayName = null
                    )
                }
            }

            // Test with long names
            TestSection(
                title = "Avatar with Long Names",
                description = "Testing with longer names to verify initials extraction"
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SmallAvatar(
                        profileImageUrl = null,
                        displayName = "Alexander Benjamin Christopher"
                    )
                    Avatar(
                        profileImageUrl = null,
                        displayName = "Maria Elena Rodriguez Garcia",
                        size = 64.dp
                    )
                    LargeAvatar(
                        profileImageUrl = null,
                        displayName = "Jean-Pierre François"
                    )
                    ExtraLargeAvatar(
                        profileImageUrl = null,
                        displayName = "Dr. Elizabeth Catherine Johnson"
                    )
                }
            }

            // Test with invalid image URL
            TestSection(
                title = "Avatar with Invalid Image URL",
                description = "Testing fallback when image URL is invalid"
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SmallAvatar(
                        profileImageUrl = "invalid-url",
                        displayName = "Test User"
                    )
                    Avatar(
                        profileImageUrl = "https://invalid-domain.com/image.jpg",
                        displayName = "Test User",
                        size = 64.dp
                    )
                    LargeAvatar(
                        profileImageUrl = "",
                        displayName = "Test User"
                    )
                    ExtraLargeAvatar(
                        profileImageUrl = "   ",
                        displayName = "Test User"
                    )
                }
            }

            // Size comparison
            TestSection(
                title = "Size Comparison",
                description = "All avatar sizes with the same content"
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SmallAvatar(
                            profileImageUrl = null,
                            displayName = "EcoMax User"
                        )
                        Text(
                            text = "Small (32dp)",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Avatar(
                            profileImageUrl = null,
                            displayName = "EcoMax User",
                            size = 64.dp
                        )
                        Text(
                            text = "Medium (64dp)",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LargeAvatar(
                            profileImageUrl = null,
                            displayName = "EcoMax User"
                        )
                        Text(
                            text = "Large (80dp)",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ExtraLargeAvatar(
                            profileImageUrl = null,
                            displayName = "EcoMax User"
                        )
                        Text(
                            text = "XL (120dp)",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TestSection(
    title: String,
    description: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            content()
        }
    }
}