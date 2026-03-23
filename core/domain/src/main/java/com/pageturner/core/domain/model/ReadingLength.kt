package com.pageturner.core.domain.model

/** User's preferred book length, selected during onboarding. */
enum class ReadingLength(
    val displayName: String,
    val minPages: Int?,
    val maxPages: Int?
) {
    SHORT("Short (< 200p)", null, 200),
    MEDIUM("Medium (200–400p)", 200, 400),
    LONG("Long (400p+)", 400, null);

    /** Returns true if [pageCount] falls within this length range. */
    fun matches(pageCount: Int?): Boolean {
        if (pageCount == null) return true
        return when {
            minPages != null && pageCount < minPages -> false
            maxPages != null && pageCount >= maxPages -> false
            else -> true
        }
    }
}
