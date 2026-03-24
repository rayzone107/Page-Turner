package com.pageturner.core.ai.usecase

import com.pageturner.core.ai.dto.ProfileSummaryDto
import com.pageturner.core.domain.model.Genre
import com.pageturner.core.domain.model.SwipeDirection
import com.pageturner.core.domain.model.SwipeEvent
import com.pageturner.core.domain.model.TasteProfile
import com.pageturner.core.network.api.AnthropicApiService
import com.pageturner.core.network.dto.anthropic.AnthropicMessageDto
import com.pageturner.core.network.dto.anthropic.AnthropicRequestDto
import com.squareup.moshi.Moshi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import timber.log.Timber
import javax.inject.Inject

/**
 * Reads the user's full swipe history and produces a structured [TasteProfile].
 * Triggered automatically every 10 swipes.
 *
 * Returns null on any failure — the caller must keep the existing profile unchanged.
 * Never resets the profile to empty on failure.
 */
internal class SummarizeProfileUseCase @Inject constructor(
    private val anthropicApiService: AnthropicApiService,
    private val moshi: Moshi
) {
    suspend operator fun invoke(
        swipeEvents: List<SwipeEvent>,
        onboardingGenres: List<Genre>
    ): TasteProfile? {
        if (swipeEvents.isEmpty()) return null
        return try {
            val response = withTimeout(AI_TIMEOUT_MS) {
                anthropicApiService.createMessage(buildRequest(swipeEvents, onboardingGenres))
            }
            val text = response.firstTextContent() ?: return null
            parseProfile(text, swipeEvents.size)
        } catch (e: TimeoutCancellationException) {
            Timber.w("SummarizeProfileUseCase timed out (${swipeEvents.size} swipes)")
            null
        } catch (e: Exception) {
            Timber.e(e, "SummarizeProfileUseCase failed")
            null
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
            model     = CLAUDE_MODEL,
            maxTokens = 500,
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
            Timber.e(e, "SummarizeProfileUseCase: failed to parse profile JSON")
            null
        }
    }

    private companion object {
        const val CLAUDE_MODEL  = "claude-sonnet-4-20250514"
        const val AI_TIMEOUT_MS = 45_000L
    }
}
