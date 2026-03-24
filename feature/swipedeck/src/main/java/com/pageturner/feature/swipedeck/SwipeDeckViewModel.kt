package com.pageturner.feature.swipedeck

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pageturner.core.domain.error.UiError
import com.pageturner.core.domain.model.Book
import com.pageturner.core.domain.model.Genre
import com.pageturner.core.domain.model.SwipeDirection
import com.pageturner.core.domain.model.SwipeEvent
import com.pageturner.core.domain.model.TasteProfile
import com.pageturner.core.domain.repository.BookRepository
import com.pageturner.core.domain.repository.ProfileRepository
import com.pageturner.core.domain.repository.SwipeRepository
import com.pageturner.core.domain.service.AiService
import com.pageturner.core.domain.error.AppError
import com.pageturner.core.domain.service.AiResult
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

    /** Off-genre books used to fill wildcard slots (every 4th card). */
    private val wildcardPool = MutableStateFlow<List<Book>>(emptyList())

    /**
     * Raw domain models keyed by bookKey.
     * Needed for AI calls — avoids round-tripping through the UI model.
     */
    private val domainBookCache = mutableMapOf<String, Book>()

    /** Genres the user selected during onboarding — stored for deck replenishment. */
    private var userGenreSubjects: List<String> = emptyList()

    /** All book keys the user has ever swiped — prevents wildcard repeats. */
    private val seenBookKeys = mutableSetOf<String>()

    /** Guards against concurrent deck-refresh calls. */
    private var isRefreshingQueue = false

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

        userGenreSubjects = prefs.selectedGenres.map { it.openLibrarySubject }

        // Pick 3 genres the user did NOT select for the wildcard candidate pool.
        val offGenreSubjects = Genre.all()
            .filter { g -> prefs.selectedGenres.none { it.openLibrarySubject == g.openLibrarySubject } }
            .shuffled()
            .take(3)
            .map { it.openLibrarySubject }

        viewModelScope.launch { loadWildcardPool(offGenreSubjects) }

        // Snapshot seen keys ONCE at startup. Restarting the queue on every swipe via
        // flatMapLatest blocks the UI for 7-8 s per swipe (cache miss → network round-trip).
        // Deck replenishment is handled imperatively in performSwipe when the deck runs low.
        val initialSeen = bookRepository.getSeenBookKeys().first()
        seenBookKeys.addAll(initialSeen)
        val queueFlow = bookRepository.getSwipeQueue(userGenreSubjects, initialSeen)

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
        val currentCards = _state.value.cards
        val currentIdx = _state.value.currentCardIndex
        val profileSummary = profile?.aiSummary
        val profileVersion = profile?.profileVersion ?: 0

        books.forEach { domainBookCache[it.key] = it }
        wildcards.forEach { domainBookCache[it.key] = it }

        if (currentCards.isEmpty()) {
            // ── First build: construct the full deck from scratch ──────────
            val result = interleaveBooks(
                books, wildcards, profile, profileSummary, profileVersion, startPosition = 0
            )

            val topCard = result.firstOrNull()
            _state.update {
                it.copy(
                    cards = result,
                    isLoading = false,
                    error = null,
                    currentCardIndex = 0,
                    isGeneratingBrief = topCard != null && topCard.aiBrief == null,
                )
            }
        } else {
            // ── Subsequent update: preserve existing card order ────────────
            // Only update match scores on existing cards; append truly new books.
            val existingKeys = currentCards.map { it.bookKey }.toSet()

            val updatedExisting = currentCards.map { card ->
                val subjects = domainBookCache[card.bookKey]?.subjects ?: card.subjects
                card.copy(matchScore = computeMatchScore(subjects, profile))
            }

            val newBooks = books.filter { it.key !in existingKeys }
            val newWildcards = wildcards.filter { it.key !in existingKeys }
            val appended = interleaveBooks(
                newBooks, newWildcards, profile, profileSummary, profileVersion,
                startPosition = currentCards.size
            )

            val result = updatedExisting + appended
            val topCard = result.getOrNull(currentIdx)
            _state.update {
                it.copy(
                    cards = result,
                    isLoading = false,
                    error = null,
                    isGeneratingBrief = topCard != null && topCard.aiBrief == null,
                )
            }
        }
    }

    /**
     * Builds a card list from [books], interleaving a wildcard every 4th position.
     * Shared by initial build and append paths.
     */
    private fun interleaveBooks(
        books: List<Book>,
        wildcards: List<Book>,
        profile: TasteProfile?,
        profileSummary: String?,
        profileVersion: Int,
        startPosition: Int,
    ): List<SwipeCardUiModel> {
        val existingCards = _state.value.cards.associateBy { it.bookKey }
        val deckKeys = existingCards.keys.toMutableSet()
        // Exclude wildcards the user has already swiped OR that are already in the deck.
        val availableWildcards = wildcards.filter { it.key !in seenBookKeys && it.key !in deckKeys }
        val result = mutableListOf<SwipeCardUiModel>()
        var regularIdx = 0
        var wildcardIdx = 0
        var position = startPosition

        while (regularIdx < books.size) {
            position++
            if (position % 4 == 0 && wildcardIdx < availableWildcards.size) {
                val wc = availableWildcards[wildcardIdx++]
                deckKeys.add(wc.key) // prevent the same wildcard in a later slot
                val score = computeMatchScore(wc.subjects, profile)
                val card = existingCards[wc.key]?.copy(matchScore = score)
                    ?: wc.toUiModel(isWildcard = true, matchScore = score)
                result.add(card)
                if (existingCards[wc.key]?.aiBrief == null) {
                    scheduleBrief(card, profileSummary, profileVersion)
                }
            } else {
                val book = books[regularIdx++]
                val score = computeMatchScore(book.subjects, profile)
                val card = existingCards[book.key]?.copy(matchScore = score)
                    ?: book.toUiModel(matchScore = score)
                result.add(card)
                if (existingCards[book.key]?.aiBrief == null) {
                    scheduleBrief(card, profileSummary, profileVersion)
                }
            }
        }
        return result
    }

    // ─────────────────────────────────────────────────────────────────
    // AI brief generation (one job per bookKey, cached by profileVersion)
    // ─────────────────────────────────────────────────────────────────

    private fun scheduleBrief(card: SwipeCardUiModel, profileSummary: String?, profileVersion: Int) {
        if (briefJobs[card.bookKey]?.isActive == true) return
        briefJobs[card.bookKey] = viewModelScope.launch {
            val cached = swipeRepository.getAiBriefCache(card.bookKey, profileVersion)
            if (cached != null) {
                updateCardBrief(card.bookKey, cached, quotaExceeded = false)
                return@launch
            }
            val book = domainBookCache[card.bookKey] ?: return@launch
            when (val result = aiService.generateBrief(book, profileSummary)) {
                is AiResult.Success -> {
                    swipeRepository.cacheAiBrief(card.bookKey, profileVersion, result.data)
                    updateCardBrief(card.bookKey, result.data, quotaExceeded = false)
                }
                is AiResult.RateLimited -> {
                    // Quota exceeded — clear shimmer and show quota error on card.
                    updateCardBrief(card.bookKey, "", quotaExceeded = true)
                }
                is AiResult.Failed -> {
                    // API failed — set empty string so the shimmer clears silently.
                    updateCardBrief(card.bookKey, "", quotaExceeded = false)
                }
            }
        }
    }

    private fun updateCardBrief(bookKey: String, brief: String, quotaExceeded: Boolean) {
        _state.update { s ->
            val updated = s.cards.map { card ->
                if (card.bookKey == bookKey) card.copy(aiBrief = brief, isAiQuotaExceeded = quotaExceeded)
                else card
            }
            val topCard = updated.getOrNull(s.currentCardIndex)
            s.copy(cards = updated, isGeneratingBrief = topCard != null && topCard.aiBrief == null)
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
            when (val result = aiService.summarizeProfile(history, prefs.selectedGenres)) {
                is AiResult.Success -> profileRepository.saveProfile(result.data)
                is AiResult.RateLimited -> { /* quota exceeded — keep existing profile, quota state updated in rateLimiter */ }
                is AiResult.Failed -> { /* API error — keep existing profile */ }
            }
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
            seenBookKeys.add(bookKey)

            if (direction == SwipeDirection.RIGHT || direction == SwipeDirection.BOOKMARK) {
                swipeRepository.saveBook(
                    bookKey        = bookKey,
                    aiBrief        = card.aiBrief,
                    wildcardReason = card.wildcardReason,
                    isWildcard     = card.isWildcard,
                    isBookmarked   = direction == SwipeDirection.BOOKMARK,
                )
                // Prefetch the work description so the detail page works offline.
                // Launched in a separate coroutine so it never blocks the swipe animation.
                viewModelScope.launch { bookRepository.prefetchBookDetail(bookKey) }
            }

            val newIdx = s.currentCardIndex + 1
            val nextCard = s.cards.getOrNull(newIdx)
            _state.update {
                it.copy(
                    currentCardIndex = newIdx,
                    isGeneratingBrief = nextCard != null && nextCard.aiBrief == null,
                )
            }

            // Replenish the deck before it runs dry.
            if (s.cards.size - newIdx <= 5) {
                refreshQueueIfNeeded()
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Deck replenishment (called imperatively when deck runs low)
    // ─────────────────────────────────────────────────────────────────

    private fun refreshQueueIfNeeded() {
        if (isRefreshingQueue || userGenreSubjects.isEmpty()) return
        isRefreshingQueue = true
        _state.update { it.copy(isReplenishing = true) }
        viewModelScope.launch {
            try {
                val seenKeys = bookRepository.getSeenBookKeys().first()
                when (val result = bookRepository.fetchNextPage(userGenreSubjects, seenKeys)) {
                    is Result.Success -> {
                        _state.update { it.copy(isOffline = false) }
                        if (result.data.isNotEmpty()) appendCards(result.data)
                    }
                    is Result.Failure -> {
                        if (result.error is AppError.NoInternetError) {
                            _state.update { it.copy(isOffline = true) }
                        }
                    }
                }
            } finally {
                isRefreshingQueue = false
                _state.update { it.copy(isReplenishing = false) }
            }
        }
    }

    /**
     * Appends new books to the end of the existing deck.
     * Preserves existing card order — no rebuild, no reordering.
     */
    private suspend fun appendCards(newBooks: List<Book>) {
        val profile = profileRepository.getProfile().first()
        val profileSummary = profile?.aiSummary
        val profileVersion = profile?.profileVersion ?: 0
        val existingKeys = _state.value.cards.map { it.bookKey }.toSet()
        val wildcards = wildcardPool.value

        val uniqueNewBooks = newBooks.filter { it.key !in existingKeys && it.key !in seenBookKeys }
        uniqueNewBooks.forEach { domainBookCache[it.key] = it }

        val cardsToAppend = mutableListOf<SwipeCardUiModel>()
        var regularIdx = 0
        var position = _state.value.cards.size

        while (regularIdx < uniqueNewBooks.size) {
            position++
            // Interleave a wildcard every 4th overall position.
            if (position % 4 == 0 && wildcards.isNotEmpty()) {
                val usedKeys = existingKeys + seenBookKeys + cardsToAppend.map { it.bookKey }.toSet()
                val available = wildcards.filter { it.key !in usedKeys }
                if (available.isNotEmpty()) {
                    val wc = available.random()
                    domainBookCache[wc.key] = wc
                    val score = computeMatchScore(wc.subjects, profile)
                    val card = wc.toUiModel(isWildcard = true, matchScore = score)
                    cardsToAppend.add(card)
                    scheduleBrief(card, profileSummary, profileVersion)
                }
            }

            val book = uniqueNewBooks[regularIdx++]
            val score = computeMatchScore(book.subjects, profile)
            val card = book.toUiModel(matchScore = score)
            cardsToAppend.add(card)
            scheduleBrief(card, profileSummary, profileVersion)
        }

        if (cardsToAppend.isNotEmpty()) {
            _state.update { s -> s.copy(cards = s.cards + cardsToAppend) }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Local score calculation (mirrors BookMapper.calculateMatchScore but
    // operates on domain-layer List<String> instead of a serialised JSON string,
    // keeping the feature module free of core:data imports).
    // ─────────────────────────────────────────────────────────────────
    private fun computeMatchScore(subjects: List<String>, profile: TasteProfile?): Float {
        if (profile == null) return 0.5f
        if (profile.likedGenres.isEmpty() && profile.avoidedGenres.isEmpty()) return 0.5f
        val subjectsLower = subjects.map { it.lowercase() }
        val liked   = profile.likedGenres.map  { it.lowercase() }
        val avoided = profile.avoidedGenres.map { it.lowercase() }
        var score = 0.5f
        for (subject in subjectsLower) {
            if (liked.any   { l -> subject.contains(l) || l.contains(subject) }) score += 0.1f
            if (avoided.any { a -> subject.contains(a) || a.contains(subject) }) score -= 0.1f
        }
        return score.coerceIn(0.05f, 0.99f)
    }
}
