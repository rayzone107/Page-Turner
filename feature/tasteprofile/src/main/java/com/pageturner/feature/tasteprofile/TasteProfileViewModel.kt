package com.pageturner.feature.tasteprofile

import android.util.Log
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
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class TasteProfileViewModel @Inject constructor(
    profileRepository: ProfileRepository,
    swipeRepository: SwipeRepository,
) : ViewModel() {

    val state: StateFlow<TasteProfileUiState> = combine(
        profileRepository.getProfile()
            .onStart { Log.d("TasteProfileVM", "getProfile flow started") }
            .onEach   { Log.d("TasteProfileVM", "getProfile emitted: $it") }
            .catch    { Log.e("TasteProfileVM", "getProfile threw", it); throw it },
        swipeRepository.getSwipeCount()
            .onStart { Log.d("TasteProfileVM", "getSwipeCount flow started") }
            .onEach   { Log.d("TasteProfileVM", "getSwipeCount emitted: $it") }
            .catch    { Log.e("TasteProfileVM", "getSwipeCount threw", it); throw it },
        swipeRepository.getSavedBooks()
            .onStart { Log.d("TasteProfileVM", "getSavedBooks flow started") }
            .onEach   { Log.d("TasteProfileVM", "getSavedBooks emitted: ${it.size} books") }
            .catch    { Log.e("TasteProfileVM", "getSavedBooks threw", it); throw it },
    ) { profile, totalSwiped, savedBooks ->
        Log.d("TasteProfileVM", "combine firing: profile=${profile != null}, totalSwiped=$totalSwiped, saved=${savedBooks.size}")
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
        .catch { e ->
            Log.e("TasteProfileVM", "combine pipeline threw", e)
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
            started = SharingStarted.Eagerly,
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
