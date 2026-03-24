package com.pageturner.core.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Single-row table (id always = 1). Written once on onboarding completion. */
@Entity(tableName = "onboarding")
data class OnboardingEntity(
    @PrimaryKey val id: Int = 1,
    val completed: Boolean,
    val selectedGenresJson: String,
    val selectedLength: String,   // ReadingLength.name — "SHORT", "MEDIUM", "LONG"
    val completedAt: Long
)
