package com.pageturner.core.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.pageturner.core.data.entity.OnboardingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OnboardingDao {
    @Upsert
    suspend fun saveOnboarding(onboarding: OnboardingEntity)

    @Query("SELECT * FROM onboarding WHERE id = 1")
    suspend fun getOnboarding(): OnboardingEntity?

    @Query("SELECT * FROM onboarding WHERE id = 1")
    fun getOnboardingFlow(): Flow<OnboardingEntity?>
}
