package com.pageturner.core.data.repository

import com.pageturner.core.data.db.dao.AiBriefCacheDao
import com.pageturner.core.data.db.dao.SavedBookDao
import com.pageturner.core.data.db.dao.SwipeEventDao
import com.pageturner.core.data.entity.AiBriefCacheEntity
import com.pageturner.core.data.entity.SavedBookEntity
import com.pageturner.core.data.mapper.toDomain
import com.pageturner.core.data.mapper.toEntity
import com.pageturner.core.domain.model.Book
import com.pageturner.core.domain.model.SwipeEvent
import com.pageturner.core.domain.repository.SwipeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SwipeRepositoryImpl @Inject constructor(
    private val swipeEventDao: SwipeEventDao,
    private val savedBookDao: SavedBookDao,
    private val aiBriefCacheDao: AiBriefCacheDao
) : SwipeRepository {

    override suspend fun recordSwipe(event: SwipeEvent) = withContext(Dispatchers.IO) {
        swipeEventDao.insertSwipeEvent(event.toEntity())
    }

    override fun getSwipeHistory(): Flow<List<SwipeEvent>> =
        swipeEventDao.getSwipeHistory()
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(Dispatchers.IO)

    override fun getSwipeCount(): Flow<Int> =
        swipeEventDao.getSwipeCount().flowOn(Dispatchers.IO)

    override suspend fun saveBook(
        bookKey: String,
        aiBrief: String?,
        wildcardReason: String?,
        isWildcard: Boolean,
        isBookmarked: Boolean,
    ) = withContext(Dispatchers.IO) {
        savedBookDao.saveBook(
            SavedBookEntity(
                bookKey        = bookKey,
                savedAt        = System.currentTimeMillis(),
                aiBrief        = aiBrief,
                wildcardReason = wildcardReason,
                isWildcard     = isWildcard,
                isBookmarked   = isBookmarked,
            )
        )
    }

    override fun getSavedBooks(): Flow<List<Book>> =
        savedBookDao.getSavedBooksWithDetails()
            .map { list -> list.map { it.toDomain() } }
            .flowOn(Dispatchers.IO)

    override fun getLikedBooks(): Flow<List<Book>> =
        savedBookDao.getLikedBooksWithDetails()
            .map { list -> list.map { it.toDomain() } }
            .flowOn(Dispatchers.IO)

    override fun getBookmarkedBooks(): Flow<List<Book>> =
        savedBookDao.getBookmarkedBooksWithDetails()
            .map { list -> list.map { it.toDomain() } }
            .flowOn(Dispatchers.IO)

    override suspend fun isBookSaved(bookKey: String): Boolean = withContext(Dispatchers.IO) {
        savedBookDao.isBookSaved(bookKey)
    }

    override suspend fun removeBook(bookKey: String) = withContext(Dispatchers.IO) {
        savedBookDao.removeBook(bookKey)
    }

    override suspend fun getAiBriefCache(bookKey: String, profileVersion: Int): String? =
        withContext(Dispatchers.IO) {
            aiBriefCacheDao.getBrief(bookKey, profileVersion)?.brief
        }

    override suspend fun cacheAiBrief(
        bookKey: String,
        profileVersion: Int,
        brief: String
    ) = withContext(Dispatchers.IO) {
        aiBriefCacheDao.cacheBrief(
            AiBriefCacheEntity(
                bookKey        = bookKey,
                profileVersion = profileVersion,
                brief          = brief,
                generatedAt    = System.currentTimeMillis()
            )
        )
    }
}
