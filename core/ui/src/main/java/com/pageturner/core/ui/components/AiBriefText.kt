package com.pageturner.core.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.pageturner.core.ui.theme.PageTurnerColors
import com.pageturner.core.ui.theme.PageTurnerSpacing
import com.pageturner.core.ui.theme.PageTurnerType

/** Small "✦ AI Summary" attribution row reused by both the text and shimmer variants. */
@Composable
private fun AiLabel() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = "AI generated",
            tint = PageTurnerColors.Teal,
            modifier = Modifier.size(12.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = "AI Summary",
            style = PageTurnerType.Label,
            color = PageTurnerColors.Teal,
        )
    }
}

/**
 * Displays an AI-generated brief with an AI attribution label, amber left border,
 * and italic text.
 */
@Composable
fun AiBriefText(
    brief: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        AiLabel()
        Spacer(Modifier.height(4.dp))
        Row {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(with(androidx.compose.ui.platform.LocalDensity.current) {
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

    Column(modifier = modifier) {
        AiLabel()
        Spacer(Modifier.height(4.dp))
        Row(modifier = Modifier.alpha(alpha)) {
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
}
