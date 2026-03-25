package com.pageturner.feature.onboarding

import app.cash.turbine.test
import com.pageturner.core.analytics.AnalyticsTracker
import com.pageturner.core.domain.model.Genre
import com.pageturner.core.domain.model.ReadingLength
import com.pageturner.core.domain.repository.ProfileRepository
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MockKExtension::class)
class OnboardingViewModelTest {

    @MockK private lateinit var profileRepository: ProfileRepository
    @MockK(relaxed = true) private lateinit var analytics: AnalyticsTracker

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: OnboardingViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coJustRun { profileRepository.saveOnboardingPreferences(any()) }
        viewModel = OnboardingViewModel(profileRepository, analytics)
    }

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Nested
    inner class `initial state` {

        @Test
        fun `no genres are selected`() {
            assertTrue(viewModel.uiState.value.selectedGenres.isEmpty())
        }

        @Test
        fun `no lengths are selected`() {
            assertTrue(viewModel.uiState.value.selectedLengths.isEmpty())
        }

        @Test
        fun `canProceed is false`() {
            assertFalse(viewModel.uiState.value.canProceed)
        }
    }

    @Nested
    inner class `when a genre is toggled on` {

        @Test
        fun `it is added to selectedGenres`() {
            viewModel.onIntent(OnboardingIntent.ToggleGenre(Genre.FANTASY))
            assertTrue(Genre.FANTASY in viewModel.uiState.value.selectedGenres)
        }

        @Test
        fun `canProceed remains false if no length is selected`() {
            viewModel.onIntent(OnboardingIntent.ToggleGenre(Genre.FANTASY))
            assertFalse(viewModel.uiState.value.canProceed)
        }
    }

    @Nested
    inner class `when a genre is toggled off` {

        @Test
        fun `it is removed from selectedGenres`() {
            viewModel.onIntent(OnboardingIntent.ToggleGenre(Genre.FANTASY))
            viewModel.onIntent(OnboardingIntent.ToggleGenre(Genre.FANTASY))
            assertFalse(Genre.FANTASY in viewModel.uiState.value.selectedGenres)
        }

        @Test
        fun `canProceed becomes false if no genres remain`() {
            viewModel.onIntent(OnboardingIntent.ToggleGenre(Genre.FANTASY))
            viewModel.onIntent(OnboardingIntent.ToggleLength(ReadingLength.MEDIUM))
            assertTrue(viewModel.uiState.value.canProceed)

            viewModel.onIntent(OnboardingIntent.ToggleGenre(Genre.FANTASY))
            assertFalse(viewModel.uiState.value.canProceed)
        }
    }

    @Nested
    inner class `when canProceed is true` {

        @BeforeEach
        fun selectGenreAndLength() {
            viewModel.onIntent(OnboardingIntent.ToggleGenre(Genre.FANTASY))
            viewModel.onIntent(OnboardingIntent.ToggleLength(ReadingLength.MEDIUM))
        }

        @Test
        fun `at least one genre and one length are required`() {
            assertTrue(viewModel.uiState.value.canProceed)
        }

        @Test
        fun `confirm saves preferences and emits NavigateToSwipeDeck`() = runTest(testDispatcher) {
            viewModel.sideEffects.test {
                viewModel.onIntent(OnboardingIntent.Confirm)
                advanceUntilIdle()
                coVerify { profileRepository.saveOnboardingPreferences(any()) }
                val effect = awaitItem()
                assertTrue(effect is OnboardingSideEffect.NavigateToSwipeDeck)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    inner class `when canProceed is false` {

        @Test
        fun `confirm does nothing`() = runTest(testDispatcher) {
            viewModel.onIntent(OnboardingIntent.Confirm)
            advanceUntilIdle()
            coVerify(exactly = 0) { profileRepository.saveOnboardingPreferences(any()) }
        }
    }
}
