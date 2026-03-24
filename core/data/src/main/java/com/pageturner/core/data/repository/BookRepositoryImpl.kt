package com.pageturner.core.data.repository

import com.pageturner.core.data.db.dao.BookDao
import com.pageturner.core.data.db.dao.SwipeEventDao
import com.pageturner.core.data.mapper.calculateMatchScore
import com.pageturner.core.data.mapper.toDomain
import com.pageturner.core.data.mapper.toDetailDomain
import com.pageturner.core.data.mapper.toEntity
import com.pageturner.core.domain.error.AppError
import com.pageturner.core.domain.model.Book
import com.pageturner.core.domain.model.BookDetail
import com.pageturner.core.domain.model.TasteProfile
import com.pageturner.core.domain.repository.BookRepository
import com.pageturner.core.domain.util.Result
import com.pageturner.core.network.api.OpenLibraryApiService
import com.pageturner.core.network.util.safeApiCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookRepositoryImpl @Inject constructor(
    private val bookDao: BookDao,
    private val swipeEventDao: SwipeEventDao,
    private val openLibraryApiService: OpenLibraryApiService
) : BookRepository {

    override fun getSwipeQueue(
        genres: List<String>,
        seenBookKeys: Set<String>
    ): Flow<List<Book>> = flow {
        // 1. Emit immediately from local cache
        val cached = getFilteredBooks(genres, seenBookKeys)
        emit(cached.map { it.toDomain() })

        // 2. Fetch more from Open Library if stock is low (< 5 books)
        if (cached.size < 5) {
            genres.take(2).forEach { genre ->
                safeApiCall { openLibraryApiService.searchBySubject(genre) }
                    .getOrNull()
                    ?.docs
                    ?.filter { !it.key.isNullOrEmpty() && it.key !in seenBookKeys }
                    ?.map { it.toEntity() }
                    ?.let { bookDao.upsertBooks(it) }
            }
            // 3. Re-emit with fresh data
            val fresh = getFilteredBooks(genres, seenBookKeys)
            emit(fresh.map { it.toDomain() })
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun fetchBooks(genre: String, page: Int): Result<List<Book>> =
        safeApiCall {
            openLibraryApiService.searchBySubject(genre, page = page)
                .docs
                .filter { !it.key.isNullOrEmpty() }
                .map { doc ->
                    bookDao.upsertBook(doc.toEntity())
                    doc.toEntity().toDomain()
                }
        }

    override suspend fun getBookDetail(bookKey: String): Result<BookDetail> {
        val cached = bookDao.getBook(bookKey)
        if (cached != null && cached.description != null) {
            return Result.Success(cached.toDetailDomain())
        }
        val workId = bookKey.removePrefix("/works/")
        return safeApiCall {
            val detail = openLibraryApiService.getWorkDetail(workId)
            if (detail.description != null) {
                bookDao.updateDescription(bookKey, detail.description)
            }
            (cached ?: bookDao.getBook(bookKey))
                ?.copy(description = detail.description ?: cached?.description)
                ?.toDetailDomain()
                ?: throw Exception("Book not found: $bookKey")
        }
    }

    override suspend fun cacheBook(book: Book) {
        // No-op for simple books passed in from outside; upsert is handled during queue building
    }

    override fun getSeenBookKeys(): Flow<Set<String>> =
        swipeEventDao.getSeenBookKeys().map { it.toSet() }

    private suspend fun getFilteredBooks(
        genres: List<String>,
        seenBookKeys: Set<String>
    ) = (if (seenBookKeys.isEmpty()) bookDao.getAllBooks()
         else bookDao.getUnseenBooks(seenBookKeys.toList()))
        .filter { book ->
            val subjects = com.pageturner.core.data.db.converter.Converters
                .parseList(book.subjectsJson)
                .map { it.lowercase() }
            genres.any { genre ->
                subjects.any { s -> s.contains(genre.lowercase()) || genre.lowercase().contains(s) }
            }
        }
}

private fun <T> Result<T>.getOrNull(): T? = (this as? Result.Success)?.data
