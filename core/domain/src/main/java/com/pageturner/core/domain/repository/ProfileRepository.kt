package com.pageturner.core.domain.repository

import com.pageturner.core.domain.model.OnboardingPreferences
import com.pageturner.core.domain.model.TasteProfile
import kotlinx.coroutines.flow.Flow

/** Manages the taste profile and onboarding state. */
interface ProfileRepository {
    /**
     * Emits the current [TasteProfile] from Room, or null if none exists yet.
     * Available offline — always reads from local storage.
     */
    fun getProfile(): Flow<TasteProfile?>

    /** Persists an updated [TasteProfile] and increments [TasteProfile.profileVersion]. */
    suspend fun saveProfile(profile: TasteProfile)

    /** Emits true once the user has completed onboarding. */
    fun isOnboardingComplete(): Flow<Boolean>

    /** Returns the onboarding preferences, or null if onboarding has not been completed. */
    suspend fun getOnboardingPreferences(): OnboardingPreferences?

    /** Persists the onboarding choices and marks onboarding as complete. */
    suspend fun saveOnboardingPreferences(preferences: OnboardingPreferences)
}
