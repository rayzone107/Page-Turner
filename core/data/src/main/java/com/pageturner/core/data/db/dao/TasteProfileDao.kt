package com.pageturner.core.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.pageturner.core.data.entity.TasteProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TasteProfileDao {
    @Upsert
    suspend fun saveProfile(profile: TasteProfileEntity)

    @Query("SELECT * FROM taste_profile WHERE id = 1")
    fun getProfileFlow(): Flow<TasteProfileEntity?>

    @Query("SELECT profileVersion FROM taste_profile WHERE id = 1")
    suspend fun getCurrentVersion(): Int?
}
