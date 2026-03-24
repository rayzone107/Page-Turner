package com.pageturner.core.ai.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Moshi DTO for the JSON object returned by Claude in [SummarizeProfileUseCase].
 * Not a domain model — used only to parse the raw API response.
 */
@JsonClass(generateAdapter = true)
internal data class ProfileSummaryDto(
    @Json(name = "aiSummary")       val aiSummary: String,
    @Json(name = "likedGenres")     val likedGenres: List<String>,
    @Json(name = "avoidedGenres")   val avoidedGenres: List<String>,
    @Json(name = "preferredLength") val preferredLength: String,
    @Json(name = "recurringThemes") val recurringThemes: List<String>
)
