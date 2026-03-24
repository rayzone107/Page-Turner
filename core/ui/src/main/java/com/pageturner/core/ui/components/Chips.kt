package com.pageturner.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.pageturner.core.ui.theme.PageTurnerColors
import com.pageturner.core.ui.theme.PageTurnerSpacing
import com.pageturner.core.ui.theme.PageTurnerType

private val ChipShape = RoundedCornerShape(4.dp)

/**
 * Amber-tinted genre/subject chip. Used on cards and the Taste Profile screen.
 */
@Composable
fun GenreChip(
    label: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = label.uppercase(),
        style = PageTurnerType.Chip,
        color = PageTurnerColors.Accent,
        modifier = modifier
            .clip(ChipShape)
            .background(PageTurnerColors.Accent.copy(alpha = 0.15f))
            .padding(horizontal = PageTurnerSpacing.sm, vertical = PageTurnerSpacing.xs)
    )
}

/**
 * Teal wildcard chip with the fixed label "Wildcard pick".
 * Appears on swipe cards and the Book Detail screen when [Book.isWildcard] is true.
 */
@Composable
fun WildcardChip(modifier: Modifier = Modifier) {
    Text(
        text = "WILDCARD PICK",
        style = PageTurnerType.Chip,
        color = PageTurnerColors.Teal,
        modifier = modifier
            .clip(ChipShape)
            .background(PageTurnerColors.Teal.copy(alpha = 0.15f))
            .padding(horizontal = PageTurnerSpacing.sm, vertical = PageTurnerSpacing.xs)
    )
}
