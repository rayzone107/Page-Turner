package com.pageturner.core.network.dto.anthropic

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** Response from POST /v1/messages. */
@JsonClass(generateAdapter = true)
data class AnthropicResponseDto(
    @Json(name = "id") val id: String,
    @Json(name = "type") val type: String,
    @Json(name = "role") val role: String,
    @Json(name = "content") val content: List<AnthropicContentBlockDto>,
    @Json(name = "model") val model: String,
    @Json(name = "stop_reason") val stopReason: String?
) {
    /** Convenience: extracts the first text block from the response, or null. */
    fun firstTextContent(): String? =
        content.firstOrNull { it.type == "text" }?.text
}

@JsonClass(generateAdapter = true)
data class AnthropicContentBlockDto(
    @Json(name = "type") val type: String,
    @Json(name = "text") val text: String?
)
