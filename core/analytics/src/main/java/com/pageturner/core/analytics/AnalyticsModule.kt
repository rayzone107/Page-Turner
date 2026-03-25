package com.pageturner.core.analytics

import com.pageturner.core.analytics.internal.AnalyticsAdapter
import com.pageturner.core.analytics.internal.DefaultAnalyticsTracker
import com.pageturner.core.analytics.internal.LogcatAnalyticsAdapter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Hilt bindings for the analytics pipeline. */
@Module
@InstallIn(SingletonComponent::class)
abstract class AnalyticsModule {

    @Binds
    @Singleton
    internal abstract fun bindAnalyticsTracker(impl: DefaultAnalyticsTracker): AnalyticsTracker

    @Binds
    @Singleton
    internal abstract fun bindAnalyticsAdapter(impl: LogcatAnalyticsAdapter): AnalyticsAdapter
}
