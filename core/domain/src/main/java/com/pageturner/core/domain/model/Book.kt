package com.pageturner.core.domain.model

/**
 * A book as it appears in the swipe queue.
 *
 * [aiBrief] and [wildcardReason] are nullable — they are populated asynchronously
 * and must never block the display of the card.
 */
data class Book(
    val key: String,
    val title: String,
    val authors: List<String>,
    val coverUrl: String?,
    val publishYear: Int?,
    val pageCount: Int?,
    val subjects: List<String>,
    val description: String?,
    val aiBrief: String?,
    val matchScore: Float,
    val isWildcard: Boolean,
    val wildcardReason: String?
)
