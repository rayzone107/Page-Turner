package com.pageturner.core.domain.model

/** All supported genres. Used for onboarding seed and queue building. */
enum class Genre(val displayName: String, val openLibrarySubject: String) {
    LITERARY_FICTION("Literary Fiction", "literary fiction"),
    SCIENCE_FICTION("Science Fiction", "science fiction"),
    FANTASY("Fantasy", "fantasy"),
    THRILLER("Thriller", "thriller"),
    HISTORICAL_FICTION("Historical Fiction", "historical fiction"),
    MYSTERY("Mystery", "mystery"),
    NON_FICTION("Non-Fiction", "nonfiction"),
    BIOGRAPHY("Biography", "biography"),
    HORROR("Horror", "horror"),
    PHILOSOPHY("Philosophy", "philosophy"),
    CLASSICS("Classics", "classics"),
    SHORT_STORIES("Short Stories", "short stories");

    companion object {
        /** Returns all genres — used to populate the onboarding chip grid. */
        fun all(): List<Genre> = entries.toList()
    }
}
