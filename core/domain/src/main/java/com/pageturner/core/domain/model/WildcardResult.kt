package com.pageturner.core.domain.model

/**
 * Output of [AiService.pickWildcard].
 *
 * [reason] is Claude's one-sentence explanation displayed on the card.
 * On AI failure, [reason] is null and the book is chosen randomly from the pool.
 */
data class WildcardResult(
    val book: Book,
    val reason: String?
)
