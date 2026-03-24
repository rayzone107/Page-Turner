package com.pageturner.core.data.repository

import com.pageturner.core.data.db.dao.OnboardingDao
import com.pageturner.core.data.db.dao.TasteProfileDao
import com.pageturner.core.data.mapper.toDomain
import com.pageturner.core.data.mapper.toEntity
import com.pageturner.core.domain.model.OnboardingPreferences
import com.pageturner.core.domain.model.TasteProfile
import com.pageturner.core.domain.repository.ProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val tasteProfileDao: TasteProfileDao,
    private val onboardingDao: OnboardingDao
) : ProfileRepository {

    override fun getProfile(): Flow<TasteProfile?> =
        tasteProfileDao.getProfileFlow()
            .map { it?.toDomain() }
            .flowOn(Dispatchers.IO)

    /**
     * Saves the profile, incrementing [TasteProfile.profileVersion] from the current stored value.
     * The [TasteProfile] returned by [AiService.summarizeProfile] has profileVersion = 0 (sentinel);
     * the real version is assigned here.
     */
    override suspend fun saveProfile(profile: TasteProfile) = withContext(Dispatchers.IO) {
        val nextVersion = (tasteProfileDao.getCurrentVersion() ?: 0) + 1
        tasteProfileDao.saveProfile(profile.toEntity(nextVersion))
    }

    override fun isOnboardingComplete(): Flow<Boolean> =
        onboardingDao.getOnboardingFlow()
            .map { it?.completed == true }
            .flowOn(Dispatchers.IO)

    override suspend fun getOnboardingPreferences(): OnboardingPreferences? =
        withContext(Dispatchers.IO) {
            onboardingDao.getOnboarding()?.toDomain()
        }

    override suspend fun saveOnboardingPreferences(
        preferences: OnboardingPreferences
    ) = withContext(Dispatchers.IO) {
        onboardingDao.saveOnboarding(preferences.toEntity())
    }
}
