package com.pageturner.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp

/**
 * Gradient fill bar representing how well a book aligns with the current taste profile.
 *
 * [score] must be in the range 0.0–1.0.
 * The fill colour transitions along a red → orange → green gradient:
 *   0.0 = red, 0.5 = orange/amber, 1.0 = green.
 */
@Composable
fun MatchScoreBar(
    score: Float,
    modifier: Modifier = Modifier
) {
    val clamped = score.coerceIn(0f, 1f)
    val fillColor = matchScoreColor(clamped)
    val trackColor = fillColor.copy(alpha = 0.18f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(trackColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(clamped)
                .height(4.dp)
                .background(fillColor)
        )
    }
}

/** Red (0) → Orange (0.5) → Green (1.0) continuous gradient. */
private fun matchScoreColor(score: Float): Color {
    val red    = Color(0xFFE04040)
    val orange = Color(0xFFE8A020)
    val green  = Color(0xFF5DCAA5)
    return if (score < 0.5f) {
        lerp(red, orange, (score / 0.5f).coerceIn(0f, 1f))
    } else {
        lerp(orange, green, ((score - 0.5f) / 0.5f).coerceIn(0f, 1f))
    }
}

