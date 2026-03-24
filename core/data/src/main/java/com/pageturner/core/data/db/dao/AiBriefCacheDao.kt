package com.pageturner.core.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.pageturner.core.data.entity.AiBriefCacheEntity

@Dao
interface AiBriefCacheDao {
    @Upsert
    suspend fun cacheBrief(entry: AiBriefCacheEntity)

    @Query("SELECT * FROM ai_brief_cache WHERE bookKey = :bookKey AND profileVersion = :profileVersion LIMIT 1")
    suspend fun getBrief(bookKey: String, profileVersion: Int): AiBriefCacheEntity?
}
