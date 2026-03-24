package com.pageturner.core.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.unit.dp
import com.pageturner.core.ui.theme.PageTurnerColors
import com.pageturner.core.ui.theme.PageTurnerSpacing
import com.pageturner.core.ui.theme.PageTurnerType

/**
 * Displays an AI-generated brief with an amber left border.
 * All AI content in the app uses this presentation — italic, amber-accented.
 */
@Composable
fun AiBriefText(
    brief: String,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(with(androidx.compose.ui.platform.LocalDensity.current) {
                    // Let the border stretch to match text height dynamically
                    (PageTurnerType.AiBrief.lineHeight.value * 2 + 4).dp
                })
                .background(PageTurnerColors.Accent)
        )
        Text(
            text = brief,
            style = PageTurnerType.AiBrief,
            color = PageTurnerColors.OnSurface,
            modifier = Modifier.padding(start = PageTurnerSpacing.sm)
        )
    }
}

/**
 * Pulsing shimmer placeholder shown while an AI brief is being generated.
 * The card is never blocked — this animates until the brief arrives.
 */
@Composable
fun AiBriefShimmer(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "brief_shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer_alpha"
    )

    Row(modifier = modifier.alpha(alpha)) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(36.dp)
                .background(PageTurnerColors.Accent)
        )
        Column(modifier = Modifier.padding(start = PageTurnerSpacing.sm)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .height(12.dp)
                    .background(PageTurnerColors.OnSurfaceMuted.copy(alpha = 0.3f))
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(12.dp)
                    .background(PageTurnerColors.OnSurfaceMuted.copy(alpha = 0.3f))
            )
        }
    }
}
