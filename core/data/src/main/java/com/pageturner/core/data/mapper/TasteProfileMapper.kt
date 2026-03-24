package com.pageturner.core.data.mapper

import com.pageturner.core.data.db.converter.Converters
import com.pageturner.core.data.entity.TasteProfileEntity
import com.pageturner.core.domain.model.TasteProfile

fun TasteProfileEntity.toDomain(): TasteProfile = TasteProfile(
    aiSummary             = aiSummary,
    likedGenres           = Converters.parseList(likedGenresJson),
    avoidedGenres         = Converters.parseList(avoidedGenresJson),
    preferredLength       = preferredLength,
    recurringThemes       = Converters.parseList(recurringThemesJson),
    profileVersion        = profileVersion,
    lastUpdatedSwipeCount = lastUpdatedSwipeCount,
    updatedAt             = updatedAt
)

fun TasteProfile.toEntity(nextVersion: Int): TasteProfileEntity = TasteProfileEntity(
    id                    = 1,
    aiSummary             = aiSummary,
    likedGenresJson       = Converters.serializeList(likedGenres),
    avoidedGenresJson     = Converters.serializeList(avoidedGenres),
    preferredLength       = preferredLength,
    recurringThemesJson   = Converters.serializeList(recurringThemes),
    profileVersion        = nextVersion,
    lastUpdatedSwipeCount = lastUpdatedSwipeCount,
    updatedAt             = updatedAt
)
