package com.pageturner.core.analytics.internal

import com.pageturner.core.analytics.AnalyticsEvent

/** Maps [AnalyticsEvent] subclasses to tool-agnostic [AnalyticsPayload]s. */
internal fun AnalyticsEvent.toPayload(): AnalyticsPayload = when (this) {

    is AnalyticsEvent.ScreenView -> AnalyticsPayload(
        name = "screen_view",
        properties = mapOf("screen_name" to screenName),
    )

    is AnalyticsEvent.BookSwiped -> AnalyticsPayload(
        name = "book_swiped",
        properties = mapOf(
            "book_key"     to bookKey,
            "direction"    to direction,
            "was_wildcard" to wasWildcard,
        ),
    )

    is AnalyticsEvent.BookSaved -> AnalyticsPayload(
        name = "book_saved",
        properties = mapOf(
            "book_key"      to bookKey,
            "is_bookmarked" to isBookmarked,
            "was_wildcard"  to wasWildcard,
        ),
    )

    is AnalyticsEvent.ProfileUpdated -> AnalyticsPayload(
        name = "profile_updated",
        properties = mapOf(
            "swipe_count"     to swipeCount,
            "profile_version" to profileVersion,
        ),
    )

    is AnalyticsEvent.WildcardShown -> AnalyticsPayload(
        name = "wildcard_shown",
        properties = mapOf("book_key" to bookKey),
    )

    is AnalyticsEvent.WildcardAccepted -> AnalyticsPayload(
        name = "wildcard_accepted",
        properties = mapOf("book_key" to bookKey),
    )

    is AnalyticsEvent.AiBriefGenerated -> AnalyticsPayload(
        name = "ai_brief_generated",
        properties = mapOf(
            "book_key"    to bookKey,
            "duration_ms" to durationMs,
        ),
    )

    is AnalyticsEvent.AiJobFailed -> AnalyticsPayload(
        name = "ai_job_failed",
        properties = mapOf(
            "job"        to job,
            "error_type" to errorType,
        ),
    )

    is AnalyticsEvent.ErrorOccurred -> AnalyticsPayload(
        name = "error_occurred",
        properties = mapOf(
            "error_type"  to errorType,
            "screen_name" to screenName,
        ),
    )
}
