package com.pageturner.core.data.mapper

import com.pageturner.core.data.db.converter.Converters
import com.pageturner.core.data.entity.SwipeEventEntity
import com.pageturner.core.domain.model.SwipeDirection
import com.pageturner.core.domain.model.SwipeEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class SwipeEventMapperTest {

    private fun aSwipeEvent(direction: SwipeDirection = SwipeDirection.RIGHT) = SwipeEvent(
        id = 0,
        bookKey = "/works/OL123",
        direction = direction,
        timestamp = 1000L,
        bookGenres = listOf("fantasy", "adventure"),
        bookYear = 2000,
        bookPageCount = 300,
        wasWildcard = false,
    )

    @Nested
    inner class `SwipeEvent toEntity` {

        @Test
        fun `direction is serialized as the enum name`() {
            val entity = aSwipeEvent(SwipeDirection.LEFT).toEntity()
            assertEquals("LEFT", entity.direction)
        }

        @Test
        fun `genres round-trip through Converters`() {
            val genres = listOf("fantasy", "adventure")
            val entity = aSwipeEvent().toEntity()
            assertEquals(genres, Converters.parseList(entity.bookGenresJson))
        }
    }

    @Nested
    inner class `SwipeEventEntity toDomain` {

        @Test
        fun `direction deserializes back to the correct enum value`() {
            val entity = SwipeEventEntity(
                id = 0,
                bookKey = "/works/OL123",
                direction = "BOOKMARK",
                timestamp = 1000L,
                bookGenresJson = Converters.serializeList(emptyList()),
                bookYear = null,
                bookPageCount = null,
                wasWildcard = false,
            )
            assertEquals(SwipeDirection.BOOKMARK, entity.toDomain().direction)
        }
    }

    @Nested
    inner class `all SwipeDirection values` {

        @ParameterizedTest
        @EnumSource(SwipeDirection::class)
        fun `each direction serializes and deserializes correctly`(direction: SwipeDirection) {
            val roundTripped = aSwipeEvent(direction).toEntity().toDomain().direction
            assertEquals(direction, roundTripped)
        }
    }
}
