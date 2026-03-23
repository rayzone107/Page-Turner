package com.pageturner.core.domain.util

import com.pageturner.core.domain.error.AppError

/** Discriminated union for domain operation results. */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Failure(val error: AppError) : Result<Nothing>()
}

/** Returns the [Success] value or null. */
fun <T> Result<T>.getOrNull(): T? = (this as? Result.Success)?.data

/** Returns the [Failure] error or null. */
fun <T> Result<T>.errorOrNull(): AppError? = (this as? Result.Failure)?.error

/** Maps a [Success] value; passes [Failure] through unchanged. */
inline fun <T, R> Result<T>.map(transform: (T) -> R): Result<R> = when (this) {
    is Result.Success -> Result.Success(transform(data))
    is Result.Failure -> this
}
