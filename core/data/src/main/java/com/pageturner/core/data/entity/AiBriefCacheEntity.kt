package com.pageturner.core.data.entity

import androidx.room.Entity

/**
 * Caches AI-generated briefs keyed by (bookKey, profileVersion).
 * When the profile version increments, old briefs are not used — new ones are generated.
 */
@Entity(tableName = "ai_brief_cache", primaryKeys = ["bookKey", "profileVersion"])
data class AiBriefCacheEntity(
    val bookKey: String,
    val profileVersion: Int,
    val brief: String,
    val generatedAt: Long
)
