package com.pageturner.core.domain.repository

import com.pageturner.core.domain.model.Book
import com.pageturner.core.domain.model.BookDetail
import com.pageturner.core.domain.util.Result
import kotlinx.coroutines.flow.Flow

/** Provides books for the swipe queue and detail screen. */
interface BookRepository {
    /**
     * Returns a flow of books available for swiping, filtered by [genres].
     * Applies cache-first strategy: reads Room, triggers network fetch when stock is low.
     * Never emits a book whose key is in [seenBookKeys].
     */
    fun getSwipeQueue(genres: List<String>, seenBookKeys: Set<String>): Flow<List<Book>>

    /** Fetches the next page of books from Open Library for [genre]. */
    suspend fun fetchBooks(genre: String, page: Int): Result<List<Book>>

    /** Returns the full detail for a book. Checks Room cache first, then network. */
    suspend fun getBookDetail(bookKey: String): Result<BookDetail>

    /** Upserts a book into the local cache. */
    suspend fun cacheBook(book: Book)

    /** Keys of all books the user has ever seen (swiped in any direction). */
    fun getSeenBookKeys(): Flow<Set<String>>
}
