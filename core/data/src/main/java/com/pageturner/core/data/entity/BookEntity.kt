package com.pageturner.core.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey val key: String,
    val title: String,
    val authorNamesJson: String,
    val coverUrl: String?,
    val publishYear: Int?,
    val pageCount: Int?,
    val subjectsJson: String,
    val description: String?,
    val cachedAt: Long
)
