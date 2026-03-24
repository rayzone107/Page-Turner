package com.pageturner

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/** Application class — Hilt entry point. */
@HiltAndroidApp
class PageTurnerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
        if (BuildConfig.ANTHROPIC_API_KEY.isBlank()) {
            Timber.e("ANTHROPIC_API_KEY is not set in local.properties — all AI features will fail")
        }
    }
}
