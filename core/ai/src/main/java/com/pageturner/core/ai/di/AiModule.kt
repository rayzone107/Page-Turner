package com.pageturner.core.ai.di

import android.content.Context
import androidx.room.Room
import com.pageturner.core.ai.ratelimit.AiCallDao
import com.pageturner.core.ai.ratelimit.AiRateLimitDatabase
import com.pageturner.core.ai.service.ClaudeAiService
import com.pageturner.core.domain.service.AiService
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds [AiService] to [ClaudeAiService] and provides the Room database used by
 * [com.pageturner.core.ai.ratelimit.AiRateLimiter] for persistent quota tracking.
 *
 * Feature modules inject [AiService] — they never see [ClaudeAiService] or the Anthropic SDK.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AiModule {

    @Binds
    @Singleton
    internal abstract fun bindAiService(impl: ClaudeAiService): AiService

    companion object {

        @Provides
        @Singleton
        internal fun provideAiRateLimitDatabase(
            @ApplicationContext context: Context
        ): AiRateLimitDatabase = Room.databaseBuilder(
            context,
            AiRateLimitDatabase::class.java,
            "ai_rate_limit.db"
        ).fallbackToDestructiveMigration().build()

        @Provides
        @Singleton
        internal fun provideAiCallDao(db: AiRateLimitDatabase): AiCallDao = db.callDao()
    }
}
