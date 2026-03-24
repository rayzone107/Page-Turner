package com.pageturner.core.data.mapper

import com.pageturner.core.data.db.converter.Converters
import com.pageturner.core.data.entity.OnboardingEntity
import com.pageturner.core.domain.model.Genre
import com.pageturner.core.domain.model.OnboardingPreferences
import com.pageturner.core.domain.model.ReadingLength

fun OnboardingPreferences.toEntity(): OnboardingEntity = OnboardingEntity(
    id                = 1,
    completed         = true,
    selectedGenresJson = Converters.serializeList(selectedGenres.map { it.name }),
    selectedLength    = selectedLength.name,
    completedAt       = completedAt
)

fun OnboardingEntity.toDomain(): OnboardingPreferences = OnboardingPreferences(
    selectedGenres = Converters.parseList(selectedGenresJson)
        .mapNotNull { runCatching { Genre.valueOf(it) }.getOrNull() },
    selectedLength = runCatching { ReadingLength.valueOf(selectedLength) }
        .getOrDefault(ReadingLength.MEDIUM),
    completedAt    = completedAt
)
