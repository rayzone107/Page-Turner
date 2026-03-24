package com.pageturner.core.domain.service

/**
 * Result type for AI operations that can be rate-limited, distinguishing three outcomes:
 *
 * - [Success]     — the operation completed and returned data
 * - [RateLimited] — the per-minute/hour/day quota was exceeded; no API call was made
 * - [Failed]      — the API call was made but returned an error, blank content, or timed out
 *
 * Callers must handle [RateLimited] distinctly from [Failed]:
 * - [RateLimited] → show a quota-exceeded message to the user
 * - [Failed]      → degrade silently (shimmer clears, no error shown)
 */
sealed class AiResult<out T> {
    data class Success<out T>(val data: T) : AiResult<T>()
    data object RateLimited : AiResult<Nothing>()
    data object Failed : AiResult<Nothing>()
}
