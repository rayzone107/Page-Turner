package com.pageturner.feature.readinglist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pageturner.core.domain.repository.SwipeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReadingListViewModel @Inject constructor(
    private val swipeRepository: SwipeRepository,
) : ViewModel() {

    /** Reading list is offline-first: Room is the sole source of truth, zero network calls. */
    val state: StateFlow<ReadingListUiState> = swipeRepository.getSavedBooks()
        .map { books ->
            ReadingListUiState(
                books = books.map { it.toSavedUiModel() },
                isLoading = false,
                isEmpty = books.isEmpty(),
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ReadingListUiState(isLoading = true),
        )

    private val _sideEffects = Channel<ReadingListSideEffect>(Channel.BUFFERED)
    val sideEffects = _sideEffects.receiveAsFlow()

    fun handleIntent(intent: ReadingListIntent) {
        when (intent) {
            is ReadingListIntent.SelectBook -> viewModelScope.launch {
                _sideEffects.send(ReadingListSideEffect.NavigateToDetail(intent.bookKey))
            }
            is ReadingListIntent.RemoveBook -> viewModelScope.launch {
                swipeRepository.removeBook(intent.bookKey)
            }
        }
    }
}
