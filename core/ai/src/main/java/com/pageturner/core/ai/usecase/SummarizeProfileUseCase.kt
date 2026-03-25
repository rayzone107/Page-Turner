package com.pageturner.core.ai.usecase

import com.pageturner.core.ai.AiModels
import com.pageturner.core.ai.dto.ProfileSummaryDto
import com.pageturner.core.ai.ratelimit.AiRateLimiter
import com.pageturner.core.domain.model.Genre
import com.pageturner.core.domain.model.SwipeDirection
import com.pageturner.core.domain.model.SwipeEvent
import com.pageturner.core.domain.model.TasteProfile
import com.pageturner.core.domain.service.AiResult
import com.pageturner.core.logging.AppLogger
import com.pageturner.core.network.api.AnthropicApiService
import com.pageturner.core.network.dto.anthropic.AnthropicMessageDto
import com.pageturner.core.network.dto.anthropic.AnthropicRequestDto
import com.squareup.moshi.Moshi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

/**
 * Reads the user's full swipe history and produces a structured [TasteProfile].
 * Triggered automatically every 10 swipes.
 *
 * Returns [AiResult.RateLimited] when quota is exceeded — the caller must keep the
 * existing profile unchanged and surface a "quota reached" indicator.
 * Returns [AiResult.Failed] on API error, timeout, or parse failure — existing profile preserved.
 * Never resets the profile to empty on any failure.
 */
internal class SummarizeProfileUseCase @Inject constructor(
    private val anthropicApiService: AnthropicApiService,
    private val moshi: Moshi,
    private val rateLimiter: AiRateLimiter,
    private val logger: AppLogger,
) {
    suspend operator fun invoke(
        swipeEvents: List<SwipeEvent>,
        onboardingGenres: List<Genre>
    ): AiResult<TasteProfile> {
        if (swipeEvents.isEmpty()) return AiResult.Failed
        if (!rateLimiter.checkAndRecord()) return AiResult.RateLimited
        return try {
            val response = withTimeout(AI_TIMEOUT_MS) {
                anthropicApiService.createMessage(buildRequest(swipeEvents, onboardingGenres))
            }
            val text = response.firstTextContent() ?: return AiResult.Failed
            val profile = parseProfile(text, swipeEvents.size)
            if (profile != null) AiResult.Success(profile) else AiResult.Failed
        } catch (e: TimeoutCancellationException) {
            logger.w(TAG, "SummarizeProfileUseCase timed out (${swipeEvents.size} swipes)")
            AiResult.Failed
        } catch (e: Exception) {
            logger.e(TAG, "SummarizeProfileUseCase failed", e)
            AiResult.Failed
        }
    }

    private fun buildRequest(
        swipeEvents: List<SwipeEvent>,
        onboardingGenres: List<Genre>
    ): AnthropicRequestDto {
        val historyLines = swipeEvents.joinToString("\n") { event ->
            val dir = when (event.direction) {
                SwipeDirection.RIGHT    -> "saved"
                SwipeDirection.LEFT     -> "skipped"
                SwipeDirection.BOOKMARK -> "bookmarked"
            }
            val genres  = event.bookGenres.take(3).joinToString(", ").ifBlank { "unknown genre" }
            val year    = event.bookYear?.toString() ?: "?"
            val pages   = event.bookPageCount?.toString() ?: "?"
            val wildcard = if (event.wasWildcard) " [wildcard]" else ""
            "- $dir: genres=[$genres], year=$year, pages=$pages$wildcard"
        }
        val seedGenres = onboardingGenres.joinToString(", ") { it.displayName }

        val prompt = """
            Swipe history (${swipeEvents.size} most recent swipes):
            $historyLines

            The user started with genre preferences: $seedGenres

            Analyze this reading behavior. Return a JSON object only, no preamble:
            {
              "aiSummary": "1-3 sentence plain English description of their taste",
              "likedGenres": ["genre1", "genre2"],
              "avoidedGenres": ["genre1"],
              "preferredLength": "short|medium|long|any",
              "recurringThemes": ["theme1", "theme2"]
            }
        """.trimIndent()

        return AnthropicRequestDto(
            model     = AiModels.SONNET,
            maxTokens = 500,
            system    = SYSTEM_PROMPT,
            messages  = listOf(AnthropicMessageDto(role = "user", content = prompt))
        )
    }

    private fun parseProfile(text: String, swipeCount: Int): TasteProfile? {
        return try {
            val json    = extractJson(text)
            val adapter = moshi.adapter(ProfileSummaryDto::class.java)
            val dto     = adapter.fromJson(json) ?: return null
            TasteProfile(
                aiSummary             = dto.aiSummary,
                likedGenres           = dto.likedGenres,
                avoidedGenres         = dto.avoidedGenres,
                preferredLength       = dto.preferredLength,
                recurringThemes       = dto.recurringThemes,
                profileVersion        = 0,   // incremented by ProfileRepositoryImpl on save
                lastUpdatedSwipeCount = swipeCount,
                updatedAt             = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            logger.e(TAG, "SummarizeProfileUseCase: failed to parse profile JSON", e)
            null
        }
    }

    private companion object {
        const val TAG           = "SummarizeProfileUseCase"
        const val AI_TIMEOUT_MS = 45_000L
        const val SYSTEM_PROMPT =
            "You analyze reading behavior and produce structured taste profiles. " +
            "Always respond with valid JSON only — no markdown fences, no preamble, no explanation. " +
            "Follow the exact schema provided in the user message."
    }
}
