package com.pageturner.core.domain.model

/** The three possible swipe outcomes on the swipe deck. */
enum class SwipeDirection {
    /** Skip — book de-prioritized in future queue. */
    LEFT,
    /** Save to reading list. */
    RIGHT,
    /** Save to "Maybe" pile — softer save. */
    BOOKMARK
}
