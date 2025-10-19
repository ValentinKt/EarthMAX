package com.earthmax.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/**
 * A reusable Avatar component that displays a profile image or falls back to initials
 */
@Composable
fun Avatar(
    profileImageUrl: String?,
    displayName: String?,
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        if (!profileImageUrl.isNullOrBlank()) {
            AsyncImage(
                model = profileImageUrl,
                contentDescription = "Profile picture",
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            // Fallback to initials
            val initials = getInitials(displayName)
            Text(
                text = initials,
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Small avatar variant (32dp)
 */
@Composable
fun SmallAvatar(
    profileImageUrl: String?,
    displayName: String?,
    modifier: Modifier = Modifier
) {
    Avatar(
        profileImageUrl = profileImageUrl,
        displayName = displayName,
        size = 32.dp,
        modifier = modifier
    )
}

/**
 * Large avatar variant (80dp)
 */
@Composable
fun LargeAvatar(
    profileImageUrl: String?,
    displayName: String?,
    modifier: Modifier = Modifier
) {
    Avatar(
        profileImageUrl = profileImageUrl,
        displayName = displayName,
        size = 80.dp,
        modifier = modifier
    )
}

/**
 * Extra large avatar variant (120dp)
 */
@Composable
fun ExtraLargeAvatar(
    profileImageUrl: String?,
    displayName: String?,
    modifier: Modifier = Modifier
) {
    Avatar(
        profileImageUrl = profileImageUrl,
        displayName = displayName,
        size = 120.dp,
        modifier = modifier
    )
}

/**
 * Helper function to extract initials from a display name
 */
private fun getInitials(displayName: String?): String {
    if (displayName.isNullOrBlank()) return "?"
    
    val words = displayName.trim().split(" ")
    return when {
        words.size >= 2 -> "${words[0].first().uppercase()}${words[1].first().uppercase()}"
        words.size == 1 && words[0].isNotEmpty() -> words[0].first().uppercase()
        else -> "?"
    }
}