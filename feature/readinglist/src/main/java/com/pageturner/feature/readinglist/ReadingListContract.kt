package com.pageturner.feature.readinglist

import com.pageturner.core.domain.model.Book

data class ReadingListUiState(
    val books: List<SavedBookUiModel> = emptyList(),
    val isLoading: Boolean = false,
    val isEmpty: Boolean = false,
)

data class SavedBookUiModel(
    val bookKey: String,
    val title: String,
    val coverUrl: String?,
    val authors: List<String>,
    val savedAt: Long,
)

/** [savedAt] is not currently exposed by the domain Book model — defaults to 0. */
internal fun Book.toSavedUiModel() = SavedBookUiModel(
    bookKey = key,
    title = title,
    coverUrl = coverUrl,
    authors = authors,
    savedAt = 0L,
)

sealed class ReadingListIntent {
    data class SelectBook(val bookKey: String) : ReadingListIntent()
    data class RemoveBook(val bookKey: String) : ReadingListIntent()
}

sealed class ReadingListSideEffect {
    data class NavigateToDetail(val bookKey: String) : ReadingListSideEffect()
}
