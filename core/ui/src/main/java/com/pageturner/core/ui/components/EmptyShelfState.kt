package com.pageturner.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pageturner.core.ui.theme.PageTurnerColors
import com.pageturner.core.ui.theme.PageTurnerSpacing
import com.pageturner.core.ui.theme.PageTurnerType

/**
 * Empty state for the Reading List screen.
 * Never a blank screen — always shows this illustrated shelf with the tagline.
 */
@Composable
fun EmptyShelfState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(PageTurnerSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.AutoStories,
            contentDescription = null,
            tint = PageTurnerColors.OnSurfaceMuted,
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(PageTurnerSpacing.lg))
        Text(
            text = "Your next great read is waiting.",
            style = PageTurnerType.CardTitle,
            color = PageTurnerColors.OnSurfaceMuted,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(PageTurnerSpacing.sm))
        Text(
            text = "Start swiping to build your reading list.",
            style = PageTurnerType.Body,
            color = PageTurnerColors.OnSurfaceMuted.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
    }
}
