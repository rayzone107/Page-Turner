package com.pageturner.feature.swipedeck

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pageturner.core.domain.model.SwipeDirection
import com.pageturner.core.ui.components.AiBriefShimmer
import com.pageturner.core.ui.components.AiBriefText
import com.pageturner.core.ui.components.AiLearningIndicator
import com.pageturner.core.ui.components.BookCoverImage
import com.pageturner.core.ui.components.EmptyShelfState
import com.pageturner.core.ui.components.ErrorState
import com.pageturner.core.ui.components.GenreChip
import com.pageturner.core.ui.components.LoadingIndicator
import com.pageturner.core.ui.components.MatchScoreBar
import com.pageturner.core.ui.components.OfflineBanner
import com.pageturner.core.ui.components.PageTurnerTopBar
import com.pageturner.core.ui.components.SwipeActionButton
import com.pageturner.core.ui.components.SwipeActionType
import com.pageturner.core.ui.components.WildcardChip
import com.pageturner.core.ui.theme.PageTurnerColors
import com.pageturner.core.ui.theme.PageTurnerSpacing
import com.pageturner.core.ui.theme.PageTurnerType
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private fun lerp(start: Float, stop: Float, fraction: Float) =
    start + (stop - start) * fraction.coerceIn(0f, 1f)

@Composable
fun SwipeDeckScreen(
    onNavigateToDetail: (String) -> Unit,
    viewModel: SwipeDeckViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Tracks a pending programmatic swipe triggered by an action button.
    // Cleared after the animation fires (inside the SwipeCard LaunchedEffect).
    var pendingButtonSwipe by remember { mutableStateOf<SwipeDirection?>(null) }

    LaunchedEffect(Unit) {
        viewModel.sideEffects.collect { effect ->
            when (effect) {
                is SwipeDeckSideEffect.NavigateToDetail -> onNavigateToDetail(effect.bookKey)
                is SwipeDeckSideEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
                SwipeDeckSideEffect.TriggerProfileUpdate -> Unit // handled inside VM
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
        PageTurnerTopBar(
            title = "Page Turner",
            actions = { AiLearningIndicator() }
        )
        },
        containerColor = PageTurnerColors.Background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (state.isOffline) OfflineBanner()

            when {
                state.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        LoadingIndicator()
                    }
                }

                state.error != null -> {
                    ErrorState(
                        message = state.error!!.message,
                        onRetry = { viewModel.handleIntent(SwipeDeckIntent.Retry) },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                state.currentCardIndex >= state.cards.size -> {
                    if (state.isReplenishing) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            LoadingIndicator()
                        }
                    } else {
                        EmptyShelfState(modifier = Modifier.fillMaxSize())
                    }
                }

                else -> {
                    val currentCard = state.cards[state.currentCardIndex]

                    SwipeCardStack(
                        cards = state.cards,
                        currentIndex = state.currentCardIndex,
                        pendingButtonSwipe = pendingButtonSwipe,
                        onSwipeLeft = { bookKey ->
                            viewModel.handleIntent(SwipeDeckIntent.SwipeLeft(bookKey))
                            pendingButtonSwipe = null
                        },
                        onSwipeRight = { bookKey ->
                            viewModel.handleIntent(SwipeDeckIntent.SwipeRight(bookKey))
                            pendingButtonSwipe = null
                        },
                        onBookmark = { bookKey ->
                            viewModel.handleIntent(SwipeDeckIntent.Bookmark(bookKey))
                            pendingButtonSwipe = null
                        },
                        onExpand = { viewModel.handleIntent(SwipeDeckIntent.ExpandCard(it)) },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = PageTurnerSpacing.md, vertical = PageTurnerSpacing.sm)
                    )

                    SwipeCountFooter(
                        swipeCount = state.swipeCount,
                        swipesUntilUpdate = state.swipesUntilProfileUpdate,
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = PageTurnerSpacing.xl)
                            .padding(bottom = PageTurnerSpacing.lg),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SwipeActionButton(
                            type = SwipeActionType.SKIP,
                            onClick = { pendingButtonSwipe = SwipeDirection.LEFT },
                        )
                        SwipeActionButton(
                            type = SwipeActionType.BOOKMARK,
                            onClick = { pendingButtonSwipe = SwipeDirection.BOOKMARK },
                        )
                        SwipeActionButton(
                            type = SwipeActionType.SAVE,
                            onClick = { pendingButtonSwipe = SwipeDirection.RIGHT },
                        )
                    }
                }
            }
        }
    }
}

// --- Card stack ---

@Composable
private fun SwipeCardStack(
    cards: List<SwipeCardUiModel>,
    currentIndex: Int,
    pendingButtonSwipe: SwipeDirection?,
    onSwipeLeft: (String) -> Unit,
    onSwipeRight: (String) -> Unit,
    onBookmark: (String) -> Unit,
    onExpand: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Normalised progress of the top card's drag: 0 = at rest, 1 = threshold reached.
    // Background cards interpolate toward their "promoted" (one-level-closer) position.
    var swipeProgress by remember { mutableFloatStateOf(0f) }

    // Snap back to rest whenever a new top card is shown.
    LaunchedEffect(currentIndex) { swipeProgress = 0f }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Render back-to-front so the top card paints last (on top).
        for (stackDepth in 2 downTo 0) {
            val card = cards.getOrNull(currentIndex + stackDepth) ?: continue
            val isTop = stackDepth == 0

            // Resting position for this depth.
            val restingScale  = 1f - stackDepth * 0.04f
            val restingOffset = stackDepth * 14

            // Target position one step closer to the top (where this card will be
            // once the current top card is swiped away).
            val promotedScale  = if (stackDepth > 0) 1f - (stackDepth - 1) * 0.04f else 1f
            val promotedOffset = if (stackDepth > 0) (stackDepth - 1) * 14 else 0

            val currentScale  = lerp(restingScale,  promotedScale,  swipeProgress)
            val currentOffset = lerp(restingOffset.toFloat(), promotedOffset.toFloat(), swipeProgress).dp

            // Using key(bookKey) for ALL depths means Compose reuses the same StackedCard
            // composable when a card rises from depth-1 → depth-0.  The Animatable state
            // (offsetX = 0) is preserved, so there is no layout flash on the new top card.
            key(card.bookKey) {
                StackedCard(
                    card = card,
                    isTop = isTop,
                    swipeTrigger = if (isTop) pendingButtonSwipe else null,
                    onSwipeLeft = { onSwipeLeft(card.bookKey) },
                    onSwipeRight = { onSwipeRight(card.bookKey) },
                    onBookmark = { onBookmark(card.bookKey) },
                    onExpand = { onExpand(card.bookKey) },
                    onDragProgress = { p -> swipeProgress = p },
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(currentScale)
                        .offset(y = currentOffset),
                )
            }
        }
    }
}

// Single composable for all stack depths — Compose reuses it via key(bookKey)
// when a card rises from depth-1 to depth-0, eliminating the flash.

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StackedCard(
    card: SwipeCardUiModel,
    isTop: Boolean,
    swipeTrigger: SwipeDirection?,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    onBookmark: () -> Unit,
    onExpand: () -> Unit,
    onDragProgress: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }

    // Captured via onSizeChanged so the programmatic animation uses the real card width.
    var cardWidthPx by remember { mutableStateOf(0f) }

    // Swipe threshold in px — drag past this to commit the swipe.
    val swipeThresholdPx = 120.dp.value * 3f // ~360f at typical density

    val normalizedX = (offsetX.value / swipeThresholdPx).coerceIn(-1f, 1f)
    // Only the top card rotates and shows overlays; background cards stay upright.
    val rotation = if (isTop) normalizedX * 12f else 0f
    val overlayAlpha = abs(normalizedX).coerceIn(0f, 0.75f)

    // Report drag progress to SwipeCardStack after every recomposition of the top card
    // (offsetX changes → recompose → SideEffect fires → background cards animate upward).
    SideEffect {
        if (isTop) onDragProgress(abs(normalizedX))
    }

    // Programmatic swipe driven by action buttons (top card only).
    LaunchedEffect(swipeTrigger) {
        if (!isTop) return@LaunchedEffect
        val w = if (cardWidthPx > 0f) cardWidthPx else 1080f
        when (swipeTrigger) {
            SwipeDirection.LEFT -> {
                offsetX.animateTo(-w * 2f, tween(durationMillis = 300))
                onSwipeLeft()
            }
            SwipeDirection.RIGHT -> {
                offsetX.animateTo(w * 2f, tween(durationMillis = 300))
                onSwipeRight()
            }
            SwipeDirection.BOOKMARK -> {
                // Animate upward for a distinct bookmark gesture.
                offsetY.animateTo(-w * 2f, tween(durationMillis = 300))
                onBookmark()
            }
            null -> { /* gesture-driven swipes manage their own animation */ }
        }
    }

    Card(
        onClick = if (isTop) onExpand else { {} },
        modifier = modifier
            .then(if (isTop) Modifier.onSizeChanged { cardWidthPx = it.width.toFloat() } else Modifier)
            .offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
            .rotate(rotation)
            .border(1.dp, PageTurnerColors.CardBorder, RoundedCornerShape(20.dp))
            .then(
                if (isTop) Modifier.pointerInput(Unit) {
                    val widthPx = size.width.toFloat()
                    detectDragGestures(
                        onDragEnd = {
                            coroutineScope.launch {
                                when {
                                    offsetX.value > swipeThresholdPx -> {
                                        offsetX.animateTo(widthPx * 2f, tween(durationMillis = 280))
                                        onSwipeRight()
                                    }
                                    offsetX.value < -swipeThresholdPx -> {
                                        offsetX.animateTo(-widthPx * 2f, tween(durationMillis = 280))
                                        onSwipeLeft()
                                    }
                                    else -> {
                                        launch { offsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow)) }
                                        launch { offsetY.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow)) }
                                    }
                                }
                            }
                        },
                        onDragCancel = {
                            coroutineScope.launch {
                                launch { offsetX.animateTo(0f, spring()) }
                                launch { offsetY.animateTo(0f, spring()) }
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            coroutineScope.launch {
                                offsetX.snapTo(offsetX.value + dragAmount.x)
                                offsetY.snapTo(offsetY.value + dragAmount.y * 0.35f)
                            }
                        },
                    )
                } else Modifier
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PageTurnerColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isTop) 8.dp else 2.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Full card content shown at every stack depth
            // Rendering the info panel on background cards means there is zero
            // layout jump when a card rises from depth-1 to depth-0 (top).
            // Vertical scrolling and interactive buttons are top-card-only.
            Column(modifier = Modifier.fillMaxSize()) {
                BookCoverImage(
                    coverUrl = card.coverUrl,
                    contentDescription = if (isTop) "${card.title} cover" else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.58f)
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
                )
                Column(
                    modifier = Modifier
                        .weight(0.42f)
                        .fillMaxWidth()
                        .then(
                            if (isTop) Modifier.verticalScroll(rememberScrollState())
                            else Modifier
                        )
                        .padding(PageTurnerSpacing.md),
                ) {
                    if (card.isWildcard) {
                        WildcardChip()
                        Spacer(Modifier.height(PageTurnerSpacing.xs))
                    }
                    Text(
                        text = card.title,
                        style = PageTurnerType.CardTitle,
                        color = PageTurnerColors.OnSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = card.authors.take(2).joinToString(", "),
                        style = PageTurnerType.Body,
                        color = PageTurnerColors.OnSurfaceMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val meta = listOfNotNull(
                        card.publishYear?.toString(),
                        card.pageCount?.let { "$it pages" },
                    ).joinToString(" · ")
                    if (meta.isNotBlank()) {
                        Text(
                            text = meta,
                            style = PageTurnerType.BodySmall,
                            color = PageTurnerColors.OnSurfaceMuted,
                        )
                    }
                    Spacer(Modifier.height(PageTurnerSpacing.xs))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(PageTurnerSpacing.xs),
                        verticalArrangement = Arrangement.spacedBy(PageTurnerSpacing.xs),
                    ) {
                        card.subjects.take(3).forEach { subject -> GenreChip(label = subject) }
                    }
                    Spacer(Modifier.height(PageTurnerSpacing.sm))
                    MatchScoreBar(score = card.matchScore, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(PageTurnerSpacing.sm))
                    // Only show shimmer on the top card to avoid infinite-animation
                    // recomposition on obscured background cards.
                    when {
                        card.isAiQuotaExceeded -> Text(
                            text = "AI quota reached — brief unavailable",
                            style = PageTurnerType.BodySmall,
                            color = PageTurnerColors.Error,
                        )
                        card.aiBrief == null && isTop -> AiBriefShimmer()
                        !card.aiBrief.isNullOrBlank() -> AiBriefText(brief = card.aiBrief)
                    }
                    // "Details →" button only makes sense on the top interactive card.
                    if (isTop) {
                        TextButton(
                            onClick = onExpand,
                            modifier = Modifier.align(Alignment.End),
                        ) {
                            Text(
                                text = "Details →",
                                style = PageTurnerType.Label,
                                color = PageTurnerColors.Accent,
                            )
                        }
                    }
                }
            }

            // Swipe direction overlays (top card only)
            if (isTop) {
                if (normalizedX > 0.05f) {
                    SwipeOverlay(
                        color = PageTurnerColors.SaveGreen,
                        labelColor = PageTurnerColors.Teal,
                        label = "ADD TO LIST →",
                        alpha = overlayAlpha,
                        labelAlignment = Alignment.TopEnd,
                    )
                } else if (normalizedX < -0.05f) {
                    SwipeOverlay(
                        color = PageTurnerColors.SkipRed,
                        labelColor = PageTurnerColors.Error,
                        label = "← SKIP",
                        alpha = overlayAlpha,
                        labelAlignment = Alignment.TopStart,
                    )
                }
            }
        }
    }
}

@Composable
private fun SwipeOverlay(
    color: Color,
    labelColor: Color,
    label: String,
    alpha: Float,
    labelAlignment: Alignment,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color.copy(alpha = alpha * 0.6f))
    ) {
        Text(
            text = label,
            style = PageTurnerType.Label,
            color = labelColor.copy(alpha = alpha),
            modifier = Modifier
                .align(labelAlignment)
                .padding(PageTurnerSpacing.md),
        )
    }
}

// --- Footer ---

@Composable
private fun SwipeCountFooter(
    swipeCount: Int,
    swipesUntilUpdate: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PageTurnerSpacing.md, vertical = PageTurnerSpacing.xs),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "$swipeCount swipes",
            style = PageTurnerType.Label,
            color = PageTurnerColors.OnSurfaceMuted,
        )
        Spacer(Modifier.width(PageTurnerSpacing.sm))
        Text(
            text = "·",
            style = PageTurnerType.Label,
            color = PageTurnerColors.OnSurfaceMuted,
        )
        Spacer(Modifier.width(PageTurnerSpacing.sm))
        Text(
            text = "profile updating in $swipesUntilUpdate",
            style = PageTurnerType.Label,
            color = PageTurnerColors.OnSurfaceMuted,
        )
    }
}
