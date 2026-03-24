package com.pageturner.core.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.pageturner.core.data.entity.BookEntity

@Dao
interface BookDao {
    @Upsert
    suspend fun upsertBook(book: BookEntity)

    @Upsert
    suspend fun upsertBooks(books: List<BookEntity>)

    @Query("SELECT * FROM books WHERE key = :key LIMIT 1")
    suspend fun getBook(key: String): BookEntity?

    @Query("SELECT * FROM books LIMIT 50")
    suspend fun getAllBooks(): List<BookEntity>

    @Query("SELECT * FROM books WHERE key NOT IN (:seenKeys) LIMIT 50")
    suspend fun getUnseenBooks(seenKeys: List<String>): List<BookEntity>

    @Query("UPDATE books SET description = :description WHERE key = :key")
    suspend fun updateDescription(key: String, description: String)
}
