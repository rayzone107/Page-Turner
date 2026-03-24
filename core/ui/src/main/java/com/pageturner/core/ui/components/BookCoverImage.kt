package com.pageturner.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.palette.graphics.Palette
import coil3.BitmapImage
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
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
 * Falls back to a placeholder icon if the URL is null or the load fails.
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
                onState = { state ->
                    if (state is AsyncImagePainter.State.Success) {
                        val bitmap = (state.result.image as? BitmapImage)?.bitmap
                        if (bitmap != null) {
                            coroutineScope.launch {
                                val extracted = withContext(Dispatchers.Default) {
                                    // Coil decodes images as HARDWARE bitmaps by default.
                                    // HARDWARE bitmaps don't allow pixel access (getPixels),
                                    // which Palette requires — copy to a software bitmap first.
                                    val softCopy = bitmap.copy(android.graphics.Bitmap.Config.ARGB_8888, false)
                                    val color = Palette.from(softCopy).generate()
                                        .getDominantColor(0xFF888888.toInt())
                                    softCopy.recycle()
                                    color
                                }
                                dominantColor = Color(extracted)
                            }
                        }
                    }
                },
            ) {
                when (painter.state) {
                    is AsyncImagePainter.State.Loading,
                    is AsyncImagePainter.State.Empty,
                    is AsyncImagePainter.State.Error -> BookCoverPlaceholder(modifier = Modifier.fillMaxSize())
                    else -> SubcomposeAsyncImageContent()
                }
            }
        } else {
            BookCoverPlaceholder(modifier = Modifier.fillMaxSize())
        }
    }
}

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
