package com.pageturner.core.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Single-row table (id always = 1). Updated in-place on every profile refresh. */
@Entity(tableName = "taste_profile")
data class TasteProfileEntity(
    @PrimaryKey val id: Int = 1,
    val aiSummary: String,
    val likedGenresJson: String,
    val avoidedGenresJson: String,
    val preferredLength: String,
    val recurringThemesJson: String,
    val profileVersion: Int,
    val lastUpdatedSwipeCount: Int,
    val updatedAt: Long
)
