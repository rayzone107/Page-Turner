package com.pageturner.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pageturner.core.domain.model.Genre
import com.pageturner.core.domain.model.OnboardingPreferences
import com.pageturner.core.domain.model.ReadingLength
import com.pageturner.core.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _sideEffects = Channel<OnboardingSideEffect>(Channel.BUFFERED)
    val sideEffects = _sideEffects.receiveAsFlow()

    fun onIntent(intent: OnboardingIntent) {
        when (intent) {
            is OnboardingIntent.ToggleGenre   -> toggleGenre(intent.genre)
            is OnboardingIntent.SelectLength  -> selectLength(intent.length)
            is OnboardingIntent.Confirm       -> confirm()
        }
    }

    private fun toggleGenre(genre: Genre) {
        _uiState.update { state ->
            val updated = if (genre in state.selectedGenres) {
                state.selectedGenres - genre
            } else {
                state.selectedGenres + genre
            }
            state.copy(
                selectedGenres = updated,
                canProceed = updated.isNotEmpty() && state.selectedLength != null
            )
        }
    }

    private fun selectLength(length: ReadingLength) {
        _uiState.update { state ->
            state.copy(
                selectedLength = length,
                canProceed = state.selectedGenres.isNotEmpty()
            )
        }
    }

    private fun confirm() {
        val state = _uiState.value
        if (!state.canProceed || state.isLoading) return
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            profileRepository.saveOnboardingPreferences(
                OnboardingPreferences(
                    selectedGenres = state.selectedGenres.toList(),
                    selectedLength = state.selectedLength!!,
                    completedAt    = System.currentTimeMillis()
                )
            )
            _sideEffects.send(OnboardingSideEffect.NavigateToSwipeDeck)
        }
    }
}
