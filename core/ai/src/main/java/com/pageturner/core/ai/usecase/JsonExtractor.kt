package com.pageturner.core.ai.usecase

/**
 * Strips Markdown code fences from Claude's response if present.
 *
 * Claude sometimes wraps JSON in ```json ... ``` blocks. This normalises
 * both fenced and raw JSON responses to a plain JSON string.
 */
internal fun extractJson(text: String): String {
    val fencePattern = Regex("""```(?:json)?\s*([\s\S]*?)```""")
    return fencePattern.find(text)?.groupValues?.getOrNull(1)?.trim()
        ?: text.trim()
}
