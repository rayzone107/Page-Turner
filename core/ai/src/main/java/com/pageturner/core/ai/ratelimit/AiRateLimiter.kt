package com.pageturner.core.ai.ratelimit

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enforces per-device AI call quotas backed by a Room timestamp log.
 *
 * Limits (per device, resets with the rolling window):
 *   - 20 calls / minute
 *   - 100 calls / hour
 *   - 200 calls / day
 *
 * These limits survive app restarts (stored in Room) but can be reset by clearing app data.
 * [quotaExceeded] is a reactive state that feature modules can observe to show quota messages.
 */
@Singleton
internal class AiRateLimiter @Inject constructor(private val dao: AiCallDao) {

    private val _quotaExceeded = MutableStateFlow(false)
    val quotaExceeded: StateFlow<Boolean> = _quotaExceeded.asStateFlow()

    /**
     * Returns `true` if this call is allowed (quota not exceeded) AND records it.
     * Returns `false` if any limit is hit — the caller must NOT make the API call.
     */
    suspend fun checkAndRecord(): Boolean {
        val now = System.currentTimeMillis()
        val perMinute = dao.countSince(now - 60_000L)
        val perHour   = dao.countSince(now - 3_600_000L)
        val perDay    = dao.countSince(now - 86_400_000L)

        return if (perMinute >= MAX_PER_MINUTE || perHour >= MAX_PER_HOUR || perDay >= MAX_PER_DAY) {
            _quotaExceeded.value = true
            false
        } else {
            _quotaExceeded.value = false
            dao.insert(AiCallEntity(timestamp = now))
            // Prune entries older than 24 h to prevent unbounded table growth.
            dao.deleteOlderThan(now - 86_400_000L)
            true
        }
    }

    companion object {
        const val MAX_PER_MINUTE = 20
        const val MAX_PER_HOUR   = 100
        const val MAX_PER_DAY    = 200
    }
}
