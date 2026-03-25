package com.pageturner.core.analytics

/**
 * Entry point for analytics tracking.
 *
 * Inject this interface wherever an event should be tracked.
 * The implementation is bound in [AnalyticsModule] and routes
 * events through the internal adapter pipeline.
 */
interface AnalyticsTracker {
    fun track(event: AnalyticsEvent)
}
