package com.pageturner.feature.swipedeck

import com.pageturner.core.domain.error.UiError
import com.pageturner.core.domain.model.Book

/** Full MVI contract for the Swipe Deck screen. */

data class SwipeDeckUiState(
    val cards: List<SwipeCardUiModel> = emptyList(),
    val isLoading: Boolean = true,
    val isGeneratingBrief: Boolean = false,
    val isReplenishing: Boolean = false,
    val error: UiError? = null,
    val isOffline: Boolean = false,
    val swipeCount: Int = 0,
    val swipesUntilProfileUpdate: Int = 10,
    val currentCardIndex: Int = 0,
)

data class SwipeCardUiModel(
    val bookKey: String,
    val title: String,
    val authors: List<String>,
    val coverUrl: String?,
    val publishYear: Int?,
    val pageCount: Int?,
    val subjects: List<String>,
    val aiBrief: String?,
    val matchScore: Float,
    val isWildcard: Boolean,
    val wildcardReason: String?,
    /** True when the AI brief could not be generated due to the quota being exceeded. */
    val isAiQuotaExceeded: Boolean = false,
)

internal fun Book.toUiModel(
    isWildcard: Boolean = this.isWildcard,
    wildcardReason: String? = this.wildcardReason,
    matchScore: Float = this.matchScore,
): SwipeCardUiModel = SwipeCardUiModel(
    bookKey = key,
    title = title,
    authors = authors,
    coverUrl = coverUrl,
    publishYear = publishYear,
    pageCount = pageCount,
    subjects = subjects,
    aiBrief = aiBrief,
    matchScore = matchScore,
    isWildcard = isWildcard,
    wildcardReason = wildcardReason,
)

sealed class SwipeDeckIntent {
    data class SwipeLeft(val bookKey: String) : SwipeDeckIntent()
    data class SwipeRight(val bookKey: String) : SwipeDeckIntent()
    data class Bookmark(val bookKey: String) : SwipeDeckIntent()
    data class ExpandCard(val bookKey: String) : SwipeDeckIntent()
    data object Retry : SwipeDeckIntent()
    data object LoadMore : SwipeDeckIntent()
}

sealed class SwipeDeckSideEffect {
    data class NavigateToDetail(val bookKey: String) : SwipeDeckSideEffect()
    data object TriggerProfileUpdate : SwipeDeckSideEffect()
    data class ShowSnackbar(val message: String) : SwipeDeckSideEffect()
}
