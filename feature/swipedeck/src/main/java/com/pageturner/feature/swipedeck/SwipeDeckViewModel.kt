package com.pageturner.feature.swipedeck

import android.util.Log
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

    companion object {
        private const val TAG = "SwipeDeck"
    }

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

    /**
     * Number of regular cards placed since the last wildcard.
     * When this reaches [nextWildcardGap], the next card becomes a wildcard.
     * Persisted across interleaveBooks / appendCards calls so spacing stays natural.
     */
    private var sinceLastWildcard = 0
    private var nextWildcardGap = (4..10).random() // first gap

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
                books, wildcards, profile, profileSummary, profileVersion
            )

            Log.d(TAG, "First build: ${result.size} cards, " +
                    "${result.count { it.isWildcard }} wildcards, " +
                    "pool=${wildcards.size}")

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
            val existingKeys = currentCards.map { it.bookKey }.toSet()
            val hasWildcardsInDeck = currentCards.any { it.isWildcard }

            val updatedExisting = currentCards.map { card ->
                val subjects = domainBookCache[card.bookKey]?.subjects ?: card.subjects
                card.copy(matchScore = computeMatchScore(subjects, profile, card.isWildcard))
            }

            // If wildcards just arrived and the deck has none yet, inject them
            // into the unswiped portion of the existing deck.
            val result = if (!hasWildcardsInDeck && wildcards.isNotEmpty()) {
                injectWildcards(updatedExisting, wildcards, profile, profileSummary, profileVersion)
            } else {
                // Append truly new books (+ interleaved wildcards) at the end.
                val newBooks = books.filter { it.key !in existingKeys }
                val newWildcards = wildcards.filter { it.key !in existingKeys }
                val appended = interleaveBooks(
                    newBooks, newWildcards, profile, profileSummary, profileVersion
                )
                updatedExisting + appended
            }

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
     * Inserts wildcard cards into the unswiped portion of an existing deck
     * that was built before the wildcard pool was available.
     */
    private fun injectWildcards(
        existingCards: List<SwipeCardUiModel>,
        wildcards: List<Book>,
        profile: TasteProfile?,
        profileSummary: String?,
        profileVersion: Int,
    ): List<SwipeCardUiModel> {
        val currentIdx = _state.value.currentCardIndex
        val deckKeys = existingCards.map { it.bookKey }.toMutableSet()
        val available = wildcards.filter { it.key !in seenBookKeys && it.key !in deckKeys }.toMutableList()
        if (available.isEmpty()) return existingCards

        val result = existingCards.toMutableList()
        // Reset wildcard counter — the first build placed zero wildcards.
        sinceLastWildcard = 0
        nextWildcardGap = (4..10).random()

        // Walk unswiped cards and insert wildcards at the right intervals.
        var i = currentIdx
        while (i < result.size && available.isNotEmpty()) {
            sinceLastWildcard++
            if (sinceLastWildcard >= nextWildcardGap) {
                val wc = available.removeAt(0)
                deckKeys.add(wc.key)
                domainBookCache[wc.key] = wc
                val score = computeMatchScore(wc.subjects, profile, isWildcard = true)
                val card = wc.toUiModel(isWildcard = true, matchScore = score)
                result.add(i + 1, card) // insert after current position
                scheduleBrief(card, profileSummary, profileVersion)
                sinceLastWildcard = 0
                nextWildcardGap = (4..10).random()
                i++ // skip past the wildcard we just inserted
            }
            i++
        }

        Log.d(TAG, "Injected wildcards: ${result.count { it.isWildcard }} wildcards in ${result.size} cards")
        return result
    }

    /**
     * Builds a card list from [books], interleaving a wildcard at random intervals
     * (every 4–10 regular cards, averaging ~7). Shared by initial build and append paths.
     */
    private fun interleaveBooks(
        books: List<Book>,
        wildcards: List<Book>,
        profile: TasteProfile?,
        profileSummary: String?,
        profileVersion: Int,
    ): List<SwipeCardUiModel> {
        val existingCards = _state.value.cards.associateBy { it.bookKey }
        val deckKeys = existingCards.keys.toMutableSet()
        // Exclude wildcards the user has already swiped OR that are already in the deck.
        val availableWildcards = wildcards.filter { it.key !in seenBookKeys && it.key !in deckKeys }
        val result = mutableListOf<SwipeCardUiModel>()
        var regularIdx = 0
        var wildcardIdx = 0

        while (regularIdx < books.size) {
            // Time for a wildcard?
            if (sinceLastWildcard >= nextWildcardGap && wildcardIdx < availableWildcards.size) {
                val wc = availableWildcards[wildcardIdx++]
                deckKeys.add(wc.key)
                val score = computeMatchScore(wc.subjects, profile, isWildcard = true)
                val card = existingCards[wc.key]?.copy(matchScore = score)
                    ?: wc.toUiModel(isWildcard = true, matchScore = score)
                result.add(card)
                if (existingCards[wc.key]?.aiBrief == null) {
                    scheduleBrief(card, profileSummary, profileVersion)
                }
                sinceLastWildcard = 0
                nextWildcardGap = (4..10).random()
            } else {
                val book = books[regularIdx++]
                val score = computeMatchScore(book.subjects, profile)
                val card = existingCards[book.key]?.copy(matchScore = score)
                    ?: book.toUiModel(matchScore = score)
                result.add(card)
                if (existingCards[book.key]?.aiBrief == null) {
                    scheduleBrief(card, profileSummary, profileVersion)
                }
                sinceLastWildcard++
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

            // Diagnostic: log when a wildcard reaches the top of the stack.
            if (nextCard != null) {
                Log.d(TAG, "Card #$newIdx → ${nextCard.title} " +
                        "(wildcard=${nextCard.isWildcard}, key=${nextCard.bookKey})")
            }

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

        while (regularIdx < uniqueNewBooks.size) {
            // Time for a wildcard?
            if (sinceLastWildcard >= nextWildcardGap && wildcards.isNotEmpty()) {
                val usedKeys = existingKeys + seenBookKeys + cardsToAppend.map { it.bookKey }.toSet()
                val available = wildcards.filter { it.key !in usedKeys }
                if (available.isNotEmpty()) {
                    val wc = available.random()
                    domainBookCache[wc.key] = wc
                    val score = computeMatchScore(wc.subjects, profile, isWildcard = true)
                    val card = wc.toUiModel(isWildcard = true, matchScore = score)
                    cardsToAppend.add(card)
                    scheduleBrief(card, profileSummary, profileVersion)
                    sinceLastWildcard = 0
                    nextWildcardGap = (4..10).random()
                }
            }

            val book = uniqueNewBooks[regularIdx++]
            val score = computeMatchScore(book.subjects, profile)
            val card = book.toUiModel(matchScore = score)
            cardsToAppend.add(card)
            scheduleBrief(card, profileSummary, profileVersion)
            sinceLastWildcard++
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
    /**
     * Computes a match score in 0.0–1.0 that reflects how well a book fits the profile.
     *
     * Design goals:
     *  - New users (no profile) start around 0.20.
     *  - As the profile matures (profileVersion increases with every 10 swipes),
     *    regular books gradually climb toward 0.85–0.92 but never reach 1.0.
     *  - There's always variance — two books with similar genre overlap get different scores.
     *  - Wildcards are capped significantly lower (0.15–0.45) — that's the point.
     */
    private fun computeMatchScore(
        subjects: List<String>,
        profile: TasteProfile?,
        isWildcard: Boolean = false,
    ): Float {
        // Deterministic jitter unique to this book: 0.00 – 0.12
        val jitter = (subjects.sumOf { it.length } % 13) / 100f

        // ── No profile yet → flat 0.20 with jitter ──────────────────────
        if (profile == null || (profile.likedGenres.isEmpty() && profile.avoidedGenres.isEmpty())) {
            return (0.18f + jitter).coerceIn(0.08f, 0.35f)
        }

        // ── Profile maturity: 0.0 at version 0, asymptotically approaches 1.0 ──
        // version 1 (10 swipes) → 0.33,  v3 (30) → 0.60,  v5 (50) → 0.71,  v10 (100) → 0.83
        val maturity = 1f - 1f / (1f + profile.profileVersion * 0.5f)

        // ── Genre overlap ───────────────────────────────────────────────
        val subjectsLower = subjects.map { it.lowercase() }
        val liked   = profile.likedGenres.map  { it.lowercase() }
        val avoided = profile.avoidedGenres.map { it.lowercase() }

        var likedHits = 0
        var avoidedHits = 0
        for (s in subjectsLower) {
            if (liked.any   { l -> s.contains(l) || l.contains(s) }) likedHits++
            if (avoided.any { a -> s.contains(a) || a.contains(s) }) avoidedHits++
        }

        // hitRatio: fraction of liked genres this book covers (0.0 – 1.0)
        val hitRatio = if (liked.isNotEmpty()) (likedHits.toFloat() / liked.size).coerceAtMost(1f) else 0f
        val avoidPenalty = if (avoided.isNotEmpty()) (avoidedHits.toFloat() / avoided.size).coerceAtMost(1f) else 0f

        // ── Assemble the score ──────────────────────────────────────────
        // Base line that rises with maturity: 0.20 → ~0.40 over time.
        val base = 0.20f + maturity * 0.20f
        // Genre boost scales with both maturity AND how well this book matches.
        val genreBoost = hitRatio * maturity * 0.50f   // max ≈ 0.42 at full maturity
        // Penalty for avoided genres.
        val penalty = avoidPenalty * 0.25f

        val rawScore = base + genreBoost - penalty + jitter

        return if (isWildcard) {
            // Wildcards: always noticeably below the regular average.
            // Cap at 0.45 so they clearly sit in the red–orange zone.
            rawScore.coerceIn(0.08f, 0.45f)
        } else {
            // Regular: cap at 0.95 — never fully "perfect".
            rawScore.coerceIn(0.08f, 0.95f)
        }
    }
}
