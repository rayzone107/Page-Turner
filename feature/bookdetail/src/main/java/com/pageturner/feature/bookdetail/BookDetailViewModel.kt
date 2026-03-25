package com.pageturner.feature.bookdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pageturner.core.analytics.AnalyticsEvent
import com.pageturner.core.analytics.AnalyticsTracker
import com.pageturner.core.domain.error.AppError
import com.pageturner.core.domain.repository.BookRepository
import com.pageturner.core.domain.repository.SwipeRepository
import com.pageturner.core.domain.util.Result
import com.pageturner.core.logging.AppLogger
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
class BookDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val bookRepository: BookRepository,
    private val swipeRepository: SwipeRepository,
    private val analytics: AnalyticsTracker,
    private val logger: AppLogger,
) : ViewModel() {

    private val bookKey: String = checkNotNull(savedStateHandle["bookKey"])

    private val _state = MutableStateFlow(BookDetailUiState(isLoading = true))
    val state: StateFlow<BookDetailUiState> = _state.asStateFlow()

    private val _sideEffects = Channel<BookDetailSideEffect>(Channel.BUFFERED)
    val sideEffects = _sideEffects.receiveAsFlow()

    private companion object { const val TAG = "BookDetailVM" }

    init {
        analytics.track(AnalyticsEvent.ScreenView("book_detail"))
        viewModelScope.launch { loadBook() }
        viewModelScope.launch { observeSavedStatus() }
    }

    private suspend fun loadBook() {
        _state.update { it.copy(isLoading = true, error = null) }
        when (val result = bookRepository.getBookDetail(bookKey)) {
            is Result.Success -> _state.update {
                it.copy(isLoading = false, book = result.data.toUiModel())
            }
            is Result.Failure -> {
                val isOffline = result.error is AppError.NoInternetError
                logger.e(TAG, "Failed to load book detail for $bookKey: ${result.error}")
                analytics.track(AnalyticsEvent.ErrorOccurred(result.error.javaClass.simpleName, "book_detail"))
                _state.update {
                    it.copy(
                        isLoading = false,
                        isOffline = isOffline,
                        error = result.error.toUiError(),
                    )
                }
            }
        }
    }

    private suspend fun observeSavedStatus() {
        // Check once on load; the reading list screen handles real-time updates.
        val saved = swipeRepository.isBookSaved(bookKey)
        _state.update { it.copy(isSaved = saved) }
    }

    fun handleIntent(intent: BookDetailIntent) {
        when (intent) {
            BookDetailIntent.NavigateBack -> viewModelScope.launch {
                _sideEffects.send(BookDetailSideEffect.NavigateBack)
            }
            BookDetailIntent.OpenOnOpenLibrary -> viewModelScope.launch {
                val url = _state.value.book?.openLibraryUrl ?: return@launch
                _sideEffects.send(BookDetailSideEffect.OpenUrl(url))
            }
            BookDetailIntent.Retry -> viewModelScope.launch { loadBook() }
            BookDetailIntent.RemoveFromList -> viewModelScope.launch {
                swipeRepository.removeBook(bookKey)
                _state.update { it.copy(isSaved = false) }
            }
        }
    }
}
