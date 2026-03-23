package com.pageturner.core.domain.model

/**
 * The AI-generated taste profile, updated every 10 swipes by [AiService.summarizeProfile].
 *
 * [aiSummary] is Claude's verbatim output — displayed directly on the Taste Profile screen.
 * [profileVersion] increments on every successful update; used as cache key for AI briefs.
 */
data class TasteProfile(
    val aiSummary: String,
    val likedGenres: List<String>,
    val avoidedGenres: List<String>,
    val preferredLength: String,
    val recurringThemes: List<String>,
    val profileVersion: Int,
    val lastUpdatedSwipeCount: Int,
    val updatedAt: Long
)
