package com.pageturner.feature.tasteprofile

import com.pageturner.core.domain.model.Book
import com.pageturner.core.domain.model.TasteProfile
import com.pageturner.core.domain.repository.ProfileRepository
import com.pageturner.core.domain.repository.SwipeRepository
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MockKExtension::class)
class TasteProfileViewModelTest {

    @MockK private lateinit var profileRepository: ProfileRepository
    @MockK private lateinit var swipeRepository: SwipeRepository

    private val testDispatcher = StandardTestDispatcher()

    private val profileFlow = MutableStateFlow<TasteProfile?>(null)
    private val swipeCountFlow = MutableStateFlow(0)
    private val savedBooksFlow = MutableStateFlow<List<Book>>(emptyList())

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { profileRepository.getProfile() } returns profileFlow
        every { swipeRepository.getSwipeCount() } returns swipeCountFlow
        every { swipeRepository.getSavedBooks() } returns savedBooksFlow
    }

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    private fun aProfile(
        summary: String = "You love dark, character-driven fiction.",
        liked: List<String> = listOf("literary fiction", "classics"),
        avoided: List<String> = listOf("romance"),
    ) = TasteProfile(
        aiSummary = summary,
        likedGenres = liked,
        avoidedGenres = avoided,
        preferredLength = "long",
        recurringThemes = listOf("moral complexity"),
        profileVersion = 2,
        lastUpdatedSwipeCount = 20,
        updatedAt = 0L,
    )

    @Nested
    inner class `when a profile exists` {

        @Test
        fun `state shows the AI summary text`() = runTest(testDispatcher) {
            profileFlow.value = aProfile(summary = "You love dark fiction.")
            val vm = TasteProfileViewModel(profileRepository, swipeRepository)
            advanceUntilIdle()

            assertEquals("You love dark fiction.", vm.state.value.profile?.aiSummary)
        }

        @Test
        fun `state shows liked and avoided genres`() = runTest(testDispatcher) {
            profileFlow.value = aProfile(
                liked = listOf("literary fiction"),
                avoided = listOf("romance")
            )
            val vm = TasteProfileViewModel(profileRepository, swipeRepository)
            advanceUntilIdle()

            val profile = vm.state.value.profile
            assertNotNull(profile)
            assertTrue("literary fiction" in profile!!.likedGenres)
            assertTrue("romance" in profile.avoidedGenres)
        }

        @Test
        fun `swipe stats are populated from the repository`() = runTest(testDispatcher) {
            profileFlow.value = aProfile()
            swipeCountFlow.value = 25
            val vm = TasteProfileViewModel(profileRepository, swipeRepository)
            advanceUntilIdle()

            assertEquals(25, vm.state.value.swipeStats.totalSwiped)
        }
    }

    @Nested
    inner class `when no profile exists yet` {

        @Test
        fun `profile in state is null`() = runTest(testDispatcher) {
            profileFlow.value = null
            val vm = TasteProfileViewModel(profileRepository, swipeRepository)
            advanceUntilIdle()

            assertNull(vm.state.value.profile)
        }
    }
}
