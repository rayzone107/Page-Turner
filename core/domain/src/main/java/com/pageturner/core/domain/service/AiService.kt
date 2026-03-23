package com.pageturner.core.domain.service

import com.pageturner.core.domain.model.Book
import com.pageturner.core.domain.model.Genre
import com.pageturner.core.domain.model.SwipeEvent
import com.pageturner.core.domain.model.TasteProfile
import com.pageturner.core.domain.model.WildcardResult

/**
 * Contract for the three AI jobs. Implemented in :core:ai by ClaudeAiService.
 * Feature modules never depend on :core:ai directly — only on this interface.
 *
 * All methods return null on any failure — callers must handle null gracefully
 * per the AI degradation rules in Section 10 of the project plan.
 */
interface AiService {
    /**
     * Generates a 2-sentence personalized book brief for the given [book],
     * tailored to the user's taste expressed in [profileSummary].
     *
     * Returns null on API failure or timeout — the card renders without a brief.
     */
    suspend fun generateBrief(book: Book, profileSummary: String?): String?

    /**
     * Reads [swipeEvents] and produces a structured [TasteProfile].
     * Triggered after every 10 swipes.
     *
     * Returns null on failure — the caller must keep the existing profile unchanged.
     */
    suspend fun summarizeProfile(
        swipeEvents: List<SwipeEvent>,
        onboardingGenres: List<Genre>
    ): TasteProfile?

    /**
     * Selects the best wildcard book from [candidates] based on [profile].
     * Every 7th card slot triggers this.
     *
     * Returns null on failure — the caller must fall back to a random candidate.
     */
    suspend fun pickWildcard(
        profile: TasteProfile,
        candidates: List<Book>
    ): WildcardResult?
}
