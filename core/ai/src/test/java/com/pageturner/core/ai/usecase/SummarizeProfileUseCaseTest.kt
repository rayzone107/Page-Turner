package com.pageturner.core.ai.usecase

import com.pageturner.core.ai.ratelimit.AiRateLimiter
import com.pageturner.core.domain.model.Genre
import com.pageturner.core.logging.AppLogger
import com.pageturner.core.domain.model.SwipeDirection
import com.pageturner.core.domain.model.SwipeEvent
import com.pageturner.core.domain.service.AiResult
import com.pageturner.core.network.api.AnthropicApiService
import com.pageturner.core.network.dto.anthropic.AnthropicContentBlockDto
import com.pageturner.core.network.dto.anthropic.AnthropicResponseDto
import com.squareup.moshi.Moshi
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class SummarizeProfileUseCaseTest {

    @MockK private lateinit var anthropicApiService: AnthropicApiService
    @MockK private lateinit var rateLimiter: AiRateLimiter
    @MockK(relaxed = true) private lateinit var logger: AppLogger

    // Moshi.Builder().build() is sufficient — @JsonClass(generateAdapter = true) causes
    // Moshi to auto-discover the KSP-generated adapter by class name convention
    // (KotlinJsonAdapterFactory is NOT used because it explicitly skips codegen classes)
    private val moshi = Moshi.Builder().build()

    private lateinit var useCase: SummarizeProfileUseCase

    private fun aSwipeEvent(key: String = "/works/OL1") = SwipeEvent(
        id = 0, bookKey = key, direction = SwipeDirection.RIGHT,
        timestamp = 0L, bookGenres = listOf("fantasy"), bookYear = 2000,
        bookPageCount = 300, wasWildcard = false,
    )

    private fun aResponse(text: String) = AnthropicResponseDto(
        id = "msg_1", type = "message", role = "assistant",
        content = listOf(AnthropicContentBlockDto(type = "text", text = text)),
        model = "claude-sonnet-4-6", stopReason = "end_turn"
    )

    private val validProfileJson = """
        {
          "aiSummary": "You love speculative fiction.",
          "likedGenres": ["fantasy", "science fiction"],
          "avoidedGenres": ["romance"],
          "preferredLength": "long",
          "recurringThemes": ["moral complexity"]
        }
    """.trimIndent()

    @BeforeEach
    fun setUp() {
        coEvery { rateLimiter.checkAndRecord() } returns true
        useCase = SummarizeProfileUseCase(anthropicApiService, moshi, rateLimiter, logger)
    }

    @Nested
    inner class `when swipe history is empty` {

        @Test
        fun `returns Failed without making any API call`() = runTest {
            val result = useCase(emptyList(), listOf(Genre.FANTASY))
            assertTrue(result is AiResult.Failed)
        }
    }

    @Nested
    inner class `when API returns valid JSON` {

        @Test
        fun `returns a TasteProfile with correct likedGenres and aiSummary`() = runTest {
            coEvery { anthropicApiService.createMessage(any()) } returns aResponse(validProfileJson)

            val result = useCase(listOf(aSwipeEvent()), listOf(Genre.FANTASY))

            assertTrue(result is AiResult.Success)
            val profile = (result as AiResult.Success).data
            assertTrue("fantasy" in profile.likedGenres)
            assertTrue(profile.aiSummary.isNotBlank())
        }
    }

    @Nested
    inner class `when API returns JSON in markdown fences` {

        @Test
        fun `fences are stripped and profile is parsed correctly`() = runTest {
            val fenced = "```json\n$validProfileJson\n```"
            coEvery { anthropicApiService.createMessage(any()) } returns aResponse(fenced)

            val result = useCase(listOf(aSwipeEvent()), listOf(Genre.FANTASY))

            assertTrue(result is AiResult.Success)
        }
    }

    @Nested
    inner class `when API returns malformed JSON` {

        @Test
        fun `returns null`() = runTest {
            coEvery { anthropicApiService.createMessage(any()) } returns aResponse("not json at all")

            val result = useCase(listOf(aSwipeEvent()), listOf(Genre.FANTASY))

            assertTrue(result is AiResult.Failed)
        }
    }

    @Nested
    inner class `when API call fails` {

        @Test
        fun `returns null without propagating`() = runTest {
            coEvery { anthropicApiService.createMessage(any()) } throws RuntimeException("API down")

            val result = useCase(listOf(aSwipeEvent()), listOf(Genre.FANTASY))

            assertTrue(result is AiResult.Failed)
        }
    }
}
