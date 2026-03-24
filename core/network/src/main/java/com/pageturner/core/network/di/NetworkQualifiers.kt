package com.pageturner.core.network.di

import javax.inject.Qualifier

/**
 * Hilt qualifier for the Anthropic-specific OkHttpClient and Retrofit instance.
 * Separates the Anthropic client (which carries auth headers) from the Open Library client.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AnthropicClient
