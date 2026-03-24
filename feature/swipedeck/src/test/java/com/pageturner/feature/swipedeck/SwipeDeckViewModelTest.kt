package com.pageturner.feature.swipedeck

import app.cash.turbine.test
import com.pageturner.core.domain.model.Book
import com.pageturner.core.domain.model.Genre
import com.pageturner.core.domain.model.OnboardingPreferences
import com.pageturner.core.domain.model.ReadingLength
import com.pageturner.core.domain.model.SwipeDirection
import com.pageturner.core.domain.model.TasteProfile
import com.pageturner.core.domain.repository.BookRepository
import com.pageturner.core.domain.repository.ProfileRepository
import com.pageturner.core.domain.repository.SwipeRepository
import com.pageturner.core.domain.service.AiService
import com.pageturner.core.domain.util.Result
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MockKExtension::class)
class SwipeDeckViewModelTest {

    @MockK private lateinit var bookRepository: BookRepository
    @MockK private lateinit var swipeRepository: SwipeRepository
    @MockK private lateinit var profileRepository: ProfileRepository
    @MockK private lateinit var aiService: AiService

    private val testDispatcher = StandardTestDispatcher()

    private val swipeCountFlow = MutableStateFlow(0)
    private val profileFlow = MutableStateFlow<TasteProfile?>(null)

    private fun aBook(key: String = "/works/OL1", subjects: List<String> = listOf("fantasy")) = Book(
        key = key, title = "Book $key", authors = listOf("Author"),
        coverUrl = null, publishYear = 2000, pageCount = 300,
        subjects = subjects, description = null, aiBrief = null,
        matchScore = 0.5f, isWildcard = false, wildcardReason = null,
    )

    private val preferences = OnboardingPreferences(
        selectedGenres = listOf(Genre.FANTASY),
        selectedLengths = listOf(ReadingLength.MEDIUM),
        completedAt = 0L,
    )

    /** Sets up mocks and creates a ViewModel with 10 cards loaded. */
    private fun createInitializedViewModel(): SwipeDeckViewModel {
        val books = (1..10).map { aBook("/works/OL$it") }
        val queueFlow = MutableStateFlow(books)

        coEvery { profileRepository.getOnboardingPreferences() } returns preferences
        every { bookRepository.getSeenBookKeys() } returns MutableStateFlow(emptySet())
        every { bookRepository.getSwipeQueue(any(), any()) } returns queueFlow
        every { profileRepository.getProfile() } returns profileFlow
        every { swipeRepository.getSwipeCount() } returns swipeCountFlow
        coEvery { bookRepository.fetchBooks(any(), any()) } returns Result.Success(emptyList())
        coEvery { aiService.generateBrief(any(), any()) } returns null
        coEvery { aiService.pickWildcard(any(), any()) } returns null
        coJustRun { swipeRepository.recordSwipe(any()) }
        coJustRun { swipeRepository.saveBook(any(), any(), any(), any(), any()) }
        coJustRun { swipeRepository.cacheAiBrief(any(), any(), any()) }
        coEvery { swipeRepository.getAiBriefCache(any(), any()) } returns null
        coJustRun { bookRepository.prefetchBookDetail(any()) }
        // getSwipeHistory is called by runProfileUpdate() when swipeCount hits a multiple of 10
        every { swipeRepository.getSwipeHistory() } returns flowOf(emptyList())

        return SwipeDeckViewModel(bookRepository, swipeRepository, profileRepository, aiService)
    }

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Nested
    inner class `when onboarding is not complete` {

        @Test
        fun `state shows a non-retryable error`() = runTest(testDispatcher) {
            coEvery { profileRepository.getOnboardingPreferences() } returns null
            every { swipeRepository.getSwipeCount() } returns swipeCountFlow

            val vm = SwipeDeckViewModel(bookRepository, swipeRepository, profileRepository, aiService)
            advanceUntilIdle()

            val error = vm.state.value.error
            assertNotNull(error)
            assertFalse(error!!.isRetryable)
        }

        @Test
        fun `isLoading becomes false`() = runTest(testDispatcher) {
            coEvery { profileRepository.getOnboardingPreferences() } returns null
            every { swipeRepository.getSwipeCount() } returns swipeCountFlow

            val vm = SwipeDeckViewModel(bookRepository, swipeRepository, profileRepository, aiService)
            advanceUntilIdle()

            assertFalse(vm.state.value.isLoading)
        }
    }

    @Nested
    inner class `when swiping left` {

        @Test
        fun `currentCardIndex advances by 1`() = runTest(testDispatcher) {
            val vm = createInitializedViewModel()
            advanceUntilIdle()

            val bookKey = vm.state.value.cards.first().bookKey
            vm.handleIntent(SwipeDeckIntent.SwipeLeft(bookKey))
            advanceUntilIdle()

            assertEquals(1, vm.state.value.currentCardIndex)
        }

        @Test
        fun `saveBook is never called`() = runTest(testDispatcher) {
            val vm = createInitializedViewModel()
            advanceUntilIdle()

            val bookKey = vm.state.value.cards.first().bookKey
            vm.handleIntent(SwipeDeckIntent.SwipeLeft(bookKey))
            advanceUntilIdle()

            coVerify(exactly = 0) { swipeRepository.saveBook(any(), any(), any(), any(), any()) }
        }
    }

    @Nested
    inner class `when swiping right` {

        @Test
        fun `currentCardIndex advances by 1`() = runTest(testDispatcher) {
            val vm = createInitializedViewModel()
            advanceUntilIdle()

            val bookKey = vm.state.value.cards.first().bookKey
            vm.handleIntent(SwipeDeckIntent.SwipeRight(bookKey))
            advanceUntilIdle()

            assertEquals(1, vm.state.value.currentCardIndex)
        }

        @Test
        fun `saveBook is called with isBookmarked false`() = runTest(testDispatcher) {
            val vm = createInitializedViewModel()
            advanceUntilIdle()

            val bookKey = vm.state.value.cards.first().bookKey
            vm.handleIntent(SwipeDeckIntent.SwipeRight(bookKey))
            advanceUntilIdle()

            coVerify { swipeRepository.saveBook(bookKey, any(), any(), any(), isBookmarked = false) }
        }

        @Test
        fun `prefetchBookDetail is called for the swiped book`() = runTest(testDispatcher) {
            val vm = createInitializedViewModel()
            advanceUntilIdle()

            val bookKey = vm.state.value.cards.first().bookKey
            vm.handleIntent(SwipeDeckIntent.SwipeRight(bookKey))
            advanceUntilIdle()

            coVerify { bookRepository.prefetchBookDetail(bookKey) }
        }
    }

    @Nested
    inner class `when bookmarking` {

        @Test
        fun `currentCardIndex advances by 1`() = runTest(testDispatcher) {
            val vm = createInitializedViewModel()
            advanceUntilIdle()

            val bookKey = vm.state.value.cards.first().bookKey
            vm.handleIntent(SwipeDeckIntent.Bookmark(bookKey))
            advanceUntilIdle()

            assertEquals(1, vm.state.value.currentCardIndex)
        }

        @Test
        fun `saveBook is called with isBookmarked true`() = runTest(testDispatcher) {
            val vm = createInitializedViewModel()
            advanceUntilIdle()

            val bookKey = vm.state.value.cards.first().bookKey
            vm.handleIntent(SwipeDeckIntent.Bookmark(bookKey))
            advanceUntilIdle()

            coVerify { swipeRepository.saveBook(bookKey, any(), any(), any(), isBookmarked = true) }
        }

        @Test
        fun `prefetchBookDetail is called for the swiped book`() = runTest(testDispatcher) {
            val vm = createInitializedViewModel()
            advanceUntilIdle()

            val bookKey = vm.state.value.cards.first().bookKey
            vm.handleIntent(SwipeDeckIntent.Bookmark(bookKey))
            advanceUntilIdle()

            coVerify { bookRepository.prefetchBookDetail(bookKey) }
        }
    }

    @Nested
    inner class `when swipe count reaches a multiple of 10` {

        @Test
        fun `TriggerProfileUpdate side effect is emitted`() = runTest(testDispatcher) {
            coEvery { aiService.summarizeProfile(any(), any()) } returns null

            val vm = createInitializedViewModel()
            advanceUntilIdle()

            vm.sideEffects.test {
                swipeCountFlow.value = 10
                advanceUntilIdle()
                val effect = awaitItem()
                assertEquals(SwipeDeckSideEffect.TriggerProfileUpdate, effect)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    inner class `when fewer than 5 cards remain after a swipe` {

        @Test
        fun `isReplenishing becomes true while fetching then false on completion`() = runTest(testDispatcher) {
            coEvery { bookRepository.fetchNextPage(any(), any()) } returns emptyList()

            val vm = createInitializedViewModel()
            advanceUntilIdle()

            // Collect all isReplenishing state transitions during the swipe sequence
            val replenishingHistory = mutableListOf<Boolean>()
            val collectJob = launch { vm.state.collect { replenishingHistory.add(it.isReplenishing) } }

            // Swipe 5 times on a 10-card deck — leaves 5 cards, triggering replenishment.
            // Advance between swipes so each coroutine updates currentCardIndex before the next.
            repeat(5) {
                val key = vm.state.value.cards.getOrNull(vm.state.value.currentCardIndex)?.bookKey
                    ?: return@repeat
                vm.handleIntent(SwipeDeckIntent.SwipeLeft(key))
                advanceUntilIdle()
            }
            collectJob.cancel()

            // isReplenishing was true during fetch, then settled back to false
            assertTrue(true in replenishingHistory, "expected isReplenishing = true to be emitted")
            assertFalse(vm.state.value.isReplenishing)
        }
    }

    @Nested
    inner class `when ExpandCard intent is received` {

        @Test
        fun `NavigateToDetail side effect is emitted with the correct bookKey`() = runTest(testDispatcher) {
            val vm = createInitializedViewModel()
            advanceUntilIdle()

            val bookKey = vm.state.value.cards.first().bookKey
            vm.sideEffects.test {
                vm.handleIntent(SwipeDeckIntent.ExpandCard(bookKey))
                advanceUntilIdle()
                val effect = awaitItem()
                assertEquals(SwipeDeckSideEffect.NavigateToDetail(bookKey), effect)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    inner class `when Retry intent is received` {

        @Test
        fun `state resets to loading`() = runTest(testDispatcher) {
            val vm = createInitializedViewModel()
            advanceUntilIdle()
            assertFalse(vm.state.value.isLoading)

            vm.handleIntent(SwipeDeckIntent.Retry)

            assertTrue(vm.state.value.isLoading)
        }
    }
}
