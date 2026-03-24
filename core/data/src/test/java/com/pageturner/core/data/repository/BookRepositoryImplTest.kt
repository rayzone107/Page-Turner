package com.pageturner.core.data.repository

import app.cash.turbine.test
import com.pageturner.core.data.db.converter.Converters
import com.pageturner.core.data.db.dao.BookDao
import com.pageturner.core.data.db.dao.SwipeEventDao
import com.pageturner.core.data.entity.BookEntity
import com.pageturner.core.network.api.OpenLibraryApiService
import com.pageturner.core.network.dto.openlib.SearchDocDto
import com.pageturner.core.network.dto.openlib.SearchResponseDto
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class BookRepositoryImplTest {

    @MockK private lateinit var bookDao: BookDao
    @MockK private lateinit var swipeEventDao: SwipeEventDao
    @MockK private lateinit var openLibraryApiService: OpenLibraryApiService

    private lateinit var repo: BookRepositoryImpl

    private fun anEntity(key: String = "/works/OL1") = BookEntity(
        key = key,
        title = "Book",
        authorNamesJson = Converters.serializeList(listOf("Author")),
        coverUrl = null,
        publishYear = 2000,
        pageCount = 300,
        subjectsJson = Converters.serializeList(listOf("fantasy")),
        description = null,
        cachedAt = 0L,
    )

    private fun aDoc(key: String = "/works/OL1") = SearchDocDto(
        key = key, title = "Book", authorName = listOf("Author"),
        firstPublishYear = 2000, coverId = null, subject = listOf("fantasy"),
        numberOfPagesMedian = 300
    )

    private fun aSearchResponse(vararg docs: SearchDocDto) =
        SearchResponseDto(numFound = docs.size, docs = docs.toList())

    @BeforeEach
    fun setUp() {
        repo = BookRepositoryImpl(bookDao, swipeEventDao, openLibraryApiService)
    }

    @Nested
    inner class `getSwipeQueue` {

        @Test
        fun `emits cached books immediately before any network call`() = runTest {
            val cached = (1..15).map { anEntity("/works/OL$it") }
            coEvery { bookDao.getUnseenBooks(any()) } returns cached
            coEvery { bookDao.getAllBooks() } returns cached

            repo.getSwipeQueue(listOf("fantasy"), emptySet()).test {
                val first = awaitItem()
                assertTrue(first.isNotEmpty())
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        fun `fetches from network and re-emits when local cache has fewer than 10 books`() = runTest {
            val sparse = (1..5).map { anEntity("/works/OL$it") }
            val networkDoc = aDoc("/works/OL99")
            coEvery { bookDao.getAllBooks() } returns sparse
            coEvery { bookDao.getUnseenBooks(any()) } returns sparse
            coEvery { openLibraryApiService.searchBySubject(any(), any(), any(), any()) } returns aSearchResponse(networkDoc)
            coJustRun { bookDao.upsertBooks(any()) }
            coJustRun { bookDao.upsertBook(any()) }

            repo.getSwipeQueue(listOf("fantasy"), emptySet()).test {
                skipItems(1) // first emit from cache
                cancelAndIgnoreRemainingEvents()
            }

            coVerify { openLibraryApiService.searchBySubject("fantasy", any(), any(), any()) }
        }
    }

    @Nested
    inner class `prefetchBookDetail` {

        @Test
        fun `skips network when description is already cached`() = runTest {
            coEvery { bookDao.getBook(any()) } returns anEntity().copy(description = "Cached desc")

            repo.prefetchBookDetail("/works/OL1")

            coVerify(exactly = 0) { openLibraryApiService.getWorkDetail(any()) }
        }
    }

    @Nested
    inner class `fetchNextPage` {

        @Test
        fun `advances the page counter on each call`() = runTest {
            coEvery { openLibraryApiService.searchBySubject(eq("fantasy"), any(), eq(2), any()) } returns aSearchResponse()
            coEvery { openLibraryApiService.searchBySubject(eq("fantasy"), any(), eq(3), any()) } returns aSearchResponse()

            repo.fetchNextPage(listOf("fantasy"), emptySet()) // page 2
            repo.fetchNextPage(listOf("fantasy"), emptySet()) // page 3

            coVerify { openLibraryApiService.searchBySubject("fantasy", any(), 2, any()) }
            coVerify { openLibraryApiService.searchBySubject("fantasy", any(), 3, any()) }
        }

        @Test
        fun `deduplicates books appearing across genre pages`() = runTest {
            val doc = aDoc("/works/OL_DUPE")
            coEvery { openLibraryApiService.searchBySubject(eq("fantasy"), any(), any(), any()) } returns aSearchResponse(doc)
            coEvery { openLibraryApiService.searchBySubject(eq("classics"), any(), any(), any()) } returns aSearchResponse(doc)
            coJustRun { bookDao.upsertBook(any()) }

            val result = repo.fetchNextPage(listOf("fantasy", "classics"), emptySet())

            assertEquals(1, (result as com.pageturner.core.domain.util.Result.Success).data.size)
        }
    }

    @Nested
    inner class `getSeenBookKeys` {

        @Test
        fun `returns keys from swipeEventDao as a Set`() = runTest {
            every { swipeEventDao.getSeenBookKeys() } returns flowOf(listOf("/works/OL1", "/works/OL2"))

            repo.getSeenBookKeys().test {
                val keys = awaitItem()
                assertEquals(setOf("/works/OL1", "/works/OL2"), keys)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }
}
