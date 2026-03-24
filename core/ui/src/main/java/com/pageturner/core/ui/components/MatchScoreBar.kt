package com.pageturner.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.pageturner.core.ui.theme.PageTurnerColors

/**
 * Amber fill bar representing how well a book aligns with the current taste profile.
 *
 * [score] must be in the range 0.0–1.0. This is a local calculation — it is NOT
 * an external quality rating (no Goodreads scores).
 */
@Composable
fun MatchScoreBar(
    score: Float,
    modifier: Modifier = Modifier
) {
    val clampedScore = score.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(PageTurnerColors.Accent.copy(alpha = 0.2f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(clampedScore)
                .height(4.dp)
                .background(PageTurnerColors.Accent)
        )
    }
}
