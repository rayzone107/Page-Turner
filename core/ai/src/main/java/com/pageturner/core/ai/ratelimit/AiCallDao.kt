package com.pageturner.core.ai.ratelimit

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
internal interface AiCallDao {
    @Query("SELECT COUNT(*) FROM ai_call_log WHERE timestamp > :since")
    suspend fun countSince(since: Long): Int

    @Insert
    suspend fun insert(call: AiCallEntity)

    @Query("DELETE FROM ai_call_log WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long)
}
