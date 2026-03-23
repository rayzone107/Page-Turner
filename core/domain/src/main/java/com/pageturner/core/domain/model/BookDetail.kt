package com.pageturner.core.domain.model

/**
 * Full book detail, shown on the Book Detail screen.
 *
 * [openLibraryUrl] is always present — links to the book's Open Library page.
 */
data class BookDetail(
    val key: String,
    val title: String,
    val authors: List<String>,
    val coverUrl: String?,
    val publishYear: Int?,
    val pageCount: Int?,
    val subjects: List<String>,
    val description: String?,
    val aiBrief: String?,
    val isWildcard: Boolean,
    val wildcardReason: String?,
    val openLibraryUrl: String
)
