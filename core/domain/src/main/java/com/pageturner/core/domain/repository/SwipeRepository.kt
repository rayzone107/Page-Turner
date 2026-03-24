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

    /**
     * Saves a book to the user's reading list.
     * [isBookmarked] distinguishes a BOOKMARK swipe (true) from a RIGHT/like swipe (false).
     */
    suspend fun saveBook(
        bookKey: String,
        aiBrief: String?,
        wildcardReason: String?,
        isWildcard: Boolean,
        isBookmarked: Boolean = false,
    )

    /** All books the user has saved (both liked and bookmarked). Offline-first — Room only. */
    fun getSavedBooks(): Flow<List<Book>>

    /** Only books the user swiped right (liked). */
    fun getLikedBooks(): Flow<List<Book>>

    /** Only books the user bookmarked (BOOKMARK swipe). */
    fun getBookmarkedBooks(): Flow<List<Book>>

    /** Whether a specific book is currently in the reading list. */
    suspend fun isBookSaved(bookKey: String): Boolean

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
