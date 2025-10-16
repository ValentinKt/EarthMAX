package com.earthmax.core.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class EcoButtonType {
    PRIMARY,
    SECONDARY,
    OUTLINED,
    TEXT
}

@Composable
fun EcoButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    type: EcoButtonType = EcoButtonType.PRIMARY,
    enabled: Boolean = true,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit
) {
    when (type) {
        EcoButtonType.PRIMARY -> {
            Button(
                onClick = onClick,
                modifier = modifier.height(48.dp),
                enabled = enabled,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                contentPadding = contentPadding,
                content = content
            )
        }
        EcoButtonType.SECONDARY -> {
            Button(
                onClick = onClick,
                modifier = modifier.height(48.dp),
                enabled = enabled,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                ),
                contentPadding = contentPadding,
                content = content
            )
        }
        EcoButtonType.OUTLINED -> {
            OutlinedButton(
                onClick = onClick,
                modifier = modifier.height(48.dp),
                enabled = enabled,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                contentPadding = contentPadding,
                content = content
            )
        }
        EcoButtonType.TEXT -> {
            TextButton(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                contentPadding = contentPadding,
                content = content
            )
        }
    }
}

@Composable
fun EcoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    type: EcoButtonType = EcoButtonType.PRIMARY,
    enabled: Boolean = true
) {
    EcoButton(
        onClick = onClick,
        modifier = modifier,
        type = type,
        enabled = enabled
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.Medium
        )
    }
}