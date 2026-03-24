package com.pageturner.core.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.palette.graphics.Palette
import coil3.BitmapImage
import coil3.compose.SubcomposeAsyncImage
import com.pageturner.core.ui.theme.PageTurnerColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Loads a book cover from [coverUrl] using Coil.
 *
 * The image is displayed with [ContentScale.Fit] so the entire cover is always visible.
 * Any empty space around the image is filled with the dominant colour extracted from
 * the cover using the Palette API, giving a natural-looking background.
 *
 * Shows a pulsing shimmer placeholder while loading, and falls back to a static icon
 * if the URL is null or the load fails.
 */
@Composable
fun BookCoverImage(
    coverUrl: String?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    var dominantColor by remember { mutableStateOf<Color?>(null) }
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = modifier.background(dominantColor ?: PageTurnerColors.SurfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (coverUrl != null) {
            SubcomposeAsyncImage(
                model = coverUrl,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
                loading = { CoverShimmerPlaceholder(modifier = Modifier.fillMaxSize()) },
                error = { BookCoverPlaceholder(modifier = Modifier.fillMaxSize()) },
                onSuccess = { successState ->
                    val bitmap = (successState.result.image as? BitmapImage)?.bitmap
                    if (bitmap != null) {
                        coroutineScope.launch {
                            val extracted = withContext(Dispatchers.Default) {
                                val softCopy = bitmap.copy(android.graphics.Bitmap.Config.ARGB_8888, false)
                                val color = Palette.from(softCopy).generate()
                                    .getDominantColor(0xFF888888.toInt())
                                softCopy.recycle()
                                color
                            }
                            dominantColor = Color(extracted)
                        }
                    }
                },
            )
        } else {
            BookCoverPlaceholder(modifier = Modifier.fillMaxSize())
        }
    }
}

/** Pulsing shimmer shown while a cover image is loading. */
@Composable
private fun CoverShimmerPlaceholder(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "cover_shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "cover_shimmer_alpha",
    )

    Box(
        modifier = modifier.background(PageTurnerColors.SurfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.MenuBook,
            contentDescription = null,
            tint = PageTurnerColors.OnSurfaceMuted,
            modifier = Modifier
                .size(48.dp)
                .alpha(alpha),
        )
    }
}

/** Static fallback shown when the URL is null or the load fails. */
@Composable
private fun BookCoverPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(PageTurnerColors.SurfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.MenuBook,
            contentDescription = null,
            tint = PageTurnerColors.OnSurfaceMuted
        )
    }
}
