package com.pageturner.core.network.di

import com.pageturner.core.network.adapter.WorkDescriptionAdapter
import com.pageturner.core.network.api.AnthropicApiService
import com.pageturner.core.network.api.OpenLibraryApiService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.pageturner.core.network.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val OPEN_LIBRARY_BASE_URL = "https://openlibrary.org/"
    private const val ANTHROPIC_BASE_URL    = "https://api.anthropic.com/"
    private const val ANTHROPIC_VERSION     = "2023-06-01"

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .add(WorkDescriptionAdapter())
        .addLast(KotlinJsonAdapterFactory())
        .build()

    // Open Library

    @Provides
    @Singleton
    fun provideOpenLibraryOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
                        else HttpLoggingInterceptor.Level.NONE
            }
        )
        .build()

    @Provides
    @Singleton
    fun provideOpenLibraryRetrofit(
        okHttpClient: OkHttpClient,
        moshi: Moshi
    ): Retrofit = Retrofit.Builder()
        .baseUrl(OPEN_LIBRARY_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    @Provides
    @Singleton
    fun provideOpenLibraryApiService(retrofit: Retrofit): OpenLibraryApiService =
        retrofit.create(OpenLibraryApiService::class.java)

    // Anthropic

    @Provides
    @Singleton
    @AnthropicClient
    fun provideAnthropicOkHttpClient(
        @Named("anthropic_api_key") apiKey: String
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", ANTHROPIC_VERSION)
                    .header("content-type", "application/json")
                    .build()
            )
        }
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
                        else HttpLoggingInterceptor.Level.NONE
            }
        )
        .build()

    @Provides
    @Singleton
    @AnthropicClient
    fun provideAnthropicRetrofit(
        @AnthropicClient okHttpClient: OkHttpClient,
        moshi: Moshi
    ): Retrofit = Retrofit.Builder()
        .baseUrl(ANTHROPIC_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    @Provides
    @Singleton
    fun provideAnthropicApiService(
        @AnthropicClient retrofit: Retrofit
    ): AnthropicApiService = retrofit.create(AnthropicApiService::class.java)
}
