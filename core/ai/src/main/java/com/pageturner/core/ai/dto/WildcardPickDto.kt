package com.pageturner.core.ai.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Moshi DTO for the JSON object returned by Claude in [PickWildcardUseCase].
 * [selectedIndex] is the 0-based index into the candidates list.
 */
@JsonClass(generateAdapter = true)
internal data class WildcardPickDto(
    @Json(name = "selectedIndex") val selectedIndex: Int,
    @Json(name = "reason")        val reason: String
)
