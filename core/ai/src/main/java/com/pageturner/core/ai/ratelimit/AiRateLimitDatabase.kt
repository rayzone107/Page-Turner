package com.pageturner.core.ai.ratelimit

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [AiCallEntity::class], version = 1, exportSchema = false)
internal abstract class AiRateLimitDatabase : RoomDatabase() {
    abstract fun callDao(): AiCallDao
}
