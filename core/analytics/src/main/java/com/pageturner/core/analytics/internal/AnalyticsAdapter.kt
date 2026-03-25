package com.pageturner.core.analytics.internal

/**
 * Tool-specific sink for analytics events.
 *
 * To integrate a new analytics tool (Firebase, Amplitude, Mixpanel…):
 *  1. Create a new class implementing this interface.
 *  2. Update the [com.pageturner.core.analytics.AnalyticsModule] binding.
 *
 * The [AnalyticsPayload] it receives uses normalised snake_case names and
 * typed values — adapt property names in [EventMapper] if the tool requires
 * a different convention.
 */
internal interface AnalyticsAdapter {
    fun send(payload: AnalyticsPayload)
}
