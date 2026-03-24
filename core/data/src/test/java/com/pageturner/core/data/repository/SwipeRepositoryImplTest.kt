package com.pageturner.core.data.repository

import app.cash.turbine.test
import com.pageturner.core.data.db.converter.Converters
import com.pageturner.core.data.db.dao.AiBriefCacheDao
import com.pageturner.core.data.db.dao.SavedBookDao
import com.pageturner.core.data.db.dao.SwipeEventDao
import com.pageturner.core.data.entity.BookEntity
import com.pageturner.core.data.entity.SavedBookEntity
import com.pageturner.core.data.entity.SavedBookWithDetail
import com.pageturner.core.domain.model.SwipeDirection
import com.pageturner.core.domain.model.SwipeEvent
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class SwipeRepositoryImplTest {

    @MockK private lateinit var swipeEventDao: SwipeEventDao
    @MockK private lateinit var savedBookDao: SavedBookDao
    @MockK private lateinit var aiBriefCacheDao: AiBriefCacheDao

    private lateinit var repo: SwipeRepositoryImpl

    private fun anEntity(key: String = "/works/OL1") = BookEntity(
        key = key, title = "Book",
        authorNamesJson = Converters.serializeList(listOf("Author")),
        coverUrl = null, publishYear = null, pageCount = null,
        subjectsJson = Converters.serializeList(emptyList()),
        description = null, cachedAt = 0L,
    )

    private fun aSavedBook(key: String, isBookmarked: Boolean = false) = SavedBookWithDetail(
        book = anEntity(key),
        savedBook = SavedBookEntity(
            bookKey = key, savedAt = 0L, aiBrief = null,
            wildcardReason = null, isWildcard = false, isBookmarked = isBookmarked,
        )
    )

    @BeforeEach
    fun setUp() {
        repo = SwipeRepositoryImpl(swipeEventDao, savedBookDao, aiBriefCacheDao)
    }

    @Nested
    inner class `recordSwipe` {

        @Test
        fun `inserts a SwipeEventEntity with the correct direction name`() = runTest {
            val entitySlot = slot<com.pageturner.core.data.entity.SwipeEventEntity>()
            coJustRun { swipeEventDao.insertSwipeEvent(capture(entitySlot)) }

            repo.recordSwipe(
                SwipeEvent(
                    id = 0, bookKey = "/works/OL1", direction = SwipeDirection.RIGHT,
                    timestamp = 1000L, bookGenres = emptyList(), bookYear = null,
                    bookPageCount = null, wasWildcard = false,
                )
            )

            assertEquals("RIGHT", entitySlot.captured.direction)
        }
    }

    @Nested
    inner class `saveBook` {

        @Test
        fun `inserts with isBookmarked false for a right swipe`() = runTest {
            val entitySlot = slot<SavedBookEntity>()
            coJustRun { savedBookDao.saveBook(capture(entitySlot)) }

            repo.saveBook(
                bookKey = "/works/OL1",
                aiBrief = null,
                wildcardReason = null,
                isWildcard = false,
                isBookmarked = false,
            )

            assertFalse(entitySlot.captured.isBookmarked)
        }

        @Test
        fun `inserts with isBookmarked true for a bookmark swipe`() = runTest {
            val entitySlot = slot<SavedBookEntity>()
            coJustRun { savedBookDao.saveBook(capture(entitySlot)) }

            repo.saveBook(
                bookKey = "/works/OL1",
                aiBrief = null,
                wildcardReason = null,
                isWildcard = false,
                isBookmarked = true,
            )

            assertTrue(entitySlot.captured.isBookmarked)
        }
    }

    @Nested
    inner class `getLikedBooks` {

        @Test
        fun `only non-bookmarked saved books appear`() = runTest {
            every { savedBookDao.getLikedBooksWithDetails() } returns
                flowOf(listOf(aSavedBook("/works/OL1", isBookmarked = false)))

            repo.getLikedBooks().test {
                val books = awaitItem()
                assertEquals(1, books.size)
                assertEquals("/works/OL1", books.first().key)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    inner class `getBookmarkedBooks` {

        @Test
        fun `only bookmarked books appear`() = runTest {
            every { savedBookDao.getBookmarkedBooksWithDetails() } returns
                flowOf(listOf(aSavedBook("/works/OL2", isBookmarked = true)))

            repo.getBookmarkedBooks().test {
                val books = awaitItem()
                assertEquals(1, books.size)
                assertEquals("/works/OL2", books.first().key)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    inner class `isBookSaved` {

        @Test
        fun `returns true when book key is in saved books`() = runTest {
            coEvery { savedBookDao.isBookSaved("/works/OL1") } returns true
            assertTrue(repo.isBookSaved("/works/OL1"))
        }

        @Test
        fun `returns false when book key is absent`() = runTest {
            coEvery { savedBookDao.isBookSaved("/works/OL999") } returns false
            assertFalse(repo.isBookSaved("/works/OL999"))
        }
    }
}
