package com.pageturner.core.domain.error

/** All recoverable error states in PageTurner. */
sealed class AppError {
    /** HTTP error from Open Library or Anthropic API. */
    data class NetworkError(val code: Int?) : AppError()
    /** Device has no internet connection. */
    data object NoInternetError : AppError()
    /** Requested resource does not exist (404). */
    data object NotFoundError : AppError()
    /** Claude API returned an error. Never blocks the UI — AI degrades gracefully. */
    data class AiError(val message: String?) : AppError()
    /** Claude API did not respond within the timeout. */
    data object AiTimeoutError : AppError()
    /** Unexpected error. */
    data class UnknownError(val message: String?) : AppError()
    /** Cached data has expired and remote fetch also failed. */
    data object CacheExpiredError : AppError()
}
