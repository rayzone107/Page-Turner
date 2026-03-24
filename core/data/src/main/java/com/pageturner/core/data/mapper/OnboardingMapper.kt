package com.pageturner.core.data.mapper

import com.pageturner.core.data.db.converter.Converters
import com.pageturner.core.data.entity.OnboardingEntity
import com.pageturner.core.domain.model.Genre
import com.pageturner.core.domain.model.OnboardingPreferences
import com.pageturner.core.domain.model.ReadingLength

fun OnboardingPreferences.toEntity(): OnboardingEntity = OnboardingEntity(
    id                 = 1,
    completed          = true,
    selectedGenresJson = Converters.serializeList(selectedGenres.map { it.name }),
    // Store lengths as a JSON array in the existing `selectedLength` column.
    selectedLength     = Converters.serializeList(selectedLengths.map { it.name }),
    completedAt        = completedAt,
)

fun OnboardingEntity.toDomain(): OnboardingPreferences = OnboardingPreferences(
    selectedGenres  = Converters.parseList(selectedGenresJson)
        .mapNotNull { runCatching { Genre.valueOf(it) }.getOrNull() },
    selectedLengths = parseLengths(selectedLength),
    completedAt     = completedAt,
)

/**
 * Handles both old format ("SHORT") and new JSON-array format (["SHORT","MEDIUM"]).
 * Returns [ReadingLength.MEDIUM] as the default if parsing fails entirely.
 */
private fun parseLengths(raw: String): List<ReadingLength> {
    // Try JSON array first (new format)
    val fromJson = Converters.parseList(raw)
        .mapNotNull { runCatching { ReadingLength.valueOf(it) }.getOrNull() }
    if (fromJson.isNotEmpty()) return fromJson
    // Fallback: treat as a single enum name (old format)
    val single = runCatching { ReadingLength.valueOf(raw) }.getOrNull()
    return if (single != null) listOf(single) else listOf(ReadingLength.MEDIUM)
}

