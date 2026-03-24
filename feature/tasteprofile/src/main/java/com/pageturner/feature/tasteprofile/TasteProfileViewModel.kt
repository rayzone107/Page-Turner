package com.pageturner.feature.tasteprofile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pageturner.core.domain.error.UiError
import com.pageturner.core.domain.repository.ProfileRepository
import com.pageturner.core.domain.repository.SwipeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class TasteProfileViewModel @Inject constructor(
    profileRepository: ProfileRepository,
    swipeRepository: SwipeRepository,
) : ViewModel() {

    val state: StateFlow<TasteProfileUiState> = combine(
        profileRepository.getProfile(),
        swipeRepository.getSwipeCount(),
        swipeRepository.getSavedBooks(),
    ) { profile, totalSwiped, savedBooks ->
        val stats = SwipeStats(
            totalSwiped = totalSwiped,
            totalSaved = savedBooks.size,
            wildcardKept = savedBooks.count { it.isWildcard },
        )
        TasteProfileUiState(
            profile = profile?.toUiModel(),
            swipeStats = stats,
        )
    }
        .catch {
            emit(
                TasteProfileUiState(
                    error = UiError(
                        title = "Could not load profile",
                        message = "Something went wrong loading your taste profile.",
                        isRetryable = true,
                    )
                )
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TasteProfileUiState(isLoading = true),
        )

    private val _sideEffects = Channel<TasteProfileSideEffect>(Channel.BUFFERED)
    val sideEffects = _sideEffects.receiveAsFlow()

    fun handleIntent(intent: TasteProfileIntent) {
        when (intent) {
            // The combine pipeline auto-refreshes from Room — no manual trigger needed.
            TasteProfileIntent.Refresh -> Unit
        }
    }
}
