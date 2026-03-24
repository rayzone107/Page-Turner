package com.pageturner.feature.bookdetail

import com.pageturner.core.domain.error.AppError
import com.pageturner.core.domain.error.UiError
import com.pageturner.core.domain.model.BookDetail

data class BookDetailUiState(
    val isLoading: Boolean = false,
    val book: BookDetailUiModel? = null,
    val error: UiError? = null,
    val isOffline: Boolean = false,
)

data class BookDetailUiModel(
    val bookKey: String,
    val title: String,
    val authors: List<String>,
    val coverUrl: String?,
    val publishYear: Int?,
    val pageCount: Int?,
    val subjects: List<String>,
    val description: String?,
    val aiBrief: String?,
    val isWildcard: Boolean,
    val wildcardReason: String?,
    val openLibraryUrl: String,
)

internal fun BookDetail.toUiModel() = BookDetailUiModel(
    bookKey = key,
    title = title,
    authors = authors,
    coverUrl = coverUrl,
    publishYear = publishYear,
    pageCount = pageCount,
    subjects = subjects,
    description = description,
    aiBrief = aiBrief,
    isWildcard = isWildcard,
    wildcardReason = wildcardReason,
    openLibraryUrl = openLibraryUrl,
)

internal fun AppError.toUiError(): UiError = when (this) {
    is AppError.NoInternetError -> UiError(
        title = "No internet",
        message = "Connect to load this book's full details.",
        isRetryable = true,
    )
    is AppError.NotFoundError -> UiError(
        title = "Not found",
        message = "This book couldn't be found on Open Library.",
        isRetryable = false,
    )
    else -> UiError(
        title = "Something went wrong",
        message = "Couldn't load the book details. Please try again.",
        isRetryable = true,
    )
}

sealed class BookDetailIntent {
    data object NavigateBack : BookDetailIntent()
    data object OpenOnOpenLibrary : BookDetailIntent()
    data object Retry : BookDetailIntent()
}

sealed class BookDetailSideEffect {
    data object NavigateBack : BookDetailSideEffect()
    data class OpenUrl(val url: String) : BookDetailSideEffect()
}
