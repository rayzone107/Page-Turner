package com.pageturner.core.network.dto.openlib

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SearchResponseDto(
    @Json(name = "numFound") val numFound: Int = 0,
    @Json(name = "docs") val docs: List<SearchDocDto> = emptyList()
)
