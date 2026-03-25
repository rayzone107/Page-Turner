package com.pageturner.feature.readinglist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pageturner.core.analytics.AnalyticsEvent
import com.pageturner.core.analytics.AnalyticsTracker
import com.pageturner.core.domain.repository.SwipeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReadingListViewModel @Inject constructor(
    private val swipeRepository: SwipeRepository,
    private val analytics: AnalyticsTracker,
) : ViewModel() {

    init {
        analytics.track(AnalyticsEvent.ScreenView("reading_list"))
    }

    val state: StateFlow<ReadingListUiState> = combine(
        swipeRepository.getLikedBooks(),
        swipeRepository.getBookmarkedBooks(),
    ) { liked, bookmarked ->
        ReadingListUiState(
            likedBooks = liked.map { it.toSavedUiModel() },
            bookmarkedBooks = bookmarked.map { it.toSavedUiModel() },
            isLoading = false,
        )
    }.stateIn(
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
