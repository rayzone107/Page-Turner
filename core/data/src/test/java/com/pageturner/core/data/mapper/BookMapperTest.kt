package com.pageturner.core.data.mapper

import com.pageturner.core.data.db.converter.Converters
import com.pageturner.core.data.entity.BookEntity
import com.pageturner.core.data.entity.SavedBookEntity
import com.pageturner.core.data.entity.SavedBookWithDetail
import com.pageturner.core.network.dto.openlib.SearchDocDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class BookMapperTest {

    private fun aDoc(
        key: String? = "/works/OL123",
        title: String? = "Test Book",
        authorName: List<String>? = listOf("Author One"),
        coverId: Long? = 12345L,
        subject: List<String>? = listOf("fantasy", "adventure"),
        firstPublishYear: Int? = 2000,
        numberOfPagesMedian: Int? = 300
    ) = SearchDocDto(
        key = key,
        title = title,
        authorName = authorName,
        firstPublishYear = firstPublishYear,
        coverId = coverId,
        subject = subject,
        numberOfPagesMedian = numberOfPagesMedian
    )

    private fun anEntity(
        key: String = "/works/OL123",
        title: String = "Test Book",
        subjectsJson: String = Converters.serializeList(listOf("fantasy")),
        description: String? = null
    ) = BookEntity(
        key = key,
        title = title,
        authorNamesJson = Converters.serializeList(listOf("Author One")),
        coverUrl = "https://covers.openlibrary.org/b/id/12345-M.jpg",
        publishYear = 2000,
        pageCount = 300,
        subjectsJson = subjectsJson,
        description = description,
        cachedAt = 0L
    )

    @Nested
    inner class `SearchDocDto toEntity` {

        @Test
        fun `all fields map correctly`() {
            val entity = aDoc().toEntity()
            assertEquals("/works/OL123", entity.key)
            assertEquals("Test Book", entity.title)
            assertEquals("https://covers.openlibrary.org/b/id/12345-M.jpg", entity.coverUrl)
            assertEquals(2000, entity.publishYear)
            assertEquals(300, entity.pageCount)
        }

        @Test
        fun `null key becomes empty string`() {
            assertEquals("", aDoc(key = null).toEntity().key)
        }

        @Test
        fun `null title becomes Unknown Title`() {
            assertEquals("Unknown Title", aDoc(title = null).toEntity().title)
        }

        @Test
        fun `null subjects become empty JSON list`() {
            val entity = aDoc(subject = null).toEntity()
            assertTrue(Converters.parseList(entity.subjectsJson).isEmpty())
        }

        @Test
        fun `null coverId produces null coverUrl`() {
            assertNull(aDoc(coverId = null).toEntity().coverUrl)
        }
    }

    @Nested
    inner class `BookEntity toDomain` {

        @Test
        fun `subjects round-trip through Converters`() {
            val subjects = listOf("fantasy", "adventure")
            val entity = anEntity(subjectsJson = Converters.serializeList(subjects))
            assertEquals(subjects, entity.toDomain().subjects)
        }

        @Test
        fun `explicit aiBrief is carried through`() {
            val domain = anEntity().toDomain(aiBrief = "A great read.")
            assertEquals("A great read.", domain.aiBrief)
        }

        @Test
        fun `default matchScore is 0_5`() {
            assertEquals(0.5f, anEntity().toDomain().matchScore)
        }
    }

    @Nested
    inner class `BookEntity toDetailDomain` {

        @Test
        fun `openLibraryUrl concatenates the openlibrary base url with the book key`() {
            val detail = anEntity(key = "/works/OL45804W").toDetailDomain()
            assertEquals("https://openlibrary.org/works/OL45804W", detail.openLibraryUrl)
        }
    }

    @Nested
    inner class `SavedBookWithDetail toDomain` {

        @Test
        fun `matchScore is fixed at 1_0`() {
            val saved = SavedBookWithDetail(
                book = anEntity(),
                savedBook = SavedBookEntity(
                    bookKey = "/works/OL123",
                    savedAt = 0L,
                    aiBrief = null,
                    wildcardReason = null,
                    isWildcard = false,
                    isBookmarked = false,
                )
            )
            assertEquals(1.0f, saved.toDomain().matchScore)
        }

        @Test
        fun `isWildcard and wildcardReason come from the savedBook`() {
            val saved = SavedBookWithDetail(
                book = anEntity(),
                savedBook = SavedBookEntity(
                    bookKey = "/works/OL123",
                    savedAt = 0L,
                    aiBrief = "Brief.",
                    wildcardReason = "Shares moral complexity.",
                    isWildcard = true,
                    isBookmarked = false,
                )
            )
            val domain = saved.toDomain()
            assertTrue(domain.isWildcard)
            assertEquals("Shares moral complexity.", domain.wildcardReason)
        }
    }
}
