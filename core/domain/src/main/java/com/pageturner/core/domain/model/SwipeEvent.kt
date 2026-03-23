package com.pageturner.core.domain.model

/**
 * A single swipe action, persisted to Room and fed to the profile summariser.
 *
 * [bookGenres] and other book metadata are denormalized here so the AI prompt
 * can be built without joining back to the book table.
 */
data class SwipeEvent(
    val id: Long = 0L,
    val bookKey: String,
    val direction: SwipeDirection,
    val timestamp: Long,
    val bookGenres: List<String>,
    val bookYear: Int?,
    val bookPageCount: Int?,
    val wasWildcard: Boolean
)
