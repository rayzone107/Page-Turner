package com.pageturner.core.analytics.internal

import com.pageturner.core.logging.AppLogger
import javax.inject.Inject

/** Debug [AnalyticsAdapter] that writes events to logcat via [AppLogger]. */
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
