package com.pageturner.core.domain.error

/** All recoverable error states in PageTurner. */
sealed class AppError {
    data class NetworkError(val code: Int?) : AppError()
    data object NoInternetError : AppError()
    data object NotFoundError : AppError()
    /** AI degrades gracefully — never blocks the UI. */
    data class AiError(val message: String?) : AppError()
    data object AiTimeoutError : AppError()
    data class UnknownError(val message: String?) : AppError()
    data object CacheExpiredError : AppError()
}
