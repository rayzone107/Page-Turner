package com.pageturner.core.network.dto.anthropic

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** Request body for POST /v1/messages. */
@JsonClass(generateAdapter = true)
data class AnthropicRequestDto(
    @Json(name = "model") val model: String,
    @Json(name = "max_tokens") val maxTokens: Int,
    @Json(name = "messages") val messages: List<AnthropicMessageDto>
)

@JsonClass(generateAdapter = true)
data class AnthropicMessageDto(
    @Json(name = "role") val role: String,
    @Json(name = "content") val content: String
)
