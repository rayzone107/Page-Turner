package com.pageturner.core.network.dto.openlib

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** A single book entry from the Open Library search response. */
@JsonClass(generateAdapter = true)
data class SearchDocDto(
    @Json(name = "key") val key: String?,
    @Json(name = "title") val title: String?,
    @Json(name = "author_name") val authorName: List<String>?,
    @Json(name = "first_publish_year") val firstPublishYear: Int?,
    @Json(name = "cover_i") val coverId: Long?,
    @Json(name = "subject") val subject: List<String>?,
    @Json(name = "number_of_pages_median") val numberOfPagesMedian: Int?
) {
    /** Builds the Coil-loadable cover URL, or null if no cover is available. */
    fun coverUrl(): String? =
        coverId?.let { "https://covers.openlibrary.org/b/id/$it-M.jpg" }

    /**
     * Strips the "/works/" prefix from the key to get the bare work ID.
     * e.g. "/works/OL45804W" -> "OL45804W"
     */
    fun workId(): String? = key?.removePrefix("/works/")
}
