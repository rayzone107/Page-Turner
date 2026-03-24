package com.pageturner.feature.swipedeck

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pageturner.core.domain.error.UiError
import com.pageturner.core.domain.model.Book
import com.pageturner.core.domain.model.Genre
import com.pageturner.core.domain.model.SwipeDirection
import com.pageturner.core.domain.model.SwipeEvent
import com.pageturner.core.domain.model.TasteProfile
import com.pageturner.core.domain.model.WildcardResult
import com.pageturner.core.domain.repository.BookRepository
import com.pageturner.core.domain.repository.ProfileRepository
import com.pageturner.core.domain.repository.SwipeRepository
import com.pageturner.core.domain.service.AiService
import com.pageturner.core.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SwipeDeckViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    private val swipeRepository: SwipeRepository,
    private val profileRepository: ProfileRepository,
    private val aiService: AiService,
) : ViewModel() {

    private val _state = MutableStateFlow(SwipeDeckUiState())
    val state: StateFlow<SwipeDeckUiState> = _state.asStateFlow()

    private val _sideEffects = Channel<SwipeDeckSideEffect>(Channel.BUFFERED)
    val sideEffects = _sideEffects.receiveAsFlow()

    /** Running brief-generation jobs keyed by bookKey — prevents duplicates. */
    private val briefJobs = mutableMapOf<String, Job>()

    /** Off-genre books used to fill wildcard slots (every 7th card). */
    private val wildcardPool = MutableStateFlow<List<Book>>(emptyList())

    /**
     * Raw domain models keyed by bookKey.
     * Needed for AI calls — avoids round-tripping through the UI model.
     */
    private val domainBookCache = mutableMapOf<String, Book>()

    init {
        viewModelScope.launch { initialize() }
        viewModelScope.launch { observeSwipeCount() }
    }

    // ─────────────────────────────────────────────────────────────────
    // Initialisation
    // ─────────────────────────────────────────────────────────────────

    private suspend fun initialize() {
        val prefs = profileRepository.getOnboardingPreferences() ?: run {
            _state.update {
                it.copy(
                    isLoading = false,
                    error = UiError(
                        title = "Onboarding incomplete",
                        message = "Please complete onboarding to start swiping.",
                        isRetryable = false
                    )
                )
            }
            return
        }

        val userGenreSubjects = prefs.selectedGenres.map { it.openLibrarySubject }

        // Pick 3 genres the user did NOT select for the wildcard candidate pool.
        val offGenreSubjects = Genre.all()
            .filter { g -> prefs.selectedGenres.none { it.openLibrarySubject == g.openLibrarySubject } }
            .shuffled()
            .take(3)
            .map { it.openLibrarySubject }

        viewModelScope.launch { loadWildcardPool(offGenreSubjects) }

        // Restart the queue whenever seen keys change so already-swiped books are filtered.
        val queueFlow = bookRepository.getSeenBookKeys()
            .flatMapLatest { seen -> bookRepository.getSwipeQueue(userGenreSubjects, seen) }

        combine(queueFlow, wildcardPool, profileRepository.getProfile()) { books, wildcards, profile ->
            Triple(books, wildcards, profile)
        }.collect { (books, wildcards, profile) ->
            rebuildCards(books, wildcards, profile)
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Queue building
    // ─────────────────────────────────────────────────────────────────

    private fun rebuildCards(books: List<Book>, wildcards: List<Book>, profile: TasteProfile?) {
        val existingCards = _state.value.cards.associateBy { it.bookKey }
        val profileSummary = profile?.aiSummary
        val profileVersion = profile?.profileVersion ?: 0

        books.forEach { domainBookCache[it.key] = it }
        wildcards.forEach { domainBookCache[it.key] = it }

        val result = mutableListOf<SwipeCardUiModel>()
        var regularIdx = 0
        var wildcardIdx = 0
        var position = 0

        // Interleave regular books with wildcard slots: every 7th card is a wildcard.
        while (regularIdx < books.size) {
            position++
            if (position % 7 == 0 && wildcardIdx < wildcards.size) {
                val wc = wildcards[wildcardIdx++]
                val card = existingCards[wc.key] ?: wc.toUiModel(isWildcard = true)
                result.add(card)
                if (existingCards[wc.key] == null) {
                    scheduleWildcardPick(result.lastIndex, wildcards, profile)
                    scheduleBrief(card, profileSummary, profileVersion)
                }
            } else {
                val book = books[regularIdx++]
                val card = existingCards[book.key] ?: book.toUiModel()
                result.add(card)
                if (existingCards[book.key] == null) {
                    scheduleBrief(card, profileSummary, profileVersion)
                }
            }
        }

        val currentIdx = _state.value.currentCardIndex.coerceAtMost(maxOf(0, result.size - 1))
        val topCard = result.getOrNull(currentIdx)
        _state.update {
            it.copy(
                cards = result,
                isLoading = false,
                error = null,
                currentCardIndex = currentIdx,
                isGeneratingBrief = topCard != null && topCard.aiBrief == null,
            )
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // AI brief generation (one job per bookKey, cached by profileVersion)
    // ─────────────────────────────────────────────────────────────────

    private fun scheduleBrief(card: SwipeCardUiModel, profileSummary: String?, profileVersion: Int) {
        if (briefJobs[card.bookKey]?.isActive == true) return
        briefJobs[card.bookKey] = viewModelScope.launch {
            val cached = swipeRepository.getAiBriefCache(card.bookKey, profileVersion)
            if (cached != null) {
                updateCardBrief(card.bookKey, cached)
                return@launch
            }
            val book = domainBookCache[card.bookKey] ?: return@launch
            val brief = aiService.generateBrief(book, profileSummary) ?: return@launch
            swipeRepository.cacheAiBrief(card.bookKey, profileVersion, brief)
            updateCardBrief(card.bookKey, brief)
        }
    }

    private fun updateCardBrief(bookKey: String, brief: String) {
        _state.update { s ->
            val updated = s.cards.map { if (it.bookKey == bookKey) it.copy(aiBrief = brief) else it }
            val topCard = updated.getOrNull(s.currentCardIndex)
            s.copy(cards = updated, isGeneratingBrief = topCard != null && topCard.aiBrief == null)
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Wildcard slot AI pick (every 7th slot)
    // ─────────────────────────────────────────────────────────────────

    private fun scheduleWildcardPick(cardIndex: Int, candidates: List<Book>, profile: TasteProfile?) {
        viewModelScope.launch {
            if (candidates.isEmpty()) return@launch
            val result: WildcardResult = if (profile != null) {
                aiService.pickWildcard(profile, candidates)
                    ?: WildcardResult(book = candidates.random(), reason = null)
            } else {
                WildcardResult(book = candidates.random(), reason = null)
            }

            domainBookCache[result.book.key] = result.book
            val pickedCard = result.book.toUiModel(isWildcard = true, wildcardReason = result.reason)

            _state.update { s ->
                // Only replace the slot if the user hasn't already swiped past it.
                if (cardIndex >= s.currentCardIndex && cardIndex < s.cards.size) {
                    s.copy(cards = s.cards.toMutableList().also { it[cardIndex] = pickedCard })
                } else s
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Wildcard candidate pool (off-genre books)
    // ─────────────────────────────────────────────────────────────────

    private suspend fun loadWildcardPool(offGenreSubjects: List<String>) {
        val candidates = mutableListOf<Book>()
        for (subject in offGenreSubjects) {
            val result = bookRepository.fetchBooks(subject, page = 1)
            if (result is Result.Success) candidates.addAll(result.data)
        }
        wildcardPool.value = candidates.shuffled().take(20)
    }

    // ─────────────────────────────────────────────────────────────────
    // Swipe count observation + profile update trigger (every 10 swipes)
    // ─────────────────────────────────────────────────────────────────

    private fun observeSwipeCount() {
        viewModelScope.launch {
            swipeRepository.getSwipeCount()
                .distinctUntilChanged()
                .collect { count ->
                    val swipesUntil = 10 - (count % 10)
                    _state.update { it.copy(swipeCount = count, swipesUntilProfileUpdate = swipesUntil) }
                    if (count > 0 && count % 10 == 0) {
                        _sideEffects.send(SwipeDeckSideEffect.TriggerProfileUpdate)
                        runProfileUpdate()
                    }
                }
        }
    }

    private fun runProfileUpdate() {
        viewModelScope.launch {
            val history = swipeRepository.getSwipeHistory().first()
            val prefs = profileRepository.getOnboardingPreferences() ?: return@launch
            val newProfile = aiService.summarizeProfile(history, prefs.selectedGenres) ?: return@launch
            profileRepository.saveProfile(newProfile)
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Intent handling
    // ─────────────────────────────────────────────────────────────────

    fun handleIntent(intent: SwipeDeckIntent) {
        when (intent) {
            is SwipeDeckIntent.SwipeLeft -> performSwipe(intent.bookKey, SwipeDirection.LEFT)
            is SwipeDeckIntent.SwipeRight -> performSwipe(intent.bookKey, SwipeDirection.RIGHT)
            is SwipeDeckIntent.Bookmark -> performSwipe(intent.bookKey, SwipeDirection.BOOKMARK)
            is SwipeDeckIntent.ExpandCard -> viewModelScope.launch {
                _sideEffects.send(SwipeDeckSideEffect.NavigateToDetail(intent.bookKey))
            }
            SwipeDeckIntent.Retry -> {
                _state.update { SwipeDeckUiState() }
                viewModelScope.launch { initialize() }
            }
            SwipeDeckIntent.LoadMore -> Unit // repository auto-replenishes
        }
    }

    private fun performSwipe(bookKey: String, direction: SwipeDirection) {
        viewModelScope.launch {
            val s = _state.value
            val card = s.cards.getOrNull(s.currentCardIndex)
            if (card?.bookKey != bookKey) return@launch // stale event

            swipeRepository.recordSwipe(
                SwipeEvent(
                    bookKey = bookKey,
                    direction = direction,
                    timestamp = System.currentTimeMillis(),
                    bookGenres = card.subjects.take(3),
                    bookYear = card.publishYear,
                    bookPageCount = card.pageCount,
                    wasWildcard = card.isWildcard,
                )
            )

            if (direction == SwipeDirection.RIGHT) {
                swipeRepository.saveBook(bookKey, card.aiBrief, card.wildcardReason, card.isWildcard)
            }

            val newIdx = s.currentCardIndex + 1
            val nextCard = s.cards.getOrNull(newIdx)
            _state.update {
                it.copy(
                    currentCardIndex = newIdx,
                    isGeneratingBrief = nextCard != null && nextCard.aiBrief == null,
                )
            }
        }
    }
}
