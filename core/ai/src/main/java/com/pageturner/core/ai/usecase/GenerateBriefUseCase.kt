package com.pageturner.core.ai.usecase

import com.pageturner.core.domain.model.Book
import com.pageturner.core.network.api.AnthropicApiService
import com.pageturner.core.network.dto.anthropic.AnthropicMessageDto
import com.pageturner.core.network.dto.anthropic.AnthropicRequestDto
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import timber.log.Timber
import javax.inject.Inject

/**
 * Generates a 2-sentence personalised brief for a book, tailored to the reader's taste.
 *
 * Returns null on any failure — the swipe card must render without a brief rather than block.
 * Cached by (bookKey, profileVersion) in Room by the data layer; this use case only generates.
 */
internal class GenerateBriefUseCase @Inject constructor(
    private val anthropicApiService: AnthropicApiService
) {
    suspend operator fun invoke(book: Book, profileSummary: String?): String? {
        return try {
            val response = withTimeout(AI_TIMEOUT_MS) {
                anthropicApiService.createMessage(buildRequest(book, profileSummary))
            }
            response.firstTextContent()?.trim().takeIf { !it.isNullOrBlank() }
        } catch (e: TimeoutCancellationException) {
            Timber.w("GenerateBriefUseCase timed out for book=${book.key}")
            null
        } catch (e: Exception) {
            Timber.e(e, "GenerateBriefUseCase failed for book=${book.key}")
            null
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
        const val CLAUDE_MODEL    = "claude-sonnet-4-20250514"
        const val AI_TIMEOUT_MS   = 30_000L
    }
}
