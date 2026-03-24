package com.pageturner.navigation

import android.net.Uri

object AppRoute {
    const val ONBOARDING = "onboarding"
    const val SWIPE_DECK = "swipedeck"
    const val TASTE_PROFILE = "tasteprofile"
    const val READING_LIST = "readinglist"
    const val BOOK_DETAIL = "bookdetail/{bookKey}"

    /** URL-encodes [bookKey] to safely handle Open Library keys that contain '/'. */
    fun bookDetail(bookKey: String) = "bookdetail/${Uri.encode(bookKey)}"
}

val BOTTOM_NAV_ROUTES = setOf(
    AppRoute.SWIPE_DECK,
    AppRoute.TASTE_PROFILE,
    AppRoute.READING_LIST,
)

