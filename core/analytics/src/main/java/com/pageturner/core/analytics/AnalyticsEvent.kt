package com.pageturner.core.analytics

/**
 * All trackable events in the app.
 *
 * Each subclass carries exactly the properties needed to describe the event —
 * no raw strings, no untyped maps at the call site.
 *
 * The [internal.EventMapper] translates these into [internal.AnalyticsPayload]
 * using the naming conventions required by the analytics tool.
 * To add a new event: add a subclass here and a mapping branch in EventMapper.
 */
sealed class AnalyticsEvent {

    /** Fired when a screen becomes visible. */
    data class ScreenView(val screenName: String) : AnalyticsEvent()

    /** Fired on every card swipe (left, right, or bookmark). */
    data class BookSwiped(
        val bookKey: String,
        val direction: String,
        val wasWildcard: Boolean,
    ) : AnalyticsEvent()

    /** Fired when a book is saved (right-swipe or bookmark). */
    data class BookSaved(
        val bookKey: String,
        val isBookmarked: Boolean,
        val wasWildcard: Boolean,
    ) : AnalyticsEvent()

    /** Fired when the AI taste profile is successfully updated. */
    data class ProfileUpdated(
        val swipeCount: Int,
        val profileVersion: Int,
    ) : AnalyticsEvent()

    /** Fired when a wildcard card reaches the top of the swipe deck. */
    data class WildcardShown(val bookKey: String) : AnalyticsEvent()

    /** Fired when the user saves a wildcard card (right-swipe or bookmark). */
    data class WildcardAccepted(val bookKey: String) : AnalyticsEvent()

    /** Fired when an AI brief is successfully generated and cached. */
    data class AiBriefGenerated(
        val bookKey: String,
        val durationMs: Long,
    ) : AnalyticsEvent()

    /**
     * Fired when an AI job fails or is rate-limited.
     * [job] identifies which use case failed (e.g. "generate_brief", "summarize_profile").
     * [errorType] is "rate_limited" or "failed".
     */
    data class AiJobFailed(
        val job: String,
        val errorType: String,
    ) : AnalyticsEvent()

    /** Fired when an error state is shown to the user. */
    data class ErrorOccurred(
        val errorType: String,
        val screenName: String,
    ) : AnalyticsEvent()
}
