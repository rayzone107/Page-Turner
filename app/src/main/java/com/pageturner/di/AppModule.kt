package com.pageturner.di

import com.pageturner.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Provides the Anthropic API key from BuildConfig.
     * The key is read from local.properties at build time and never hard-coded or logged.
     */
    @Provides
    @Named("anthropic_api_key")
    fun provideAnthropicApiKey(): String = BuildConfig.ANTHROPIC_API_KEY
}
