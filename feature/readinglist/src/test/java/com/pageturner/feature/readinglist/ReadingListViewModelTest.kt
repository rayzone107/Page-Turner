package com.pageturner.feature.readinglist

import app.cash.turbine.test
import com.pageturner.core.domain.model.Book
import com.pageturner.core.domain.repository.SwipeRepository
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MockKExtension::class)
class ReadingListViewModelTest {

    @MockK
    private lateinit var swipeRepository: SwipeRepository

    private val testDispatcher = StandardTestDispatcher()

    private fun aBook(key: String = "/works/OL1") = Book(
        key = key, title = "T", authors = emptyList(), coverUrl = null,
        publishYear = null, pageCount = null, subjects = emptyList(),
        description = null, aiBrief = null, matchScore = 1.0f,
        isWildcard = false, wildcardReason = null,
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { swipeRepository.getLikedBooks() } returns flowOf(emptyList())
        every { swipeRepository.getBookmarkedBooks() } returns flowOf(emptyList())
    }

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Nested
    inner class `state` {

        @Test
        fun `initial state has isLoading true`() {
            val vm = ReadingListViewModel(swipeRepository)
            assertTrue(vm.state.value.isLoading)
        }

        @Test
        fun `liked books from repository appear in likedBooks list`() = runTest(testDispatcher) {
            val book = aBook("/works/OL1")
            every { swipeRepository.getLikedBooks() } returns flowOf(listOf(book))

            val vm = ReadingListViewModel(swipeRepository)
            vm.state.test {
                skipItems(1) // initial loading state
                val state = awaitItem()
                assertEquals(1, state.likedBooks.size)
                assertEquals("/works/OL1", state.likedBooks.first().bookKey)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        fun `bookmarked books appear in bookmarkedBooks list`() = runTest(testDispatcher) {
            val book = aBook("/works/OL2")
            every { swipeRepository.getBookmarkedBooks() } returns flowOf(listOf(book))

            val vm = ReadingListViewModel(swipeRepository)
            vm.state.test {
                skipItems(1)
                val state = awaitItem()
                assertEquals(1, state.bookmarkedBooks.size)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    inner class `when SelectBook is received` {

        @Test
        fun `NavigateToDetail side effect is emitted with the correct bookKey`() = runTest(testDispatcher) {
            val vm = ReadingListViewModel(swipeRepository)
            vm.sideEffects.test {
                vm.handleIntent(ReadingListIntent.SelectBook("/works/OL99"))
                advanceUntilIdle()
                val effect = awaitItem()
                assertEquals(ReadingListSideEffect.NavigateToDetail("/works/OL99"), effect)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    inner class `when RemoveBook is received` {

        @Test
        fun `repository removeBook is called with the correct bookKey`() = runTest(testDispatcher) {
            coJustRun { swipeRepository.removeBook(any()) }
            val vm = ReadingListViewModel(swipeRepository)
            vm.handleIntent(ReadingListIntent.RemoveBook("/works/OL5"))
            advanceUntilIdle()

            coVerify { swipeRepository.removeBook("/works/OL5") }
        }
    }
}
