package com.pageturner.core.ai.di

import com.pageturner.core.ai.service.ClaudeAiService
import com.pageturner.core.domain.service.AiService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds [AiService] to [ClaudeAiService].
 *
 * Feature modules inject [AiService] — they never see [ClaudeAiService] or the Anthropic SDK.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AiModule {

    @Binds
    @Singleton
    abstract fun bindAiService(impl: ClaudeAiService): AiService
}
