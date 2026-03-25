package com.pageturner.feature.bookdetail

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.pageturner.core.analytics.AnalyticsTracker
import com.pageturner.core.domain.error.AppError
import com.pageturner.core.domain.model.BookDetail
import com.pageturner.core.domain.repository.BookRepository
import com.pageturner.core.domain.repository.SwipeRepository
import com.pageturner.core.domain.util.Result
import com.pageturner.core.logging.AppLogger
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MockKExtension::class)
class BookDetailViewModelTest {

    @MockK private lateinit var bookRepository: BookRepository
    @MockK private lateinit var swipeRepository: SwipeRepository
    @MockK(relaxed = true) private lateinit var analytics: AnalyticsTracker
    @MockK(relaxed = true) private lateinit var logger: AppLogger

    private val testDispatcher = StandardTestDispatcher()
    private val bookKey = "/works/OL123"
    private val savedStateHandle = SavedStateHandle(mapOf("bookKey" to bookKey))

    private fun aBookDetail() = BookDetail(
        key = bookKey,
        title = "Great Expectations",
        authors = listOf("Charles Dickens"),
        coverUrl = null,
        publishYear = 1861,
        pageCount = 544,
        subjects = listOf("classics"),
        description = "A story of ambition.",
        aiBrief = null,
        isWildcard = false,
        wildcardReason = null,
        openLibraryUrl = "https://openlibrary.org$bookKey",
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Nested
    inner class `initial state` {

        @Test
        fun `isLoading is true`() {
            coEvery { bookRepository.getBookDetail(any()) } returns Result.Success(aBookDetail())
            coEvery { swipeRepository.isBookSaved(any()) } returns false
            val vm = BookDetailViewModel(savedStateHandle, bookRepository, swipeRepository, analytics, logger)
            assertTrue(vm.state.value.isLoading)
        }
    }

    @Nested
    inner class `when book detail loads successfully` {

        @Test
        fun `isLoading becomes false`() = runTest(testDispatcher) {
            coEvery { bookRepository.getBookDetail(bookKey) } returns Result.Success(aBookDetail())
            coEvery { swipeRepository.isBookSaved(bookKey) } returns false

            val vm = BookDetailViewModel(savedStateHandle, bookRepository, swipeRepository, analytics, logger)
            advanceUntilIdle()

            assertFalse(vm.state.value.isLoading)
        }

        @Test
        fun `book is populated in state`() = runTest(testDispatcher) {
            coEvery { bookRepository.getBookDetail(bookKey) } returns Result.Success(aBookDetail())
            coEvery { swipeRepository.isBookSaved(bookKey) } returns false

            val vm = BookDetailViewModel(savedStateHandle, bookRepository, swipeRepository, analytics, logger)
            advanceUntilIdle()

            assertEquals("Great Expectations", vm.state.value.book?.title)
        }
    }

    @Nested
    inner class `when book detail load fails` {

        @Test
        fun `isLoading becomes false`() = runTest(testDispatcher) {
            coEvery { bookRepository.getBookDetail(bookKey) } returns
                Result.Failure(AppError.NetworkError(500))
            coEvery { swipeRepository.isBookSaved(bookKey) } returns false

            val vm = BookDetailViewModel(savedStateHandle, bookRepository, swipeRepository, analytics, logger)
            advanceUntilIdle()

            assertFalse(vm.state.value.isLoading)
        }

        @Test
        fun `error is shown in state`() = runTest(testDispatcher) {
            coEvery { bookRepository.getBookDetail(bookKey) } returns
                Result.Failure(AppError.NetworkError(500))
            coEvery { swipeRepository.isBookSaved(bookKey) } returns false

            val vm = BookDetailViewModel(savedStateHandle, bookRepository, swipeRepository, analytics, logger)
            advanceUntilIdle()

            assertNotNull(vm.state.value.error)
            assertNull(vm.state.value.book)
        }
    }

    @Nested
    inner class `when book is already saved` {

        @Test
        fun `isSaved is true`() = runTest(testDispatcher) {
            coEvery { bookRepository.getBookDetail(bookKey) } returns Result.Success(aBookDetail())
            coEvery { swipeRepository.isBookSaved(bookKey) } returns true

            val vm = BookDetailViewModel(savedStateHandle, bookRepository, swipeRepository, analytics, logger)
            advanceUntilIdle()

            assertTrue(vm.state.value.isSaved)
        }
    }

    @Nested
    inner class `when RemoveFromList is received` {

        @Test
        fun `repository removeBook is called`() = runTest(testDispatcher) {
            coEvery { bookRepository.getBookDetail(bookKey) } returns Result.Success(aBookDetail())
            coEvery { swipeRepository.isBookSaved(bookKey) } returns true
            coJustRun { swipeRepository.removeBook(any()) }

            val vm = BookDetailViewModel(savedStateHandle, bookRepository, swipeRepository, analytics, logger)
            advanceUntilIdle()
            vm.handleIntent(BookDetailIntent.RemoveFromList)
            advanceUntilIdle()

            coVerify { swipeRepository.removeBook(bookKey) }
        }

        @Test
        fun `isSaved becomes false`() = runTest(testDispatcher) {
            coEvery { bookRepository.getBookDetail(bookKey) } returns Result.Success(aBookDetail())
            coEvery { swipeRepository.isBookSaved(bookKey) } returns true
            coJustRun { swipeRepository.removeBook(any()) }

            val vm = BookDetailViewModel(savedStateHandle, bookRepository, swipeRepository, analytics, logger)
            advanceUntilIdle()
            vm.handleIntent(BookDetailIntent.RemoveFromList)
            advanceUntilIdle()

            assertFalse(vm.state.value.isSaved)
        }
    }

    @Nested
    inner class `when NavigateBack is received` {

        @Test
        fun `NavigateBack side effect is emitted`() = runTest(testDispatcher) {
            coEvery { bookRepository.getBookDetail(bookKey) } returns Result.Success(aBookDetail())
            coEvery { swipeRepository.isBookSaved(bookKey) } returns false

            val vm = BookDetailViewModel(savedStateHandle, bookRepository, swipeRepository, analytics, logger)
            advanceUntilIdle()

            vm.sideEffects.test {
                vm.handleIntent(BookDetailIntent.NavigateBack)
                advanceUntilIdle()
                val effect = awaitItem()
                assertTrue(effect is BookDetailSideEffect.NavigateBack)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    inner class `when OpenOnOpenLibrary is received with a loaded book` {

        @Test
        fun `OpenUrl side effect is emitted with the book URL`() = runTest(testDispatcher) {
            coEvery { bookRepository.getBookDetail(bookKey) } returns Result.Success(aBookDetail())
            coEvery { swipeRepository.isBookSaved(bookKey) } returns false

            val vm = BookDetailViewModel(savedStateHandle, bookRepository, swipeRepository, analytics, logger)
            advanceUntilIdle()

            vm.sideEffects.test {
                vm.handleIntent(BookDetailIntent.OpenOnOpenLibrary)
                advanceUntilIdle()
                val effect = awaitItem()
                assertEquals(
                    BookDetailSideEffect.OpenUrl("https://openlibrary.org$bookKey"),
                    effect
                )
                cancelAndIgnoreRemainingEvents()
            }
        }
    }
}
