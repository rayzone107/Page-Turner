package com.pageturner.core.domain.model

/**
 * Cold-start seed set by the user during onboarding.
 * Drives the initial swipe queue before the AI profile has enough data.
 */
data class OnboardingPreferences(
    val selectedGenres: List<Genre>,
    val selectedLengths: List<ReadingLength>,
    val completedAt: Long
)
