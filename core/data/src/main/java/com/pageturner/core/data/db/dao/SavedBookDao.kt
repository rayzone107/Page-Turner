package com.pageturner.core.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.pageturner.core.data.entity.SavedBookEntity
import com.pageturner.core.data.entity.SavedBookWithDetail
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedBookDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveBook(book: SavedBookEntity)

    @Transaction
    @Query("SELECT * FROM saved_books ORDER BY savedAt DESC")
    fun getSavedBooksWithDetails(): Flow<List<SavedBookWithDetail>>

    /** Only books swiped right (liked). */
    @Transaction
    @Query("SELECT * FROM saved_books WHERE isBookmarked = 0 ORDER BY savedAt DESC")
    fun getLikedBooksWithDetails(): Flow<List<SavedBookWithDetail>>

    /** Only books bookmarked (BOOKMARK swipe). */
    @Transaction
    @Query("SELECT * FROM saved_books WHERE isBookmarked = 1 ORDER BY savedAt DESC")
    fun getBookmarkedBooksWithDetails(): Flow<List<SavedBookWithDetail>>

    @Query("DELETE FROM saved_books WHERE bookKey = :bookKey")
    suspend fun removeBook(bookKey: String)

    @Query("SELECT EXISTS(SELECT 1 FROM saved_books WHERE bookKey = :bookKey)")
    suspend fun isBookSaved(bookKey: String): Boolean
}
