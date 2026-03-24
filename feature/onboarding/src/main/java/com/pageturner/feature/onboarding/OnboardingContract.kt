package com.pageturner.feature.onboarding

import com.pageturner.core.domain.model.Genre
import com.pageturner.core.domain.model.ReadingLength

data class OnboardingUiState(
    val genres: List<Genre> = Genre.all(),
    val selectedGenres: Set<Genre> = emptySet(),
    val selectedLength: ReadingLength? = null,
    val canProceed: Boolean = false,   // true when >= 1 genre AND 1 length selected
    val isLoading: Boolean = false
)

sealed class OnboardingIntent {
    data class ToggleGenre(val genre: Genre) : OnboardingIntent()
    data class SelectLength(val length: ReadingLength) : OnboardingIntent()
    data object Confirm : OnboardingIntent()
}

sealed class OnboardingSideEffect {
    data object NavigateToSwipeDeck : OnboardingSideEffect()
}
