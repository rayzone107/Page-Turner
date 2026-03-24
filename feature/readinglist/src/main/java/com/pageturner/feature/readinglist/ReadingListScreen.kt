package com.pageturner.feature.readinglist

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pageturner.core.ui.components.BookCoverImage
import com.pageturner.core.ui.components.EmptyShelfState
import com.pageturner.core.ui.components.PageTurnerTopBar
import com.pageturner.core.ui.theme.PageTurnerColors
import com.pageturner.core.ui.theme.PageTurnerSpacing
import com.pageturner.core.ui.theme.PageTurnerType
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingListScreen(
    onNavigateToDetail: (String) -> Unit,
    viewModel: ReadingListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var bookPendingRemoval by remember { mutableStateOf<SavedBookUiModel?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.sideEffects.collect { effect ->
            when (effect) {
                is ReadingListSideEffect.NavigateToDetail -> onNavigateToDetail(effect.bookKey)
            }
        }
    }

    // Remove confirmation bottom sheet
    bookPendingRemoval?.let { book ->
        ModalBottomSheet(
            onDismissRequest = { bookPendingRemoval = null },
            sheetState = sheetState,
            containerColor = PageTurnerColors.SurfaceVariant,
        ) {
            RemoveConfirmationSheet(
                book = book,
                onConfirm = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        viewModel.handleIntent(ReadingListIntent.RemoveBook(book.bookKey))
                        bookPendingRemoval = null
                    }
                },
                onDismiss = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        bookPendingRemoval = null
                    }
                },
            )
        }
    }

    Scaffold(
        topBar = { PageTurnerTopBar(title = "Reading List") },
        containerColor = PageTurnerColors.Background,
    ) { innerPadding ->
        when {
            state.isLoading -> {
                ShimmerGrid(modifier = Modifier.padding(innerPadding))
            }

            state.isEmpty -> {
                EmptyShelfState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState()),
                ) {
                    if (state.likedBooks.isNotEmpty()) {
                        SectionHeader(
                            title = "❤️ Liked",
                            modifier = Modifier.padding(
                                horizontal = PageTurnerSpacing.md,
                                vertical = PageTurnerSpacing.sm,
                            ),
                        )
                        BookGrid(
                            books = state.likedBooks,
                            onBookClick = { viewModel.handleIntent(ReadingListIntent.SelectBook(it)) },
                            onBookLongClick = { book -> bookPendingRemoval = book },
                        )
                    }

                    if (state.bookmarkedBooks.isNotEmpty()) {
                        SectionHeader(
                            title = "🔖 Bookmarked",
                            modifier = Modifier.padding(
                                horizontal = PageTurnerSpacing.md,
                                vertical = PageTurnerSpacing.sm,
                            ),
                        )
                        BookGrid(
                            books = state.bookmarkedBooks,
                            onBookClick = { viewModel.handleIntent(ReadingListIntent.SelectBook(it)) },
                            onBookLongClick = { book -> bookPendingRemoval = book },
                        )
                    }

                    Spacer(Modifier.height(PageTurnerSpacing.xl))
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Section header
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = PageTurnerType.CardTitle,
        color = PageTurnerColors.OnSurface,
        modifier = modifier,
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Non-lazy book grid (used inside a verticalScroll Column)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BookGrid(
    books: List<SavedBookUiModel>,
    onBookClick: (String) -> Unit,
    onBookLongClick: (SavedBookUiModel) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Simple manual 3-column grid that plays nicely inside a scrollable Column.
    val rows = books.chunked(3)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PageTurnerSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(PageTurnerSpacing.sm),
    ) {
        rows.forEach { rowBooks ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PageTurnerSpacing.sm),
            ) {
                rowBooks.forEach { book ->
                    BookGridCell(
                        book = book,
                        onClick = { onBookClick(book.bookKey) },
                        onLongClick = { onBookLongClick(book) },
                        modifier = Modifier.weight(1f),
                    )
                }
                // Fill empty slots in the last row so items don't stretch.
                repeat(3 - rowBooks.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Grid cell
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookGridCell(
    book: SavedBookUiModel,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .aspectRatio(0.67f) // portrait book proportions
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .semantics { contentDescription = "${book.title} — long press to remove" },
    ) {
        BookCoverImage(
            coverUrl = book.coverUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
        )

        // Title scrim at the bottom so text is readable over any cover art.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(PageTurnerColors.Background.copy(alpha = 0.72f))
                .padding(horizontal = PageTurnerSpacing.xs, vertical = PageTurnerSpacing.xs),
        ) {
            Text(
                text = book.title,
                style = PageTurnerType.BodySmall,
                color = PageTurnerColors.OnSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Remove confirmation sheet
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun RemoveConfirmationSheet(
    book: SavedBookUiModel,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PageTurnerSpacing.lg, vertical = PageTurnerSpacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Remove from list?",
            style = PageTurnerType.CardTitle,
            color = PageTurnerColors.OnSurface,
        )
        Spacer(Modifier.height(PageTurnerSpacing.sm))
        Text(
            text = "\"${book.title}\" will be removed from your reading list.",
            style = PageTurnerType.Body,
            color = PageTurnerColors.OnSurfaceMuted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(PageTurnerSpacing.lg))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Keep",
                    style = PageTurnerType.Body,
                    color = PageTurnerColors.OnSurfaceMuted,
                )
            }
            Spacer(Modifier.width(PageTurnerSpacing.sm))
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PageTurnerColors.Error,
                    contentColor = PageTurnerColors.OnBackground,
                ),
            ) {
                Text(text = "Remove", style = PageTurnerType.Body)
            }
        }
        Spacer(Modifier.height(PageTurnerSpacing.xl))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shimmer loading grid
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ShimmerGrid(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "grid_shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "shimmer_alpha",
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(PageTurnerSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(PageTurnerSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(PageTurnerSpacing.sm),
        userScrollEnabled = false,
    ) {
        items(12) {
            Box(
                modifier = Modifier
                    .aspectRatio(0.67f)
                    .clip(RoundedCornerShape(12.dp))
                    .alpha(alpha)
                    .background(PageTurnerColors.SurfaceVariant),
            )
        }
    }
}
