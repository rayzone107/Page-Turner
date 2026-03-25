package com.pageturner.feature.tasteprofile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pageturner.core.domain.error.UiError
import com.pageturner.core.domain.repository.ProfileRepository
import com.pageturner.core.domain.repository.SwipeRepository
import com.pageturner.core.domain.service.AiService
import com.pageturner.core.logging.AppLogger
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
    aiService: AiService,
    private val logger: AppLogger,
) : ViewModel() {

    val state: StateFlow<TasteProfileUiState> = combine(
        profileRepository.getProfile()
            .catch { logger.e(TAG, "getProfile threw", it); throw it },
        swipeRepository.getSwipeCount()
            .catch { logger.e(TAG, "getSwipeCount threw", it); throw it },
        swipeRepository.getSavedBooks()
            .catch { logger.e(TAG, "getSavedBooks threw", it); throw it },
        aiService.observeQuotaExceeded(),
    ) { profile, totalSwiped, savedBooks, isQuotaExceeded ->
        val stats = SwipeStats(
            totalSwiped = totalSwiped,
            totalSaved = savedBooks.size,
            wildcardKept = savedBooks.count { it.isWildcard },
        )
        TasteProfileUiState(
            profile = profile?.toUiModel(),
            swipeStats = stats,
            isAiQuotaExceeded = isQuotaExceeded,
        )
    }
        .catch { e ->
            logger.e(TAG, "combine pipeline threw", e)
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

    private companion object { const val TAG = "TasteProfileVM" }

    private val _sideEffects = Channel<TasteProfileSideEffect>(Channel.BUFFERED)
    val sideEffects = _sideEffects.receiveAsFlow()

    fun handleIntent(intent: TasteProfileIntent) {
        when (intent) {
            // The combine pipeline auto-refreshes from Room — no manual trigger needed.
            TasteProfileIntent.Refresh -> Unit
        }
    }
}
