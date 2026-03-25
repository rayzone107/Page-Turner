package com.pageturner.core.ai.usecase

import com.pageturner.core.ai.ratelimit.AiRateLimiter
import com.pageturner.core.domain.model.Book
import com.pageturner.core.domain.service.AiResult
import com.pageturner.core.logging.AppLogger
import com.pageturner.core.network.api.AnthropicApiService
import com.pageturner.core.network.dto.anthropic.AnthropicMessageDto
import com.pageturner.core.network.dto.anthropic.AnthropicRequestDto
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

/**
 * Generates a 2-sentence personalised brief for a book, tailored to the reader's taste.
 *
 * Returns [AiResult.RateLimited] when the per-minute/hour/day quota is exceeded.
 * Returns [AiResult.Failed] on API error, timeout, or blank response — the swipe card
 * must render without a brief rather than block.
 * Cached by (bookKey, profileVersion) in Room by the data layer; this use case only generates.
 */
internal class GenerateBriefUseCase @Inject constructor(
    private val anthropicApiService: AnthropicApiService,
    private val rateLimiter: AiRateLimiter,
    private val logger: AppLogger,
) {
    suspend operator fun invoke(book: Book, profileSummary: String?): AiResult<String> {
        if (!rateLimiter.checkAndRecord()) return AiResult.RateLimited
        return try {
            val response = withTimeout(AI_TIMEOUT_MS) {
                anthropicApiService.createMessage(buildRequest(book, profileSummary))
            }
            val text = response.firstTextContent()?.trim().takeIf { !it.isNullOrBlank() }
            if (text != null) AiResult.Success(text) else AiResult.Failed
        } catch (e: TimeoutCancellationException) {
            logger.w(TAG, "GenerateBriefUseCase timed out for book=${book.key}")
            AiResult.Failed
        } catch (e: Exception) {
            logger.e(TAG, "GenerateBriefUseCase failed for book=${book.key}", e)
            AiResult.Failed
        }
    }

    private fun buildRequest(book: Book, profileSummary: String?): AnthropicRequestDto {
        val authorStr = book.authors.joinToString(", ").ifBlank { "Unknown author" }
        val yearStr   = book.publishYear?.toString() ?: "unknown year"
        val pagesStr  = book.pageCount?.let { "$it pages" } ?: "unknown length"
        val subjects  = book.subjects.take(5).joinToString(", ").ifBlank { "none listed" }
        val desc      = book.description?.take(500) ?: "no description available"
        val profile   = profileSummary ?: "new user — no taste profile yet, highlight the book's best qualities"

        val prompt = """
            Book: ${book.title} by $authorStr ($yearStr, $pagesStr)
            Subjects: $subjects
            Description: $desc

            Reader profile: $profile

            Write exactly 2 sentences. A personal pitch for why THIS reader might love this book.
            Not a plot summary. A hook. Speak directly to them. Be specific to their taste.
            Output only the 2 sentences. No preamble, no quotes.
        """.trimIndent()

        return AnthropicRequestDto(
            model     = CLAUDE_MODEL,
            maxTokens = 200,
            messages  = listOf(AnthropicMessageDto(role = "user", content = prompt))
        )
    }

    private companion object {
        const val TAG             = "GenerateBriefUseCase"
        const val CLAUDE_MODEL    = "claude-haiku-4-5-20251001"
        const val AI_TIMEOUT_MS   = 30_000L
    }
}
