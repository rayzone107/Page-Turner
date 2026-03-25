package com.pageturner.core.analytics.internal

/** Normalised, tool-agnostic representation of an analytics event. */
internal data class AnalyticsPayload(
    val name: String,
    val properties: Map<String, Any> = emptyMap(),
)
