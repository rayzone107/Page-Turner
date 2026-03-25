package com.pageturner.core.ai

/**
 * Centralized model names for all AI use cases.
 *
 * Haiku — fast, low-cost: used for brief generation (latency-sensitive, fires per card).
 * Sonnet — most capable: used for profile summarization and wildcard picking (heavier tasks).
 */
internal object AiModels {
    const val HAIKU  = "claude-haiku-4-5-20251001"
    const val SONNET = "claude-sonnet-4-6"
}
