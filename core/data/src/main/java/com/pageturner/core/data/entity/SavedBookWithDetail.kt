package com.pageturner.core.data.entity

import androidx.room.Embedded
import androidx.room.Relation

/** Room relation used to load a saved book with its full cached metadata. */
data class SavedBookWithDetail(
    @Embedded val savedBook: SavedBookEntity,
    @Relation(
        parentColumn = "bookKey",
        entityColumn = "key"
    )
    val book: BookEntity
)
