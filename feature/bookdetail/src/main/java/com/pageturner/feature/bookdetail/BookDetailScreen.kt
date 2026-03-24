package com.pageturner.feature.bookdetail

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pageturner.core.ui.components.AiBriefText
import com.pageturner.core.ui.components.BookCoverImage
import com.pageturner.core.ui.components.ErrorState
import com.pageturner.core.ui.components.GenreChip
import com.pageturner.core.ui.components.LoadingIndicator
import com.pageturner.core.ui.components.OfflineBanner
import com.pageturner.core.ui.components.PageTurnerTopBar
import com.pageturner.core.ui.components.WildcardChip
import com.pageturner.core.ui.theme.PageTurnerColors
import com.pageturner.core.ui.theme.PageTurnerSpacing
import com.pageturner.core.ui.theme.PageTurnerType

/** Shared element key convention: must match the key used in swipe deck and reading list. */
fun bookCoverSharedKey(bookKey: String) = "book_cover_$bookKey"

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun BookDetailScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onNavigateBack: () -> Unit,
    onOpenUrl: (String) -> Unit,
    viewModel: BookDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.sideEffects.collect { effect ->
            when (effect) {
                BookDetailSideEffect.NavigateBack -> onNavigateBack()
                is BookDetailSideEffect.OpenUrl -> onOpenUrl(effect.url)
            }
        }
    }

    Scaffold(
        topBar = {
            PageTurnerTopBar(
                title = state.book?.title ?: "",
                onBackClick = { viewModel.handleIntent(BookDetailIntent.NavigateBack) },
            )
        },
        containerColor = PageTurnerColors.Background,
    ) { innerPadding ->
        when {
            state.isLoading -> {
                LoadingIndicator(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }

            state.error != null && state.book == null -> {
                ErrorState(
                    message = state.error!!.message,
                    onRetry = { viewModel.handleIntent(BookDetailIntent.Retry) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }

            state.book != null -> {
                with(sharedTransitionScope) {
                    BookDetailContent(
                        book = state.book!!,
                        isOffline = state.isOffline,
                        isSaved = state.isSaved,
                        animatedVisibilityScope = animatedVisibilityScope,
                        onOpenLibrary = { viewModel.handleIntent(BookDetailIntent.OpenOnOpenLibrary) },
                        onRemoveFromList = { viewModel.handleIntent(BookDetailIntent.RemoveFromList) },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Content
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SharedTransitionScope.BookDetailContent(
    book: BookDetailUiModel,
    isOffline: Boolean,
    isSaved: Boolean,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onOpenLibrary: () -> Unit,
    onRemoveFromList: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        if (isOffline) OfflineBanner()

        // ── Hero cover with shared element transition ──────────────────────
        Box {
            BookCoverImage(
                coverUrl = book.coverUrl,
                contentDescription = "${book.title} cover",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .sharedElement(
                        sharedContentState = rememberSharedContentState(key = bookCoverSharedKey(book.bookKey)),
                        animatedVisibilityScope = animatedVisibilityScope,
                    ),
            )

            // Dark gradient scrim so title/author overlay is readable.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                PageTurnerColors.Background.copy(alpha = 0f),
                                PageTurnerColors.Background.copy(alpha = 0.95f),
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(PageTurnerSpacing.md),
            ) {
                if (book.isWildcard) {
                    WildcardChip()
                    Spacer(Modifier.height(PageTurnerSpacing.xs))
                }
                Text(
                    text = book.title,
                    style = PageTurnerType.DetailTitle,
                    color = PageTurnerColors.OnBackground,
                )
                Text(
                    text = book.authors.joinToString(", "),
                    style = PageTurnerType.Body,
                    color = PageTurnerColors.OnSurfaceMuted,
                )
            }
        }

        // ── Body ──────────────────────────────────────────────────────────
        Column(modifier = Modifier.padding(PageTurnerSpacing.md)) {

            // Year + page count chips
            val metaChips = listOfNotNull(
                book.publishYear?.toString(),
                book.pageCount?.let { "$it pages" },
            )
            if (metaChips.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(PageTurnerSpacing.sm)) {
                    metaChips.forEach { label ->
                        MetaChip(label = label)
                    }
                }
                Spacer(Modifier.height(PageTurnerSpacing.md))
            }

            // Subject chips — horizontally scrollable so they never wrap
            if (book.subjects.isNotEmpty()) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(PageTurnerSpacing.xs),
                ) {
                    book.subjects.forEach { subject -> GenreChip(label = subject) }
                }
                Spacer(Modifier.height(PageTurnerSpacing.md))
            }

            // AI brief
            book.aiBrief?.takeIf { it.isNotBlank() }?.let { brief ->
                AiBriefText(brief = brief, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(PageTurnerSpacing.md))
            }

            // Wildcard reason
            book.wildcardReason?.let { reason ->
                WildcardReasonBlock(reason = reason)
                Spacer(Modifier.height(PageTurnerSpacing.md))
            }

            // Description with expand/collapse
            book.description?.let { desc ->
                ExpandableDescription(description = desc)
                Spacer(Modifier.height(PageTurnerSpacing.md))
            }

            // Open Library link
            Text(
                text = "View on Open Library →",
                style = PageTurnerType.Body,
                color = PageTurnerColors.Accent,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable(onClick = onOpenLibrary),
            )

            // Remove from list button — only shown when book is in the reading list
            if (isSaved) {
                Spacer(Modifier.height(PageTurnerSpacing.md))
                Button(
                    onClick = onRemoveFromList,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PageTurnerColors.Error.copy(alpha = 0.12f),
                        contentColor = PageTurnerColors.Error,
                    ),
                ) {
                    Text(text = "Remove from Reading List", style = PageTurnerType.Body)
                }
            }

            Spacer(Modifier.height(PageTurnerSpacing.xl))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Local components
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MetaChip(label: String, modifier: Modifier = Modifier) {
    Text(
        text = label,
        style = PageTurnerType.Chip,
        color = PageTurnerColors.OnSurfaceMuted,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(PageTurnerColors.SurfaceVariant)
            .padding(horizontal = PageTurnerSpacing.sm, vertical = PageTurnerSpacing.xs),
    )
}

@Composable
private fun WildcardReasonBlock(reason: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(56.dp)
                .background(PageTurnerColors.Teal)
        )
        Column(modifier = Modifier.padding(start = PageTurnerSpacing.sm)) {
            Text(
                text = "We picked this because…",
                style = PageTurnerType.Label,
                color = PageTurnerColors.Teal,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = reason,
                style = PageTurnerType.AiBrief,
                color = PageTurnerColors.OnSurface,
            )
        }
    }
}

@Composable
private fun ExpandableDescription(description: String, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(
            text = description,
            style = PageTurnerType.Body,
            color = PageTurnerColors.OnSurfaceMuted,
            maxLines = if (expanded) Int.MAX_VALUE else 3,
            overflow = if (expanded) TextOverflow.Clip else TextOverflow.Ellipsis,
        )
        if (!expanded) {
            Spacer(Modifier.height(PageTurnerSpacing.xs))
            Text(
                text = "Show more",
                style = PageTurnerType.BodySmall,
                color = PageTurnerColors.Accent,
                modifier = Modifier.clickable { expanded = true },
            )
        }
    }
}

