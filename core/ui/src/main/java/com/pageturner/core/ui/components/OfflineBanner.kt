package com.pageturner.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.pageturner.core.ui.theme.PageTurnerColors
import com.pageturner.core.ui.theme.PageTurnerSpacing
import com.pageturner.core.ui.theme.PageTurnerType

/**
 * Amber banner shown when the device is offline.
 * Displayed at the top of the Swipe Deck screen — the reading list is always available offline.
 */
@Composable
fun OfflineBanner(modifier: Modifier = Modifier) {
    Text(
        text = "You're offline — showing saved books",
        style = PageTurnerType.Label,
        color = PageTurnerColors.OnAccent,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .background(PageTurnerColors.Accent)
            .padding(vertical = PageTurnerSpacing.sm)
    )
}
