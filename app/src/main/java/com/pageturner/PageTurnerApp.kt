package com.pageturner

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/** Application class — Hilt entry point. */
@HiltAndroidApp
class PageTurnerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        if (BuildConfig.ANTHROPIC_API_KEY.isBlank()) {
            Timber.tag("App").e("ANTHROPIC_API_KEY is not set in local.properties — all AI features will fail")
        }
    }
}
