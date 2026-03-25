package com.pageturner.core.network.util

import com.pageturner.core.domain.error.AppError
import com.pageturner.core.domain.util.Result
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * Wraps a Retrofit [call] in a [Result], mapping exceptions to [AppError] variants.
 *
 * Usage:
 * ```kotlin
 * val result = safeApiCall { apiService.searchBySubject("fantasy") }
 * ```
 */
suspend fun <T> safeApiCall(call: suspend () -> T): Result<T> = try {
    Result.Success(call())
} catch (e: SocketTimeoutException) {
    Result.Failure(AppError.TimeoutError)
} catch (e: HttpException) {
    when (e.code()) {
        404  -> Result.Failure(AppError.NotFoundError)
        else -> Result.Failure(AppError.NetworkError(e.code()))
    }
} catch (e: IOException) {
    Result.Failure(AppError.NoInternetError)
} catch (e: Exception) {
    Result.Failure(AppError.UnknownError(e.message))
}
