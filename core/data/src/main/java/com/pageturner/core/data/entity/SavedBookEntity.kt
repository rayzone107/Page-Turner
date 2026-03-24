package com.pageturner.core.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "saved_books",
    foreignKeys = [ForeignKey(
        entity = BookEntity::class,
        parentColumns = ["key"],
        childColumns = ["bookKey"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("bookKey")]
)
data class SavedBookEntity(
    @PrimaryKey val bookKey: String,
    val savedAt: Long,
    val aiBrief: String?,
    val wildcardReason: String?,
    val isWildcard: Boolean,
    /** True when the book was bookmarked (BOOKMARK swipe); false when liked (RIGHT swipe). */
    val isBookmarked: Boolean = false,
)
