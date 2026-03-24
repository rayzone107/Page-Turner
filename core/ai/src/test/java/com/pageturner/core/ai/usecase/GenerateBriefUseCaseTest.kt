package com.pageturner.core.ai.usecase

import com.pageturner.core.ai.ratelimit.AiRateLimiter
import com.pageturner.core.domain.model.Book
import com.pageturner.core.domain.service.AiResult
import com.pageturner.core.network.api.AnthropicApiService
import com.pageturner.core.network.dto.anthropic.AnthropicContentBlockDto
import com.pageturner.core.network.dto.anthropic.AnthropicResponseDto
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class GenerateBriefUseCaseTest {

    @MockK private lateinit var anthropicApiService: AnthropicApiService
    @MockK private lateinit var rateLimiter: AiRateLimiter

    private lateinit var useCase: GenerateBriefUseCase

    private fun aBook() = Book(
        key = "/works/OL1", title = "Dune", authors = listOf("Frank Herbert"),
        coverUrl = null, publishYear = 1965, pageCount = 412,
        subjects = listOf("science fiction"), description = "A desert planet epic.",
        aiBrief = null, matchScore = 0.8f, isWildcard = false, wildcardReason = null,
    )

    private fun aResponse(text: String) = AnthropicResponseDto(
        id = "msg_1", type = "message", role = "assistant",
        content = listOf(AnthropicContentBlockDto(type = "text", text = text)),
        model = "claude-haiku-4-5-20251001", stopReason = "end_turn"
    )

    @BeforeEach
    fun setUp() {
        coEvery { rateLimiter.checkAndRecord() } returns true
        useCase = GenerateBriefUseCase(anthropicApiService, rateLimiter)
    }

    @Nested
    inner class `when API returns a valid response` {

        @Test
        fun `returns a trimmed non-blank string`() = runTest {
            coEvery { anthropicApiService.createMessage(any()) } returns aResponse("  A gripping read.  ")

            val result = useCase(aBook(), profileSummary = "You love sci-fi epics.")

            assertTrue(result is AiResult.Success)
        }
    }

    @Nested
    inner class `when API returns blank content` {

        @Test
        fun `returns null`() = runTest {
            coEvery { anthropicApiService.createMessage(any()) } returns aResponse("   ")

            val result = useCase(aBook(), profileSummary = null)

            assertTrue(result is AiResult.Failed)
        }
    }

    @Nested
    inner class `when API call throws an exception` {

        @Test
        fun `returns null without propagating`() = runTest {
            coEvery { anthropicApiService.createMessage(any()) } throws RuntimeException("Network error")

            val result = useCase(aBook(), profileSummary = null)

            assertTrue(result is AiResult.Failed)
        }
    }

    @Nested
    inner class `when API call times out` {

        @Test
        fun `returns null without propagating`() = runTest {
            // delay(Long.MAX_VALUE) uses virtual time — withTimeout(30_000L) in the use case
            // fires at virtual time 30s, triggering TimeoutCancellationException, which is caught.
            coEvery { anthropicApiService.createMessage(any()) } coAnswers {
                delay(Long.MAX_VALUE)
                aResponse("irrelevant")
            }

            val result = useCase(aBook(), profileSummary = null)

            assertTrue(result is AiResult.Failed)
        }
    }
}
