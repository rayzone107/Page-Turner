package com.pageturner.core.analytics.internal

/** Tool-specific sink for analytics events. */
internal interface AnalyticsAdapter {
    fun send(payload: AnalyticsPayload)
}
