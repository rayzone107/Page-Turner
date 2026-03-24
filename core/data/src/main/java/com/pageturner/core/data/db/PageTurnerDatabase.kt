package com.pageturner.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.pageturner.core.data.db.converter.Converters
import com.pageturner.core.data.db.dao.AiBriefCacheDao
import com.pageturner.core.data.db.dao.BookDao
import com.pageturner.core.data.db.dao.OnboardingDao
import com.pageturner.core.data.db.dao.SavedBookDao
import com.pageturner.core.data.db.dao.SwipeEventDao
import com.pageturner.core.data.db.dao.TasteProfileDao
import com.pageturner.core.data.entity.AiBriefCacheEntity
import com.pageturner.core.data.entity.BookEntity
import com.pageturner.core.data.entity.OnboardingEntity
import com.pageturner.core.data.entity.SavedBookEntity
import com.pageturner.core.data.entity.SwipeEventEntity
import com.pageturner.core.data.entity.TasteProfileEntity

@Database(
    entities = [
        BookEntity::class,
        SwipeEventEntity::class,
        TasteProfileEntity::class,
        SavedBookEntity::class,
        AiBriefCacheEntity::class,
        OnboardingEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class PageTurnerDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun swipeEventDao(): SwipeEventDao
    abstract fun tasteProfileDao(): TasteProfileDao
    abstract fun savedBookDao(): SavedBookDao
    abstract fun aiBriefCacheDao(): AiBriefCacheDao
    abstract fun onboardingDao(): OnboardingDao
}
