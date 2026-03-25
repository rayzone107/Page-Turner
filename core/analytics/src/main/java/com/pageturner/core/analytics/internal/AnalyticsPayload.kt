package com.pageturner.core.analytics.internal

/**
 * Normalised, tool-agnostic representation of an analytics event.
 *
 * [name] is the event name as the analytics tool will see it (e.g. "book_swiped").
 * [properties] holds the key-value pairs for the event. Values are [Any] to support
 * String, Int, Long, Boolean — all types accepted by common analytics SDKs.
 *
 * Produced by [EventMapper] from a typed [com.pageturner.core.analytics.AnalyticsEvent].
 * Consumed by [AnalyticsAdapter] implementations.
 */
internal data class AnalyticsPayload(
    val name: String,
    val properties: Map<String, Any> = emptyMap(),
)
