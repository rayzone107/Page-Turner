package com.pageturner.core.data.di

import android.content.Context
import androidx.room.Room
import com.pageturner.core.data.db.PageTurnerDatabase
import com.pageturner.core.data.db.dao.AiBriefCacheDao
import com.pageturner.core.data.db.dao.BookDao
import com.pageturner.core.data.db.dao.OnboardingDao
import com.pageturner.core.data.db.dao.SavedBookDao
import com.pageturner.core.data.db.dao.SwipeEventDao
import com.pageturner.core.data.db.dao.TasteProfileDao
import com.pageturner.core.data.repository.BookRepositoryImpl
import com.pageturner.core.data.repository.ProfileRepositoryImpl
import com.pageturner.core.data.repository.SwipeRepositoryImpl
import com.pageturner.core.domain.repository.BookRepository
import com.pageturner.core.domain.repository.ProfileRepository
import com.pageturner.core.domain.repository.SwipeRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PageTurnerDatabase =
        Room.databaseBuilder(
            context,
            PageTurnerDatabase::class.java,
            "pageturner.db"
        ).build()

    @Provides fun provideBookDao(db: PageTurnerDatabase): BookDao = db.bookDao()
    @Provides fun provideSwipeEventDao(db: PageTurnerDatabase): SwipeEventDao = db.swipeEventDao()
    @Provides fun provideTasteProfileDao(db: PageTurnerDatabase): TasteProfileDao = db.tasteProfileDao()
    @Provides fun provideSavedBookDao(db: PageTurnerDatabase): SavedBookDao = db.savedBookDao()
    @Provides fun provideAiBriefCacheDao(db: PageTurnerDatabase): AiBriefCacheDao = db.aiBriefCacheDao()
    @Provides fun provideOnboardingDao(db: PageTurnerDatabase): OnboardingDao = db.onboardingDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class DataBindingsModule {

    @Binds @Singleton
    abstract fun bindBookRepository(impl: BookRepositoryImpl): BookRepository

    @Binds @Singleton
    abstract fun bindSwipeRepository(impl: SwipeRepositoryImpl): SwipeRepository

    @Binds @Singleton
    abstract fun bindProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository
}
