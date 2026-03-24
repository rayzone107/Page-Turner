package com.pageturner.core.network.api

import com.pageturner.core.network.dto.anthropic.AnthropicRequestDto
import com.pageturner.core.network.dto.anthropic.AnthropicResponseDto
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit interface for the Anthropic Messages API.
 *
 * Auth headers (x-api-key, anthropic-version) are injected by the OkHttp
 * interceptor configured in [com.pageturner.core.network.di.NetworkModule].
 * The API key never appears in this interface or its callers.
 */
interface AnthropicApiService {

    @POST("v1/messages")
    suspend fun createMessage(@Body request: AnthropicRequestDto): AnthropicResponseDto
}
