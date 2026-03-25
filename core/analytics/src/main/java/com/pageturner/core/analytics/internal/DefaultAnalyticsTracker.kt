package com.pageturner.core.analytics.internal

import com.pageturner.core.analytics.AnalyticsEvent
import com.pageturner.core.analytics.AnalyticsTracker
import javax.inject.Inject

/** Routes [AnalyticsEvent]s through [EventMapper] to the bound [AnalyticsAdapter]. */
internal class DefaultAnalyticsTracker @Inject constructor(
    private val adapter: AnalyticsAdapter,
) : AnalyticsTracker {

    override fun track(event: AnalyticsEvent) {
        adapter.send(event.toPayload())
    }
}
