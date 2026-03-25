package com.pageturner.core.analytics.internal

import com.pageturner.core.logging.AppLogger
import javax.inject.Inject

/**
 * [AnalyticsAdapter] that writes events to logcat via [AppLogger].
 *
 * This is the default adapter. Replace the binding in [AnalyticsModule]
 * to route events to a real analytics SDK instead.
 */
internal class LogcatAnalyticsAdapter @Inject constructor(
    private val logger: AppLogger,
) : AnalyticsAdapter {

    override fun send(payload: AnalyticsPayload) {
        val props = if (payload.properties.isEmpty()) ""
        else " {${payload.properties.entries.joinToString(", ") { "${it.key}=${it.value}" }}}"
        logger.d(TAG, "[analytics] ${payload.name}$props")
    }

    private companion object {
        const val TAG = "Analytics"
    }
}
