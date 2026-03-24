package com.pageturner.core.domain.service

import com.pageturner.core.domain.model.Book
import com.pageturner.core.domain.model.Genre
import com.pageturner.core.domain.model.SwipeEvent
import com.pageturner.core.domain.model.TasteProfile
import com.pageturner.core.domain.model.WildcardResult
import kotlinx.coroutines.flow.Flow

/**
 * Contract for the three AI jobs. Implemented in :core:ai by ClaudeAiService.
 * Feature modules never depend on :core:ai directly — only on this interface.
 *
 * [generateBrief] and [summarizeProfile] return [AiResult] to distinguish
 * rate-limited calls (show quota message) from other failures (degrade silently).
 * [pickWildcard] always returns a wildcard (random fallback on any failure).
 */
interface AiService {
    /**
     * Generates a 2-sentence personalized book brief for [book],
     * tailored to the user's taste expressed in [profileSummary].
     *
     * Returns [AiResult.RateLimited] when quota is exceeded.
     * Returns [AiResult.Failed] on API error, timeout, or blank content.
     */
    suspend fun generateBrief(book: Book, profileSummary: String?): AiResult<String>

    /**
     * Reads [swipeEvents] and produces a structured [TasteProfile].
     * Triggered after every 10 swipes.
     *
     * Returns [AiResult.RateLimited] when quota is exceeded — existing profile is preserved.
     * Returns [AiResult.Failed] on API error, timeout, or parse failure.
     */
    suspend fun summarizeProfile(
        swipeEvents: List<SwipeEvent>,
        onboardingGenres: List<Genre>
    ): AiResult<TasteProfile>

    /**
     * Selects the best wildcard book from [candidates] based on [profile].
     * Falls back to a random candidate on any failure, including rate limiting.
     * Never returns null when [candidates] is non-empty.
     */
    suspend fun pickWildcard(
        profile: TasteProfile,
        candidates: List<Book>
    ): WildcardResult?

    /**
     * Emits true whenever any quota window (minute/hour/day) is currently exceeded.
     * Observed by screens that need to show a "AI quota reached" indicator.
     */
    fun observeQuotaExceeded(): Flow<Boolean>
}
