package com.pageturner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pageturner.core.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    profileRepository: ProfileRepository,
) : ViewModel() {

    /**
     * Emits null while loading, then the correct start destination.
     * Uses [SharingStarted.Eagerly] so the Room read begins immediately on creation —
     * before any subscriber is attached — minimising the splash duration.
     */
    val startDestination: StateFlow<String?> = profileRepository.isOnboardingComplete()
        .map { isComplete -> if (isComplete) AppRoute.SWIPE_DECK else AppRoute.ONBOARDING }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null,
        )
}
