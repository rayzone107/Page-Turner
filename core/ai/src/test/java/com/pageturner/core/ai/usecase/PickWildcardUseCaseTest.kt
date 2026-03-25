package com.pageturner.core.ai.usecase

import com.pageturner.core.ai.ratelimit.AiRateLimiter
import com.pageturner.core.domain.model.Book
import com.pageturner.core.logging.AppLogger
import com.pageturner.core.domain.model.TasteProfile
import com.pageturner.core.network.api.AnthropicApiService
import com.pageturner.core.network.dto.anthropic.AnthropicContentBlockDto
import com.pageturner.core.network.dto.anthropic.AnthropicResponseDto
import com.squareup.moshi.Moshi
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class PickWildcardUseCaseTest {

    @MockK private lateinit var anthropicApiService: AnthropicApiService
    @MockK private lateinit var rateLimiter: AiRateLimiter
    @MockK(relaxed = true) private lateinit var logger: AppLogger

    // Moshi.Builder().build() auto-discovers the KSP-generated WildcardPickDtoJsonAdapter
    // via class name convention. KotlinJsonAdapterFactory explicitly skips codegen classes.
    private val moshi = Moshi.Builder().build()

    private lateinit var useCase: PickWildcardUseCase

    private fun aBook(key: String = "/works/OL1") = Book(
        key = key, title = "Book $key", authors = listOf("Author"),
        coverUrl = null, publishYear = 2000, pageCount = 300,
        subjects = listOf("horror"), description = null, aiBrief = null,
        matchScore = 0.5f, isWildcard = false, wildcardReason = null,
    )

    private fun aProfile() = TasteProfile(
        aiSummary = "You love dark fiction.", likedGenres = listOf("fantasy"),
        avoidedGenres = emptyList(), preferredLength = "long",
        recurringThemes = emptyList(), profileVersion = 1,
        lastUpdatedSwipeCount = 10, updatedAt = 0L,
    )

    private fun aResponse(text: String) = AnthropicResponseDto(
        id = "msg_1", type = "message", role = "assistant",
        content = listOf(AnthropicContentBlockDto(type = "text", text = text)),
        model = "claude-sonnet-4-6", stopReason = "end_turn"
    )

    @BeforeEach
    fun setUp() {
        coEvery { rateLimiter.checkAndRecord() } returns true
        useCase = PickWildcardUseCase(anthropicApiService, moshi, rateLimiter, logger)
    }

    @Nested
    inner class `when candidates list is empty` {

        @Test
        fun `returns null without making any API call`() = runTest {
            val result = useCase(aProfile(), emptyList())
            assertNull(result)
        }
    }

    @Nested
    inner class `when API picks a valid index within candidates` {

        @Test
        fun `returns WildcardResult with matching book and reason`() = runTest {
            val candidates = listOf(aBook("/works/OL1"), aBook("/works/OL2"))
            val json = """{"selectedIndex": 1, "reason": "Shares moral complexity."}"""
            coEvery { anthropicApiService.createMessage(any()) } returns aResponse(json)

            val result = useCase(aProfile(), candidates)

            assertNotNull(result)
            assertTrue(result!!.book.key in candidates.map { it.key })
        }
    }

    @Nested
    inner class `when API picks an out-of-range index` {

        @Test
        fun `returns WildcardResult with a random candidate as fallback`() = runTest {
            val candidates = listOf(aBook("/works/OL1"))
            val json = """{"selectedIndex": 999, "reason": "Reason."}"""
            coEvery { anthropicApiService.createMessage(any()) } returns aResponse(json)

            val result = useCase(aProfile(), candidates)

            assertNotNull(result)
            assertTrue(result!!.book.key in candidates.map { it.key })
        }
    }

    @Nested
    inner class `when API call fails or times out` {

        @Test
        fun `returns WildcardResult with a random candidate as fallback`() = runTest {
            val candidates = listOf(aBook("/works/OL1"), aBook("/works/OL2"))
            coEvery { anthropicApiService.createMessage(any()) } throws RuntimeException("fail")

            val result = useCase(aProfile(), candidates)

            assertNotNull(result)
            assertTrue(result!!.book.key in candidates.map { it.key })
        }
    }
}
