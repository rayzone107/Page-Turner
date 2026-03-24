package com.pageturner.core.ai.service

import com.pageturner.core.ai.usecase.GenerateBriefUseCase
import com.pageturner.core.ai.usecase.PickWildcardUseCase
import com.pageturner.core.ai.usecase.SummarizeProfileUseCase
import com.pageturner.core.domain.model.Book
import com.pageturner.core.domain.model.Genre
import com.pageturner.core.domain.model.SwipeEvent
import com.pageturner.core.domain.model.TasteProfile
import com.pageturner.core.domain.model.WildcardResult
import com.pageturner.core.domain.service.AiService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production implementation of [AiService] backed by the Anthropic Claude API.
 *
 * Each of the three AI jobs is handled by a dedicated use case:
 * - [GenerateBriefUseCase]   — 2-sentence personalised book hook
 * - [SummarizeProfileUseCase] — structured taste profile from swipe history
 * - [PickWildcardUseCase]    — intentional wildcard book with reason
 *
 * All three degrade gracefully: failures return null or a fallback, never crash the UI.
 * The Anthropic SDK and API key are fully isolated within this module.
 */
@Singleton
internal class ClaudeAiService @Inject constructor(
    private val generateBriefUseCase: GenerateBriefUseCase,
    private val summarizeProfileUseCase: SummarizeProfileUseCase,
    private val pickWildcardUseCase: PickWildcardUseCase
) : AiService {

    override suspend fun generateBrief(book: Book, profileSummary: String?): String? =
        generateBriefUseCase(book, profileSummary)

    override suspend fun summarizeProfile(
        swipeEvents: List<SwipeEvent>,
        onboardingGenres: List<Genre>
    ): TasteProfile? = summarizeProfileUseCase(swipeEvents, onboardingGenres)

    override suspend fun pickWildcard(
        profile: TasteProfile,
        candidates: List<Book>
    ): WildcardResult? = pickWildcardUseCase(profile, candidates)
}
