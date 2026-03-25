package com.pageturner.core.analytics

/** Entry point for analytics tracking — inject wherever an event should be tracked. */
interface AnalyticsTracker {
    fun track(event: AnalyticsEvent)
}
