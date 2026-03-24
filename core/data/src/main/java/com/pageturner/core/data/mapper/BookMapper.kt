package com.pageturner.core.data.mapper

import com.pageturner.core.data.db.converter.Converters
import com.pageturner.core.data.entity.BookEntity
import com.pageturner.core.data.entity.SavedBookWithDetail
import com.pageturner.core.domain.model.Book
import com.pageturner.core.domain.model.BookDetail
import com.pageturner.core.domain.model.TasteProfile
import com.pageturner.core.network.dto.openlib.SearchDocDto

fun SearchDocDto.toEntity(): BookEntity = BookEntity(
    key         = key ?: "",
    title       = title ?: "Unknown Title",
    authorNamesJson = Converters.serializeList(authorName ?: emptyList()),
    coverUrl    = coverUrl(),
    publishYear = firstPublishYear,
    pageCount   = numberOfPagesMedian,
    subjectsJson = Converters.serializeList(subject ?: emptyList()),
    description = null,
    cachedAt    = System.currentTimeMillis()
)

fun BookEntity.toDomain(
    aiBrief: String? = null,
    matchScore: Float = 0.5f,
    isWildcard: Boolean = false,
    wildcardReason: String? = null
): Book = Book(
    key           = key,
    title         = title,
    authors       = Converters.parseList(authorNamesJson),
    coverUrl      = coverUrl,
    publishYear   = publishYear,
    pageCount     = pageCount,
    subjects      = Converters.parseList(subjectsJson),
    description   = description,
    aiBrief       = aiBrief,
    matchScore    = matchScore,
    isWildcard    = isWildcard,
    wildcardReason = wildcardReason
)

fun BookEntity.toDetailDomain(): BookDetail = BookDetail(
    key           = key,
    title         = title,
    authors       = Converters.parseList(authorNamesJson),
    coverUrl      = coverUrl,
    publishYear   = publishYear,
    pageCount     = pageCount,
    subjects      = Converters.parseList(subjectsJson),
    description   = description,
    aiBrief       = null,
    isWildcard    = false,
    wildcardReason = null,
    openLibraryUrl = "https://openlibrary.org$key"
)

fun SavedBookWithDetail.toDomain(): Book = book.toDomain(
    aiBrief       = savedBook.aiBrief,
    matchScore    = 1.0f,
    isWildcard    = savedBook.isWildcard,
    wildcardReason = savedBook.wildcardReason
)

/**
 * Calculates a local match score (0.0–1.0) based on subject overlap with the taste profile.
 * Not an external quality signal — measures taste alignment only.
 */
fun calculateMatchScore(bookSubjectsJson: String, profile: TasteProfile?): Float {
    if (profile == null) return 0.5f
    if (profile.likedGenres.isEmpty() && profile.avoidedGenres.isEmpty()) return 0.5f

    val subjects = Converters.parseList(bookSubjectsJson).map { it.lowercase() }
    val liked    = profile.likedGenres.map { it.lowercase() }
    val avoided  = profile.avoidedGenres.map { it.lowercase() }

    var score = 0.5f
    for (subject in subjects) {
        if (liked.any   { l -> subject.contains(l) || l.contains(subject) }) score += 0.1f
        if (avoided.any { a -> subject.contains(a) || a.contains(subject) }) score -= 0.1f
    }
    return score.coerceIn(0.05f, 0.99f)
}
