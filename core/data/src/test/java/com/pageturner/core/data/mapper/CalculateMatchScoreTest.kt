package com.pageturner.core.data.mapper

import com.pageturner.core.data.db.converter.Converters
import com.pageturner.core.domain.model.TasteProfile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class CalculateMatchScoreTest {

    private fun profile(liked: List<String> = emptyList(), avoided: List<String> = emptyList()) =
        TasteProfile(
            aiSummary = "",
            likedGenres = liked,
            avoidedGenres = avoided,
            preferredLength = "any",
            recurringThemes = emptyList(),
            profileVersion = 1,
            lastUpdatedSwipeCount = 10,
            updatedAt = 0L
        )

    private fun subjectsJson(vararg subjects: String) =
        Converters.serializeList(subjects.toList())

    @Nested
    inner class `when profile is null` {

        @Test
        fun `score is neutral 0_5`() {
            assertEquals(0.5f, calculateMatchScore(subjectsJson("fantasy"), null))
        }
    }

    @Nested
    inner class `when profile has no liked or avoided genres` {

        @Test
        fun `score is neutral 0_5`() {
            val p = profile()
            assertEquals(0.5f, calculateMatchScore(subjectsJson("fantasy"), p))
        }
    }

    @Nested
    inner class `when book subject matches a liked genre` {

        @Test
        fun `score is greater than 0_5`() {
            val p = profile(liked = listOf("fantasy"))
            assertTrue(calculateMatchScore(subjectsJson("fantasy"), p) > 0.5f)
        }
    }

    @Nested
    inner class `when book subject matches an avoided genre` {

        @Test
        fun `score is less than 0_5`() {
            val p = profile(avoided = listOf("romance"))
            assertTrue(calculateMatchScore(subjectsJson("romance"), p) < 0.5f)
        }
    }

    @Nested
    inner class `when book has many liked matches` {

        @Test
        fun `score is capped at 0_99`() {
            val p = profile(liked = listOf("fantasy", "fiction", "adventure", "magic", "quest", "dragons"))
            val score = calculateMatchScore(
                subjectsJson("fantasy", "fiction", "adventure", "magic", "quest", "dragons"), p
            )
            assertEquals(0.99f, score)
        }
    }

    @Nested
    inner class `when book has many avoided matches` {

        @Test
        fun `score is floored at 0_05`() {
            val p = profile(avoided = listOf("romance", "thriller", "horror", "mystery", "crime", "drama"))
            val score = calculateMatchScore(
                subjectsJson("romance", "thriller", "horror", "mystery", "crime", "drama"), p
            )
            assertEquals(0.05f, score)
        }
    }

    @Nested
    inner class `when liked and avoided subjects both match` {

        @Test
        fun `effects cancel out and score stays near 0_5`() {
            val p = profile(liked = listOf("fiction"), avoided = listOf("fiction"))
            val score = calculateMatchScore(subjectsJson("fiction"), p)
            assertEquals(0.5f, score)
        }
    }

    @Nested
    inner class `partial string containment` {

        @Test
        fun `literary fiction matches liked genre fiction`() {
            val p = profile(liked = listOf("fiction"))
            assertTrue(calculateMatchScore(subjectsJson("literary fiction"), p) > 0.5f)
        }

        @Test
        fun `fiction matches liked genre literary fiction`() {
            val p = profile(liked = listOf("literary fiction"))
            assertTrue(calculateMatchScore(subjectsJson("fiction"), p) > 0.5f)
        }
    }
}
