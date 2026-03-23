package com.pageturner.core.domain.repository

import com.pageturner.core.domain.model.Book
import com.pageturner.core.domain.model.SwipeEvent
import kotlinx.coroutines.flow.Flow

/** Manages swipe events, saved books, and AI brief cache. */
interface SwipeRepository {
    /** Records a swipe event to Room. */
    suspend fun recordSwipe(event: SwipeEvent)

    /** All swipe events, newest first. */
    fun getSwipeHistory(): Flow<List<SwipeEvent>>

    /** Total number of swipes across all sessions. */
    fun getSwipeCount(): Flow<Int>

    /** Saves a book to the user's reading list. */
    suspend fun saveBook(
        bookKey: String,
        aiBrief: String?,
        wildcardReason: String?,
        isWildcard: Boolean
    )

    /** All books the user has saved (swiped right). Offline-first — Room only. */
    fun getSavedBooks(): Flow<List<Book>>

    /** Removes a book from the reading list. */
    suspend fun removeBook(bookKey: String)

    /**
     * Returns a cached AI brief for [(bookKey, profileVersion)], or null on a cache miss.
     * Cache key includes [profileVersion] so briefs are regenerated when the profile updates.
     */
    suspend fun getAiBriefCache(bookKey: String, profileVersion: Int): String?

    /** Stores a generated brief in the cache. */
    suspend fun cacheAiBrief(bookKey: String, profileVersion: Int, brief: String)
}
