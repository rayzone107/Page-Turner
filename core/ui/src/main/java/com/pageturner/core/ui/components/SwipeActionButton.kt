package com.pageturner.core.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.pageturner.core.ui.theme.PageTurnerColors

/** The three action types in the swipe deck button row. */
enum class SwipeActionType {
    /** Skip this book — swipe left equivalent. */
    SKIP,
    /** Soft save to Maybe pile — bookmark equivalent. */
    BOOKMARK,
    /** Save to reading list — swipe right equivalent. */
    SAVE
}

/**
 * Circular action button used in the swipe deck's button row.
 * Skip (X) is red-tinted, Bookmark is muted, Save (✓) is amber.
 */
@Composable
fun SwipeActionButton(
    type: SwipeActionType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (icon, contentDesc, containerColor, iconTint) = when (type) {
        SwipeActionType.SKIP -> ActionButtonConfig(
            icon = Icons.Default.Close,
            contentDescription = "Skip",
            containerColor = PageTurnerColors.SurfaceVariant,
            iconTint = PageTurnerColors.Error
        )
        SwipeActionType.BOOKMARK -> ActionButtonConfig(
            icon = Icons.Default.BookmarkBorder,
            contentDescription = "Save for later",
            containerColor = PageTurnerColors.SurfaceVariant,
            iconTint = PageTurnerColors.OnSurfaceMuted
        )
        SwipeActionType.SAVE -> ActionButtonConfig(
            icon = Icons.Default.Check,
            contentDescription = "Add to reading list",
            containerColor = PageTurnerColors.Accent,
            iconTint = PageTurnerColors.OnAccent
        )
    }

    FilledIconButton(
        onClick = onClick,
        modifier = modifier
            .size(56.dp)
            .clip(CircleShape),
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = containerColor,
            contentColor = iconTint
        )
    ) {
        Icon(imageVector = icon, contentDescription = contentDesc)
    }
}

private data class ActionButtonConfig(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val contentDescription: String,
    val containerColor: androidx.compose.ui.graphics.Color,
    val iconTint: androidx.compose.ui.graphics.Color
)
