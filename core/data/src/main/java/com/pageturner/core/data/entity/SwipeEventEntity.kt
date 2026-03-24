package com.pageturner.core.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "swipe_events")
data class SwipeEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val bookKey: String,
    val direction: String,   // SwipeDirection.name — "LEFT", "RIGHT", "BOOKMARK"
    val timestamp: Long,
    val bookGenresJson: String,
    val bookYear: Int?,
    val bookPageCount: Int?,
    val wasWildcard: Boolean
)
