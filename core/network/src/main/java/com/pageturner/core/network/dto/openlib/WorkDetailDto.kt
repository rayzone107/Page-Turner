package com.pageturner.core.network.dto.openlib

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Full work detail from GET /works/{workId}.json.
 *
 * [description] is handled by [com.pageturner.core.network.adapter.WorkDescriptionAdapter]
 * because the Open Library API returns it as either a plain String or a
 * {"type": "/type/text", "value": "..."} object.
 */
@JsonClass(generateAdapter = true)
data class WorkDetailDto(
    @Json(name = "key") val key: String?,
    @Json(name = "description") val description: String?,
    @Json(name = "subjects") val subjects: List<String>?
)
