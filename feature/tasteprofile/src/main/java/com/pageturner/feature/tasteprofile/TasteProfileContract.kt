package com.pageturner.feature.tasteprofile

import com.pageturner.core.domain.error.UiError
import com.pageturner.core.domain.model.TasteProfile

data class TasteProfileUiState(
    val isLoading: Boolean = false,
    val profile: TasteProfileUiModel? = null,
    val swipeStats: SwipeStats = SwipeStats(),
    val error: UiError? = null,
)

data class TasteProfileUiModel(
    val aiSummary: String,
    val likedGenres: List<String>,
    val avoidedGenres: List<String>,
    val preferredLength: String,
    val lastUpdatedSwipeCount: Int,
)

data class SwipeStats(
    val totalSwiped: Int = 0,
    val totalSaved: Int = 0,
    val wildcardKept: Int = 0,
)

internal fun TasteProfile.toUiModel() = TasteProfileUiModel(
    aiSummary = aiSummary,
    likedGenres = likedGenres,
    avoidedGenres = avoidedGenres,
    preferredLength = preferredLength,
    lastUpdatedSwipeCount = lastUpdatedSwipeCount,
)

sealed class TasteProfileIntent {
    data object Refresh : TasteProfileIntent()
}

sealed class TasteProfileSideEffect {
    data class ShowSnackbar(val message: String) : TasteProfileSideEffect()
}
