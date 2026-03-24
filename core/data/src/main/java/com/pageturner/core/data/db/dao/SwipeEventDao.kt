package com.pageturner.core.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pageturner.core.data.entity.SwipeEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SwipeEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSwipeEvent(event: SwipeEventEntity)

    @Query("SELECT * FROM swipe_events ORDER BY timestamp DESC")
    fun getSwipeHistory(): Flow<List<SwipeEventEntity>>

    @Query("SELECT COUNT(*) FROM swipe_events")
    fun getSwipeCount(): Flow<Int>

    @Query("SELECT DISTINCT bookKey FROM swipe_events")
    fun getSeenBookKeys(): Flow<List<String>>
}
