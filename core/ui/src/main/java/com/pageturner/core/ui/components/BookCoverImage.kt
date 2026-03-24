package com.pageturner.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import com.pageturner.core.ui.theme.PageTurnerColors

/**
 * Loads a book cover from [coverUrl] using Coil.
 * Falls back to a placeholder icon if the URL is null or the load fails.
 */
@Composable
fun BookCoverImage(
    coverUrl: String?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    if (coverUrl != null) {
        SubcomposeAsyncImage(
            model = coverUrl,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Crop
        ) {
            when (painter.state) {
                is AsyncImagePainter.State.Loading,
                is AsyncImagePainter.State.Empty -> BookCoverPlaceholder(modifier = Modifier.fillMaxSize())
                is AsyncImagePainter.State.Error  -> BookCoverPlaceholder(modifier = Modifier.fillMaxSize())
                else -> SubcomposeAsyncImageContent()
            }
        }
    } else {
        BookCoverPlaceholder(modifier = modifier)
    }
}

@Composable
private fun BookCoverPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(PageTurnerColors.SurfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.MenuBook,
            contentDescription = null,
            tint = PageTurnerColors.OnSurfaceMuted
        )
    }
}
