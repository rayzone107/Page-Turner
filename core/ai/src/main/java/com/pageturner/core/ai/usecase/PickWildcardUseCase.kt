package com.pageturner.core.ai.usecase

import com.pageturner.core.ai.dto.WildcardPickDto
import com.pageturner.core.ai.ratelimit.AiRateLimiter
import com.pageturner.core.domain.model.Book
import com.pageturner.core.domain.model.TasteProfile
import com.pageturner.core.domain.model.WildcardResult
import com.pageturner.core.network.api.AnthropicApiService
import com.pageturner.core.network.dto.anthropic.AnthropicMessageDto
import com.pageturner.core.network.dto.anthropic.AnthropicRequestDto
import com.squareup.moshi.Moshi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import timber.log.Timber
import javax.inject.Inject

/**
 * Selects an intentional wildcard book from [candidates] — a book outside the user's
 * typical genres that shares a quality they demonstrably love.
 *
 * On any failure (including rate limiting), falls back to a random candidate with no reason shown.
 * The wildcard slot is never left empty.
 */
internal class PickWildcardUseCase @Inject constructor(
    private val anthropicApiService: AnthropicApiService,
    private val moshi: Moshi,
    private val rateLimiter: AiRateLimiter,
) {
    suspend operator fun invoke(
        profile: TasteProfile,
        candidates: List<Book>
    ): WildcardResult? {
        if (candidates.isEmpty()) return null
        if (!rateLimiter.checkAndRecord()) return randomFallback(candidates)
        return try {
            val response = withTimeout(AI_TIMEOUT_MS) {
                anthropicApiService.createMessage(buildRequest(profile, candidates))
            }
            val text = response.firstTextContent() ?: return randomFallback(candidates)
            parsePickResult(text, candidates) ?: randomFallback(candidates)
        } catch (e: TimeoutCancellationException) {
            Timber.w("PickWildcardUseCase timed out, using random fallback")
            randomFallback(candidates)
        } catch (e: Exception) {
            Timber.e(e, "PickWildcardUseCase failed, using random fallback")
            randomFallback(candidates)
        }
    }

    private fun buildRequest(profile: TasteProfile, candidates: List<Book>): AnthropicRequestDto {
        val candidateList = candidates.take(10).mapIndexed { index, book ->
            val author  = book.authors.firstOrNull() ?: "Unknown"
            val year    = book.publishYear?.toString() ?: "?"
            val pages   = book.pageCount?.toString() ?: "?"
            val subjects = book.subjects.take(3).joinToString(", ")
            "$index. \"${book.title}\" by $author ($year, $pages pages) — $subjects"
        }.joinToString("\n")

        val likedGenres = profile.likedGenres.joinToString(", ").ifBlank { "not yet determined" }

        val prompt = """
            Reader profile: ${profile.aiSummary}
            Liked genres: $likedGenres

            Here are candidate books from genres outside their usual preferences:
            $candidateList

            Pick exactly ONE book that shares a quality this reader demonstrably loves,
            expressed through an unfamiliar genre. Return JSON only:
            {
              "selectedIndex": 0,
              "reason": "one sentence explaining the connection to their taste"
            }
        """.trimIndent()

        return AnthropicRequestDto(
            model     = CLAUDE_MODEL,
            maxTokens = 200,
            messages  = listOf(AnthropicMessageDto(role = "user", content = prompt))
        )
    }

    private fun parsePickResult(text: String, candidates: List<Book>): WildcardResult? {
        return try {
            val json    = extractJson(text)
            val adapter = moshi.adapter(WildcardPickDto::class.java)
            val dto     = adapter.fromJson(json) ?: return null
            val book    = candidates.getOrElse(dto.selectedIndex) { candidates.random() }
            WildcardResult(book = book, reason = dto.reason)
        } catch (e: Exception) {
            Timber.e(e, "PickWildcardUseCase: failed to parse wildcard JSON")
            null
        }
    }

    private fun randomFallback(candidates: List<Book>): WildcardResult =
        WildcardResult(book = candidates.random(), reason = null)

    private companion object {
        const val CLAUDE_MODEL  = "claude-sonnet-4-6"
        const val AI_TIMEOUT_MS = 30_000L
    }
}
