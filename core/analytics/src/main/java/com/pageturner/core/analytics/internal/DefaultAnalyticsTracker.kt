package com.pageturner.core.analytics.internal

import com.pageturner.core.analytics.AnalyticsEvent
import com.pageturner.core.analytics.AnalyticsTracker
import javax.inject.Inject

/**
 * Routes every [AnalyticsEvent] through [EventMapper] and forwards the resulting
 * [AnalyticsPayload] to the bound [AnalyticsAdapter].
 *
 * To add a second destination (e.g. Crashlytics breadcrumbs alongside Firebase),
 * change the [AnalyticsAdapter] binding to a composite that fans out to both.
 */
internal class DefaultAnalyticsTracker @Inject constructor(
    private val adapter: AnalyticsAdapter,
) : AnalyticsTracker {

    override fun track(event: AnalyticsEvent) {
        adapter.send(event.toPayload())
    }
}
