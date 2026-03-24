package com.pageturner.core.data.mapper

import com.pageturner.core.data.db.converter.Converters
import com.pageturner.core.data.entity.SwipeEventEntity
import com.pageturner.core.domain.model.SwipeDirection
import com.pageturner.core.domain.model.SwipeEvent

fun SwipeEvent.toEntity(): SwipeEventEntity = SwipeEventEntity(
    id            = id,
    bookKey       = bookKey,
    direction     = direction.name,
    timestamp     = timestamp,
    bookGenresJson = Converters.serializeList(bookGenres),
    bookYear      = bookYear,
    bookPageCount = bookPageCount,
    wasWildcard   = wasWildcard
)

fun SwipeEventEntity.toDomain(): SwipeEvent = SwipeEvent(
    id            = id,
    bookKey       = bookKey,
    direction     = SwipeDirection.valueOf(direction),
    timestamp     = timestamp,
    bookGenres    = Converters.parseList(bookGenresJson),
    bookYear      = bookYear,
    bookPageCount = bookPageCount,
    wasWildcard   = wasWildcard
)
